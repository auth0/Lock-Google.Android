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

package com.auth0.android.google.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.google.GoogleAuthProvider;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.result.Credentials;
import com.auth0.google.app.R;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();
    private static final int RC_PERMISSIONS = 110;
    private static final int RC_AUTHENTICATION = 111;

    private GoogleAuthProvider provider;
    private AuthCallback callback;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        message = (TextView) findViewById(R.id.textView);
        Button loginButton = (Button) findViewById(R.id.loginButton);
        Button logoutButton = (Button) findViewById(R.id.logoutButton);
        loginButton.setOnClickListener(this);
        logoutButton.setOnClickListener(this);

        Auth0 auth0 = new Auth0(getString(R.string.com_auth0_client_id), getString(R.string.com_auth0_domain));
        final AuthenticationAPIClient client = new AuthenticationAPIClient(auth0);
        provider = new GoogleAuthProvider(getString(R.string.google_server_client_id), client);
        callback = new AuthCallback() {
            @Override
            public void onFailure(@NonNull final Dialog dialog) {
                Log.e(TAG, "Failed with dialog");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.show();
                    }
                });
            }

            @Override
            public void onFailure(final AuthenticationException exception) {
                Log.e(TAG, "Failed", exception);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Failed!")
                                .setMessage(exception.getLocalizedMessage())
                                .create();
                        dialog.show();
                    }
                });
            }

            @Override
            public void onSuccess(@NonNull final Credentials credentials) {
                Log.d(TAG, "Authenticated with accessToken " + credentials.getAccessToken());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        message.setText(String.format("Welcome %s", credentials.getAccessToken()));
                    }
                });
            }

        };
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        switch (id) {
            case R.id.loginButton:
                provider.start(this, callback, RC_PERMISSIONS, RC_AUTHENTICATION);
                break;
            case R.id.logoutButton:
                provider.clearSession();
                message.setText("");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (provider.authorize(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
