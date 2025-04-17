// package bgu.spl.net.impl.tftp;

// import java.nio.charset.StandardCharsets;
// import java.util.Arrays;
// import java.util.LinkedList;
// import java.util.List;
// //import java.util.concurrent.ConcurrentHashMap;

// import bgu.spl.net.api.BidiMessagingProtocol;
// // import bgu.spl.net.srv.BlockingConnectionHandler;
// import bgu.spl.net.srv.ConnectionHandler;
// import bgu.spl.net.srv.Connections;
// //import bgu.spl.net.impl.tftp.FolderHandler;

// public class TftpProtocol<T> implements BidiMessagingProtocol<byte[]> {

//     // private boolean isFirst;
//     private Connections<byte[]> connectMap;
//     private int clientId;
//     private ConnectionHandler<byte[]> handler;
//     private byte[] ansMsg = new byte[1 << 10];
//     private byte[] ACK;
//     // private int blockCounter=0;
//     private boolean coonect;
//     private int len;
//     // private LinkedList<String> userNames = new LinkedList<>();
//     private String fileTOupload;
//     private short currblockingNumber = 1;
//     private int blockingNumber = 0;
//     private String personalName="";
//     private byte[] DATA;
//     private int dataSent = 0;
//     private boolean terminate=false;
//     // private ConcurrentHashMap <String,Integer> userNames= new ConcurrentHashMap
//     // <>();

//     @Override
//     public void start(int connectionId, Connections<byte[]> connections) {
//         // TODO implement this
//         connectMap = connections;
//         clientId = connectionId;
//         len = 0;
//         short[] ackShort = { 0, 4, 0, 0 };
//         ACK = shortsToBytes(ackShort);

//     }

//     // int offset = 2; // Start at "W" in "World"
//     // int length = message.length-offset; // Length of "World"
//     // String name = new String(message, offset, length, StandardCharsets.UTF_8);

//     @Override
//     public void process(byte[] message) {

//         short opCode = bytesToShort(message);

//         if (opCode > 10) {
//             short[] firstBytes = { 0, 5, 0, 4 };
//             ansMsg = shortsToBytes(firstBytes);
//             String ErrMsg = "Unknown Opcode";
//             ansMsg = convertToByte(ErrMsg, ansMsg);// encode
//             connectMap.send(clientId, ansMsg);

//         }
//         if (opCode == 1 || opCode == 2 || opCode == 8)// if the name is illegal we should return the lower error(0)
//         {
//             if (!legalName(message)) {
//                 short[] firstBytes = { 0, 5, 0, 0 };// the lower error
//                 ansMsg = shortsToBytes(firstBytes);
//                 String ErrMsg = "Illegal name";
//                 ansMsg = convertToByte(ErrMsg, ansMsg);// encode
//                 connectMap.send(clientId, ansMsg);
//             }
//         }

//         if (opCode == 1 || opCode == 8) {
//             String filename = converToString(message);
//             if (!FolderHandler.contsinsName(filename)) {
//                 short[] firstBytes = { 0, 5, 0, 1 };// the lower error
//                 ansMsg = shortsToBytes(firstBytes);
//                 String ErrMsg = "File not found RRQ DELRQ of non-existing file.";
//                 ansMsg = convertToByte(ErrMsg, ansMsg);// encode
//                 connectMap.send(clientId, ansMsg);
//             }

//         } else if (opCode == 2) {
//             String filename = converToString(message);
//             if (FolderHandler.contsinsName(filename)) {
//                 short[] firstBytes = { 0, 5, 0, 5 };// the lower error
//                 ansMsg = shortsToBytes(firstBytes);
//                 String ErrMsg = "File already exists.";
//                 ansMsg = convertToByte(ErrMsg, ansMsg);// encode
//                 connectMap.send(clientId, ansMsg);
//             }

//         } else if (opCode == 7) {
//             processLOGRQ(message);
//         } else if (coonect) {
//             if (opCode == 10) {
//                 processDISC(message);
//             } else if (opCode == 1) {
//                 processRRQ(message);
//             }

//             else if (opCode == 2) {
//                 processWRQ(message);///// not completed

//             } else if (opCode == 3) {
//                 processDATA(message);
//             }

//             else if (opCode == 8) {
//                 processDELRQ(message);

//             } else if (opCode == 6) {
//                 processDIRQ(message);
//             } else if (opCode == 4) {
//                 processACK(message);
//             }

//         } else {
//             // in case that the user isnt logged in
//             short[] firstBytes = { 0, 5, 0, 6 };
//             ansMsg = shortsToBytes(firstBytes);
//             String ErrMsg = "User not logged in";
//             ansMsg = convertToByte(ErrMsg, ansMsg);// encode
//             connectMap.send(clientId, ansMsg);

//         }

//     }

//     public short bytesToShort(byte[] byteArr) {
//         short result = (short) ((byteArr[0] & 0x00ff) << 8);
//         result += (short) (byteArr[1] & 0x00ff);
//         return result;

//     }

//     public byte[] shortsToBytes(short[] values) {
//         byte[] result = new byte[values.length * 2];
//         for (int i = 0; i < values.length; i++) {
//             result[i * 2] = (byte) (values[i] >> 8); // Higher-order byte
//             result[i * 2 + 1] = (byte) values[i];
//             len = len + 2; // Lower-order byte
//         }
//         return result;
//     }

//     public void processACK(byte[] message) {
//         if (message[4] < blockingNumber)
//             processSendDATA();
//     }

//     public void processLOGRQ(byte[] message) {

//         if (!(connectMap.connect(clientId, handler)) && !checkName(message)) {/// was not connected
//             coonect = true;
//             ansMsg = ACK;
//             connectMap.send(clientId, ansMsg);

//         } else {
//             short[] firstBytes = { 0, 5, 0, 7 };
//             ansMsg = shortsToBytes(firstBytes);
//             String ErrMsg = "User already logged in";
//             ansMsg = convertToByte(ErrMsg, ansMsg);// encode
//             connectMap.send(clientId, ansMsg);

//         }
//     }

//     public void processDISC(byte[] message) {
//         coonect = false;
//         connectMap.removeFromList(clientId);
//         // userNames.remove(personalName);
//         ansMsg = ACK;
//         connectMap.send(clientId, ansMsg);
//         connectMap.disconnect(clientId);
//         terminate=true;

//     }

//     public void processDATA(byte[] message) {
//  // are we sure that the client after wrq will only send data>?
//         FolderHandler.saveFile(Arrays.copyOfRange(message, 6, message.length), fileTOupload);
//     }

//     public void processRRQ(byte[] message) {
//         // short blockCounter=1;
//         String filename = converToString(message);
//         DATA = FolderHandler.readFile(filename);//the file that the client wants to download
//         if(DATA.length % 512!=0){
//         blockingNumber = DATA.length/512 +1;}
//         else{blockingNumber = DATA.length/512 ; }
//         processSendDATA();

//     }

//     public void processWRQ(byte[] message) {
//         fileTOupload = converToString(message);//the file i will download the recived packets in it 
//         FolderHandler. createFile(fileTOupload);
//         connectMap.send(clientId, ACK);
//         processBCASTadd(fileTOupload);

//     }

//     public void processDIRQ(byte[] message) {
//         LinkedList<String> names = FolderHandler.listOfFiles();
//         DATA = listStringsToByteArray(names);// convert the list to array of bytes
//         blockingNumber = DATA.length % 512;
//         processSendDATA();

//     }

//     public byte[] listStringsToByteArray(List<String> strings) {// after each name we should adding 0
//         // Joining all strings with a 0 character as a separator
//         String combined = String.join("0", strings);
//         // Converting the combined string to a byte array using UTF-8 encoding
//         byte[] ans = combined.getBytes(StandardCharsets.UTF_8);
//         return ans;

//     }

//     public void processDELRQ(byte[] message) {
//         String filename = converToString(message);
//         FolderHandler.deleteFile(filename);// delting the file from folder
//         connectMap.send(clientId, ACK);// sending ack to the client
//         processBCASTdelete(filename);// notify all the clients that this file has been deleted

//     }

//     public void processSendDATA() {

//         short dataSize = (short) DATA.length;// the DATA
//         short[] firstBytes = { 0, 3, 0, dataSize, 0, currblockingNumber };/// 01 is the block number
//         ansMsg = shortsToBytes(firstBytes);
//         ansMsg = unionArrays(DATA, ansMsg);//// the

//         int size = ansMsg.length;

//         if (dataSize <= 512)
//             connectMap.send(clientId, ansMsg);
//         else
//             while (dataSent < size) {
//                 byte[] tempMsg = Arrays.copyOfRange(ansMsg, dataSent, Math.min(512 + dataSent, size));// to make sure
//                                                                                                       // that the index
//                                                                                                       // is legal
//                 tempMsg[3] = (byte) tempMsg.length;// may the datasize take more bytes than the tempmsg length
//                 dataSent = dataSent + 512;
//                 tempMsg[5] = (byte) blockingNumber;
//                 currblockingNumber++;
//                 connectMap.send(clientId, tempMsg);

//             }
        
//     }

//     public void processBCASTdelete(String filenName) {
//         short[] firstBytes = { 0, 9, 0 };// deleted
//         ansMsg = shortsToBytes(firstBytes);
//         String ErrMsg = filenName + " has been deleted ";
//         ansMsg = convertToByte(ErrMsg, ansMsg);// encode
//         // connectMap.sendToAll(ansMsg);
//         Connections.sendToAll(ansMsg);

//     }

//     public void processBCASTadd(String filenName) {
//         short[] firstBytes = { 0, 9, 1 };// added
//         ansMsg = shortsToBytes(firstBytes);
//         String ErrMsg = filenName + " has been added ";
//         ansMsg = convertToByte(ErrMsg, ansMsg);// encode
//         // connectMap.sendToAll(ansMsg);
//         Connections.sendToAll(ansMsg);

//     }

//     // private void pushByte(byte nextByte) {
//     //     if (len >= ansMsg.length) {
//     //         ansMsg = Arrays.copyOf(ansMsg, len * 2);
//     //     }

//     //     ansMsg[len++] = nextByte;
//     // }

//     @Override
//     public boolean shouldTerminate() {
//        return terminate;
//     }

//     // public void getConnectionHnadler(ConnectionHandler<T> hand){
//     // this.handler= hand;

//     // }

//     public boolean checkName(byte[] name) {// check if the name already exists
//         String user = converToString(name);
 
//         if (connectMap.containsInList(user))
//             return false;
//         else {
//             personalName = user;
//             connectMap.addToList(clientId,user);
//             // userNames.add(user);

//             return true;
//         }

//     }

//     public byte[] convertToByte(String s, byte[] msg) {
//         byte[] temp = s.getBytes(); // the message as a byte array
//         byte[] newAns = new byte[temp.length + msg.length + 1];// (+1)for the termenating 0
//         System.arraycopy(temp, 0, newAns, 0, temp.length);// adding the content of the two arrays to newans
//         System.arraycopy(msg, 0, newAns, temp.length, msg.length);
//         newAns[newAns.length - 1] = 0;
//         return newAns;

//     }

//     public byte[] unionArrays(byte[] msg, byte[] data) {
//         byte[] newAns = new byte[data.length + data.length];// (+1)for the termenating 0
//         System.arraycopy(data, 0, newAns, 0, data.length);// adding the content of the two arrays to newans
//         System.arraycopy(msg, 0, newAns, msg.length, msg.length);
//         return newAns;

//     }

//     public String converToString(byte[] name) {
//         int offset = 3;
//         int length = name.length - offset; // name length
//         String s = new String(name, offset, length - 1, StandardCharsets.UTF_8);// extract the name fron index 2 to
//                                                                                 // length-2
//         return s;

//     }

//     public boolean legalName(byte[] name) {
//         for (int i = 0; i < name.length; i++) {
//             if (name[i] == 0) {
//                 return false;
//             }
//         }
//         return true;
//     }

//     @Override
//     public void setConnectionHnadler(TftpBlockingConnectionHandler blockingConnectionHandler) {
//         // TODO Auto-generated method stub

//         this.handler = blockingConnectionHandler;

//     }
// }
