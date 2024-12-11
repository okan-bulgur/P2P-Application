package src;

import javax.swing.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ApplicationManager {
    Peer peer = null;

    public static void main(String[] args) throws UnknownHostException {
        String ip = JOptionPane.showInputDialog("Enter the IP address of the peer: ");
        int port = Integer.parseInt(JOptionPane.showInputDialog("Enter the port number of the peer: "));
        Peer peer = new Peer(InetAddress.getByName(ip).getHostName(), port);
        NetworkManager.getInstance(peer);

        SwingUtilities.invokeLater(() -> {
            Screen screen = new Screen();
            screen.setVisible(true);
        });
    }
}
