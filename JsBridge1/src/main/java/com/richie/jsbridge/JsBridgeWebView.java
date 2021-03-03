package com.richie.jsbridge;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Patterns;
import android.webkit.*;

/**
 * Created by lylaut on 2021/02/22
 */
public class JsBridgeWebView extends WebView {
    public interface BridgeWebViewHandler {
        void onProgressChanged(int progress);
    }

    private JsBridgeInterface jsBridgeInterface;

    private String customInjectedJs;

    private BridgeWebViewHandler bridgeWebViewHandler;

    public void setCustomInjectedJs(String customInjectedJs) {
        this.customInjectedJs = customInjectedJs;
    }

    public void setBridgeWebViewHandler(BridgeWebViewHandler bridgeWebViewHandler) {
        this.bridgeWebViewHandler = bridgeWebViewHandler;
    }

    public JsBridgeWebView(Context context) {
        super(context);
    }

    public JsBridgeWebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public JsBridgeWebView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }

    public JsBridgeWebView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
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
                    evaluateJavascript(JsBridgeInterface.WVJB_JS, null);
                    if (customInjectedJs != null) {
                        evaluateJavascript(customInjectedJs, null);
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
