package src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;

public class NetworkManager {
    private static NetworkManager instance;
    private final Peer peer;
    private DatagramSocket udpSocket;
    private boolean isConnected = false;
    private Thread listenerThread;
    final int MAX_TTL = 3;

    public static synchronized NetworkManager getInstance(Peer peer) {
        if (instance == null) {
            instance = new NetworkManager(peer);
        }
        return instance;
    }

    public static synchronized NetworkManager getInstance() {
        return getInstance(new Peer("localhost", 8080));
    }

    public NetworkManager(Peer peer) {
        this.peer = peer;
    }

    public void connect() throws IOException {

        if (isConnected) {
            return;
        }

        InetAddress address = InetAddress.getByName(peer.getIp());
        int port = peer.getPort();

        udpSocket = new DatagramSocket(port, address);
        udpSocket.setBroadcast(true);
        isConnected = true;

        listenerThread = new Thread(this::listenForResponses);
        listenerThread.start();

        sendBootstrapRequest();

        System.out.println("IP: " + peer.getIp() + " Port: " + peer.getPort() + " is connecting to the network.");
    }

    public void disconnect() {
        if (!isConnected) {
            return;
        }

        isConnected = false;
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
        }
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        peer.getPeers().clear();

        System.out.println("IP: " + peer.getIp() + " Port: " + peer.getPort() + " is disconnecting from the network.");
    }

    private void listenForResponses() {
        byte[] buffer = new byte[1024];
        while (isConnected) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                System.out.println("Received packet from: " + packet.getAddress() + ":" + packet.getPort());
                System.out.println("Message: " + new String(packet.getData(), 0, packet.getLength()));
                processResponse(packet);
            } catch (IOException e) {
                if(!isConnected) break;
            }
        }
    }

    private void processResponse(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        if (message.startsWith("SEARCH_REQUEST")) { // SEARCH_REQUEST:filename=x:ip=x.x.x.x:port=xxxx:ttl=x
            String[] parts = message.split(":");
            System.out.println(Arrays.toString(parts));
            String filename = parts[1].split("=")[1];
            String ip = parts[2].split("=")[1];
            int port = Integer.parseInt(parts[3].split("=")[1]);
            int ttl = Integer.parseInt(parts[4].split("=")[1]);

            if (peer.hasChunk(filename)) {
                String responseMessage = "SEARCH_RESULT:" +
                        filename + ":" + InetAddress.getLocalHost().getHostAddress() + ":" + udpSocket.getLocalPort();

                byte[] responseData = responseMessage.getBytes();

                DatagramPacket responsePacket = new DatagramPacket(
                        responseData, responseData.length,
                        InetAddress.getByName(ip), port
                );
                udpSocket.send(responsePacket);
            }
            else {
                PeerDTO chunkOwner = PeerConnectionManager.getInstance().searchChunkOwner(peer, filename, new HashSet<>(), ttl);
                if (chunkOwner != null) {
                    String responseMessage = "SEARCH_RESULT:" +
                            filename + ":" + chunkOwner.ip() + ":" + chunkOwner.port();

                    byte[] responseData = responseMessage.getBytes();

                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length,
                            InetAddress.getByName(ip), port
                    );
                    udpSocket.send(responsePacket);
                }
            }
        }

        else if (message.startsWith("SEARCH_RESULT")) { // SEARCH_RESULT:filename=x:ip=x.x.x.x:port=xxxx
            String[] parts = message.split(":");
            String filename = parts[1].split("=")[1];
            String ip = parts[2].split("=")[1];
            int port = Integer.parseInt(parts[3].split("=")[1]);

            PeerDTO newPeer = new PeerDTO(ip, port);
            peer.addPeer(newPeer);

            // todo: download file from newPeer
        }

        else if (message.equals("BOOTSTRAP_REQUEST")) { // BOOTSTRAP_REQUEST:ip=x.x.x.x:port=xxxx
            String responseMessage = "BOOTSTRAP_RESPONSE:ip=" +
                    InetAddress.getLocalHost().getHostAddress() +
                    ":port=" + udpSocket.getLocalPort();

            byte[] responseData = responseMessage.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length,
                    packet.getAddress(), packet.getPort()
            );

            String ip = packet.getAddress().getHostAddress();
            int port = packet.getPort();

            udpSocket.send(responsePacket);
            System.out.println("Sent response to: " + packet.getAddress() + ":" + packet.getPort());

            String peerIP = InetAddress.getByName(peer.getIp()).getHostAddress();
            if (peerIP.equals(ip) && peer.getPort() == port) {
                return;
            }

            PeerDTO newPeer = new PeerDTO(ip, port);
            peer.addPeer(newPeer);
        }

        else if (message.startsWith("BOOTSTRAP_RESPONSE")) { // BOOTSTRAP_RESPONSE:peer_ip=x.x.x.x:peer_port=xxxx
            String[] parts = message.split(":");
            String[] peerInfo = parts[1].split("=");
            String ip = peerInfo[1].split(":")[0];
            int port = Integer.parseInt(parts[2].split("=")[1]);

            PeerDTO newPeer = new PeerDTO(ip, port);
            peer.addPeer(newPeer);
        }
    }

    private void sendBootstrapRequest() throws IOException {
        String message = "BOOTSTRAP_REQUEST";
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName("255.255.255.255"), 5000);
        udpSocket.send(packet);
    }

    public void sendSearchRequest(String filename) throws IOException { // SEARCH_REQUEST:filename=x:ip=x.x.x.x:port=xxxx:ttl=x
        String message = "SEARCH_REQUEST:" +
                "filename=" + filename +
                ":ip=" + InetAddress.getLocalHost().getHostAddress() +
                ":port=" + udpSocket.getLocalPort() +
                ":ttl=" + MAX_TTL;

        System.out.println("Peer " + peer.getIp() + ":" + peer.getPort() + " messge: " + message);
        byte[] data = message.getBytes();

        if (peer.getPeers().isEmpty()) {
            System.out.println("No peers to send search request to.");
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    InetAddress.getByName("255.255.255.255"), 5000);
            udpSocket.send(packet);
            return;
        }

        for(PeerDTO peer : peer.getPeers()) {

            System.out.println("Sending search request to: " + peer.ip() + ":" + peer.port());
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    InetAddress.getByName(peer.ip()), peer.port());
            udpSocket.send(packet);
        }

        for (PeerDTO peer : peer.getPeers()) {
            try {
                System.out.println("Sending search request to: " + peer.ip() + ":" + peer.port());

                DatagramPacket packet = new DatagramPacket(data, data.length,
                        InetAddress.getByName(peer.ip()), peer.port());

                udpSocket.send(packet);
            } catch (Exception e) {
                System.err.println("Failed to send request to: " + peer.ip() + ":" + peer.port());
            }
        }
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