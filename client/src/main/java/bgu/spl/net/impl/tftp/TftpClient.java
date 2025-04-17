package bgu.spl.net.impl.tftp;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;

import javax.print.DocFlavor.STRING;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

public class TftpClient {

    private static boolean isRRQ = false;
    private static boolean isDIRQ = false;
    private static boolean isWRQ = false;
    private static boolean isLOGRQ = false;
    private static boolean logged = false;

    private static boolean isDISC=false;
    private static MessageEncoderDecoder<byte[]> encdec;
    private static MessagingProtocol<byte[]> protocol;
   
    private static BufferedInputStream in;
    private static BufferedOutputStream out; 
    private static Object lock = new Object();
    private static String name_to_print="";



    public static String curr_filename = "";
    private  static Vector<byte[]> packets_to_send=new Vector<>();
    private static boolean isDELRQ=false;;

    public static void doWRQData(){

        String filePath =  curr_filename;

        File file = new File(filePath);

        if (file.exists()) {
            SplitFile(filePath);
            for (int i = 0; i < packets_to_send.size(); i++) {
                try {
                    out.write(encdec.encode(packets_to_send.get(i)));
                    byte[] temp =new byte[2];
                    temp[0]=packets_to_send.get(i)[4];  temp[1]=packets_to_send.get(i)[5];

                    System.out.println("ACK "+bytesToShort(temp));
                    out.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            packets_to_send.clear();
        } else { // file doesnt exist
            System.out.println("File not found");

        }
    }
     public static void SplitFile(String file_path_str) {

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
    public static byte[] createDataPacket(byte[] data, short length, short block_number) {
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

    public static byte[] ShortToBytes(short number) {
        byte[] a_bytes = new byte[] { (byte) (number >> 8), (byte) (number & 0xff) };
        return a_bytes;
    }
    public static void doDATA(byte[] message) {

        // take block number
        byte[] block_number = new byte[2];
        block_number[0] = message[4];
        block_number[1] = message[5];
        // short blockNUmber = bytesToShort(block_number);

        // save data
        byte[] data = Arrays.copyOfRange(message, 6, message.length);
        if (data.length <= 512) {
            String filePath = curr_filename;

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
            
        } if(data.length<512){
        System.out.println("File transfer complete");
        if(isRRQ){
            isRRQ=false;

        }
        if(isDIRQ){
            isDIRQ=false;
        }
        curr_filename="";

        }
    }
    public static void doDIRCData(byte[] message){
        
        Vector<Byte> name_in_bytes = new Vector<>();
        byte[] size_in_bytes = new byte[2];
        size_in_bytes[0] = message[2];
        size_in_bytes[1] = message[3];

        short message_size = bytesToShort(size_in_bytes); 

        for(int i = 6 ; i < message.length ; i++){ // 
            if(message[i]!=0  ){
                name_in_bytes.add(message[i]);
            }    
            if(message[i] == 0 || i == message.length-1){
                byte[] temp = new byte[name_in_bytes.size()];
                int index = 0;
                for(byte b:name_in_bytes){
                    temp[index] = b;
                    index++;
                }
                String temp_str = new String(temp,0,temp.length,StandardCharsets.UTF_8);
                name_to_print = name_to_print + temp_str;
                if(i==message.length-1 && message[i] !=0 && message.length>=512){
                    // dont print (wait for the next pakcet)
                } else{
                System.out.println(name_to_print);
                name_to_print = "";
                }
                
                name_in_bytes.clear();
            }
        }

        if(message.length < 512){
            name_to_print = "";
            System.out.println("finish DIRQ DATA SENDING");
            synchronized(lock){
                lock.notifyAll();
            }
        }
    }


    public static void main(String[] args) throws IOException {
         encdec = new TftpEncoderDecoder();
         protocol = new TftpProtocol();
        
          

         //String filename = "";

        if (args.length == 0) {
            args = new String[]{"localhost", "hello"};
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }
        else 
        System.out.println("Client Started");
        

        //BufferedReader and BufferedWriter automatically using UTF-8 encodingInteger.parseInt(args[1])
        Socket sock = new Socket(args[0],Integer.parseInt(args[1]) );
         in  = new BufferedInputStream((sock.getInputStream()));
         out = new BufferedOutputStream((sock.getOutputStream()));   

         Thread listeningThread=new Thread(()  -> {
            Thread listening_thread = Thread.currentThread();
            int read;
            try {
                while(!listening_thread.isInterrupted() && (read = in.read()) >=0 ){
                    byte[] message= encdec.decodeNextByte((byte)read);
                    if(message!=null){ // ------------------process stage------------------------
                    byte [] opcodeByte=new  byte[2];
                    opcodeByte[0]=message[0];opcodeByte[1]=message[1];
                    short opcode=bytesToShort(opcodeByte);
                    
                    if(opcode==4){//--------------------------ACK----------------
                             if(isWRQ){ //
                                System.out.println("ACK 0");
                                doWRQData();
                                synchronized(lock){
                                    lock.notifyAll();
                                }
                                System.out.println("Transfer File Completed");

                            

                            }else if(isDISC){
                                Thread.currentThread().interrupt();
                                
                                System.out.println("ACK 0");

                            }else if(isLOGRQ){
                                System.out.println("Connected to Server");
                                System.out.println("ACK 0");
                                synchronized(lock){
                                    lock.notifyAll();
                                }
                            }
                            else if(isDELRQ){
                                System.out.println("File is deleted");
                                System.out.println("ACK 0");
                                synchronized(lock){
                                    lock.notifyAll();
                                }

                            }

                    } else if(opcode==5){//------------ERROR---------------
                       byte[] errNum=new byte[2];
                       errNum[0]=message[2];errNum[1]=message[3];
                       short errNumber=bytesToShort(errNum);
                       String err_msg = new String(message, 4, message.length - 5, StandardCharsets.UTF_8);

                       System.out.println("Error " + errNumber+" "+err_msg );
                       synchronized(lock){
                        lock.notifyAll();
                       }




                    } else if(opcode==9){//--------------BCAST-------------
                        String name = new String(message, 3, message.length - 4, StandardCharsets.UTF_8);
                        if(message[2]==1)
                          System.out.println("Bcast Added " + name  );
                          if(message[2]==0)
                          System.out.println("Bcast Deleted " + name  );
                        
                    }
                    else if(opcode==3){//-------------DATA-----------------
                        if(isRRQ){
                            doDATA(message);
                            synchronized(lock){
                                lock.notifyAll();
                            }

                        }else if(isDIRQ){
                     doDIRCData(message);

                                
                        }
                    }







                     





                    }


                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            System.out.println("Listening thread is terminated");

         });  
         listeningThread.start();  





         Thread readingThread=new Thread(()  -> {
            
            Thread keyboard_thread = Thread.currentThread();
            Scanner scanner= new Scanner(System.in);
            while(!keyboard_thread.interrupted()){
                String input_Line=scanner.nextLine();// take the line of the command
                String [] split_Words=input_Line.split(" ");
                String command=split_Words[0];



                if(command.equals("RRQ")){  // ---------------RRQ----------------------
                 String filename =split_Words[1];
                 byte[] opcode={0,1};
                 byte[] bytes=mergeWithString(opcode, filename);
                 try {
                    File file = new File(filename);
                    if(file.exists()){
                        //error
                    } else{
                    file.createNewFile();
                    out.write(bytes);
                    out.flush();
                    
                        curr_filename = filename;
                        isRRQ =  true;
                        try {
                            System.out.println("keyboard thread is waiting");
                            synchronized(lock){
                            lock.wait();
                           
                            }
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                 



                }else if(command.equals("WRQ")){ // ---------------WRQ----------------------
                    String filename = split_Words[1];
                    byte[] opcode={0,2};
                    byte[] bytes=mergeWithString(opcode, filename);
                    File file=  new File(filename);
                    if(file.exists()){
                    try {
                       out.write(bytes);
                       out.flush();
                       isWRQ = true;
                       curr_filename = filename;
                       
                   } catch (IOException e) {
                       // TODO Auto-generated catch block
                       e.printStackTrace();
                   }

                   try {
                    synchronized(lock){
                        lock.wait();                    isWRQ = false;
                    curr_filename = "";}
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else{
                System.out.println("File not exists!");
            }

                    
                }  else if(command.equals("DIRQ")){
                    byte[] opcode={0,6};
                    
                    try {
                        out.write(opcode);
                        out.flush();
                        isDIRQ=true;

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    try {
                        synchronized(lock){
                            lock.wait();                     
                              }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    
                } else if(command.equals("LOGRQ")){
                    if(!logged){
                        logged=true;
                    String userName = split_Words[1];
                    byte[] opcode={0,7};
                    byte[] bytes=mergeWithString(opcode, userName);
                    try {
                        out.write(bytes);
                        out.flush();
                        isLOGRQ =  true;                        
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    try {
                        synchronized(lock){
                            lock.wait();
                        isLOGRQ=false;}
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                
                
                }
                else {
                    System.out.println("Error  7 User already logged in - Login username already connected.");

                }


                } else if(command.equals("DISC")){
                    byte[] opcode={0,10};
                    
                    try {
                        out.write(opcode);
                        out.flush();
                        isDISC=true;

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    try {
                        listeningThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    listeningThread.interrupt();
                    Thread.currentThread().interrupt(); 

                } else if(command.equals("DELRQ")){
                    String userName = split_Words[1];
                    byte[] opcode={0,8};
                    byte[] bytes=mergeWithString(opcode, userName);
                    try {
                        out.write(bytes);
                        out.flush();
                        isDELRQ=true;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    try {
                        synchronized(lock){
                            lock.wait();
                            isDELRQ=false;
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }



                }else

                    System.out.println("Invalid command");

            }

         }); 


         readingThread.start();

       
         

















        
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



    public static short bytesToShort(byte[] byteArr) {
        short result = (short) (((short) byteArr[0]) << 8 | (short) (byteArr[1])& 0xff);
        return result;
    }
}
