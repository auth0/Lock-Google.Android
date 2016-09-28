package com.auth0.android.google;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.auth0.android.authentication.AuthenticationAPIClient;

public class GoogleAuthProviderMock extends GoogleAuthProvider {
    GoogleAPI apiHelper;
    GoogleCallback googleCallback;

    public GoogleAuthProviderMock(@NonNull AuthenticationAPIClient client, @NonNull String serverClientId, GoogleAPI apiHelper) {
        super(client, serverClientId);
        this.apiHelper = apiHelper;
    }

    @Override
    GoogleAPI createAPIHelper(Activity activity) {
        createTokenListener();
        return apiHelper;
    }

    @Override
    GoogleCallback createTokenListener() {
        googleCallback = super.createTokenListener();
        return googleCallback;
    }
}
