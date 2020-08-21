package com.dion.a2048;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.dion.a2048.game.GameActivity;
import com.dion.a2048.game.PasswordActivity;
import com.dion.a2048.game.RequestAccessCodeTask;
import com.dion.a2048.game.ServerCommunicationHandler;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    public static int appVariant; // App variant (1 = control, 2 = gradual, 3 = all at once)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        boolean appOpened = sharedPref.getBoolean("app_opened", false);
        boolean appFinished = sharedPref.getBoolean("app_finished", false);

        if (appFinished) {
            Intent intent = new Intent(this, PasswordActivity.class);
            startActivity(intent);
        } else if (appOpened){
            int id = sharedPref.getInt("serverID_id", -1);
            String accessCode = sharedPref.getString("serverID_access_code", "");
            String accessToken = sharedPref.getString("serverID_access_token", "");

            GameActivity.serverID = new ServerCommunicationHandler.ServerIdentity(id, accessCode, accessToken);
            MainActivity.appVariant = sharedPref.getInt("appVariant", 1);

            Intent intent = new Intent(this, GameActivity.class);
            startActivity(intent);
        } else {
            Button launchButton = findViewById(R.id.btn_launch);
            launchButton.setOnClickListener(v -> {
                // Request access code from server
                ServerCommunicationHandler.ServerIdentity serverID = null;
                RequestAccessCodeTask requestAccessCodeTask = new RequestAccessCodeTask(GameActivity.getHttpClient());
                MainActivity.appVariant = (int) (Math.random() * 3 + 1);

                try {
                    serverID = requestAccessCodeTask.execute().get();
                } catch (ExecutionException e) {
                    Log.e(TAG, "onCreate: Error while requesting access code");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    Log.e(TAG, "onCreate: Interrupted while requesting access code");
                    e.printStackTrace();
                }

                if (serverID == null) {
                    throw new NullPointerException("Could not get server identity.");
                }

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("app_opened", true);
                editor.putInt("appVariant", appVariant);
                editor.putInt("serverID_id", serverID.id);
                editor.putString("serverID_access_code", serverID.accessCode);
                editor.putString("serverID_access_token", serverID.accessToken);
                editor.apply();

                GameActivity.serverID = new ServerCommunicationHandler.ServerIdentity(serverID.id, serverID.accessCode, serverID.accessToken);

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.popup_title))
                        .setMessage(getString(R.string.popup_text) + serverID.accessCode)

                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.ok, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                                startActivity(intent);
                            }
                        })
                        .show();
            });
        }
    }
}
