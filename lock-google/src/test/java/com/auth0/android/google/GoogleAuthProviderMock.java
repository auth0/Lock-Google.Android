package com.auth0.android.google;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.auth0.android.authentication.AuthenticationAPIClient;

public class GoogleAuthProviderMock extends GoogleAuthProvider {
    GoogleAPI apiHelper;
    GoogleCallback googleCallback;
    boolean logoutBeforeLogin;

    public GoogleAuthProviderMock(@NonNull String connectionName, @NonNull String serverClientId, @NonNull AuthenticationAPIClient client, GoogleAPI apiHelper) {
        super(connectionName, serverClientId, client);
        this.apiHelper = apiHelper;
    }

    public GoogleAuthProviderMock(@NonNull String serverClientId, @NonNull AuthenticationAPIClient client, GoogleAPI apiHelper) {
        super(serverClientId, client);
        this.apiHelper = apiHelper;
    }

    @Override
    GoogleAPI createGoogleAPI(Activity activity, boolean forceRequestAccount) {
        createTokenListener();
        this.logoutBeforeLogin = forceRequestAccount;
        return apiHelper;
    }

    @Override
    GoogleCallback createTokenListener() {
        googleCallback = super.createTokenListener();
        return googleCallback;
    }

    boolean willLogoutBeforeLogin() {
        return logoutBeforeLogin;
    }
}
