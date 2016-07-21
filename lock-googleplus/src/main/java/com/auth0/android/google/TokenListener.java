package com.auth0.android.google;

import android.app.Dialog;

public interface TokenListener {

    void onTokenReceived(String token);

    void onErrorReceived(Dialog errorDialog);
}
