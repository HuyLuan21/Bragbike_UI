package com.example.bragbike.socket;

import com.example.bragbike.BuildConfig;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import java.util.Collections;

public class SocketManager {
    private static Socket socket;

    public static Socket getSocket(String token) {
        if (socket == null) {
            try {
                IO.Options options = new IO.Options();
                options.forceNew = true;
                options.reconnection = true;
                if (token != null) {
                    options.auth = Collections.singletonMap("token", token);
                }
                socket = IO.socket(BuildConfig.BASE_URL, options);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return socket;
    }

    public static void connect(String token) {
        getSocket(token).connect();
    }

    public static void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }
}
