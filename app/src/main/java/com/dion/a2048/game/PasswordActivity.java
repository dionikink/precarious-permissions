package com.dion.a2048.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.dion.a2048.R;

public class PasswordActivity extends AppCompatActivity {

    public static final String TAG = "PasswordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("app_finished", true);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        // Do nothing, the user should not be able to go back.
    }
}
