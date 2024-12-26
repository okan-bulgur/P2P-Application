package app.dto;

import java.util.Objects;

public record FileDTO(String filename, String fileType, long fileSize, int chunkCount, String hash, PeerDTO owner) {

    public String toString() {
        double sizeInKB = fileSize / 1024.0;
        return String.format("%s (%.2f KB)", filename, sizeInKB);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FileDTO fileDTO = (FileDTO) obj;
        return Objects.equals(hash, fileDTO.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

}