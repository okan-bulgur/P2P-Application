package app.socketHandler;

import app.dto.FileDTO;
import app.manager.NetworkManager;
import app.Peer;
import app.dto.PeerDTO;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpSocketHandler {

    final private Peer peer = NetworkManager.getInstance().getPeer();
    private final DatagramSocket udpSocket;

    private final ExecutorService requestExecutor;

    final int MAX_TTL = 3;
    static boolean isSentFileRequest = false;

    public UdpSocketHandler(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
        requestExecutor = Executors.newFixedThreadPool(5);
    }

    public void processResponse(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        System.out.println("UDP Received packet from: " + packet.getAddress() + ":" + packet.getPort() + " " + "Message: " + new String(packet.getData(), 0, packet.getLength()) + "\n");

        if (message.startsWith("CHUNK_REQUEST")) {  // CHUNK_REQUEST:hash=x:index=x:ip=x.x.x.x:port=xxxx:ttl=x:visited=ip:port,...
            chunkRequestHandler(packet);
        }
        else if (message.startsWith("FRIEND_REQUEST")) { // FRIEND_REQUEST
            friendRequestHandler(packet);
        }
        else if (message.startsWith("FILE_NOTIFICATION")) { // FILE_NOTIFICATION:filename=x:fileType=x:fileSize=x:chunkCount=x:hash=x:ip=x.x.x.x:port=xxxx
            fileNotificationHandler(packet);
        }
        else if (message.startsWith("FILE_INFO_REQUEST")) { // FILE_INFO_REQUEST
            sendFilesInfoForNewPeer(new PeerDTO(packet.getAddress().getHostAddress(), packet.getPort()));
        }
    }

    protected void spreadChunkRequest(String hash, int index, String requesterIP, int requesterPort, int ttl, HashSet<PeerDTO> visited) throws IOException {
        System.out.println("Spreading chunk request for: " + hash + " index: " + index + " ttl: " + ttl);

        peer.addPeer(new PeerDTO(requesterIP, requesterPort));
        NetworkManager.getInstance().getUdpSocketHandler().sendFriendRequest(requesterIP, requesterPort);

        if (peer.getPeers().isEmpty()) {
            System.out.println("No peers to send chunk request to.");
            return;
        }

        StringBuilder visitedInfo = new StringBuilder();
        for (PeerDTO v : visited) {
            visitedInfo.append(v.ip()).append(":").append(v.port()).append(",");
        }

        String message = "CHUNK_REQUEST" +
                ":hash=" + hash +
                ":index=" + index +
                ":ip=" + requesterIP +
                ":port=" + requesterPort +
                ":ttl=" + ttl +
                ":visited=" + (!visited.isEmpty() ? visitedInfo.substring(0, visitedInfo.length() - 1) : "");

        byte[] data = message.getBytes();

        for(PeerDTO peer : peer.getPeers()) {
            if (visited.contains(peer)) {
                continue;
            }

            System.out.println("Sending chunk request to: " + peer.ip() + ":" + peer.port());
            sendPacket(data, peer.ip(), peer.port());
        }
    }

    public void sendChunkRequest(String hash, int index) throws IOException { // CHUNK_REQUEST:hash=x:index:xip=x.x.x.x:port=xxxx:ttl=x:visited=ip:port,...
        String message = "CHUNK_REQUEST" +
                ":hash=" + hash +
                ":index=" + index +
                ":ip=" + peer.getIp() +
                ":port=" + peer.getPort() +
                ":ttl=" + MAX_TTL +
                ":visited=" + peer.getIp() + ":" + peer.getPort();

        byte[] data = message.getBytes();

        if (peer.getPeers().isEmpty()) {
            System.out.println("\nNo peers to send chunk request to." + " Message: " + message);
            // todo: fix there
            return;
        }

        HashSet<PeerDTO> peers = peer.getPeers();

        for (PeerDTO peer : peers) {
            requestExecutor.submit(() -> {
                try {
                    System.out.println("Sending Chunk Request to: " + peer.ip() + ":" + peer.port() + " Message: " + message);
                    sendPacket(data, peer.ip(), peer.port());
                } catch (IOException e) {
                    System.err.println("Failed to send chunk request to: " + peer.ip() + ":" + peer.port() + " Message: " + message);
                }
            });
        }

    }

    private void chunkRequestHandler(DatagramPacket packet) throws IOException { // CHUNK_REQUEST:hash=x:index=x:ip=x.x.x.x:port=xxxx:ttl=x:visited=ip:port,...
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        String[] parts = message.split(":");
        String fileHash = parts[1].split("=")[1];
        int index = Integer.parseInt(parts[2].split("=")[1]);
        String ip = parts[3].split("=")[1];
        int port = Integer.parseInt(parts[4].split("=")[1]);
        int ttl = Integer.parseInt(parts[5].split("=")[1]);

        System.out.println(peer.getOwnedChunks());

        if (peer.hasChunk(fileHash, index)) {
            NetworkManager.getInstance().getTcpSocketHandler().sendChunk(fileHash, index, ip, NetworkManager.TCP_PORT);
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
                    NetworkManager.getInstance().getUdpSocketHandler().sendFriendRequest(v.ip(), v.port());
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

    protected void sendFriendRequest(String ip, int port) throws IOException {
        String message = "FRIEND_REQUEST";
        byte[] data = message.getBytes();

        sendPacket(data, ip, port);
        System.out.println("Friend request sent to: " + ip + ":" + port + " (" + message + ")");
    }

    private void friendRequestHandler(DatagramPacket packet) throws IOException { // FRIEND_REQUEST

        String ip = packet.getAddress().getHostAddress();
        int port = packet.getPort();

        if (InetAddress.getByName(peer.getIp()).getHostAddress().equals(ip) && peer.getPort() == port) {
            return;
        }

        PeerDTO newPeer = new PeerDTO(ip, port);
        peer.addPeer(newPeer);

        if (!isSentFileRequest) {
            isSentFileRequest = true;
            sendFileRequest(newPeer);
        }
    }

    private void fileNotificationHandler(DatagramPacket packet) throws IOException { // FILE_NOTIFICATION:filename=x:fileType=x:fileSize=x:chunkCount=x:hash=x:ip=x.x.x.x:port=xxxx
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        String[] parts = message.split(":");
        String filename = parts[1].split("=")[1];
        String fileType = parts[2].split("=")[1];
        int fileSize = Integer.parseInt(parts[3].split("=")[1]);
        int chunkCount = Integer.parseInt(parts[4].split("=")[1]);
        String hash = parts[5].split("=")[1];
        String ip = parts[6].split("=")[1];
        int port = Integer.parseInt(parts[7].split("=")[1]);

        if (peer.getFiles().containsKey(hash) || peer.getUploadedFiles().containsKey(hash)) {
            return;
        }

        FileDTO file = new FileDTO(filename, fileType, fileSize, chunkCount, hash, new PeerDTO(ip, port));

        peer.addFiles(hash, file);
    }

    private void sendFilesInfoForNewPeer(PeerDTO requesterPeer) throws IOException {
        for (String fileHash : peer.getUploadedFiles().keySet()) {
            String notify = "filename=" + peer.getUploadedFiles().get(fileHash).filename() +
                    ":fileType=" + peer.getUploadedFiles().get(fileHash).fileType() +
                    ":fileSize=" + peer.getUploadedFiles().get(fileHash).fileSize() +
                    ":chunkCount=" + peer.getUploadedFiles().get(fileHash).chunkCount() +
                    ":hash=" + fileHash +
                    ":ip=" + peer.getUploadedFiles().get(fileHash).owner().ip() +
                    ":port=" + peer.getUploadedFiles().get(fileHash).owner().port();

            sendFileNotification(notify, requesterPeer);
        }

        for (String fileHash : peer.getFiles().keySet()) {
            String notify = "filename=" + peer.getFiles().get(fileHash).filename() +
                    ":fileType=" + peer.getFiles().get(fileHash).fileType() +
                    ":fileSize=" + peer.getFiles().get(fileHash).fileSize() +
                    ":chunkCount=" + peer.getFiles().get(fileHash).chunkCount() +
                    ":hash=" + fileHash +
                    ":ip=" + peer.getFiles().get(fileHash).owner().ip() +
                    ":port=" + peer.getFiles().get(fileHash).owner().port();

            sendFileNotification(notify, requesterPeer);
        }
    }

    private void sendFileNotification(String notify, PeerDTO peer) throws IOException { // FILE_NOTIFICATION:filename=x:fileType=x:fileSize=x:chunkCount=x:hash=x:ip=x.x.x.x:port=xxxx

        String message = "FILE_NOTIFICATION:" + notify;

        byte[] data = message.getBytes();

        System.out.println("Sending file notification: (" + message + ")");

        sendPacket(data, peer.ip(), peer.port());
    }

    protected void sendFileRequest(PeerDTO peer) throws IOException {
        String message = "FILE_INFO_REQUEST";
        byte[] data = message.getBytes();

        System.out.println("Sending file info request to: " + peer.ip() + ":" + peer.port());

        sendPacket(data, peer.ip(), peer.port());
    }

    public DatagramSocket getSocket() {
        return udpSocket;
    }

    private void sendPacket(byte [] data, String ip, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getByName(ip),
                port
        );
        getSocket().send(packet);
    }
}
