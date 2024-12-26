package app.socket;

import app.manager.FileManager;
import app.manager.NetworkManager;
import app.Peer;
import app.dto.PeerDTO;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;

public class UdpSocketHandler implements SocketHandler {

    final private Peer peer = NetworkManager.getInstance().getPeer();
    private final DatagramSocket udpSocket;

    final int MAX_TTL = 3;

    public UdpSocketHandler(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }

    public void processResponse(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        System.out.println("Received packet from: " + packet.getAddress() + ":" + packet.getPort() + " " + "Message: " + new String(packet.getData(), 0, packet.getLength()) + "\n");

        if (message.startsWith("CHUNK_REQUEST")) {  // CHUNK_REQUEST:hash=x:index=x:ip=x.x.x.x:port=xxxx:ttl=x:visited=ip:port,...
            chunkRequestHandler(packet);
        }

        else if (message.startsWith("CHUNK_RESULT")) { // CHUNK_RESULT:fileHash=x:index=x:chunk_hash=x:chunkSize=x:ip=x.x.x.x:port=xxxx<data>chunkData
            chunkResultHandler(packet);
        }

        else if (message.startsWith("FRIEND_REQUEST")) { // FRIEND_REQUEST
            friendRequestHandler(packet);
        }
    }

    public void sendChunkRequest(String hash, int index) throws IOException { // CHUNK_REQUEST:hash=x:index:xip=x.x.x.x:port=xxxx:ttl=x:visited=ip:port,...
        String message = "CHUNK_REQUEST:" +
                "hash=" + hash +
                ":index=" + index +
                ":ip=" + peer.getIp() +
                ":port=" + peer.getPort() +
                ":ttl=" + MAX_TTL + ":" +
                "visited=" + peer.getIp() + ":" + peer.getPort();

        System.out.println("\nSend Chunk Request from " + peer.getIp() + ":" + peer.getPort() + " messge: " + message);
        byte[] data = message.getBytes();

        if (peer.getPeers().isEmpty()) {
            System.out.println("\nNo peers to send chunk request to.");
            // todo: fix there
            return;
        }

        for(PeerDTO peer : peer.getPeers()) {
            System.out.println("Sending Chunk Request to: " + peer.ip() + ":" + peer.port());
            sendPacket(data, peer.ip(), peer.port());
        }
    }

    protected void spreadChunkRequest(String hash, int index, String requesterIP, int requesterPort, int ttl, HashSet<PeerDTO> visited) throws IOException {
        peer.addPeer(new PeerDTO(requesterIP, requesterPort));
        sendFriendRequest(requesterIP, requesterPort);

        if (peer.getPeers().isEmpty()) {
            System.out.println("No peers to send chunk request to.");
            return;
        }

        StringBuilder visitedInfo = new StringBuilder();
        for (PeerDTO v : visited) {
            visitedInfo.append(v.ip()).append(":").append(v.port()).append(",");
        }

        String message = "CHUNK_REQUEST:" +
                "hash=" + hash +
                ":index=" + index +
                "ip=" + requesterIP + ":" +
                "port=" + requesterPort + ":" +
                "ttl=" + ttl + ":" +
                "visited=" + (!visited.isEmpty() ? visitedInfo.substring(0, visitedInfo.length() - 1) : "");

        byte[] data = message.getBytes();

        for(PeerDTO peer : peer.getPeers()) {
            if (visited.contains(peer)) {
                continue;
            }

            System.out.println("Sending chunk request to: " + peer.ip() + ":" + peer.port());
            sendPacket(data, peer.ip(), peer.port());
        }
    }

    protected void sendFriendRequest(String ip, int port) throws IOException {
        String message = "FRIEND_REQUEST";
        byte[] data = message.getBytes();

        sendPacket(data, ip, port);
        System.out.println("Friend request sent to: " + ip + ":" + port + " (" + message + ")");
    }

    private void chunkRequestHandler(DatagramPacket packet) throws IOException { // CHUNK_REQUEST:hash=x:index=x:ip=x.x.x.x:port=xxxx:ttl=x:visited=ip:port,...
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        String[] parts = message.split(":");
        String fileHash = parts[1].split("=")[1];
        int index = Integer.parseInt(parts[2].split("=")[1]);
        String ip = parts[3].split("=")[1];
        int port = Integer.parseInt(parts[4].split("=")[1]);
        int ttl = Integer.parseInt(parts[5].split("=")[1]);

        if (peer.hasChunk(fileHash, index)) {
            byte[] chunkData = FileManager.getInstance().getChunkData(fileHash, index);

            String responseMessage = "CHUNK_RESULT:" +
                    "fileHash=" + fileHash +
                    ":index=" + index +
                    ":chunkHash=" + peer.getChunkHash(fileHash, index) +
                    ":chunkSize=" + chunkData.length +
                    ":ip=" + peer.getIp() +
                    ":port=" + peer.getPort() + "<data>";

            byte[] responseHeader = responseMessage.getBytes();
            byte[] responseData = new byte[responseHeader.length + chunkData.length];

            System.arraycopy(responseHeader, 0, responseData, 0, responseHeader.length);
            System.arraycopy(chunkData, 0, responseData, responseHeader.length, chunkData.length);

            sendPacket(responseData, ip, port);

            System.out.println("Sent Chunk Result to: " + ip + ":" + port + " (" + responseMessage + ")");
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

            visited.add(new PeerDTO(peer.getIp(), peer.getPort()));

            for (PeerDTO v : visited) {
                if (v.ip().equals(peer.getIp()) && v.port() == peer.getPort()) {
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

            spreadChunkRequest(fileHash, index, ip, port, ttl - 1, visited);
        }

        peer.addPeer(new PeerDTO(ip, port));
    }

    private void chunkResultHandler(DatagramPacket packet) throws IOException { // CHUNK_RESULT:fileHash=x:index=x:chunk_hash=x:chunkSize=x:ip=x.x.x.x:port=xxxx<data>chunkData
        String message = new String(packet.getData(), 0, packet.getLength()).trim();
        byte[] data = packet.getData();

        String header = message.split("<data>")[0];

        String[] headerParts = header.split(":");
        String fileHash = headerParts[1].split("=")[1];
        int index = Integer.parseInt(headerParts[2].split("=")[1]);
        String chunkHash = headerParts[3].split("=")[1];
        int chunkSize = Integer.parseInt(headerParts[4].split("=")[1]);
        String ip = headerParts[5].split("=")[1];
        int port = Integer.parseInt(headerParts[6].split("=")[1]);

        if(peer.hasChunk(fileHash, index)) {
            return;
        }

        int dataLength = message.split("<data>")[1].getBytes().length;
        int headerLength = header.getBytes().length;
        int headerRegexLength = "<data>".getBytes().length;
        int offset = headerLength + headerRegexLength;
        int packetLength = packet.getLength();

        if (offset + chunkSize > packetLength) {
            System.err.println("Insufficient data for chunk: offset=" + offset + ", chunkSize=" + chunkSize + ", dataLength=" + data.length);
            return;
        }

        byte[] chunkData = new byte[chunkSize];
        System.arraycopy(data, offset, chunkData, 0, chunkSize);

        FileManager.getInstance().saveChunkData(fileHash, chunkHash, index, chunkData);

        System.out.println("Received chunk result from: " + ip + ":" + port + " (" + header + ")");

    }

    private void friendRequestHandler(DatagramPacket packet) throws IOException { // FRIEND_REQUEST

        String ip = packet.getAddress().getHostAddress();
        int port = packet.getPort();

        if (InetAddress.getByName(peer.getIp()).getHostAddress().equals(ip) && peer.getPort() == port) {
            return;
        }

        PeerDTO newPeer = new PeerDTO(ip, port);
        peer.addPeer(newPeer);
    }

    public DatagramSocket getSocket() {
        return udpSocket;
    }
}
