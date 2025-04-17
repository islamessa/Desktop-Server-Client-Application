package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<byte[]> handler);

    boolean send(int connectionId, byte[] msg);

    void disconnect(int connectionId);

    ConcurrentHashMap<Integer, ConnectionHandler<byte[]>> getHandlers();
}

