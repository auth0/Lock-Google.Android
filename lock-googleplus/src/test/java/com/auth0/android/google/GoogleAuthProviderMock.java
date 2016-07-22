package com.auth0.android.google;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.auth0.android.authentication.AuthenticationAPIClient;

public class GoogleAuthProviderMock extends GoogleAuthProvider {
    GoogleAPIHelper apiHelper;
    TokenListener tokenListener;

    public GoogleAuthProviderMock(@NonNull AuthenticationAPIClient client, @NonNull String serverClientId, GoogleAPIHelper apiHelper) {
        super(client, serverClientId);
        this.apiHelper = apiHelper;
    }

    @Override
    GoogleAPIHelper createAPIHelper(Activity activity) {
        createTokenListener();
        return apiHelper;
    }

    @Override
    TokenListener createTokenListener() {
        tokenListener = super.createTokenListener();
        return tokenListener;
    }
}
