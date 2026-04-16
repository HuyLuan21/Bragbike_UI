package com.example.bragbike.socket;

import com.example.bragbike.BuildConfig;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class SocketManager {
    private static Socket socket;

    public static Socket getSocket() {
        if (socket == null) {
            try {
                IO.Options options = new IO.Options();
                options.forceNew = true;
                options.reconnection = true;
                socket = IO.socket(BuildConfig.BASE_URL, options);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return socket;
    }

    public static void connect() {
        getSocket().connect();
    }

    public static void disconnect() {
        if (socket != null) {
            socket.disconnect();
        }
    }
}