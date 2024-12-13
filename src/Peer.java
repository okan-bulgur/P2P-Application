package src;

import src.dto.FileDTO;
import src.dto.PeerDTO;

import java.util.HashMap;
import java.util.HashSet;

public class Peer {
    private final String ip;
    private final int port;
    private HashSet<String> ownedChunks;
    private HashSet<PeerDTO> peers;
    private HashMap<String, FileDTO> files;
    private HashMap<String, FileDTO> uploadedFiles;

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.ownedChunks = new HashSet<>();
        this.peers = new HashSet<>();
        this.files = new HashMap<>();
        this.uploadedFiles = new HashMap<>();
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

    public void addFiles(String hash, FileDTO fileDTO) {
        files.put(hash, fileDTO);
    }

    public void removeFiles(String hash) {
        files.remove(hash);
    }

    public void addUploadedFiles(String hash, FileDTO fileDTO) {
        uploadedFiles.put(hash, fileDTO);
    }

    public void removeUploadedFiles(String hash) {
        uploadedFiles.remove(hash);
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

    public HashMap<String, FileDTO> getFiles() {
        return files;
    }

    public HashMap<String, FileDTO> getUploadedFiles() {
        return uploadedFiles;
    }

    public String toString() {
        return ip + ":" + port;
    }
}
