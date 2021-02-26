package com.richie.jsbridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import com.tencent.smtt.sdk.WebView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("JavascriptInterface")
public class JsBridgeInterface {
    private final static String CALLBACK_ID_FORMAT = "JAVA_CB_%s";
    private final static String UNDERLINE_STR = "_";
    private final static String JS_HANDLE_MESSAGE_FROM_JAVA = "javascript:JsBridge._handleMessageFromNative('%s');";
    private final static String JAVASCRIPT_STR = "javascript:";

    private final Map<String, CallBackFunction> responseCallbacks = new HashMap<>();

    private final Map<String, BridgeHandler> messageHandlers = new HashMap<>();

    private long uniqueId = 1;

    private final WeakReference<WebView> webView;
    private final WeakReference<Activity> mActivity;

    public JsBridgeInterface(WebView webView, Activity activity) {
        this.webView = new WeakReference<>(webView);
        this.mActivity = new WeakReference<>(activity);
    }

    @JavascriptInterface
    public void postMessage(String o) {
        Message m = Message.toObject(o);
        String responseId = m.getResponseId();
        // 是否是response
        if (!TextUtils.isEmpty(responseId)) {
            CallBackFunction function = responseCallbacks.get(responseId);
            String responseData = m.getResponseData();
            function.onCallBack(responseData);
            responseCallbacks.remove(responseId);
        } else {
            CallBackFunction responseFunction = null;
            // if had callbackId
            final String callbackId = m.getCallbackId();
            if (!TextUtils.isEmpty(callbackId)) {
                final WebView wk = this.webView.get();
                if (wk == null) {
                    return;
                }
                responseFunction = data -> {
                    Message responseMsg = new Message();
                    responseMsg.setResponseId(callbackId);
                    responseMsg.setResponseData(data);

                    sendData(wk, responseMsg);
                };
            } else {
                responseFunction = new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                        // do nothing
                    }
                };
            }
            BridgeHandler handler;
            if (!TextUtils.isEmpty(m.getHandlerName())) {
                handler = messageHandlers.get(m.getHandlerName());
            } else {
                handler = (data, function) -> {

                };
            }
            if (handler != null){
                handler.handler(m.getData(), responseFunction);
            }
        }
    }

    private void sendData(final WebView webView, Message message) {
        String messageJson = message.toJson();
        //escape special characters for json string
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            webView.evaluateJavascript(javascriptCommand, null);
        } else {
            Activity activity = mActivity.get();
            if (activity == null) return;
            activity.runOnUiThread(() -> webView.evaluateJavascript(javascriptCommand, null));
        }
    }

    public void registerHandler(String handlerName, BridgeHandler handler) {
        messageHandlers.put(handlerName, handler);
    }

    public void callHandler(String handlerName, String data, CallBackFunction responseCallback) {
        WebView webView = this.webView.get();
        if (webView == null) {
            return;
        }

        Message m = new Message();
        if (!TextUtils.isEmpty(data)) {
            m.setData(data);
        }
        if (responseCallback != null) {
            String callbackStr = String.format(CALLBACK_ID_FORMAT, ++uniqueId + (UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            responseCallbacks.put(callbackStr, responseCallback);
            m.setCallbackId(callbackStr);
        }
        if (!TextUtils.isEmpty(handlerName)) {
            m.setHandlerName(handlerName);
        }

        sendData(webView, m);
    }

    public static final String WVJB_JS =
            "(function() {\n" +
                    "\tif (window.JsBridge) {\n" +
                    "\t\treturn;\n" +
                    "\t}\n" +
                    "\n" +
                    "\twindow.JsBridge = {\n" +
                    "\t\tregisterHandler: registerHandler,\n" +
                    "\t\tcallHandler: callHandler,\n" +
                    "\t\t_handleMessageFromNative: _handleMessageFromNative\n" +
                    "\t};\n" +
                    "\n" +
                    "\tvar messageHandlers = {};\n" +
                    "\t\n" +
                    "\tvar responseCallbacks = {};\n" +
                    "\tvar uniqueId = 1;\n" +
                    "\n" +
                    "\tfunction registerHandler(handlerName, handler) {\n" +
                    "\t\tmessageHandlers[handlerName] = handler;\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tfunction callHandler(handlerName, data, responseCallback) {\n" +
                    "\t\tif (arguments.length === 2 && typeof data == 'function') {\n" +
                    "\t\t\tresponseCallback = data;\n" +
                    "\t\t\tdata = null;\n" +
                    "\t\t}\n" +
                    "\t\t_doSend({ handlerName:handlerName, data:data }, responseCallback);\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tfunction _doSend(message, responseCallback) {\n" +
                    "\t\tif (responseCallback) {\n" +
                    "\t\t\tvar callbackId = 'cb_'+(uniqueId++)+'_'+new Date().getTime();\n" +
                    "\t\t\tresponseCallbacks[callbackId] = responseCallback;\n" +
                    "\t\t\tmessage['callbackId'] = callbackId;\n" +
                    "\t\t}\n" +
                    "\t\twindow.Android.postMessage(JSON.stringify(message));\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tfunction _handleMessageFromNative(messageJSON) {\n" +
                    "\t\tvar message = JSON.parse(messageJSON);\n" +
                    "\t\tvar responseCallback;\n" +
                    "\t\t\n" +
                    "\t\tif (message.responseId) {\n" +
                    "\t\t\tresponseCallback = responseCallbacks[message.responseId];\n" +
                    "\t\t\tif (!responseCallback) {\n" +
                    "\t\t\t\treturn;\n" +
                    "\t\t\t}\n" +
                    "\t\t\tconst isDelete = message.responseData == null || (message.responseData.JsBridgeIsDelete == null || message.responseData.JsBridgeIsDelete);\n" +
                    "\t\t\tif (message.responseData != null) {\n" +
                    "\t\t\t\tdelete message.responseData.JsBridgeIsDelete;\n" +
                    "\t\t\t}\n" +
                    "\n" +
                    "\t\t\tresponseCallback(message.responseData);\n" +
                    "\t\t\tif (isDelete) {\n" +
                    "\t\t\t\tdelete responseCallbacks[message.responseId];\n" +
                    "\t\t\t}\n" +
                    "\t\t} else {\n" +
                    "\t\t\tif (message.callbackId) {\n" +
                    "\t\t\t\tvar callbackResponseId = message.callbackId;\n" +
                    "\t\t\t\tresponseCallback = function(responseData) {\n" +
                    "\t\t\t\t\t_doSend({ handlerName:message.handlerName, responseId:callbackResponseId, responseData:responseData });\n" +
                    "\t\t\t\t};\n" +
                    "\t\t\t}\n" +
                    "\t\t\t\n" +
                    "\t\t\tvar handler = messageHandlers[message.handlerName];\n" +
                    "\t\t\tif (!handler) {\n" +
                    "\t\t\t\tconsole.log(\"JsBridge: WARNING: no handler for message from ObjC:\", message);\n" +
                    "\t\t\t} else {\n" +
                    "\t\t\t\thandler(message.data, responseCallback);\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t}\n" +
                    "})()";
}
