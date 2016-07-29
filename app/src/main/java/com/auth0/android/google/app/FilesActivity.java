package com.auth0.android.google.app;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.google.GoogleAuthProvider;
import com.auth0.android.lock.AuthenticationCallback;
import com.auth0.android.lock.Lock;
import com.auth0.android.lock.LockCallback;
import com.auth0.android.lock.provider.AuthProviderResolver;
import com.auth0.android.lock.utils.LockException;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.provider.AuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilesActivity extends AppCompatActivity {

    private static final String TAG = FilesActivity.class.getSimpleName();
    private static final String GOOGLE_CONNECTION = "google-oauth2";

    private Lock lock;
    private ProgressBar progressBar;
    private ArrayList<String> files;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);

        lock = Lock.newBuilder(getAccount(), authCallback)
                .withProviderResolver(providerResolver)
                .onlyUseConnections(Collections.singletonList(GOOGLE_CONNECTION))
                .closable(true)
                .build();
        lock.onCreate(this);


        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(lock.newIntent(FilesActivity.this));
            }
        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        ListView listView = (ListView) findViewById(R.id.list);
        files = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, files);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lock.onDestroy(this);
        lock = null;
    }

    private Auth0 getAccount() {
        return new Auth0(getString(R.string.com_auth0_client_id), getString(R.string.com_auth0_domain));
    }

    private LockCallback authCallback = new AuthenticationCallback() {
        @Override
        public void onAuthentication(Credentials credentials) {
            Log.i(TAG, "Auth ok! User has given us all google requested permissions.");
            AuthenticationAPIClient client = new AuthenticationAPIClient(getAccount());
            client.tokenInfo(credentials.getIdToken())
                    .start(new BaseCallback<UserProfile, AuthenticationException>() {
                        @Override
                        public void onSuccess(UserProfile payload) {
                            final GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(FilesActivity.this, Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY));
                            credential.setSelectedAccountName(payload.getEmail());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new FetchFilesTask().execute(credential);
                                }
                            });
                        }

                        @Override
                        public void onFailure(AuthenticationException error) {
                        }
                    });

        }

        @Override
        public void onCanceled() {
        }

        @Override
        public void onError(LockException error) {
            Log.e(TAG, "Error occurred: " + error.getMessage());
        }
    };

    private AuthProviderResolver providerResolver = new AuthProviderResolver() {
        @Nullable
        @Override
        public AuthProvider onAuthProviderRequest(Context context, @NonNull AuthCallback callback, @NonNull String connectionName) {
            if (GOOGLE_CONNECTION.equals(connectionName)) {
                AuthenticationAPIClient client = new AuthenticationAPIClient(getAccount());
                GoogleAuthProvider googleProvider = new GoogleAuthProvider(getString(R.string.google_server_client_id), client);
                googleProvider.setScopes(new Scope(DriveScopes.DRIVE_METADATA_READONLY));
                googleProvider.setRequiredPermissions(new String[]{"android.permission.GET_ACCOUNTS"});
                googleProvider.forceRequestAccount(true);
                return googleProvider;
            }
            return null;
        }
    };

    private class FetchFilesTask extends AsyncTask<GoogleAccountCredential, Void, List<String>> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<String> doInBackground(GoogleAccountCredential... params) {
            final Drive service = new Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(),
                    params[0])
                    .setApplicationName("Auth0 Google Native Demo")
                    .build();

            List<String> fileInfo = new ArrayList<>();
            FileList result;
            try {
                result = service.files().list()
                        .setPageSize(30)
                        .setFields("files(name, size)")
                        .execute();
                List<File> files = result.getFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getSize() != null) {
                            fileInfo.add(String.format("[File] %s %s", file.getName(), file.getSize()));
                        } else {
                            fileInfo.add(String.format("[Folder] %s", file.getName()));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return fileInfo;
        }

        @Override
        protected void onPostExecute(List<String> names) {
            files.addAll(names);
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        }
    }
}