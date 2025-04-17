package bgu.spl.net.impl.tftp;


import bgu.spl.net.srv.Server;

public class TftpServer {
    //TODO: Implement this

        public static void main(String[] args) {
        Server.threadPerClient(
            Integer.parseInt(args[0]), //port
                () -> new TftpProtocol(), //protocol factory
                TftpEncoderDecoder::new //message encoder decoder factory
        ).serve();

    }
}
