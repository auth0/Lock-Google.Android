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
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.provider.AuthProvider;
import com.auth0.android.result.Credentials;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Native Google Sign In implementation of the Auth0 AuthProvider.
 */
public class GoogleAuthProvider extends AuthProvider {

    private static final String TAG = GoogleAuthProvider.class.getSimpleName();
    /**
     * Used internally to dispatch error resolver dialogs.
     */
    static final int REQUEST_RESOLVE_ERROR = 1001;

    private final AuthenticationAPIClient auth0;
    private final String serverClientId;
    private final String connectionName;
    private Scope[] scopes;
    private GoogleAPI google;
    private String[] androidPermissions;
    private boolean rememberLastLogin;

    /**
     * Creates Google Auth provider for default Google connection 'google-oauth2'.
     *
     * @param client         an Auth0 AuthenticationAPIClient instance
     * @param serverClientId the OAuth 2.0 server client id obtained when creating a new credential on the Google API's console.
     */
    public GoogleAuthProvider(@NonNull String serverClientId, @NonNull AuthenticationAPIClient client) {
        this("google-oauth2", serverClientId, client);
    }

    /**
     * Creates Google Auth provider for a specific Google connection.
     *
     * @param connectionName of Auth0's connection used to Authenticate after user is authenticated with Google. Must be a Google connection
     * @param serverClientId the OAuth 2.0 server client id obtained when creating a new credential on the Google API's console.
     * @param client         an Auth0 AuthenticationAPIClient instance
     */
    public GoogleAuthProvider(@NonNull String connectionName, @NonNull String serverClientId, @NonNull AuthenticationAPIClient client) {
        this.auth0 = client;
        this.serverClientId = serverClientId;
        this.scopes = new Scope[]{new Scope(Scopes.PLUS_LOGIN)};
        this.connectionName = connectionName;
        this.androidPermissions = new String[0];
        this.rememberLastLogin = true;
    }

    /**
     * Sets the required Android Manifest Permissions for this provider. By default, no permissions are needed for the Auth API to work.
     * Use it only if the scope you need requires that the user approves a certain Android Permission first.
     *
     * @param androidPermissions the permissions to request to the user before asking for the account.
     */
    public void setRequiredPermissions(@NonNull String[] androidPermissions) {
        this.androidPermissions = androidPermissions;
    }
    
    /**
     * Whether it should remember the last account used to log in or it should ask the user to input his credentials.
     * By default it's true, meaning it will not ask for the user account if he's already logged in.
     *
     * @param rememberLastLogin flag to remember last Google login
     */
    public void rememberLastLogin(boolean rememberLastLogin) {
        this.rememberLastLogin = rememberLastLogin;
    }

    /**
     * Change the scopes to request on the user login. Use any of the scopes defined in the com.google.android.gms.common.Scopes class. Must be called before start().
     * The scope Scopes.PLUG_LOGIN is requested by default.
     *
     * @param scope the scope to add to the request
     */
    public void setScopes(@NonNull Scope... scope) {
        this.scopes = scope;
    }

    @Override
    protected void requestAuth(Activity activity, int requestCode) {
        if (google != null) {
            google.disconnect();
        }
        google = createGoogleAPI(activity, rememberLastLogin);
        final int availabilityStatus = google.isGooglePlayServicesAvailable();
        if (availabilityStatus == ConnectionResult.SUCCESS) {
            google.connectAndRequestGoogleAccount(requestCode, REQUEST_RESOLVE_ERROR);
            return;
        }

        Log.w(TAG, "Google services availability failed with status " + availabilityStatus);
        getSafeCallback().onFailure(google.getErrorDialog(availabilityStatus, REQUEST_RESOLVE_ERROR));
    }

    @Override
    public boolean authorize(int requestCode, int resultCode, @Nullable Intent intent) {
        return google.parseSignInResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean authorize(@Nullable Intent intent) {
        Log.w(TAG, "Called authorize in a native Google auth provider");
        return false;
    }

    @Override
    public String[] getRequiredAndroidPermissions() {
        return androidPermissions;
    }

    @Override
    public void stop() {
        super.stop();
        clearSession();
    }

    @Override
    public void clearSession() {
        super.clearSession();
        if (google != null) {
            google.logoutAndClearState();
            google = null;
        }
    }

    private void requestAuth0Token(String token) {
        this.auth0
                .loginWithOAuthAccessToken(token, connectionName)
                .start(new AuthenticationCallback<Credentials>() {
                    @Override
                    public void onSuccess(Credentials credentials) {
                        getSafeCallback().onSuccess(credentials);
                    }

                    @Override
                    public void onFailure(AuthenticationException error) {
                        getSafeCallback().onFailure(error);
                    }
                });
    }

    Scope[] getScopes() {
        return scopes;
    }

    String getConnection() {
        return connectionName;
    }

    GoogleAPI createGoogleAPI(Activity activity, boolean rememberLastLogin) {
        final GoogleAPI googleAPI = new GoogleAPI(activity, serverClientId, scopes, createTokenListener());
        googleAPI.rememberLastLogin(rememberLastLogin);
        return googleAPI;
    }

    GoogleCallback createTokenListener() {
        return new GoogleCallback() {
            @Override
            public void onSuccess(GoogleSignInAccount account) {
                final Set<Scope> grantedScopes = account.getGrantedScopes();
                final Set<Scope> requestedScopes = new HashSet<>(Arrays.asList(scopes));
                if (grantedScopes.containsAll(requestedScopes)) {
                    requestAuth0Token(account.getIdToken());
                } else {
                    final Set<Scope> notGrantedScopes = new HashSet<>(requestedScopes);
                    notGrantedScopes.removeAll(grantedScopes);
                    Log.w(TAG, "Some scopes were not granted: " + notGrantedScopes.toString());
                    getSafeCallback().onFailure(new AuthenticationException("Some of the requested permissions were not granted."));
                }
            }

            @Override
            public void onCancel() {
                Log.w(TAG, "User cancelled the log in dialog");
                getSafeCallback().onFailure(new AuthenticationException("You need to authorize the application"));
            }

            @Override
            public void onError(Dialog errorDialog) {
                getSafeCallback().onFailure(errorDialog);
            }
        };
    }

    private AuthCallback getSafeCallback() {
        final AuthCallback callback = getCallback();
        return callback != null ? callback : new AuthCallback() {
            @Override
            public void onFailure(@NonNull Dialog dialog) {
                Log.w(TAG, "Using callback when no auth session was running");
            }

            @Override
            public void onFailure(AuthenticationException exception) {
                Log.w(TAG, "Using callback when no auth session was running");
            }

            @Override
            public void onSuccess(@NonNull Credentials credentials) {
                Log.w(TAG, "Using callback when no auth session was running");
            }
        };
    }
}
