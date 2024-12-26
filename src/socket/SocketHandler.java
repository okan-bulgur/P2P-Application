package src.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public interface SocketHandler {
    void processResponse(DatagramPacket packet) throws IOException;

    DatagramSocket getSocket();

    default void sendPacket(byte [] data, String ip, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getByName(ip),
                port
        );
        getSocket().send(packet);
    }
}
