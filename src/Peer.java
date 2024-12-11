package src;

import java.util.HashMap;
import java.util.HashSet;

public class Peer {
    private final String ip;
    private final int port;
    private HashSet<String> ownedChunks;
    private HashSet<PeerDTO> peers;
    private HashMap<String, PeerDTO> fileToPeer;

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.ownedChunks = new HashSet<>();
        this.peers = new HashSet<>();
        this.fileToPeer = new HashMap<>();

        if (port == 5004) {
            ownedChunks.add("\\Users\\Okan\\OneDrive\\Belgeler\\CV\\okan_bulgur_cv_1.pdf");
        }
    }

    public void addOwnedChunk(String chunk) {
        ownedChunks.add(chunk);
    }

    public void removeOwnedChunk(String chunk) {
        ownedChunks.remove(chunk);
    }

    public boolean hasChunk(String chunk) {
        return ownedChunks.contains(chunk);
    }

    public void addPeer(PeerDTO peer) {
        peers.add(peer);
    }

    public void removePeer(PeerDTO peer) {
        peers.remove(peer);
    }

    public boolean hasPeer(PeerDTO peer) {
        return peers.contains(peer);
    }

    public void addFileToPeer(String file, PeerDTO peer) {
        fileToPeer.put(file, peer);
    }

    public void removeFileToPeer(String file) {
        fileToPeer.remove(file);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public HashSet<String> getOwnedChunks() {
        return ownedChunks;
    }

    public HashSet<PeerDTO> getPeers() {
        return peers;
    }

    public HashMap<String, PeerDTO> getFileToPeer() {
        return fileToPeer;
    }

    public String toString() {
        return ip + ":" + port;
    }
}
