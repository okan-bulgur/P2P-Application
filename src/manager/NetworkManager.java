package src.manager;

import src.Peer;
import src.dto.PeerDTO;
import src.socket.BroadcastSocketHandler;
import src.socket.UdpSocketHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class NetworkManager {
    private static NetworkManager instance;
    private Peer peer;

    private DatagramSocket udpSocket;
    private boolean isConnected = false;
    private Thread listenerThread;

    private DatagramSocket broadcastSocket;
    private boolean isBroadcastConnected = false;
    private Thread listenerThreadForBroadcast;

    final String BROADCAST_IP = "255.255.255.255";
    final int BROADCAST_PORT = 5050;

    private static UdpSocketHandler udpSocketHandler;
    private static BroadcastSocketHandler broadcastSocketHandler;

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
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

    public Peer getPeer() {
        return peer;
    }

    protected void setPeer(Peer peer) {
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

        System.out.println("Peer added: " + newPeer.ip() + ":" + newPeer.port());
    }

    public void showPeers() {
        System.out.println("\n\nPeers of " + peer);
        for (PeerDTO p : peer.getPeers()) {
            System.out.println(p);
        }
        System.out.println("\n\n");
    }
}