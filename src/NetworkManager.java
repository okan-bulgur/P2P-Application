package src;

import src.DTO.FileDTO;
import src.DTO.PeerDTO;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;

public class NetworkManager {
    private static NetworkManager instance;
    private final Peer peer;

    private DatagramSocket udpSocket;
    private boolean isConnected = false;
    private Thread listenerThread;

    private DatagramSocket broadcastSocket;
    private boolean isBroadcastConnected = false;
    private Thread listenerThreadForBroadcast;

    final int MAX_TTL = 3;
    final String BROADCAST_IP = "255.255.255.255";
    final int BROADCAST_PORT = 5050;

    protected static synchronized NetworkManager getInstance(Peer peer) {
        if (instance == null) {
            instance = new NetworkManager(peer);
        }
        return instance;
    }

    protected static synchronized NetworkManager getInstance() {
        return getInstance(new Peer("localhost", 8080));
    }

    protected NetworkManager(Peer peer) {
        this.peer = peer;
    }

    protected void connect() {

        if (isConnected) {
            return;
        }
        try {
            InetAddress address = InetAddress.getByName(peer.getIp());
            int port = peer.getPort();

            udpSocket = new DatagramSocket(port, address);
            isConnected = true;

            if (listenerThread == null || !listenerThread.isAlive()) {
                listenerThread = new Thread(this::listenForResponses);
                listenerThread.start();
            }

            broadcastSocket = new DatagramSocket(null);
            broadcastSocket.setReuseAddress(true);
            broadcastSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), BROADCAST_PORT));
            broadcastSocket.setBroadcast(true);
            isBroadcastConnected = true;

            if (listenerThreadForBroadcast == null || !listenerThreadForBroadcast.isAlive()) {
                listenerThreadForBroadcast = new Thread(this::listenForBroadcastResponse);
                listenerThreadForBroadcast.start();
            }
            sendBootstrapRequest();


            System.out.println("IP: " + peer.getIp() + " Port: " + peer.getPort() + " is connecting to the network.\n");
        }
        catch (IOException e) {
            disconnect();
            System.out.println("Failed to connect to the network.");
            e.printStackTrace();
        }
    }

    protected void disconnect() {
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

    private void listenForResponses() {
        byte[] buffer = new byte[1024];
        while (isConnected) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                System.out.println("Received packet from: " + packet.getAddress() + ":" + packet.getPort() + " " + "Message: " + new String(packet.getData(), 0, packet.getLength()) + "\n");
                processResponse(packet);
            } catch (IOException e) {
                if(!isConnected) break;
            }
        }
    }

    public void sendSearchRequest(String filename) throws IOException { // SEARCH_REQUEST:filename=x:ip=x.x.x.x:port=xxxx:ttl=x
        String message = "SEARCH_REQUEST:" +
                "filename=" + filename +
                ":ip=" + InetAddress.getLocalHost().getHostAddress() +
                ":port=" + udpSocket.getLocalPort() +
                ":ttl=" + MAX_TTL + ":" +
                "visited=" + InetAddress.getLocalHost().getHostAddress() + ":" + udpSocket.getLocalPort();

        System.out.println("Send Request peer " + peer.getIp() + ":" + peer.getPort() + " messge: " + message);
        byte[] data = message.getBytes();

        if (peer.getPeers().isEmpty()) {
            System.out.println("No peers to send search request to.");
            /*
            sendBootstrapRequest();

            try {
                Thread.sleep(2000); // Sleep for 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Sleep interrupted");
            }

            sendSearchRequest(filename);
            return;
            */
            return;
        }

        for(PeerDTO peer : peer.getPeers()) {

            System.out.println("Sending search request to: " + peer.ip() + ":" + peer.port());
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    InetAddress.getByName(peer.ip()), peer.port());
            udpSocket.send(packet);
        }
    }

    private void sendFriendRequest(String ip, int port) throws IOException {
        String message = "FRIEND_REQUEST:" +
                "ip=" + InetAddress.getLocalHost().getHostAddress() +
                ":port=" + udpSocket.getLocalPort();

        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(ip), port);
        udpSocket.send(packet);
        System.out.println("Friend request sent to: " + ip + ":" + port + " (" + message + ")");
    }

    protected void sendFileNotification(String notify) throws IOException { // FILE_NOTIFICATION:event=x:filename=x:fileType=x:fileSize=x:chunkCount=x:hash=x:ip=x.x.x.x:port=xxxx

        String message = "FILE_NOTIFICATION:" + notify;

        byte[] data = message.getBytes();

        System.out.println("Sending file notification: (" + message + ")");

        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(BROADCAST_IP), BROADCAST_PORT);
        broadcastSocket.send(packet);
    }

    private void spreadSearchRequest(String filename, String requesterIP, int requesterPort, int ttl, HashSet<PeerDTO> visited) throws IOException {
        if (peer.getPeers().isEmpty()) {
            System.out.println("No peers to send search request to.");
            peer.addPeer(new PeerDTO(requesterIP, requesterPort));
            return;
        }

        StringBuilder visitedInfo = new StringBuilder();
        for (PeerDTO v : visited) {
            visitedInfo.append(v.ip()).append(":").append(v.port()).append(",");
        }

        String message = "SEARCH_REQUEST:" +
                "filename=" + filename + ":" +
                "ip=" + requesterIP + ":" +
                "port=" + requesterPort + ":" +
                "ttl=" + ttl + ":" +
                "visited=" + (!visited.isEmpty() ? visitedInfo.substring(0, visitedInfo.length() - 1) : "");

        byte[] data = message.getBytes();

        for(PeerDTO peer : peer.getPeers()) {
            if (visited.contains(peer)) {
                continue;
            }

            System.out.println("Sending search request to: " + peer.ip() + ":" + peer.port());
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    InetAddress.getByName(peer.ip()), peer.port());
            udpSocket.send(packet);
        }
    }

    private void processResponse(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        if (message.startsWith("SEARCH_REQUEST")) { // SEARCH_REQUEST:filename=x:ip=x.x.x.x:port=xxxx:ttl=x:visited=ip:port,...
            String[] parts = message.split(":");
            String filename = parts[1].split("=")[1];
            String ip = parts[2].split("=")[1];
            int port = Integer.parseInt(parts[3].split("=")[1]);
            int ttl = Integer.parseInt(parts[4].split("=")[1]);

            if (peer.hasChunk(filename)) {
                String responseMessage = "SEARCH_RESULT:" +
                        "filename=" + filename + ":" +
                        "ip=" + InetAddress.getLocalHost().getHostAddress() + ":" +
                        "port=" + udpSocket.getLocalPort();

                byte[] responseData = responseMessage.getBytes();

                DatagramPacket responsePacket = new DatagramPacket(
                        responseData, responseData.length,
                        InetAddress.getByName(ip), port
                );
                udpSocket.send(responsePacket);

                System.out.println("Sent response to: " + ip + ":" + port + " (" + responseMessage + ")");
            }
            else {
                HashSet<PeerDTO> visited = new HashSet<>();
                if (message.contains("visited=")) {

                    String[] visitedPeers = message.split("visited=")[1].split(",");

                    for (String visitedPeer : visitedPeers) {
                        String[] visitedInfo = visitedPeer.split(":");
                        visited.add(new PeerDTO(visitedInfo[0], Integer.parseInt(visitedInfo[1])));
                    }
                }

                visited.add(new PeerDTO(InetAddress.getLocalHost().getHostAddress(), udpSocket.getLocalPort()));

                for (PeerDTO v : visited) {
                    if (v.ip().equals(InetAddress.getLocalHost().getHostAddress()) && v.port() == udpSocket.getLocalPort()) {
                        continue;
                    }
                    if (!peer.hasPeer(v)) {
                        sendFriendRequest(v.ip(), v.port());
                        peer.addPeer(v);
                    }
                }

                if (ttl == 1) {
                    return;
                }

                spreadSearchRequest(filename, ip, port, ttl - 1, visited);
            }

            peer.addPeer(new PeerDTO(ip, port));
        }

        else if (message.startsWith("SEARCH_RESULT")) { // SEARCH_RESULT:filename=x:ip=x.x.x.x:port=xxxx
            String[] parts = message.split(":");
            String filename = parts[1].split("=")[1];
            String ip = parts[2].split("=")[1];
            int port = Integer.parseInt(parts[3].split("=")[1]);

            peer.addPeer(new PeerDTO(ip, port));

            if (peer.hasChunk(filename)) {
                return;
            }

            // todo: download file from newPeer
        }

        else if (message.startsWith("FRIEND_REQUEST")) { // FRIEND_REQUEST:ip=x.x.x.x:port=xxxx
            String[] parts = message.split(":");
            String[] peerInfo = parts[1].split("=");
            String ip = peerInfo[1].split(":")[0];
            int port = Integer.parseInt(parts[2].split("=")[1]);

            if (InetAddress.getByName(peer.getIp()).getHostAddress().equals(ip) && peer.getPort() == port) {
                return;
            }

            PeerDTO newPeer = new PeerDTO(ip, port);
            peer.addPeer(newPeer);
        }
    }

    private void listenForBroadcastResponse(){
        byte[] buffer = new byte[1024];
        while (isBroadcastConnected) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                broadcastSocket.receive(packet);
                processBroadcastResponse(packet);
            } catch (IOException e) {
                if(!isBroadcastConnected) break;
            }
        }
    }

    private void processBroadcastResponse(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        System.out.println("Received broadcast packet from: " + packet.getAddress() + ":" + packet.getPort() + " (" + message + ")\n");

        if (message.startsWith("BOOTSTRAP_REQUEST")) { // BOOTSTRAP_REQUEST:ip=x.x.x.x:port=xxxx
            String[] parts = message.split(":");
            String[] peerInfo = parts[1].split("=");
            String ip = peerInfo[1].split(":")[0];
            int port = Integer.parseInt(parts[2].split("=")[1]);

            if (InetAddress.getByName(peer.getIp()).getHostAddress().equals(ip) && peer.getPort() == port) {
                return;
            }

            System.out.println("(BROADCAST) Added peer: " + ip + ":" + port);
            sendFriendRequest(ip, port);
            PeerDTO newPeer = new PeerDTO(ip, port);
            peer.addPeer(newPeer);
        }

        else if (message.startsWith("FILE_NOTIFICATION")) { // FILE_NOTIFICATION:event=x:filename=x:fileType=x:fileSize=x:chunkCount=x:hash=x:ip=x.x.x.x:port=xxxx
            String[] parts = message.split(":");
            String event = parts[1].split("=")[1];
            String filename = parts[2].split("=")[1];
            String fileType = parts[3].split("=")[1];
            long fileSize = Long.parseLong(parts[4].split("=")[1]);
            int chunkCount = Integer.parseInt(parts[5].split("=")[1]);
            String hash = parts[6].split("=")[1];
            String ip = parts[7].split("=")[1];
            int port = Integer.parseInt(parts[8].split("=")[1]);

            if (InetAddress.getByName(peer.getIp()).getHostAddress().equals(ip) && peer.getPort() == port) {
                return;
            }

            FileDTO fileDTO = new FileDTO(filename, fileType, fileSize, chunkCount, hash, new PeerDTO(ip, port));

            if (event.equals("ENTRY_CREATE")) {
                peer.addFileToPeer(fileDTO.hash(), fileDTO);
            }
            else if (event.equals("ENTRY_DELETE")) {
                peer.removeFileToPeer(fileDTO.hash());
            }

            peer.addPeer(fileDTO.owner());
        }
    }

    private void sendBootstrapRequest() throws IOException {
        System.out.println("Sending bootstrap request...");
        String message = "BOOTSTRAP_REQUEST:" +
                "ip=" + InetAddress.getLocalHost().getHostAddress() +
                ":port=" + udpSocket.getLocalPort();

        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(BROADCAST_IP), BROADCAST_PORT);
        broadcastSocket.send(packet);
        System.out.println("Bootstrap request sent to: " + BROADCAST_IP + ":" + BROADCAST_PORT + " (" + message + ")");
    }

    protected void addManuelPeer(String ip, int port) {
        PeerDTO newPeer = new PeerDTO(ip, port);
        peer.addPeer(newPeer);

        System.out.println("Peer added: " + newPeer.ip() + ":" + newPeer.port());
    }

    protected void showPeers() {
        System.out.println("\n\nPeers of " + peer);
        for (PeerDTO p : peer.getPeers()) {
            System.out.println(p);
        }
        System.out.println("\n\n");
    }

    protected PeerDTO getPeerDTO() {
        return new PeerDTO(peer.getIp(), peer.getPort());
    }
}