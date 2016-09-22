/*
 * GooglePlusIdentityProvider.java
 *
 * Copyright (c) 2014 Auth0 (http://auth0.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.auth0.googleplus;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.auth0.core.Application;
import com.auth0.core.Strategies;
import com.auth0.google.FetchTokenAsyncTask;
import com.auth0.google.R;
import com.auth0.identity.IdentityProvider;
import com.auth0.identity.IdentityProviderCallback;
import com.auth0.identity.IdentityProviderRequest;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

/**
 * Please use {@link com.auth0.google.GoogleIdentityProvider} instead that has support for Android M permission model.
 */
@Deprecated
public class GooglePlusIdentityProvider implements IdentityProvider, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = GooglePlusIdentityProvider.class.getName();
    private final GoogleApiClient apiClient;
    private boolean authenticating;
    private boolean requestedLogin;
    private Activity activity;
    private IdentityProviderCallback callback;

    @Deprecated
    public GooglePlusIdentityProvider(Context context) {
        this(context, null);
    }

    public GooglePlusIdentityProvider(Context context, String serverClientId) {
        this.apiClient = createAPIClient(context, serverClientId);
    }

    @Override
    public void setCallback(IdentityProviderCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(Activity activity, String serviceName) {
        this.activity = activity;
        Log.v(TAG, "Starting G+ connection");
        final int availabilityStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (availabilityStatus != ConnectionResult.SUCCESS) {
            Log.w(TAG, "Google services availability failed with status " + availabilityStatus);
            callback.onFailure(GooglePlayServicesUtil.getErrorDialog(availabilityStatus, activity, 0));
            stop();
            return;
        }
        authenticating = true;
        if (apiClient.isConnected()) {
            getGoogleAccount();
        } else {
            apiClient.connect();
        }
    }

    @Override
    public void start(Activity activity, IdentityProviderRequest event, Application application) {
        start(activity, Strategies.GooglePlus.getName());
    }

    @Override
    public void stop() {
        clearSession();
    }

    @Override
    public boolean authorize(Activity activity, int requestCode, int resultCode, Intent data) {
        this.activity = activity;
        if (resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "User canceled operation with code " + requestCode);
            authenticating = false;
            return false;
        }
        if (requestCode == GOOGLE_PLUS_REQUEST_CODE) {
            Log.v(TAG, "Received request activity result " + resultCode);
            if (!apiClient.isConnecting()) {
                apiClient.connect();
            }
            return true;
        } else if (requestCode == GOOGLE_PLUS_TOKEN_REQUEST_CODE) {
            Log.v(TAG, "Received token request activity result " + resultCode);
            if (!apiClient.isConnected()) {
                apiClient.connect();
            } else {
                if (resultCode == Activity.RESULT_OK) {
                    final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (result.isSuccess()) {
                        final GoogleSignInAccount account = result.getSignInAccount();
                        if (account.getIdToken() != null) {
                            callback.onSuccess(Strategies.GooglePlus.getName(), account.getIdToken());
                        } else {
                            fetchToken(account.getEmail());
                        }
                    } else {
                        callback.onFailure(R.string.com_auth0_google_authentication_failed_title, R.string.com_auth0_google_authentication_failed_message, null);
                    }
                }
            }
        }
        return authenticating;
    }

    @Override
    public void clearSession() {
        try {
            Auth.GoogleSignInApi.signOut(apiClient).setResultCallback(new ResultCallback<Status>() {
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
            if (apiClient != null && apiClient.isConnected()) {
                apiClient.disconnect();
            }
            activity = null;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        getGoogleAccount();
        requestedLogin = false;
    }

    @Override
    public void onConnectionSuspended(int code) {
        Log.v(TAG, "Connection suspended with code: " + code);
        authenticating = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        final int errorCode = result.getErrorCode();
        Log.v(TAG, "Connection failed with code " + errorCode);
        switch (errorCode) {
            case ConnectionResult.SERVICE_MISSING:
                requestedLogin = false;
                authenticating = false;
                Log.e(TAG, "service not available");
                callback.onFailure(GooglePlayServicesUtil.getErrorDialog(errorCode, activity, 0));
                break;
            case ConnectionResult.SIGN_IN_REQUIRED:
                if (requestedLogin) {
                    requestedLogin = false;
                    authenticating = false;
                    Log.e(TAG, "User dismissed the sign in account picker");
                    callback.onFailure(R.string.com_auth0_google_authentication_failed_title, R.string.com_auth0_google_authentication_failed_message, null);
                } else {
                    requestAuthentication(result);
                }
                break;
            case ConnectionResult.RESOLUTION_REQUIRED:
                requestAuthentication(result);
                break;
            default:
                authenticating = false;
                requestedLogin = false;
                Log.e(TAG, "Connection failed with unrecoverable error");
                callback.onFailure(R.string.com_auth0_social_error_title, R.string.com_auth0_social_error_message, null);
                break;
        }
    }

    private GoogleApiClient createAPIClient(Context context, String serverClientId) {
        final GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder()
                .requestScopes(new Scope(Scopes.PLUS_LOGIN))
                .requestEmail()
                .requestProfile();
        if (serverClientId != null) {
            gsoBuilder.requestIdToken(serverClientId);
        }
        GoogleSignInOptions gso = gsoBuilder.build();

        return new GoogleApiClient.Builder(context, this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void requestAuthentication(ConnectionResult result) {
        if (authenticating) {
            Log.v(TAG, "Showing G+ consent activity to the user");
            final PendingIntent mSignInIntent = result.getResolution();
            try {
                activity.startIntentSenderForResult(mSignInIntent.getIntentSender(), GOOGLE_PLUS_REQUEST_CODE, null, 0, 0, 0);
                requestedLogin = true;
            } catch (IntentSender.SendIntentException ignore) {
                authenticating = false;
                apiClient.connect();
                Log.w(TAG, "G+ pending intent cancelled", ignore);
            }
        }
    }

    private void getGoogleAccount() {
        final Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(apiClient);
        activity.startActivityForResult(signInIntent, GOOGLE_PLUS_TOKEN_REQUEST_CODE);
    }

    private void fetchToken(String email) {
        new FetchTokenAsyncTask(activity, email, callback).execute("https://www.googleapis.com/auth/plus.login", "email", "profile");
    }
}
