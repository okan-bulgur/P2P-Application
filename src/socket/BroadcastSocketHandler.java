package src.socket;

import src.manager.NetworkManager;
import src.Peer;
import src.dto.FileDTO;
import src.dto.PeerDTO;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BroadcastSocketHandler implements SocketHandler {

    final private Peer peer = NetworkManager.getInstance().getPeer();
    final private String BROADCAST_IP;
    final private int BROADCAST_PORT;
    final private DatagramSocket broadcastSocket;

    public BroadcastSocketHandler(String broadcastIp, int broadcastPort, DatagramSocket broadcastSocket) {
        this.BROADCAST_IP = broadcastIp;
        this.BROADCAST_PORT = broadcastPort;
        this.broadcastSocket = broadcastSocket;
    }

    public void processResponse(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        System.out.println("Received broadcast packet from: " + packet.getAddress() + ":" + packet.getPort() + " (" + message + ")\n");

        if (message.startsWith("BOOTSTRAP_REQUEST")) { // BOOTSTRAP_REQUEST:ip=x.x.x.x:port=xxxx
            bootstrapRequestHandler(packet);
        }

        else if (message.startsWith("FILE_NOTIFICATION")) { // FILE_NOTIFICATION:event=x:filename=x:fileType=x:fileSize=x:chunkCount=x:hash=x
            fileNotificationHandler(packet);
        }
    }

    public void sendBroadcastRequest() throws IOException {
        System.out.println("Sending bootstrap request...");
        String message = "BOOTSTRAP_REQUEST:" +
                "ip=" + peer.getIp() +
                ":port=" + peer.getPort();

        byte[] data = message.getBytes();
        sendPacket(data, BROADCAST_IP, BROADCAST_PORT);
        System.out.println("Bootstrap request sent to: " + BROADCAST_IP + ":" + BROADCAST_PORT + " (" + message + ")");
    }

    public void sendFileNotification(String notify) throws IOException { // FILE_NOTIFICATION:event=x:filename=x:fileType=x:fileSize=x:chunkCount=x:hash=x

        String message = "FILE_NOTIFICATION:" + notify;

        byte[] data = message.getBytes();

        System.out.println("Sending file notification: (" + message + ")");

        sendPacket(data, BROADCAST_IP, BROADCAST_PORT);
    }

    private void bootstrapRequestHandler(DatagramPacket packet) throws IOException { // BOOTSTRAP_REQUEST:ip=x.x.x.x:port=xxxx
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        String[] parts = message.split(":");
        String[] peerInfo = parts[1].split("=");
        String ip = peerInfo[1].split(":")[0];
        int port = Integer.parseInt(parts[2].split("=")[1]);

        if (InetAddress.getByName(peer.getIp()).getHostAddress().equals(ip) && peer.getPort() == port) {
            return;
        }

        System.out.println("(BROADCAST) Added peer: " + ip + ":" + port);
        NetworkManager.getInstance().getUdpSocketHandler().sendFriendRequest(ip, port);
        PeerDTO newPeer = new PeerDTO(ip, port);
        peer.addPeer(newPeer);
    }

    private void fileNotificationHandler(DatagramPacket packet) throws IOException{ // FILE_NOTIFICATION:event=x:filename=x:fileType=x:fileSize=x:chunkCount=x:hash=x
        String message = new String(packet.getData(), 0, packet.getLength()).trim();

        String[] parts = message.split(":");
        String event = parts[1].split("=")[1];
        String filename = parts[2].split("=")[1];
        String fileType = parts[3].split("=")[1];
        long fileSize = Long.parseLong(parts[4].split("=")[1]);
        int chunkCount = Integer.parseInt(parts[5].split("=")[1]);
        String hash = parts[6].split("=")[1];
        String ip = packet.getAddress().getHostAddress();
        int port = packet.getPort();

        if (InetAddress.getByName(peer.getIp()).getHostAddress().equals(ip) && peer.getPort() == port) {
            return;
        }

        FileDTO fileDTO = new FileDTO(filename, fileType, fileSize, chunkCount, hash, new PeerDTO(ip, port));

        if (event.equals("ENTRY_CREATE")) {
            peer.addFiles(fileDTO.hash(), fileDTO);
        }
        else if (event.equals("ENTRY_DELETE")) {
            peer.removeFiles(fileDTO.hash());
        }

        peer.addPeer(fileDTO.owner());
    }

    public DatagramSocket getSocket() {
        return broadcastSocket;
    }
}
