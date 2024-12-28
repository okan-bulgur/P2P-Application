package app.manager;

import app.Peer;
import app.Screen;

import javax.swing.*;
import java.net.UnknownHostException;

public class ApplicationManager {


    public static void main(String[] args) throws UnknownHostException {

        SwingUtilities.invokeLater(() -> {
            try {
                Screen screen = Screen.getInstance();
                screen.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

