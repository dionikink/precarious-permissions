package com.dion.a2048.game;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.dion.a2048.game.GameActivity.API_BASE;
import static com.dion.a2048.game.GameActivity.JSON;

public class SubmitDataTask extends AsyncTask<ServerCommunicationHandler.RequestData, Void, Void> {

    private static final String TAG = "SubmitDataTask";

    private OkHttpClient client;

    public SubmitDataTask(OkHttpClient client) {
        this.client = client;
    }

    @Override
    protected Void doInBackground(ServerCommunicationHandler.RequestData... requestData) {
        ServerCommunicationHandler.ServerIdentity serverID = requestData[0].serverID;
        String permission = requestData[0].permission.substring(19).toLowerCase(); // Remove "android.permission." prefix
        boolean result = requestData[0].result;
        float responseTime = requestData[0].responseTime;

        String url = API_BASE + "participants/" + serverID.id + "/";
        JSONObject jsonRequest = new JSONObject();
        String response = null;

        try {
            jsonRequest.put("participant_token", serverID.accessToken);
            jsonRequest.put(permission, result);
            jsonRequest.put(permission + "_response_time", responseTime);

            response = this.put(url, jsonRequest.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error while trying to contact server.");
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing JSON");
            e.printStackTrace();
        }

        return null;
    }

    private String put(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        try (Response response = this.client.newCall(request).execute()) {
            return response.body().string();
        }
    }


}
