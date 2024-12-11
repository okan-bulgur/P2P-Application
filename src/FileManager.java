package src;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileManager {
    private static FileManager instance;
    private File rootFolder;
    private File destinationFolder;

    public static FileManager getInstance() {
        if(instance == null) {
            instance = new FileManager();
        }
        return instance;
    }

    public void setRootFolder(File root) {
        this.rootFolder = root;

        for (File file : listSharedFiles()) {
            try {
                NetworkManager.getInstance().sendFileNotification("event=ENTRY_CREATE:filename=" + file.getName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        new Thread(this::watchSharedFolder).start();
    }

    public void setDestinationFolder(File dest) {
        this.destinationFolder = dest;
    }

    public List<File> listSharedFiles() {
        if(rootFolder == null || !rootFolder.exists()) return Collections.emptyList();
        File[] files = rootFolder.listFiles();
        if(files == null) return Collections.emptyList();
        return Arrays.asList(files);
    }

    public File getDestinationFolder() {
        return destinationFolder;
    }

    public File getRootFolder() {
        return rootFolder;
    }

    public void assembleChunks(List<File> chunks, File outputFile) throws IOException {
        try(FileOutputStream fos = new FileOutputStream(outputFile)) {

            chunks.sort((f1, f2) -> { // file:chunk=1, file:chunk=2, file:chunk=3, ...
                String[] f1Parts = f1.getName().split(":chunk=");
                String[] f2Parts = f2.getName().split(":chunk=");
                return Integer.compare(Integer.parseInt(f1Parts[1]), Integer.parseInt(f2Parts[1]));
            });

            System.out.println("Sorted chunks: " + chunks);

            for(File chunk : chunks) {
                Files.copy(chunk.toPath(), fos);
            }
        }
    }

    private void watchSharedFolder() {
        try (WatchService watchService = rootFolder.toPath().getFileSystem().newWatchService()) {
            rootFolder.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take();

                key.pollEvents().forEach(event -> {
                    String message = String.format("event=%s:filename=%s", event.kind(), event.context());
                    System.out.println(message);
                    try {
                        NetworkManager.getInstance().sendFileNotification(message);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
