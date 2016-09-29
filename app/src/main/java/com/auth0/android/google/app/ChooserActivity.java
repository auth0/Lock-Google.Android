package com.auth0.android.google.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class ChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chooser);
        Button tokenLogin = (Button) findViewById(R.id.tokenLoginButton);
        tokenLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChooserActivity.this, SimpleActivity.class));
            }
        });
        Button photosLogin = (Button) findViewById(R.id.photosLoginButton);
        photosLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChooserActivity.this, FilesActivity.class));
            }
        });
    }
}
