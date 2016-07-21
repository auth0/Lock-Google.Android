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

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

class GoogleAPIHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = GoogleAPIHelper.class.getSimpleName();
    private Activity activity;
    private final TokenListener tokenListener;

    private GoogleApiClient client;
    private boolean resolvingError;
    private int signInRequestCode;
    private int errorResolutionRequestCode;

    public GoogleAPIHelper(@NonNull Activity activity, @NonNull String serverClientId, @NonNull Scope[] scopes, @NonNull TokenListener tokenListener) {
        this.activity = activity;
        this.tokenListener = tokenListener;
        this.client = createGoogleAPIClient(serverClientId, scopes);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestGoogleAccount(signInRequestCode);
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
                connectionResult.startResolutionForResult(activity, errorResolutionRequestCode);
            } catch (IntentSender.SendIntentException e) {
                client.connect();
            }
        } else {
            tokenListener.onErrorReceived(getErrorDialog(connectionResult.getErrorCode(), errorResolutionRequestCode));
            resolvingError = true;
        }
    }

    public int isGooglePlayServicesAvailable() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
    }

    public Dialog getErrorDialog(int errorCode, int requestCode) {
        final Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(activity, errorCode, requestCode);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                resolvingError = false;
            }
        });
        return dialog;
    }

    public void connectAndRequestGoogleAccount(int signInRequestCode, int errorResolutionRequestCode) {
        if (client.isConnected()) {
            requestGoogleAccount(signInRequestCode);
        } else if (!client.isConnecting()) {
            this.signInRequestCode = signInRequestCode;
            this.errorResolutionRequestCode = errorResolutionRequestCode;
            client.connect();
        }
    }

    public boolean parseSignInResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == errorResolutionRequestCode) {
            resolvingError = false;
            if (resultCode == Activity.RESULT_OK) {
                connectAndRequestGoogleAccount(signInRequestCode, errorResolutionRequestCode);
            }
            return true;
        } else if (requestCode == signInRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);
                if (result.isSuccess()) {
                    tokenListener.onTokenReceived(result.getSignInAccount().getIdToken());
                }
            }
            return true;
        }

        return false;
    }

    public void logoutAndClearState() {
        try {
            Auth.GoogleSignInApi.signOut(client).setResultCallback(new ResultCallback<Status>() {
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
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
            activity = null;
            client = null;
        }
    }

    private GoogleApiClient createGoogleAPIClient(String serverClientId, Scope[] scopes) {
        final GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder()
                .requestIdToken(serverClientId);
        if (scopes.length == 1) {
            gsoBuilder.requestScopes(scopes[0]);
        } else if (scopes.length > 1) {
            gsoBuilder.requestScopes(scopes[0], scopes);
        }

        final GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity, this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gsoBuilder.build());

        return builder.build();
    }


    private void requestGoogleAccount(int signInRequestCode) {
        final Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(client);
        activity.startActivityForResult(signInIntent, signInRequestCode);
    }

}
