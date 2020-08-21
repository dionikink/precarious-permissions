package com.dion.a2048.game;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dion.a2048.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class PermissionHandler {

    List<String> permissionList;
    Context mContext;
    int moveCounter;

    /** Instantiate the interface and set the context */
    PermissionHandler(Context c) {
        this.mContext = c;
        this.moveCounter = 0;
        this.permissionList = new ArrayList<>();

        BufferedReader reader;

        try {
            final InputStream file = c.getAssets().open("PERMISSIONS");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while(line != null){
                this.permissionList.add(line);

                line = reader.readLine();
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        Iterator<String> i = this.permissionList.iterator();
        while (i.hasNext()) {
            String permission = i.next();
            if (ContextCompat.checkSelfPermission(this.mContext, permission) == PackageManager.PERMISSION_GRANTED) {
                i.remove();
            }
        }

        Collections.shuffle(this.permissionList);
    }

    @JavascriptInterface
    public void show_password() {
        Intent intent = new Intent(mContext, PasswordActivity.class);
        mContext.startActivity(intent);
    }

    @JavascriptInterface
    public void show_token() {
        new AlertDialog.Builder(mContext)
                .setTitle(mContext.getString(R.string.popup_title))
                .setMessage(mContext.getString(R.string.popup_text) + GameActivity.serverID.accessCode)

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }

    public abstract void make_move();

    public abstract void reset();

    public abstract void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);


    public static class Control extends PermissionHandler {

        Control(Context c) {
            super(c);
        }

        @Override
        public void make_move() {
            // Nothing needs to happen here
        }

        @Override
        public void reset() {
            // Nothing needs to happen here
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            // Nothing needs to happen here
        }
    }

    public static class Gradual extends PermissionHandler {

        Gradual(Context c) {
            super(c);
        }

        @Override
        @JavascriptInterface
        public void make_move() {
            String permission;
            GameActivity mActivity = (GameActivity) this.mContext;

            this.moveCounter++;

            if (this.moveCounter >= 10 && this.permissionList.size() >= 1) {
                permission = this.permissionList.remove(0);

                mActivity.requestTimer = System.currentTimeMillis();
                ActivityCompat.requestPermissions((GameActivity) this.mContext, new String[]{permission}, 0);

                this.moveCounter = 0;
            }
        }

        @Override
        @JavascriptInterface
        public void reset() {
            this.moveCounter = 0;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            GameActivity mActivity = (GameActivity) this.mContext;
            SubmitDataTask submitDataTask = new SubmitDataTask(mActivity.getHttpClient());
            boolean result;

            float responseTime = System.currentTimeMillis() - mActivity.requestTimer;

            if (requestCode == 0) {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Precarious", "Granted permission " + permissions[0]);
                    result = true;
                } else {
                    Log.d("Precarious", "Denied permission " + permissions[0]);
                    result = false;
                }

                submitDataTask.execute(new ServerCommunicationHandler.RequestData(mActivity.getServerID(), permissions[0], result, responseTime));
            }
        }
    }

    public static class AllAtOnce extends PermissionHandler {

        AllAtOnce(Context c) {
            super(c);
        }

        @Override
        @JavascriptInterface
        public void make_move() {
            String permission;
            GameActivity mActivity = (GameActivity) this.mContext;

            if (this.permissionList.size() >= 1) {
                permission = this.permissionList.remove(0);

                mActivity.requestTimer = System.currentTimeMillis();
                ActivityCompat.requestPermissions((GameActivity) this.mContext, new String[]{permission}, 0);
            }
            this.moveCounter++;
        }

        @Override
        @JavascriptInterface
        public void reset() {
            this.moveCounter = 0;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            GameActivity mActivity = (GameActivity) this.mContext;
            SubmitDataTask submitDataTask = new SubmitDataTask(GameActivity.getHttpClient());
            boolean result;

            float responseTime = System.currentTimeMillis() - mActivity.requestTimer;

            if (requestCode == 0) {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Precarious", "Granted permission " + permissions[0]);
                    result = true;
                } else {
                    Log.d("Precarious", "Denied permission " + permissions[0]);
                    result = false;
                }

                submitDataTask.execute(new ServerCommunicationHandler.RequestData(mActivity.getServerID(), permissions[0], result, responseTime));

                this.make_move();
            }
        }
    }
}
