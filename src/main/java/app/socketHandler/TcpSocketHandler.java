package app.socketHandler;

import app.Peer;
import app.manager.FileManager;
import app.manager.NetworkManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TcpSocketHandler {

    final private Peer peer = NetworkManager.getInstance().getPeer();

    public void processResponse(Socket clientSocket) throws Exception {
        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());

        chunkResultHandler(dis);

    }

    public void sendChunk(String fileHash, int index, String ip, int port) throws IOException {
        byte[] chunkData = FileManager.getInstance().getChunkData(fileHash, index);
        int chunkSize = chunkData.length;
        String chunkHash = peer.getChunkHash(fileHash, index);

        try (Socket socket = new Socket(ip, port)) {
            socket.setSendBufferSize(chunkSize + 1024);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF("CHUNK_RESULT");
            dos.writeUTF(fileHash);
            dos.writeInt(index);
            dos.writeUTF(chunkHash);
            dos.writeInt(chunkSize);
            dos.write(chunkData);

            System.out.println("Sent chunk result to: " + ip + ":" + port + " (" + fileHash + ", " + index + ", " + chunkHash + ", " + chunkSize + ")");
            dos.flush();
            dos.close();

        } catch (IOException e) {
            System.err.println("Failed to send chunk result");
        }
    }

    private void chunkResultHandler(DataInputStream dis) throws Exception {
        String header = dis.readUTF();
        if (!header.equals("CHUNK_RESULT")) {
            System.err.println("Invalid header: " + header);
            return;
        }
        String fileHash = dis.readUTF();
        int index = dis.readInt();
        String chunkHash = dis.readUTF();
        int chunkSize = dis.readInt();
        byte[] chunkData = new byte[chunkSize];
        dis.readFully(chunkData);

        if(peer.hasChunk(fileHash, index)) {
            System.out.println("Chunk " + index + " already owned. Skipping...");
            return;
        }

        if (!chunkHash.equals(FileManager.getInstance().getHashOfData(chunkData))) {
            System.err.println("Invalid chunk data");
            return;
        }

        FileManager.getInstance().saveChunkData(fileHash, chunkHash, index, chunkData);

        System.out.println("Received "+ index + ". chunk result");
    }
}
