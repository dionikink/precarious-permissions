package com.dion.a2048.game;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.dion.a2048.R;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;


public class GameActivity extends Activity {


    private static final String MAIN_ACTIVITY_TAG = "2048_MainActivity";

    public static final String TAG = "GameActivity";
    public static final String API_BASE = "https://uxperience.site:48752/analytics/";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public long requestTimer;

    private WebView mWebView;
    private PermissionHandler permissionHandler;
    private long mLastBackPress;
    private static final long mBackPressThreshold = 3500;
    private static final String IS_FULLSCREEN_PREF = "is_fullscreen_pref";
    private long mLastTouch;
    private static final long mTouchThreshold = 2000;
    private Toast pressBackToast;

    static int appVariant; // App variant (1 = control, 2 = gradual, 3 = all at once)

    private ServerCommunicationHandler.ServerIdentity serverID;

    @SuppressLint({"SetJavaScriptEnabled", "ShowToast", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        GameActivity.appVariant = (int) (Math.random() * 3 + 1);
        GameActivity.appVariant = 3;

        // Don't show an action bar or title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Enable hardware acceleration
        getWindow().setFlags(LayoutParams.FLAG_HARDWARE_ACCELERATED,
                LayoutParams.FLAG_HARDWARE_ACCELERATED);

        // Apply previous setting about showing status bar or not
        applyFullScreen(isFullScreen());

        // Check if screen rotation is locked in settings
        boolean isOrientationEnabled = false;
        try {
            isOrientationEnabled = Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (SettingNotFoundException e) {
            Log.d(MAIN_ACTIVITY_TAG, "Settings could not be loaded");
        }

        // If rotation isn't locked and it's a LARGE screen then add orientation changes based on sensor
        int screenLayout = getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (((screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE)
                || (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE))
                && isOrientationEnabled) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        setContentView(R.layout.activity_game);

        // Load webview with game
        mWebView = findViewById(R.id.mainWebView);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // Load webview with current Locale language
        mWebView.loadUrl("file:///android_asset/2048/index.html?lang=" + Locale.getDefault().getLanguage());

        Toast.makeText(getApplication(), R.string.toggle_fullscreen, Toast.LENGTH_SHORT).show();
        // Set fullscreen toggle on webview LongClick
        mWebView.setOnTouchListener((v, event) -> {
            // Implement a long touch action by comparing
            // time between action up and action down
            long currentTime = System.currentTimeMillis();
            if ((event.getAction() == MotionEvent.ACTION_UP)
                    && (Math.abs(currentTime - mLastTouch) > mTouchThreshold)) {
                boolean toggledFullScreen = !isFullScreen();
                saveFullScreen(toggledFullScreen);
                applyFullScreen(toggledFullScreen);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mLastTouch = currentTime;
            }
            // return so that the event isn't consumed but used
            // by the webview as well
            return false;
        });

        // Request access code from server
        ServerCommunicationHandler.ServerIdentity serverID = null;
        RequestAccessCodeTask requestAccessCodeTask = new RequestAccessCodeTask(getHttpClient());

        try {
            serverID = requestAccessCodeTask.execute().get();
        } catch (ExecutionException e) {
            Log.e(TAG, "onCreate: Error while requesting access code");
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.e(TAG, "onCreate: Interrupted while requesting access code");
            e.printStackTrace();
        }

        this.serverID = serverID;

        // Set PermissionHandler and register with Javascript code
        switch (GameActivity.appVariant) {
            case 1:
                this.permissionHandler = new PermissionHandler.Control(this);
                break;
            case 2:
                this.permissionHandler = new PermissionHandler.Gradual(this);
                break;
            case 3:
                this.permissionHandler = new PermissionHandler.AllAtOnce(this);
        }

        mWebView.addJavascriptInterface(this.permissionHandler, "PermissionHandler");

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.popup_title))
                .setMessage(getString(R.string.popup_text) + serverID.accessCode)

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
                .setCancelable(false);
    }

    public OkHttpClient getHttpClient() {
        return new OkHttpClient.Builder()
                .connectionSpecs(Arrays.asList(
                        ConnectionSpec.MODERN_TLS,
                        ConnectionSpec.COMPATIBLE_TLS,
                        new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                                .tlsVersions(TlsVersion.TLS_1_2)
                                .cipherSuites(
                                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                                .build()))
                .build();
    }

    public ServerCommunicationHandler.ServerIdentity getServerID() {
        return this.serverID;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        this.permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.loadUrl("file:///android_asset/2048/index.html?lang=" + Locale.getDefault().getLanguage());
    }

    /**
     * Saves the full screen setting in the SharedPreferences
     *
     * @param isFullScreen boolean value
     */

    private void saveFullScreen(boolean isFullScreen) {
        // save in preferences
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(IS_FULLSCREEN_PREF, isFullScreen);
        editor.apply();
    }

    private boolean isFullScreen() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(IS_FULLSCREEN_PREF,
                true);
    }

    /**
     * Toggles the activity's fullscreen mode by setting the corresponding window flag
     *
     * @param isFullScreen boolean value
     */
    private void applyFullScreen(boolean isFullScreen) {
        if (isFullScreen) {
            getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN,
                    LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * Prevents app from closing on pressing back button accidentally.
     * mBackPressThreshold specifies the maximum delay (ms) between two consecutive backpress to
     * quit the app.
     */

    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - mLastBackPress) > mBackPressThreshold) {
            pressBackToast.show();
            mLastBackPress = currentTime;
        } else {
            pressBackToast.cancel();
            super.onBackPressed();
        }
    }
}
