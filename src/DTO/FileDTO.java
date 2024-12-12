package src.DTO;

public record FileDTO(String filename, String fileType, long fileSize, int chunkCount, String hash, PeerDTO owner) {

    public String toString() {
        return filename + ":" + fileType + ":" + fileSize + ":" + chunkCount + ":" + hash + ":" + owner;
    }
}
