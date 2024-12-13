package src;

import src.dto.FileDTO;
import src.dto.PeerDTO;

import java.io.File;
import java.util.ArrayList;
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
    }


}

