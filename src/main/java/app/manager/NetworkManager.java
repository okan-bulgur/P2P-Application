package app.manager;

import app.Peer;
import app.dto.PeerDTO;
import app.socketHandler.BroadcastSocketHandler;
import app.socketHandler.TcpSocketHandler;
import app.socketHandler.UdpSocketHandler;

import java.io.IOException;
import java.net.*;

public class NetworkManager {
    private static NetworkManager instance;
    private app.Peer peer;

    private DatagramSocket udpSocket;
    private boolean isUdpConnected = false;
    private Thread listenerThreadForUdp;

    private DatagramSocket broadcastSocket;
    private boolean isBroadcastConnected = false;
    private Thread listenerThreadForBroadcast;

    private ServerSocket tcpServerSocket;
    private boolean isTcpConnected = false;
    private Thread listenerThreadForTcp;

    public static final int UDP_PORT = 5000;
    public static final int TCP_PORT = 5010;

    final String BROADCAST_IP = "255.255.255.255";
    final int BROADCAST_PORT = 5050;

    private static UdpSocketHandler udpSocketHandler;
    private static BroadcastSocketHandler broadcastSocketHandler;
    private static TcpSocketHandler tcpSocketHandler;

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            try {
                instance = new NetworkManager();
                instance.setPeer(new Peer(InetAddress.getLocalHost().getHostAddress(), UDP_PORT));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public void connect() {

        if (isUdpConnected) return;

        try {
            // UDP Part
            udpSocket = new DatagramSocket(peer.getPort());
            isUdpConnected = true;

            udpSocketHandler = new UdpSocketHandler(udpSocket);

            if (listenerThreadForUdp == null || !listenerThreadForUdp.isAlive()) {
                listenerThreadForUdp = new Thread(this::listenForUdpResponses);
                listenerThreadForUdp.start();
            }

            // Broadcast Part
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

            // TCP Part
            tcpServerSocket = new ServerSocket(TCP_PORT);
            isTcpConnected = true;

            tcpSocketHandler = new TcpSocketHandler();

            if (listenerThreadForTcp == null || !listenerThreadForTcp.isAlive()) {
                listenerThreadForTcp = new Thread(this::listenForTcpResponses);
                listenerThreadForTcp.start();
            }

            System.out.println("\nIP: " + peer.getIp() + " Port: " + peer.getPort() + " is connecting to the network.\n");
        }
        catch (IOException e) {
            disconnect();
            System.err.println("Failed to connect to the network. Error: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (!isUdpConnected) return;

        udpSocketHandler = null;
        broadcastSocketHandler = null;
        tcpSocketHandler = null;

        isUdpConnected = false;
        if (listenerThreadForUdp != null && listenerThreadForUdp.isAlive()) {
            listenerThreadForUdp.interrupt();
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

        isTcpConnected = false;
        if (listenerThreadForTcp != null && listenerThreadForTcp.isAlive()) {
            listenerThreadForTcp.interrupt();
        }
        if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
            try {
                tcpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        peer.getPeers().clear();

        System.out.println("IP: " + peer.getIp() + " Port: " + peer.getPort() + " is disconnecting from the network.");
    }

    private void listenForUdpResponses() {
        byte[] buffer = new byte[1024];
        while (isUdpConnected) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                udpSocketHandler.processResponse(packet);
            } catch (IOException e) {
                if(!isUdpConnected) break;
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

    private void listenForTcpResponses() {
        while (isTcpConnected) {
            Socket clientSocket = null;
            try {
                clientSocket = tcpServerSocket.accept();
                clientSocket.setReceiveBufferSize(257 * 1024);
                Socket finalClientSocket = clientSocket;
                System.out.println("New TCP connection from " + finalClientSocket.getInetAddress().getHostAddress() + ":" + finalClientSocket.getPort());
                new Thread(() -> {
                    try {
                        tcpSocketHandler.processResponse(finalClientSocket);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
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

    public TcpSocketHandler getTcpSocketHandler() {
        return tcpSocketHandler;
    }

    public boolean isConnected() {
        return isUdpConnected;
    }
}