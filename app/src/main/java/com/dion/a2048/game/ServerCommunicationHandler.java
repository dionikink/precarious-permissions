package com.dion.a2048.game;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;

public class ServerCommunicationHandler {

    public static class RequestData {
        public ServerIdentity serverID;
        public String permission;
        public boolean result;
        public float responseTime;

        public RequestData(ServerIdentity serverID, String permission, boolean result, float responseTime) {
            this.serverID = serverID;
            this.permission = permission;
            this.result = result;
            this.responseTime = responseTime;
        }

        @NotNull
        @Override
        public String toString() {
            return "[ID " + this.serverID.id + "] " + this.permission + " - " + this.result + "(" + this.responseTime + ")";
        }
    }

    public static class ServerIdentity {
        public int id;
        public String accessCode;
        public String accessToken;

        public ServerIdentity(int id, String accessCode, String accessToken) {
            this.id = id;
            this.accessCode = accessCode;
            this.accessToken = accessToken;
        }

    }
}
