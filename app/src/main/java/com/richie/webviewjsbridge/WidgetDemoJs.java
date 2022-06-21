package com.richie.webviewjsbridge;

import com.richie.jsbridge.JsBaseBridgeWidget;
import com.richie.jsbridge.CallBackFunction;

import java.util.Collections;
import java.util.Map;

/**
 * Created by lylaut on 2022/06/21
 */
public class WidgetDemoJs extends JsBaseBridgeWidget {
    @Override
    public void perform(Map<String, Object> data, CallBackFunction function) {
        System.out.println(data);
        successNotRemoveResponseId(Collections.singletonMap("a", true), function);
        success("asffa", function);
    }
}
