package com.richie.webviewjsbridge;

import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.richie.jsbridge.BridgeHandler;
import com.richie.jsbridge.CallBackFunction;
import com.richie.jsbridge.JsBridgeWebView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在调用TBS初始化、创建WebView之前进行如下配置
//        HashMap<String, Object> map = new HashMap<>();
//        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
//        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
//        QbSdk.initTbsSettings(map);
        String customJs = "(function() {\n" +
                "    function generator(method) {\n" +
                "        let splitStrs = method.split('.');\n" +
                "        let thisMtd = splitStrs[splitStrs.length - 1];\n" +
                "        this[thisMtd] = function responseNoop(param) {\n" +
                "            var p = param || {};\n" +
                "            var successCallback = function (res) {\n" +
                "                console.log('默认成功回调', method, res);\n" +
                "            };\n" +
                "            var failCallback = function (err) {\n" +
                "                console.log('默认失败回调', method, err);\n" +
                "            };\n" +
                "            if (p.success) {\n" +
                "                successCallback = p.success;\n" +
                "                delete p.success;\n" +
                "            }\n" +
                "            if (p.fail) {\n" +
                "                failCallback = p.fail;\n" +
                "                delete p.fail;\n" +
                "            }\n" +
                "        \n" +
                "            //统一回调处理\n" +
                "            var callback = function (response) {\n" +
                "                console.log('response:' + response);\n" +
                "                const data = JSON.parse(response) || {};\n" +
                "                console.log('data:' + data);\n" +
                "                const status = data.status;\n" +
                "                console.log('status:' + status);\n" +
                "                const result = data.data;\n" +
                "                console.log('result:' + result);\n" +
                "                if (status >= 0) {\n" +
                "                    successCallback && successCallback.call(null, result);\n" +
                "                } else {\n" +
                "                    failCallback && failCallback.call(null, result);\n" +
                "                }\n" +
                "            };\n" +
                "        \n" +
                "            window.JsBridge.callHandler(method, p, callback);\n" +
                "        }\n" +
                "    }\n" +
                "    window.eren = {};\n" +
                "    const array = ['navigateTo',\n" +
                "                   'redirectTo',\n" +
                "                   'navigateBack',\n" +
                "                   'navigateToMiniProgram',\n" +
                "                   'scanCode',\n" +
                "                   'uploadImages',\n" +
                "                   'uploadVideo',\n" +
                "                   'goodsUrlImport',\n" +
                "                   'configNavigationBar',\n" +
                "                   'hardwareBLEGetList',\n" +
                "                   'hardwareBLEConnectDevice',\n" +
                "                   'hardwareBLEDisconnectDevice',\n" +
                "                   'hardwareBLERemoveDevice',\n" +
                "                   'saveImageToPhotosAlbum'\n" +
                "                   ];\n" +
                "    for (let key in array) {\n" +
                "        generator.apply(window.eren, [array[key]]);\n" +
                "    }\n" +
                "})()";

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JsBridgeWebView webView = findViewById(R.id.webview);
        webView.setUp(this, customJs);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        webView.setBridgeWebViewHandler(new JsBridgeWebView.BridgeWebViewHandler() {
            @Override
            public void onProgressChanged(int progress) {
                System.out.println(progress);
            }
        });
        webView.registerHandler("navigateTo", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                function.onCallBack("{\"status\": 1, \"data\": \"------\"}");
            }
        });
        webView.loadUrl("https://www.google.com");
    }
}
