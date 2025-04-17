package bgu.spl.net.srv;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

//---------------------- this class by abed---------------------------------------------
public class ConnectionsImpl<T> implements Connections<T> {
private ConcurrentHashMap<Integer, ConnectionHandler<byte[]>> handlers;



    public ConcurrentHashMap<Integer, ConnectionHandler<byte[]>> getHandlers(){
        return handlers;
    }

    
    
	public ConnectionsImpl(){
		handlers = new ConcurrentHashMap<Integer, ConnectionHandler<byte[]>>();
	}

    public boolean findHandlerById(int connecionId){
        return getHandlers().containsKey(connecionId);


    }


    @Override
    public void connect(int connectionId, ConnectionHandler<byte[]> handler) {
        // TODO Auto-generated method stub
        handlers.put(connectionId, handler);
       


        
    }

    @Override
    public synchronized boolean send(int connectionId, byte[] msg) {
        // TODO Auto-generated method stub
        if (handlers.containsKey(connectionId)) {
			handlers.get(connectionId).send(msg);
			return true;
		}
		return false;

    }

    @Override
    public void disconnect(int connectionId) {
        // TODO Auto-generated method stub
        try {
            byte[] ACK={0,4,0,0};
            send(connectionId, ACK);
            handlers.get(connectionId).close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        handlers.remove(connectionId);


    }

    public void broadcast(byte[] msg) {
		for (Entry<Integer, ConnectionHandler<byte[]>> entry : handlers.entrySet()){
			if (handlers.containsKey(entry.getValue()))
				handlers.get(entry.getValue()).send(msg);
		}
	}
    
}
