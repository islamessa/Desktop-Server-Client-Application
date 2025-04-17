package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int length = 0;
    private short opcode = -1;
    private byte[] packetSize=new byte[2];
    private int counter=0;



    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this
            pushByte(nextByte);
            counter++;
            if(length==2){
                opcode=bytesToShort(bytes);}
                   if(opcode==1){//-------RRQ

                        if(nextByte ==0)//reaching the end{}
                        {
                            
                            opcode=-1;
                            packetSize=new byte[2];
                            byte[] ans= Arrays.copyOf(bytes, length);
                            length=0;
                            bytes=new byte[1 << 10];
                            return ans;}                           

                   }else if(opcode==2){//--------WRQ
                    if(nextByte ==0)//reaching the end
                    {
                        
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length);
                        length=0;
                        bytes=new byte[1 << 10];
                        return ans;}

                   }else if(opcode==3){//------DATA
                 //   short size =0;
                        if(length==3)
                            packetSize[0]=nextByte;
                        if(length==4){
                        packetSize[1]=nextByte;
                    }
                    if(length==bytesToShort(packetSize)+6&&bytesToShort(packetSize)!=0)
                    {
                       
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length);
                        length=0;
                        bytes=new byte[1 << 10];
                        return ans;}

                   } if(opcode==4){//-------ACK
                    if(length==4)
                    {
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length);
                        length=0;
                        bytes=new byte[1 << 10];
                        return ans;}
                   }else if(opcode==5){//-------ERROR
                    if(nextByte==0&&length>4)
                    {
                       
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length); 
                        length=0;
                        bytes=new byte[1 << 10];
                        return ans;}
                   }else if(opcode==6){//---------DIRQ
                    if(length==2)
                    {
                        
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length);
                        length=0;
                        bytes=new byte[1 << 10];
                        return ans;}
                   } if(opcode==7){//------------LOGRQ
                    if(nextByte==0)
                    {
                        
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length);length=0;
                        bytes=new byte[1 << 10];
                        counter = 0;
                        return ans;}
                   }else if(opcode==8){//--------DELRQ
                    if(nextByte==0)
                    {
                        
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length);length=0;
                        bytes=new byte[1 << 10];
                        return ans;}
                   }else if(opcode==9){//--------BCAST
                    if(nextByte==0&&length>3)
                    {
                        
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length);length=0;
                        bytes=new byte[1 << 10];
                        return ans;}
                   }else if(opcode==10){//----------DISC
                       if(length==2)
                       {
                        
                        opcode=-1;
                        packetSize=new byte[2];
                        byte[] ans= Arrays.copyOf(bytes, length);length=0;
                        bytes=new byte[1 << 10];
                        return ans;}                   }







            
            
       
        return null;
    }


    
    private void pushByte(byte nextByte) {
        if (length >= bytes.length) {
            bytes = Arrays.copyOf(bytes, length * 2);
        }

        bytes[length++] = nextByte;
    }
    public short bytesToShort(byte[] byteArr) {
    short result =(short)(((short)byteArr[0])<<8|(short)(byteArr[1])& 0xff);
        return result;
    }

    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this
return message;    }
}