package src;

import javax.swing.*;
import java.util.HashSet;

public class PeerConnectionManager {

    private static PeerConnectionManager instance;

    public PeerConnectionManager() {

    }

    public static synchronized PeerConnectionManager getInstance() {
        if (instance == null) {
            instance = new PeerConnectionManager();
        }
        return instance;
    }

    public PeerDTO searchChunkOwner(Peer searcher, String chunk, HashSet<Peer> visited, int ttl) {
        /*
        Peer owner = null;

        if (ttl <= 0) {
            return owner;
        }

        for (PeerDTO peer : searcher.getPeers()) {
            if (visited.contains(peer)) {
                continue;
            }

            if (peer.hasChunk(chunk)) {
                return peer;
            }

            else {
                owner = searchChunkOwner(peer, chunk, searcher.getPeers(), ttl - 1);
                if (owner != null) {
                    searcher.addPeer(owner);
                    return owner;
                }
            }
        }
        return owner;
        */
        return null;
    }

}
