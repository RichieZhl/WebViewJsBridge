package com.richie.jsbridge;

import android.app.Activity;

public interface BridgeHandler {
    void handler(Activity activity, String data, CallBackFunction function);
}
