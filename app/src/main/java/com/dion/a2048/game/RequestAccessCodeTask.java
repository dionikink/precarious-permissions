package com.dion.a2048.game;

import android.os.AsyncTask;
import android.util.Log;

import com.dion.a2048.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.dion.a2048.game.GameActivity.API_BASE;
import static com.dion.a2048.game.GameActivity.JSON;

public class RequestAccessCodeTask extends AsyncTask<String, Void, ServerCommunicationHandler.ServerIdentity> {

    public static final String TAG = "RequestAccessCodeTask";

    private OkHttpClient client;

    public RequestAccessCodeTask(OkHttpClient client) {
        this.client = client;
    }

    @Override
    protected ServerCommunicationHandler.ServerIdentity doInBackground(String... strings) {
        String url = API_BASE + "participants/";
        JSONObject jsonRequest = new JSONObject();

        JSONObject jsonResponse;
        String response;

        int id = 0;
        String code = null;
        String token = null;

        try {
            jsonRequest.put("app_variant", MainActivity.appVariant);

            response = this.post(url, jsonRequest.toString());
            jsonResponse = new JSONObject(response);

            Log.i(TAG, "doInBackground: " + jsonResponse);

            id = (int) jsonResponse.get("id");
            code = (String) jsonResponse.get("participant_code");
            token = (String) jsonResponse.get("participant_token");
        } catch (IOException e) {
            Log.e(TAG, "Error while trying to contact server.");
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing JSON");
            e.printStackTrace();
        }

        Log.i(TAG, "doInBackground: Successfully requested access code: " + code);

        return new ServerCommunicationHandler.ServerIdentity(id, code, token);
    }

    private String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = this.client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
