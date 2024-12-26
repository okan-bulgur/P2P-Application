package app.manager;

import app.Peer;
import app.dto.PeerDTO;
import app.socket.BroadcastSocketHandler;
import app.socket.UdpSocketHandler;

import java.io.IOException;
import java.net.*;

public class NetworkManager {
    private static NetworkManager instance;
    private app.Peer peer;

    private DatagramSocket udpSocket;
    private boolean isConnected = false;
    private Thread listenerThread;

    private DatagramSocket broadcastSocket;
    private boolean isBroadcastConnected = false;
    private Thread listenerThreadForBroadcast;

    static final int APP_PORT = 5000;

    final String BROADCAST_IP = "255.255.255.255";
    final int BROADCAST_PORT = 5050;

    private static UdpSocketHandler udpSocketHandler;
    private static BroadcastSocketHandler broadcastSocketHandler;

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            try {
                instance = new NetworkManager();
                instance.setPeer(new Peer(InetAddress.getLocalHost().getHostAddress(), APP_PORT));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public void connect() {

        if (isConnected) return;

        try {
            udpSocket = new DatagramSocket(peer.getPort());
            isConnected = true;

            udpSocketHandler = new UdpSocketHandler(udpSocket);

            if (listenerThread == null || !listenerThread.isAlive()) {
                listenerThread = new Thread(this::listenForUdpResponses);
                listenerThread.start();
            }

            broadcastSocket = new DatagramSocket(null);
            broadcastSocket.setReuseAddress(true);
            broadcastSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), BROADCAST_PORT));
            broadcastSocket.setBroadcast(true);
            isBroadcastConnected = true;

            broadcastSocketHandler = new BroadcastSocketHandler(BROADCAST_IP, BROADCAST_PORT, broadcastSocket);

            if (listenerThreadForBroadcast == null || !listenerThreadForBroadcast.isAlive()) {
                listenerThreadForBroadcast = new Thread(this::listenForBroadcastResponse);
                listenerThreadForBroadcast.start();
            }

            broadcastSocketHandler.sendBroadcastRequest();

            System.out.println("\nIP: " + peer.getIp() + " Port: " + peer.getPort() + " is connecting to the network.\n");
        }
        catch (IOException e) {
            disconnect();
            System.err.println("Failed to connect to the network. Error: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (!isConnected) return;

        udpSocketHandler = null;
        broadcastSocketHandler = null;

        isConnected = false;
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
        }
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }

        isBroadcastConnected = false;
        if (listenerThreadForBroadcast != null && listenerThreadForBroadcast.isAlive()) {
            listenerThreadForBroadcast.interrupt();
        }
        if (broadcastSocket != null && !broadcastSocket.isClosed()) {
            broadcastSocket.close();
        }

        peer.getPeers().clear();

        System.out.println("IP: " + peer.getIp() + " Port: " + peer.getPort() + " is disconnecting from the network.");
    }

    private void listenForUdpResponses() {
        byte[] buffer = new byte[1024];
        while (isConnected) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                udpSocketHandler.processResponse(packet);
            } catch (IOException e) {
                if(!isConnected) break;
            }
        }
    }

    private void listenForBroadcastResponse(){
        byte[] buffer = new byte[1024];
        while (isBroadcastConnected) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                broadcastSocket.receive(packet);
                broadcastSocketHandler.processResponse(packet);
            } catch (IOException e) {
                if(!isBroadcastConnected) break;
            }
        }
    }

    protected PeerDTO getPeerDTO() {
        return new PeerDTO(peer.getIp(), peer.getPort());
    }

    public app.Peer getPeer() {
        return peer;
    }

    public void setPeer(app.Peer peer) {
        this.peer = peer;
    }

    public UdpSocketHandler getUdpSocketHandler() {
        return udpSocketHandler;
    }

    public BroadcastSocketHandler getBroadcastSocketHandler() {
        return broadcastSocketHandler;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void addManuelPeer(String ip, int port) {
        PeerDTO newPeer = new PeerDTO(ip, port);
        peer.addPeer(newPeer);

        System.out.println("app.Peer added: " + newPeer.ip() + ":" + newPeer.port());
    }

    public void showPeers() {
        System.out.println("\n\nPeers of " + peer);
        for (PeerDTO p : peer.getPeers()) {
            System.out.println(p);
        }
        System.out.println("\n\n");
    }
}