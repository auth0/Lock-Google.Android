package com.auth0.android.google;

import android.app.Dialog;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

interface GoogleCallback {

    void onSuccess(GoogleSignInAccount account);

    void onCancel();

    void onError(Dialog errorDialog);
}
