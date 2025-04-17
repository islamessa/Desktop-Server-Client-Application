package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {
    private ConnectionsImpl<byte[]> connections;
    private int connectionId;
    private static ConcurrentHashMap<Integer, String> logged = new ConcurrentHashMap<>();
    private byte[] ACKPacket = { 0, 4, 0, 0 };
    private Vector<byte[]> packets_to_send=new Vector<>();
    private String current_file_name;
    private boolean terminate = false;
    private  ConnectionHandler<byte[]> handler;
    

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        this.connectionId = connectionId;
        this.connections = (ConnectionsImpl<byte[]>) connections;
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        byte[] opcode_bytes = new byte[2];
        opcode_bytes[0] = message[0];
        opcode_bytes[1] = message[1];
        short opcode = bytesToShort(opcode_bytes);

        if (opcode == 1) {// -------RRQ

            if (checkLogin()) {
                doRRQ(message);
            }
        } else if (opcode == 2) {// --------WRQ
            if (checkLogin())
                doWRQ(message);
        } else if (opcode == 3) {// ------DATA
            if(checkLogin())
            doDATA(message);
        } else if (opcode == 4) {// -------ACK
            if(checkLogin())
            doACK(message);
        } else if (opcode == 5) {// -------ERROR
            if(checkLogin())
            connections.send(connectionId, message);

        } else if (opcode == 6) {// ---------DIRQ
            if(checkLogin())
            doDIRQ(message);

        }
        if (opcode == 7) {// ------------LOGRQ
            String filename = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
            System.out.println(" id "+connectionId+" name "+filename+" logged size "+logged.size() );
            if(!logged.containsValue(filename)&&!logged.contains(connectionId)){
                logged.put(connectionId, filename);
                connections.connect(connectionId, handler);
                connections.send(connectionId, ACKPacket);

                }else 
              doError("User already logged in – Login username already connected.", 7);
                

        } else if (opcode == 8) {// --------DELRQ
            if(checkLogin())
            doDELRQ(message);

        } else if (opcode == 9) {// --------BCAST
            if(checkLogin())
            doBcast(message);

        } else if (opcode == 10) {// ----------DISC
            if(checkLogin())
            doDisc(connectionId);

        }

    }

    private void doError(String errmsg, int errCode) {
        // TODO Auto-generated method stub
        byte[] temp = new byte[4];
        temp[0] = 0;
        temp[1] = 5;
        temp[2] = 0;
        temp[3] = (byte) errCode;
        handler.send(mergeWithString(temp, errmsg));
    }

    private void doDisc(int id) {
        // TODO Auto-generated method stub
        if (connections.getHandlers().containsKey(id)) {
            logged.remove(id);
            connections.disconnect(id);
            terminate=true;
           // connections.send(id, ACKPacket); we send ack in the disconect
        } else {
            // send error
            doError("User not logged in – Any opcode received before Login completes.", 6);

        }

    }

    private void doBcast(byte[] message) {
        // TODO Auto-generated method stub
        connections.send(connectionId, message);

    }

    private void doDELRQ(byte[] message) {
        // TODO Auto-generated method stub
        String file_name = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
        String filePath = "Flies/" + file_name;
        File file = new File(filePath);
        if (!file.exists()) {
            doError("File not found", 1);
        } else {
            if (file.delete()) {// if delete was successful
                connections.send(connectionId, ACKPacket);
                byte[] bCast= new byte[3];
            bCast[0]=0;bCast[1]=9;bCast[2]=0;

            byte[] bytes_to_send = mergeWithString(bCast, file_name);
            for(Map.Entry<Integer, ConnectionHandler<byte[]>> tmp_handler : connections.getHandlers().entrySet()){
                connections.send(tmp_handler.getKey(), bytes_to_send);
            }
           
            } else {
                doError("Acces violatin -file cannot be deleted", 2);

              

            }

        }

    }

    private void doDIRQ(byte[] message) {
        packets_to_send.clear();
        byte[] list_bytes = listMyFiles().getBytes();

            int bytes_counter = 0; // starts from the data
            short packet_block_number = 0;

            while (bytes_counter < list_bytes.length) {
                byte[] curr_packet = new byte[512];
                short index = 0;

                while (index < 512 && bytes_counter < list_bytes.length) {
                    curr_packet[index] = list_bytes[bytes_counter];
                    bytes_counter++;
                    index++;
                }
                packet_block_number++;
                packets_to_send.add(createDataPacket(curr_packet, index, packet_block_number));

            }


            for (int i = 0; i< packets_to_send.size(); i++)
            connections.send(connectionId, packets_to_send.get(i));

        packets_to_send.clear();
        


      
   
    }

    // -------- to get the list of the files---------
    public String listMyFiles() {
        File files = new File("Flies");
        File[] list = files.listFiles();
        String res = "";
        for (int i = 0; i < list.length; i++) {
            res = res + list[i].getName() + '\0';
        }
        res=res.substring(0, res.length()-1); // to delete the last zero
        return res;
    }

    public static byte[] mergeWithString(byte[] bytes, String msg) {
        byte[] ans = null;
        try {
            byte[] input;
            input = msg.getBytes("UTF-8");
            ans = new byte[bytes.length + input.length + 1];
            for (int i = 0; i < ans.length; i++) {
                if (i < bytes.length) {

                    ans[i] = bytes[i];

                } else if (i >= bytes.length && i < ans.length - 1)
                    ans[i] = input[i - bytes.length];
                else if (i == bytes.length - 1)
                    ans[i] = 0;

            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ans;

    }

    public static byte[] convertShortArrayToBytes(short[] shortArray) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(shortArray.length * 2);
        for (short value : shortArray) {
            byteBuffer.putShort(value);
        }
        return byteBuffer.array();
    }

    public short bytesToShort(byte[] byteArr) {
        short result = (short) (((short) byteArr[0]) << 8 | (short) (byteArr[1])& 0xff);
        return result;
    }

    public boolean checkLogin() {
        if (!connections.findHandlerById(connectionId)) {
            doError("User not logged in – Any opcode received before Login completes.", 6);
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return terminate;
    }

    public void doACK(byte[] message) {

        byte[] ans = new byte[4];
        ans[0] = 0;
        ans[1] = 4;
        ans[2] = message[2];
        ans[3] = message[3];
        connections.send(connectionId, ans);

    }

    public void doDATA(byte[] message) {

        // take block number
        byte[] block_number = new byte[2];
        block_number[0] = message[4];
        block_number[1] = message[5];
        // short blockNUmber = bytesToShort(block_number);

        // save data
        byte[] data = Arrays.copyOfRange(message, 6, message.length);
        if (data.length <= 512) {
            String filePath = "Flies/" + current_file_name;

            try {
                FileOutputStream file_out = new FileOutputStream(filePath, true);
                file_out.write(data);
                file_out.close(); // maybe problem
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // send ack
            byte[] ACK_Packet = new byte[4];
            ACK_Packet[0] = 0;
            ACK_Packet[1] = 4;
            ACK_Packet[2] = block_number[0];
            ACK_Packet[3] = block_number[1];
            connections.send(connectionId, ACK_Packet);
        } else {
        }

    }

    public void doWRQ(byte[] message) {
        String file_name = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
        String filePath = "Flies/" + file_name;
        File file = new File(filePath);

        if (file.exists()) {
            // return error
            doError("File already exists - File name exists on WRQ.", 5);
        } else {
            // if there is no errors
            try {
                file.createNewFile();
                current_file_name = file_name;
                connections.send(connectionId, this.ACKPacket);
                
            } catch (IOException e) {
            }
            // send ack
            byte[] bCast= new byte[3];
            bCast[0]=0;bCast[1]=9;bCast[2]=1;

            byte[] bytes_to_send = mergeWithString(bCast, file_name);
            for(Map.Entry<Integer, ConnectionHandler<byte[]>> tmp_handler : connections.getHandlers().entrySet()){
                connections.send(tmp_handler.getKey(), bytes_to_send);
            }

        }

    }

    public void doRRQ(byte[] message) {
        String file_name = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
        String filePath = "Flies/" + file_name;

        File file = new File(filePath);

        if (file.exists()) {
            SplitFile(filePath);
            for (int i = 0; i < packets_to_send.size(); i++) {
                connections.send(connectionId, packets_to_send.get(i));
            }

            packets_to_send.clear();
        } else { // file doesnt exist
            doError("File not found – RRQ DELRQ of non-existing file.", 1);

        }

    }

    public void SplitFile(String file_path_str) {

        Path file_path = Paths.get(file_path_str);
        try {
            byte[] file_bytes = Files.readAllBytes(file_path);
            int bytes_counter = 0; // starts from the data
            short packet_block_number = 0;

            while (bytes_counter < file_bytes.length) {
                byte[] curr_packet = new byte[512];
                short index = 0;

                while (index < 512 && bytes_counter < file_bytes.length) {
                    curr_packet[index] = file_bytes[bytes_counter];
                    bytes_counter++;
                    index++;
                }
                packet_block_number++;
                packets_to_send.add(createDataPacket(curr_packet, index, packet_block_number));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public byte[] ShortToBytes(short number) {
        byte[] a_bytes = new byte[] { (byte) (number >> 8), (byte) (number & 0xff) };
        return a_bytes;
    }

    public <T> void set_Handler(BlockingConnectionHandler handler) {
        this.handler = handler;
    }

    public byte[] createDataPacket(byte[] data, short length, short block_number) {
        byte[] data_packet = new byte[length + 6];

        byte[] temp_data_packet = new byte[6];
        // save opcode
        temp_data_packet[0] = 0;
        temp_data_packet[1] = 3;

        // save packet size
        byte[] temp_bytes = ShortToBytes(length);
        temp_data_packet[2] = temp_bytes[0];
        temp_data_packet[3] = temp_bytes[1];

        // save blocking number;
        temp_bytes = ShortToBytes(block_number);
        temp_data_packet[4] = temp_bytes[0];
        temp_data_packet[5] = temp_bytes[1];

        // save data
        System.arraycopy(temp_data_packet, 0, data_packet, 0, temp_data_packet.length);
        System.arraycopy(data, 0, data_packet, temp_data_packet.length, length);

        return data_packet;
    }

}
