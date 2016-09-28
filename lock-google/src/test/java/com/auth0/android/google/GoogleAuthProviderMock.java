package com.auth0.android.google;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.auth0.android.authentication.AuthenticationAPIClient;

public class GoogleAuthProviderMock extends GoogleAuthProvider {
    GoogleAPI apiHelper;
    GoogleCallback googleCallback;

    public GoogleAuthProviderMock(@NonNull String connectionName, @NonNull String serverClientId, @NonNull AuthenticationAPIClient client, GoogleAPI apiHelper) {
        super(connectionName, serverClientId, client);
        this.apiHelper = apiHelper;
    }

    public GoogleAuthProviderMock(@NonNull String serverClientId, @NonNull AuthenticationAPIClient client, GoogleAPI apiHelper) {
        super(serverClientId, client);
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
