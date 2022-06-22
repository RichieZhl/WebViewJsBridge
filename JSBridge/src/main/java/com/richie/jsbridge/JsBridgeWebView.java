package com.richie.jsbridge;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Patterns;
import com.tencent.smtt.export.external.interfaces.JsPromptResult;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;

import java.util.Map;

/**
 * Created by lylaut on 2021/02/22
 */
public class JsBridgeWebView extends WebView {
    public interface BridgeWebViewHandler {
        void onProgressChanged(int progress);
    }

    private JsBridgeInterface jsBridgeInterface;

    private String mNamespace;
    private String customInjectedJs;

    private BridgeWebViewHandler bridgeWebViewHandler;

    public void setCustomInjectedJs(String customInjectedJs) {
        this.customInjectedJs = customInjectedJs;
    }

    public void setBridgeWebViewHandler(BridgeWebViewHandler bridgeWebViewHandler) {
        this.bridgeWebViewHandler = bridgeWebViewHandler;
    }

    public JsBridgeWebView(Context context, boolean b) {
        super(context, b);
    }

    public JsBridgeWebView(Context context) {
        super(context);
    }

    public JsBridgeWebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public JsBridgeWebView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public JsBridgeWebView(Context context, AttributeSet attributeSet, int i, boolean b) {
        super(context, attributeSet, i, b);
    }

    public JsBridgeWebView(Context context, AttributeSet attributeSet, int i, Map<String, Object> map, boolean b) {
        super(context, attributeSet, i, map, b);
    }

    private void privateSetWebChromeClient() {
        setWebChromeClient(new WebChromeClient() {
            private boolean loaded = false;

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (TextUtils.isEmpty(title)) {
                    return;
                }
                // 部分系统会优先用页面的location作为title，这种情况需要过滤掉
                if (Patterns.WEB_URL.matcher(title).matches()) {
                    return;
                }
                // Hack：过滤掉rexxar页面
                if (title.contains(".html?uri=")) {
                    return;
                }

                // 设置title
                if (view.getContext() instanceof Activity) {
                    if (TextUtils.isEmpty(view.getUrl())) {
                        Uri uri = Uri.parse(view.getUrl());
                        String tempTitle = uri.getQueryParameter("title");
                        if (TextUtils.isEmpty(tempTitle)) {
                            ((Activity) view.getContext()).setTitle(tempTitle);
                            return;
                        }
                    }
                    ((Activity) view.getContext()).setTitle(title);
                }
            }

            @Override
            public boolean onJsAlert(
                    WebView webview,
                    String url,
                    String message,
                    JsResult result) {
                //可以弹框或进行其它处理，但一定要回调result.confirm或者cancel
                //这里要返回true否则内核会进行提示
                return true;
            }
            @Override
            public boolean onJsConfirm(
                    WebView webview,
                    String url,
                    String message,
                    JsResult result) {
                //可以弹框或进行其它处理，但一定要回调result.confirm或者cancel
                return true;
            }
            @Override
            public boolean onJsBeforeUnload(
                    WebView webview,
                    String url,
                    String message,
                    JsResult result) {
                //可以弹框或进行其它处理，但一定要回调result.confirm或者cancel
                return true;
            }
            @Override
            public boolean onJsPrompt(
                    WebView webview,
                    String url,
                    String message,
                    String defaultvalue,
                    JsPromptResult result) {
                //可以弹框或进行其它处理，但一定要回调result.confirm或者cancel，confirm可以将用户输入作为参数
                return true;
            }

            public void onProgressChanged(WebView var1, int var2) {
                if (var2 >= 20 && !loaded) {
                    loaded = true;
                    evaluateJavascript(JsBridgeInterface.JAVASCRIPT_STR + JsBridgeInterface.WVJB_JS, null);
                    evaluateJavascript(JsBridgeInterface.JAVASCRIPT_STR + String.format(JsBridgeInterface.PRO_JS, JsWidgetCollections.getInstance().gen(mNamespace)), null);
                    if (customInjectedJs != null) {
                        evaluateJavascript(JsBridgeInterface.JAVASCRIPT_STR + customInjectedJs, null);
                    }
                } else {
                    loaded = false;
                }

                if (bridgeWebViewHandler != null) {
                    bridgeWebViewHandler.onProgressChanged(var2);
                }
            }
        });
    }

    public void setUp(Activity activity, String customInjectedJs) {
        if (jsBridgeInterface != null) {
            return;
        }
        this.customInjectedJs = customInjectedJs;

        privateSetWebChromeClient();

        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);

        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        JsBridgeInterface jsBridgeInterface = new JsBridgeInterface(this, activity);
        addJavascriptInterface(jsBridgeInterface, "Android");
        this.jsBridgeInterface = jsBridgeInterface;
    }

    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (jsBridgeInterface != null) {
            jsBridgeInterface.registerHandler(handlerName, handler);
        }
    }

    private void callHandler(String handlerName, String data, CallBackFunction responseCallback) {
        if (jsBridgeInterface != null) {
            jsBridgeInterface.callHandler(handlerName, data, responseCallback);
        }
    }
}
