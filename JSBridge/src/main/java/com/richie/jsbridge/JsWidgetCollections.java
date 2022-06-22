package com.richie.jsbridge;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lylaut on 2022/06/21
 */
public class JsWidgetCollections {
    private static class Instance {
        private final static JsWidgetCollections ins = new JsWidgetCollections();
    }

    public static JsWidgetCollections getInstance() {
        return Instance.ins;
    }

    private final Map<String, Class<?>> widgetCollectionMaps = new HashMap<>();

    public void registerWidget(String widgetName, Class<?> impClass) {
        widgetCollectionMaps.put(widgetName, impClass);
    }

    public void configWidgets(JsBridgeWebView webView) {
        for (Map.Entry<String, Class<?>> entry : widgetCollectionMaps.entrySet()) {
            try {
                webView.registerHandler(entry.getKey(), (BridgeHandler) entry.getValue().newInstance());
            } catch (Exception e) {
                //
            }
        }
    }

    public String gen(String namespace) {
        StringBuilder stringBuilder = new StringBuilder(String.format("if (window['%s'] == null) { window['%s'] = {}; }\n", namespace, namespace));
        for (Map.Entry<String, Class<?>> entry : widgetCollectionMaps.entrySet()) {
            String[] strings = entry.getKey().split("\\.");
            if (strings.length == 1) {
                stringBuilder.append("generator.apply(window['").append(namespace).append("'], ['").append(entry.getKey()).append("']);\n");
            } else {
                StringBuilder cmdBuilder = new StringBuilder("window['" + namespace + "']");
                for (int i = 0; i < strings.length - 1; ++i) {
                    cmdBuilder.append("['").append(strings[i]).append("']");
                }
                String cmd = cmdBuilder.toString();
                stringBuilder.append("if (").append(cmd).append(" == null) { ").append(cmd).append("= {}; }\n");
                stringBuilder.append("generator.apply(").append(cmd).append(", ['").append(strings[strings.length - 1]).append("']);\n");
            }
        }
        return stringBuilder.toString();
    }
}
