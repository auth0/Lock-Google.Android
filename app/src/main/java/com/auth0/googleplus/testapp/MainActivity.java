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

package com.auth0.googleplus.testapp;

import android.app.Dialog;
import android.os.Bundle;
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
import com.auth0.googleplus.GooglePlusIdentityProvider;
import com.auth0.identity.IdentityProviderCallback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    GooglePlusIdentityProvider provider;

    boolean authRequestInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView textView = (TextView) findViewById(R.id.textView);

        Auth0 auth0 = new Auth0(getString(R.string.auth0_client_id), getString(R.string.auth0_domain_name));
        final AuthenticationAPIClient client = new AuthenticationAPIClient(auth0);

        provider = new GooglePlusIdentityProvider(MainActivity.this);
        provider.setCallback(new IdentityProviderCallback() {
            @Override
            public void onFailure(Dialog dialog) {
                Log.d(TAG, "onFailure, showing dialog...");

                textView.setText("");
                dialog.show();
            }

            @Override
            public void onFailure(int titleResource, int messageResource, Throwable cause) {
                Log.d(TAG, "onFailure, titleResource: " + titleResource + ", messageResource: " + messageResource + ", cause: " + cause);

                textView.setText("");
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(titleResource)
                        .setMessage(messageResource)
                        .create();
                dialog.show();
            }

            @Override
            public void onSuccess(String serviceName, String accessToken) {
                Log.d(TAG, "onSuccess, serviceName: " + serviceName + ", accessToken: " + accessToken);

                textView.setText("Trying to log in with GooglePlus token: " + accessToken);

                AuthenticationRequest request = client.loginWithOAuthAccessToken(accessToken, serviceName);
                request.start(new AuthenticationCallback() {
                    @Override
                    public void onSuccess(UserProfile profile, Token token) {
                        textView.setText("Welcome " + profile.getName());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        textView.setText("Log in failed. " + error.getMessage());
                    }
                });
            }

            @Override
            public void onSuccess(Token token) {
                Log.d(TAG, "onSuccess, token: " + token);

                textView.setText("Logged in with token: " + token);
            }
        });

        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        authRequestInProgress = true;
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
    protected void onResume() {
        super.onResume();

        if (authRequestInProgress) {
            provider.authorize(this, GooglePlusIdentityProvider.GOOGLE_PLUS_TOKEN_REQUEST_CODE, 9876, getIntent());
            authRequestInProgress = false;
        }
    }
}
