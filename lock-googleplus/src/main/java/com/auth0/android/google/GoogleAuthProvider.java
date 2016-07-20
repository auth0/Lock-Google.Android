package com.auth0.android.google;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.AuthenticationCallback;
import com.auth0.android.provider.AuthProvider;
import com.auth0.android.result.Credentials;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;


public class GoogleAuthProvider extends AuthProvider implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = GoogleAuthProvider.class.getSimpleName();
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private final AuthenticationAPIClient auth0Client;

    private boolean resolvingError = false;
    private int signInRequestCode;
    private Scope[] scopes;
    private GoogleApiClient googleClient;
    private Activity activity;

    public GoogleAuthProvider(AuthenticationAPIClient client) {
        this.auth0Client = client;
        this.scopes = new Scope[]{};
    }

    /**
     * Change the scopes to request on the user login. Use any of the scopes defined in the com.google.android.gms.common.Scopes class.
     * The scope Scopes.PLUG_LOGIN is requested by default.
     *
     * @param scope the scope to add to the request
     */
    public void setScopes(Scope... scope) {
        this.scopes = scope;
    }

    @Override
    protected void requestAuth(Activity activity, int requestCode) {
        final int availabilityStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
        if (availabilityStatus == ConnectionResult.SUCCESS) {
            this.googleClient = createGoogleAPIClient(activity);
            this.activity = activity;
            signInRequestCode = requestCode;
            connectAndGetGoogleAccount();
            return;
        }

        Log.w(TAG, "Google services availability failed with status " + availabilityStatus);
        callback.onFailure(GoogleApiAvailability.getInstance().getErrorDialog(activity, availabilityStatus, 0));
    }

    @Override
    public boolean authorize(int requestCode, int resultCode, @Nullable Intent intent) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            resolvingError = false;
            if (resultCode == Activity.RESULT_OK) {
                connectAndGetGoogleAccount();
            }
            return true;
        } else if (requestCode == signInRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);
                if (result.isSuccess()) {
                    requestAuth0Token(result.getSignInAccount().getIdToken());
                }
            }
            return true;
        }
        return false;
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
        try {
            Auth.GoogleSignInApi.signOut(googleClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (!status.isSuccess()) {
                        Log.w(TAG, "Couldn't clear account and credentials");
                    }
                }
            });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to clear Google Plus Session", e);
        } finally {
            if (googleClient != null && googleClient.isConnected()) {
                googleClient.disconnect();
            }
            activity = null;
            googleClient = null;
        }
    }

    private void showErrorDialog(int errorCode) {
        final Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(activity, errorCode, REQUEST_RESOLVE_ERROR);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                resolvingError = false;
            }
        });
        callback.onFailure(dialog);
    }

    private GoogleApiClient createGoogleAPIClient(Activity activity) {
        final GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
                .requestIdToken("680447222598-gff97qkjdde6v8q3jbflc46lnsr96g6d.apps.googleusercontent.com")
                .requestScopes(new Scope(Scopes.PLUS_LOGIN), scopes)
                .build();
        final GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity, this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso);

        return builder.build();
    }

    private void connectAndGetGoogleAccount() {
        if (googleClient.isConnected()) {
            getGoogleAccount();
        } else if (!googleClient.isConnecting()) {
            googleClient.connect();
        }
    }

    private void getGoogleAccount() {
        final Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleClient);
        activity.startActivityForResult(signInIntent, signInRequestCode);
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

    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        connectAndGetGoogleAccount();
    }

    @Override
    public void onConnectionSuspended(int code) {
        Log.v(TAG, "Connection suspended with code: " + code);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (resolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                resolvingError = true;
                connectionResult.startResolutionForResult(activity, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                googleClient.connect();
            }
        } else {
            showErrorDialog(connectionResult.getErrorCode());
            resolvingError = true;
        }
    }
}
