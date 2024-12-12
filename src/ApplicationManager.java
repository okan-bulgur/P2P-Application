package src;

import javax.swing.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ApplicationManager {

    public static void main(String[] args) throws UnknownHostException {

        SwingUtilities.invokeLater(() -> {
            try {
                Screen screen = new Screen();

                int port = NetworkManager.getInstance().BROADCAST_PORT;
                while (port == NetworkManager.getInstance().BROADCAST_PORT){
                    port = screen.getPeerInfo();
                }

                Peer peer = new Peer(InetAddress.getLocalHost().getHostAddress(), port);
                NetworkManager.getInstance(peer);
                screen.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
