package app.dto;

public record PeerDTO(String ip, int port) {

    public String toString() {
        return ip + ":" + port;
    }
}
