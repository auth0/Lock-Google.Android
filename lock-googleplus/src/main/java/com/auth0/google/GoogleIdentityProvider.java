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

package com.auth0.google;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.auth0.googleplus.GooglePlusIdentityProvider;
import com.auth0.identity.AuthorizedIdentityProvider;
import com.auth0.identity.IdentityProviderCallback;

import java.util.List;

/**
 * Google Identity Provider to authenticate with native SDK with full support for Android M permission model.
 * <p>
 * In order to create it, you just need an Android context
 * <pre><code>
 *  GoogleIdentityProvider provider = new GoogleIdentityProvider(context);
 *  this.provider = provider;
 * </code></pre>
 * <p>
 * Then just set the callback
 * <p>
 * <pre><code>
 *  IdentityProviderCallback callback = ...
 *  provider.setCallback(callback);
 * </code></pre>
 * <p>
 * And start authentication from any of your Activities
 * <p>
 * <pre><code>
 *  provider.start(this, "{connection name}");
 *  this.authenticationInProgress = true;
 * </code></pre>
 * <p>
 * Then you need to pass along the result from {@link Activity#onResume()} and permission result from {@link Activity#onRequestPermissionsResult(int, String[], int[])}
 * <p>
 * <pre><code>
 *  @Override
 *  protected void onResume() {
 *      super.onResume();
 *
 *      if (authenticationInProgress) {
 *          provider.authorize(this, GoogleIdentityProvider.GOOGLE_PLUS_TOKEN_REQUEST_CODE, Activity.RESULT_OK, getIntent());
 *          authRequestInProgress = false;
 *      }
 *  }
 *
 *  @Override
 *  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
 *      if (provider != null) {
 *          provider.onRequestPermissionsResult(requestCode, permissions, grantResults);
 *      }
 *  }
 *
 * </code></pre>
 */
@TargetApi(23)
public class GoogleIdentityProvider extends AuthorizedIdentityProvider {

    @Deprecated
    public GoogleIdentityProvider(@NonNull Context context) {
        //noinspection ConstantConditions
        this(context, null);
    }

    public GoogleIdentityProvider(@NonNull Context context, @NonNull String serverClientId) {
        super(new GooglePlusIdentityProvider(context, serverClientId));
    }

    @Override
    public void onPermissionsRequireExplanation(Activity activity, List<String> permissions, final IdentityProviderCallback callback) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.com_auth0_google_permission_failed_title)
                .setMessage(R.string.com_auth0_google_permission_failed_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        retryLastPermissionRequest();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onFailure(R.string.com_auth0_google_permission_declined_title, R.string.com_auth0_google_permission_declined_message, null);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_email)
                .show();
    }

    public String[] getRequiredPermissions() {
        return new String[]{Manifest.permission.GET_ACCOUNTS};
    }
}
