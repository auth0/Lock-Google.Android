package com.auth0.android.google;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.AuthenticationCallback;
import com.auth0.android.provider.AuthProvider;
import com.auth0.android.result.Credentials;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Scope;


public class GoogleAuthProvider extends AuthProvider {

    private static final String TAG = GoogleAuthProvider.class.getSimpleName();
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private final AuthenticationAPIClient auth0Client;
    private final String serverClientId;

    private Scope[] scopes;
    private GoogleAPIHelper apiHelper;

    public GoogleAuthProvider(@NonNull AuthenticationAPIClient client, @NonNull String serverClientId) {
        this.auth0Client = client;
        this.serverClientId = serverClientId;
        this.scopes = new Scope[]{};
    }

    /**
     * Change the scopes to request on the user login. Use any of the scopes defined in the com.google.android.gms.common.Scopes class. Must be called before requestAuth().
     * The scope Scopes.PLUG_LOGIN is requested by default.
     *
     * @param scope the scope to add to the request
     */
    public void setScopes(@NonNull Scope... scope) {
        this.scopes = scope;
    }

    Scope[] getScopes() {
        return scopes;
    }

    @Override
    protected void requestAuth(Activity activity, int requestCode) {
        apiHelper = new GoogleAPIHelper(activity, serverClientId, scopes, tokenListener);
        final int availabilityStatus = apiHelper.isGooglePlayServicesAvailable();
        if (availabilityStatus == ConnectionResult.SUCCESS) {
            apiHelper.connectAndRequestGoogleAccount(requestCode, REQUEST_RESOLVE_ERROR);
            return;
        }

        Log.w(TAG, "Google services availability failed with status " + availabilityStatus);
        callback.onFailure(apiHelper.getErrorDialog(availabilityStatus, REQUEST_RESOLVE_ERROR));
    }

    @Override
    public boolean authorize(int requestCode, int resultCode, @Nullable Intent intent) {
        return apiHelper.parseSignInResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean authorize(@Nullable Intent intent) {
        //Unused
        return false;
    }

    @Override
    public String[] getRequiredAndroidPermissions() {
        return new String[0];
    }

    @Override
    public void stop() {
        super.stop();
        clearSession();
    }

    @Override
    public void clearSession() {
        super.clearSession();
        if (apiHelper != null) {
            apiHelper.logoutAndClearState();
            apiHelper = null;
        }
    }

    private void requestAuth0Token(String token) {
        auth0Client.loginWithOAuthAccessToken(token, "google-oauth2")
                .start(new AuthenticationCallback<Credentials>() {
                    @Override
                    public void onSuccess(Credentials credentials) {
                        callback.onSuccess(credentials);
                    }

                    @Override
                    public void onFailure(AuthenticationException error) {
                        callback.onFailure(R.string.com_auth0_google_authentication_failed_title, R.string.com_auth0_google_authentication_failed_message, error);
                    }
                });
    }

    private final TokenListener tokenListener = new TokenListener() {
        @Override
        public void onTokenReceived(String token) {
            requestAuth0Token(token);
        }

        @Override
        public void onErrorReceived(Dialog errorDialog) {
            callback.onFailure(errorDialog);
        }
    };
}
