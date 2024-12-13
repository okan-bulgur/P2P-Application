package src;

import src.dto.FileDTO;
import src.dto.PeerDTO;

import java.io.*;
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
    private final String DOWNLOADED_CHUNK_FOLDER = "downloaded";

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

    protected byte[] getChunkData(String filehash, int chunkIndex) throws IOException {
        File file = null;

        if (NetworkManager.getInstance().getPeer().getUploadedFiles().containsKey(filehash)) {
            FileDTO fileDTO = NetworkManager.getInstance().getPeer().getUploadedFiles().get(filehash);
            file = new File(rootFolder, fileDTO.filename());
        }
        else if (NetworkManager.getInstance().getPeer().hasChunk(filehash, chunkIndex)) {
            FileDTO fileDTO = NetworkManager.getInstance().getPeer().getFiles().get(filehash);
            String fullPath = destinationFolder + File.separator + DOWNLOADED_CHUNK_FOLDER + File.separator + fileDTO.filename();
            file = new File(fullPath);

            byte[] chunkData = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(chunkData);
            }
            return chunkData;

        }
        else if (NetworkManager.getInstance().getPeer().getDownloadedFiles().containsKey(filehash)) {
            FileDTO fileDTO = NetworkManager.getInstance().getPeer().getFiles().get(filehash);
            file = new File(destinationFolder, fileDTO.filename());
        } else {
            throw new IOException("File not found");
        }

        long fileLength = file.length();
        long chunkStart = (long) chunkIndex * CHUNK_SIZE;

        if (chunkStart >= fileLength) {
            throw new IOException("Invalid chunk index: " + chunkIndex);
        }

        int chunkSize = (int) Math.min(CHUNK_SIZE, fileLength - chunkStart);

        byte[] chunkData = new byte[chunkSize];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(chunkStart); // Chunk'ın başlangıç konumuna gidin
            raf.readFully(chunkData); // Chunk'ı okuyun
        }

        return chunkData;
    }

    protected void saveChunkData(String fileHash, String chunkHash, int chunkIndex, byte[] chunkData) throws IOException {
        try {
            String fullPath = destinationFolder + File.separator + DOWNLOADED_CHUNK_FOLDER + File.separator + fileHash + ".chunk_" + chunkIndex;
            File chunkFile  = new File(fullPath);

            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                fos.write(chunkData);
            }

            if (getHash(chunkFile).equals(chunkHash)) {
                System.out.println("Chunk " + chunkIndex + " saved successfully for file " + fileHash);
            } else {
                throw new IOException("Chunk hash mismatch: " + chunkFile.getName());
            }

            NetworkManager.getInstance().getPeer().addOwnedChunk(fileHash, chunkHash, chunkIndex);

            System.out.println("Saved chunk " + chunkIndex + " for file " + fileHash);
        } catch (IOException e) {
            System.err.println("Error saving chunk " + chunkIndex + ": " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void mergeChunk(String fileHash, int totalChunks) throws IOException {
        FileDTO fileDTO = NetworkManager.getInstance().getPeer().getFiles().get(fileHash);
        String fullPath = destinationFolder + File.separator + fileDTO.filename();
        File outputFile = new File(fullPath);

        try (FileOutputStream fos = new FileOutputStream(fullPath)) {
            for (int i = 0; i < totalChunks; i++) {

                String chunkPath = destinationFolder + File.separator + DOWNLOADED_CHUNK_FOLDER + File.separator + fileHash + ".chunk_" + i;
                File chunkFile = new File(chunkPath);

                if (!chunkFile.exists()) {
                    throw new IOException("Missing chunk file: " + chunkFile.getName());
                }

                try (FileInputStream fis = new FileInputStream(chunkFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("Merged chunk: " + chunkFile.getName());
            }

            if (getHash(outputFile).equals(fileHash)) {
                System.out.println("File hash matched: " + fileHash);
            } else {
                throw new IOException("File hash mismatch: " + outputFile.getName());
            }

            System.out.println("File merge completed: " + outputFile.getName());

        } catch (IOException e) {
            System.err.println("Error merging chunks: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
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

                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            for (FileDTO file : NetworkManager.getInstance().getPeer().getUploadedFiles().values()) {
                                if (file.filename().equals(event.context().toString())) {
                                    sendFileDelNotification(file, event.kind().name());
                                    NetworkManager.getInstance().getPeer().removeFiles(file.hash());
                                    return;
                                }
                            }
                        }

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

    private void sendFileDelNotification(FileDTO file, String event) throws Exception {
        String message = "event=" + event +
                ":filename=" + file.filename() +
                ":fileType=" + file.fileType() +
                ":fileSize=" + file.fileSize() +
                ":chunkCount=" + file.chunkCount() +
                ":hash=" + file.hash() +
                ":ip=" + file.owner().ip() +
                ":port=" + file.owner().port();

        NetworkManager.getInstance().sendFileNotification(message);
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

        FileDTO newFile = new FileDTO(file.getName(), getFileType(file), file.length(), Integer.parseInt(getChunkCount(file)), getHash(file), getOwner());
        NetworkManager.getInstance().getPeer().addUploadedFiles(getHash(file), newFile);
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
