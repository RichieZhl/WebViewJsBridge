package com.richie.webviewjsbridge;

import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.richie.jsbridge.BridgeHandler;
import com.richie.jsbridge.CallBackFunction;
import com.richie.jsbridge.JsBridgeWebView;
import com.richie.jsbridge.JsWidgetCollections;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在调用TBS初始化、创建WebView之前进行如下配置
//        HashMap<String, Object> map = new HashMap<>();
//        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
//        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
//        QbSdk.initTbsSettings(map);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JsBridgeWebView webView = findViewById(R.id.webview);
        webView.setUp(this, "eren", null);
        JsWidgetCollections.getInstance().registerWidget("navigateTo", WidgetDemoJs.class);
        JsWidgetCollections.getInstance().registerWidget("a.navigateTo", WidgetDemoJs.class);
        JsWidgetCollections.getInstance().configWidgets(webView);
        webView.setBridgeWebViewHandler(new JsBridgeWebView.BridgeWebViewHandler() {
            @Override
            public void onProgressChanged(int progress) {
                System.out.println(progress);
            }
        });
        webView.loadUrl("https://www.google.com");
    }
}
