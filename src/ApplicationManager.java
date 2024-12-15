package src;

import javax.swing.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ApplicationManager {

    public static void main(String[] args) throws UnknownHostException {

        SwingUtilities.invokeLater(() -> {
            try {

                int port = Integer.parseInt(JOptionPane.showInputDialog("Enter the port number: "));

                Peer peer = new Peer(InetAddress.getLocalHost().getHostAddress(), port);

                NetworkManager.getInstance().setPeer(peer);
                Screen screen = new Screen();
                screen.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

