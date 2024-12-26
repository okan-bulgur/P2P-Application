package app;

import app.dto.FileDTO;
import app.dto.PeerDTO;

import java.util.HashMap;
import java.util.HashSet;

public class Peer {
    private final String ip;
    private final int port;
    private final HashMap<String, String[]> ownedChunks; // hash of file, list of chunk's hash
    private final HashSet<PeerDTO> peers;
    private final HashMap<String, FileDTO> files;
    private final HashMap<String, FileDTO> uploadedFiles;
    private final HashMap<String, FileDTO> downloadedFiles;

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.ownedChunks = new HashMap<>();
        this.peers = new HashSet<>();
        this.files = new HashMap<>();
        this.uploadedFiles = new HashMap<>();
        this.downloadedFiles = new HashMap<>();
    }

    public void addOwnedChunk(String fileHash, String chunkHash, int chunkIndex) {
        ownedChunks.get(fileHash)[chunkIndex] = chunkHash;
    }

    public void removeOwnedChunk(String fileHash, int chunkIndex) {
        ownedChunks.get(fileHash)[chunkIndex] = "";

        for(String chunk : ownedChunks.get(fileHash)) {
            if(!chunk.isEmpty()) {
                return;
            }
        }
        ownedChunks.remove(fileHash);
    }

    public boolean hasChunk(String fileHash, int chunkIndex) {
        if (!ownedChunks.containsKey(fileHash)) {
            return false;
        }
        return !ownedChunks.get(fileHash)[chunkIndex].isEmpty();
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

    public void addDownloadedFiles(String hash, FileDTO fileDTO) {
        downloadedFiles.put(hash, fileDTO);
    }

    public void removeDownloadedFiles(String hash) {
        downloadedFiles.remove(hash);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public HashMap<String, String[]> getOwnedChunks() {
        return ownedChunks;
    }

    public String getChunkHash(String fileHash, int chunkIndex) {
        return ownedChunks.get(fileHash)[chunkIndex];
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

    public HashMap<String, FileDTO> getDownloadedFiles() {
        return downloadedFiles;
    }

    public String toString() {
        return ip + ":" + port;
    }
}
