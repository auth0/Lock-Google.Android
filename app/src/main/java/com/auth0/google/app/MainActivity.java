/*
 * MainActivity.java
 *
 * Copyright (c) 2015 Auth0 (http://auth0.com)
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

package com.auth0.google.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.auth0.api.authentication.AuthenticationAPIClient;
import com.auth0.api.authentication.AuthenticationRequest;
import com.auth0.api.callback.AuthenticationCallback;
import com.auth0.core.Auth0;
import com.auth0.core.Strategies;
import com.auth0.core.Token;
import com.auth0.core.UserProfile;
import com.auth0.google.GoogleIdentityProvider;
import com.auth0.identity.IdentityProviderCallback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private GoogleIdentityProvider provider;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView textView = (TextView) findViewById(R.id.textView);

        Auth0 auth0 = new Auth0(getString(R.string.auth0_client_id), getString(R.string.auth0_domain_name));
        final AuthenticationAPIClient client = new AuthenticationAPIClient(auth0);

        provider = new GoogleIdentityProvider(getApplicationContext());
        provider.setCallback(new IdentityProviderCallback() {
            @Override
            public void onFailure(Dialog dialog) {
                dismissProgress();
                Log.e(TAG, "Failed with dialog");

                textView.setText(null);
                dialog.show();
            }

            @Override
            public void onFailure(int titleResource, int messageResource, Throwable cause) {
                dismissProgress();
                Log.e(TAG, "Failed with message " + getString(messageResource), cause);

                textView.setText(null);
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(titleResource)
                        .setMessage(messageResource)
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .create();
                dialog.show();
            }

            @Override
            public void onSuccess(String serviceName, String accessToken) {
                Log.d(TAG, "Authenticated with connection name " + serviceName + " accessToken " + accessToken);

                progressDialog.setMessage(getString(R.string.progress_auth0));

                AuthenticationRequest request = client.loginWithOAuthAccessToken(accessToken, serviceName);
                request.start(new AuthenticationCallback() {
                    @Override
                    public void onSuccess(UserProfile profile, Token token) {
                        dismissProgress();
                        textView.setText("Welcome " + profile.getName());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        dismissProgress();
                        textView.setText("Log in failed. " + error.getMessage());
                    }
                });
            }

            @Override
            public void onSuccess(Token token) {
                dismissProgress();
                Log.e(TAG, "Should not call this method");
            }
        });

        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        progressDialog = ProgressDialog.show(MainActivity.this, getString(R.string.progress_title), getString(R.string.progress_message));
                        provider.start(MainActivity.this, Strategies.GooglePlus.getName());
                    }
                }
        );

        Button logoutButton = (Button) findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        textView.setText("");
                        provider.clearSession();
                    }
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (provider != null && provider.authorize(this, GoogleIdentityProvider.GOOGLE_PLUS_TOKEN_REQUEST_CODE, Activity.RESULT_OK, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (provider != null) {
            provider.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void dismissProgress() {
        progressDialog.dismiss();
        progressDialog = null;
    }
}
