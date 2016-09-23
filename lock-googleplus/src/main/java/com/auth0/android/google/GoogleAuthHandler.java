package com.auth0.android.google;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.provider.AuthHandler;
import com.auth0.android.provider.AuthProvider;

public class GoogleAuthHandler implements AuthHandler {

    private final GoogleAuthProvider provider;

    public GoogleAuthHandler(@NonNull AuthenticationAPIClient apiClient, @NonNull String serverClientId) {
        this(new GoogleAuthProvider(apiClient, serverClientId));
    }

    public GoogleAuthHandler(@NonNull GoogleAuthProvider provider) {
        this.provider = provider;
    }

    @Nullable
    @Override
    public AuthProvider providerFor(@NonNull String strategy, @NonNull String connection) {
        if ("google-oauth2".equals(connection)) {
            return provider;
        }
        return null;
    }
}
