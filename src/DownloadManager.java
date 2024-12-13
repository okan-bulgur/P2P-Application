package src;

import src.dto.FileDTO;
import src.dto.PeerDTO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class DownloadManager {
    private static DownloadManager instance;

    private final List<ActiveDownload> activeDownloads = new ArrayList<>();
    private final ExecutorService downloadExecutor;

    public static DownloadManager getInstance() {
        if(instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }

    public DownloadManager() {
        // Paralel indirme için bir thread pool oluştur
        this.downloadExecutor = Executors.newFixedThreadPool(5); // Aynı anda en fazla 5 dosya indirimi
    }

    protected void downloadFile(FileDTO file){
        System.out.println("Downloading file: " + file);
        System.out.println(file.hash());

        int totalChunks = file.chunkCount();

        String[] chunks = new String[totalChunks]; // Yeni bir String array oluştur
        Arrays.fill(chunks, ""); // Tüm elemanları "" ile doldur
        NetworkManager.getInstance().getPeer().getOwnedChunks().put(file.hash(), chunks);

        for(int i = 0; i < totalChunks; i++) {
            int index = i;
            if (NetworkManager.getInstance().getPeer().hasChunk(file.hash(), index)) {
                System.out.println("Chunk " + index + " already owned. Skipping...");
                continue;
            }
            downloadExecutor.submit(() -> downloadChunk(file, index));
        }

        for (int i = 0; i < totalChunks; i++) {
            if (NetworkManager.getInstance().getPeer().getOwnedChunks().get(file.hash())[i].isEmpty()) {
                throw new RuntimeException("Failed to download chunk " + i);
            }
        }

        System.out.println("All chunks downloaded for file: " + file);

        try {
            FileManager.getInstance().mergeChunk(file.hash(), totalChunks);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void downloadChunk(FileDTO file, int index) {
        try {
            int maxRetries = 3; // Maksimum tekrar sayısı
            int retryDelay = 1000; // Her istek arasında bekleme süresi (ms)
            int timeout = 5000; // Chunk bekleme süresi (ms)

            boolean chunkReceived = false;
            int attempts = 0;

            while (!chunkReceived && attempts < maxRetries) {
                attempts++;
                System.out.println("Requesting chunk " + index + " (Attempt " + attempts + ")");

                NetworkManager.getInstance().sendChunkRequest(file.hash(), index);

                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout) {
                    if (!NetworkManager.getInstance().getPeer().getOwnedChunks().get(file.hash())[index].isEmpty()) {
                        chunkReceived = true;
                        break;
                    }
                    Thread.sleep(500);
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
}


