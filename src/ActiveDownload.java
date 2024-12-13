package src;

import java.io.File;

public class ActiveDownload {
    private final String fileName;
    private final File destinationFolder;
    private int totalChunks;
    private ChunkStatus[] chunkStatuses;

    public ActiveDownload(String fileName, File destinationFolder, int totalChunks) {
        this.fileName = fileName;
        this.destinationFolder = destinationFolder;
        this.totalChunks = totalChunks;
    }

    public String getFileName() { return fileName; }
    public File getDestinationFolder() { return destinationFolder; }

    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }

    public ChunkStatus[] getChunkStatuses() { return chunkStatuses; }
    public void setChunkStatuses(int chunkIndex, ChunkStatus status) { this.chunkStatuses[chunkIndex] = status; }

    public int getCompletedChunksCount() {
        int completedChunks = 0;
        for (ChunkStatus status : chunkStatuses) {
            if (status == ChunkStatus.COMPLETED) {
                completedChunks++;
            }
        }
        return completedChunks;
    }

}