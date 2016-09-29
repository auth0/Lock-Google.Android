package com.auth0.android.google;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.provider.AuthHandler;
import com.auth0.android.provider.AuthProvider;

/**
 * AuthHandler class to handle the Authentication flow using the Google Android SDK.
 * By default, all the authentication requests to this provider with the strategy "google-oauth2" will be handled.
 */
public class GoogleAuthHandler implements AuthHandler {

    private final GoogleAuthProvider provider;

    /**
     * Creates a new instance of this AuthHandler with the given Auth0 AuthenticationAPIClient and the Google Server Client ID.
     * For more information about how to setup the Google Project and obtain the Server Client ID, check the README.md file.
     *
     * @param apiClient      Auth0 api client to perform the authentication request with.
     * @param serverClientId the Google Server Client ID to begin the id_token request with.
     */
    public GoogleAuthHandler(@NonNull AuthenticationAPIClient apiClient, @NonNull String serverClientId) {
        this(new GoogleAuthProvider(serverClientId, apiClient));
    }

    /**
     * Creates a new instance of this AuthHandler with the given GoogleAuthProvider.
     *
     * @param provider provider to return in the providerFor() requests.
     */
    public GoogleAuthHandler(@NonNull GoogleAuthProvider provider) {
        this.provider = provider;
    }

    @Nullable
    @Override
    public AuthProvider providerFor(@Nullable String strategy, @NonNull String connection) {
        if ("google-oauth2".equals(strategy)) {
            return provider;
        }
        return null;
    }
}
