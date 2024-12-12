package src;

import src.DTO.PeerDTO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileManager {
    final int CHUNK_SIZE = 256 * 1024;

    private static FileManager instance;
    private File rootFolder;
    private File destinationFolder;

    public static FileManager getInstance() {
        if(instance == null) {
            instance = new FileManager();
        }
        return instance;
    }

    public List<File> listSharedFiles() {
        if(rootFolder == null || !rootFolder.exists()) return Collections.emptyList();
        File[] files = rootFolder.listFiles();
        if(files == null) return Collections.emptyList();
        return Arrays.asList(files);
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

                    try {
                        Path fullPath = rootFolder.toPath().resolve(event.context().toString());

                        File file = fullPath.toFile();

                        sendFileNotification(file, event.kind().name());


                    } catch (Exception e) {
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

    private void sendFileNotification(File file, String event) throws Exception {

        String message = "event=" + event +
                ":filename=" + getFileName(file) +
                ":fileType=" + getFileType(file) +
                ":fileSize=" + getFileSize(file) +
                ":chunkCount=" + getChunkCount(file) +
                ":hash=" + getHash(file) +
                ":ip=" + getOwner().ip() +
                ":port=" + getOwner().port();

        NetworkManager.getInstance().sendFileNotification(message);
    }

    private String getFileName(File file) {
        return file.getName();
    }

    private String getFileType(File file) {
        String filename = file.getName();
        int lastDotIndex = filename.lastIndexOf('.');
        if(lastDotIndex == -1) return "unknown";
        return filename.substring(lastDotIndex + 1);
    }

    private String getFileSize(File file) {
        return String.valueOf(file.length());
    }

    private String getChunkCount(File file) {
        if (file.length() == 0) {
            return "0";
        }
        return String.valueOf((int) Math.ceil((double) file.length() / CHUNK_SIZE));
    }

    private String getHash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes  = digest.digest();
        StringBuilder hashString = new StringBuilder();

        for (byte b : hashBytes ) {
            hashString.append(String.format("%02x", b));
        }
        return hashString.toString();
    }

    private PeerDTO getOwner() {
        return NetworkManager.getInstance().getPeerDTO();
    }

    public File getDestinationFolder() {
        return destinationFolder;
    }

    public File getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(File root) {
        this.rootFolder = root;

        try {
            for (File file : listSharedFiles()) {
                sendFileNotification(file, "ENTRY_CREATE");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        new Thread(this::watchSharedFolder).start();
    }

    public void setDestinationFolder(File dest) {
        this.destinationFolder = dest;
    }
}
