package com.auth0.android.google;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.auth0.android.authentication.AuthenticationAPIClient;

public class GoogleAuthProviderMock extends GoogleAuthProvider {
    GoogleAPI google;
    GoogleCallback googleCallback;
    boolean rememberLastLogin;

    public GoogleAuthProviderMock(@NonNull String connectionName, @NonNull String serverClientId, @NonNull AuthenticationAPIClient client, @NonNull GoogleAPI google) {
        super(connectionName, serverClientId, client);
        this.google = google;
    }

    public GoogleAuthProviderMock(@NonNull String serverClientId, @NonNull AuthenticationAPIClient client, @NonNull GoogleAPI google) {
        super(serverClientId, client);
        this.google = google;
    }

    @Override
    GoogleAPI createGoogleAPI(Activity activity, boolean rememberLastLogin) {
        createTokenListener();
        this.rememberLastLogin = rememberLastLogin;
        return google;
    }

    @Override
    GoogleCallback createTokenListener() {
        googleCallback = super.createTokenListener();
        return googleCallback;
    }

    boolean willLogoutBeforeLogin() {
        return !rememberLastLogin;
    }
}
