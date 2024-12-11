package src;

import javax.swing.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class ApplicationManager {

    public static void main(String[] args) throws UnknownHostException {

        SwingUtilities.invokeLater(() -> {
            try {
                Screen screen = new Screen();
                Peer peer = new Peer(InetAddress.getLocalHost().getHostAddress(), screen.getPeerInfo());
                NetworkManager.getInstance(peer);
                screen.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
