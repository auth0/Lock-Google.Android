package com.auth0.android.google;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.auth0.android.authentication.AuthenticationAPIClient;

public class GoogleAuthProviderMock extends GoogleAuthProvider {
    GoogleAPI google;
    GoogleCallback googleCallback;
    boolean logoutBeforeLogin;

    public GoogleAuthProviderMock(@NonNull String connectionName, @NonNull String serverClientId, @NonNull AuthenticationAPIClient client, GoogleAPI google) {
        super(connectionName, serverClientId, client);
        this.google = google;
    }

    public GoogleAuthProviderMock(@NonNull String serverClientId, @NonNull AuthenticationAPIClient client, GoogleAPI google) {
        super(serverClientId, client);
        this.google = google;
    }

    @Override
    GoogleAPI createGoogleAPI(Activity activity, boolean forceRequestAccount) {
        createTokenListener();
        this.logoutBeforeLogin = forceRequestAccount;
        return google;
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
