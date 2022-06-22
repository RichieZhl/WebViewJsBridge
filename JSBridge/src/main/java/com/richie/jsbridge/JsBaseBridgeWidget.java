package com.richie.jsbridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lylaut on 2022/06/21
 */
public abstract class JsBaseBridgeWidget implements BridgeHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public JsBaseBridgeWidget() {

    }

    public abstract void perform(Activity activity, Map<String, Object> data, CallBackFunction function);

    @Override
    public void handler(Activity activity, String data, CallBackFunction function) {
        Map<String, Object> dataMap = null;
        try {
            if (data != null && !data.isEmpty()) {
                dataMap = gson.fromJson(data, Map.class);
            }
        } catch (Exception e) {
            //
        }

        perform(activity, dataMap, function);
    }

    private Object handleResData(Object data) {
        if (data == null) {
            return "";
        } else if (data instanceof String || data instanceof Number || data instanceof Boolean) {
            return data;
        } else {
            String s;
            try {
                s = gson.toJson(data);
            } catch (Exception e) {
                s = "";
            }
            return s;
        }
    }

    private Map<String, Object> createSuccessResMap(Object data, Boolean isDelete) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", 1);
        map.put("data", handleResData(data));
        map.put("JsBridgeIsDelete", isDelete);
        return map;
    }

    @SuppressLint("DefaultLocale")
    private Map<String, Object> createFailResMap(int errorCode, String message, Boolean isDelete) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", 1);
        map.put("data", String.format("{\"errorCode\": %d, \"message\": \"%s\"}", errorCode, message));
        map.put("JsBridgeIsDelete", isDelete);
        return map;
    }

    public void success(Object data, CallBackFunction function) {
        Map<String, Object> map = createSuccessResMap(data, true);
        try {
            String s = gson.toJson(map);
            function.onCallBack(s);
        } catch (Exception e) {
            //
        }
    }

    public void successNotRemoveResponseId(Object data, CallBackFunction function) {
        Map<String, Object> map = createSuccessResMap(data, false);
        try {
            String s = gson.toJson(map);
            function.onCallBack(s);
        } catch (Exception e) {
            //
        }
    }

    public void fail(int errorCode, String message, CallBackFunction function) {
        Map<String, Object> map = createFailResMap(errorCode, message, true);
        try {
            String s = gson.toJson(map);
            function.onCallBack(s);
        } catch (Exception e) {
            //
        }
    }

    public void failNotRemoveResponseId(int errorCode, String message, CallBackFunction function) {
        Map<String, Object> map = createFailResMap(errorCode, message, false);
        try {
            String s = gson.toJson(map);
            function.onCallBack(s);
        } catch (Exception e) {
            //
        }
    }
}
