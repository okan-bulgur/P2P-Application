package src.manager;

import src.dto.FileDTO;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

public class DownloadManager {
    private static DownloadManager instance;

    private final ExecutorService downloadExecutor;

    public static DownloadManager getInstance() {
        if(instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }

    public DownloadManager() {
        this.downloadExecutor = Executors.newFixedThreadPool(5);
    }

    public void downloadFile(FileDTO file){
        System.out.println("\nDownloading file: " + file);

        int totalChunks = file.chunkCount();

        for(int i = 0; i < totalChunks; i++) {
            int index = i;
            if (NetworkManager.getInstance().getPeer().hasChunk(file.hash(), index)) {
                System.out.println("\nChunk " + index + " already owned. Skipping...");
                continue;
            }
            downloadExecutor.submit(() -> downloadChunk(file, index));
        }

        while (true){
            int count = 0;
            for (int i = 0; i < totalChunks; i++) {
                if (NetworkManager.getInstance().getPeer().hasChunk(file.hash(), i)) {
                    count++;
                }
            }

            if (count == totalChunks) {
                break;
            }
        }

        System.out.println("\nAll chunks downloaded for file: " + file);

        try {
            FileManager.getInstance().mergeChunk(file.hash(), totalChunks);
            NetworkManager.getInstance().getPeer().addDownloadedFiles(file.hash(), file);
            deleteChunkFiles(file.hash());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadChunk(FileDTO file, int index) {
        try {
            int maxRetries = 3;
            int retryDelay = 1000;
            int timeout = 5000;

            boolean chunkReceived = false;
            int attempts = 0;

            while (!chunkReceived && attempts < maxRetries) {
                attempts++;
                System.out.println("\nRequesting chunk " + index + " (Attempt " + attempts + ")");

                NetworkManager.getInstance().getUdpSocketHandler().sendChunkRequest(file.hash(), index);

                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout) {
                    if (NetworkManager.getInstance().getPeer().hasChunk(file.hash(), index)) {
                        chunkReceived = true;
                        break;
                    }
                    Thread.sleep(250);
                }

                if (!chunkReceived) {
                    System.out.println("Chunk " + index + " not received. Retrying...");
                    Thread.sleep(retryDelay);
                }
            }

            if (!chunkReceived) {
                throw new RuntimeException("Failed to receive chunk " + index + " after " + maxRetries + " attempts.");
            }
        } catch (IOException e) {
            throw new RuntimeException("IO error while requesting chunk " + index, e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted while requesting chunk " + index, e);
        }

    }

    private void deleteChunkFiles(String fileHash) {
        String dirPath = FileManager.getInstance().getDestinationFolder() + File.separator + FileManager.getInstance().CHUNK_FOLDER;
        File dir = new File(dirPath);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles((_, name) -> name.startsWith(fileHash));

        if (files == null) {
            return;
        }

        for (File file : files) {
            file.delete();
        }

    }
}


