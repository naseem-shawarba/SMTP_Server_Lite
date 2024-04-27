import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import java.util.List;

public class ClientState {


    public ByteBuffer replyBuffer = ByteBuffer.allocate(1024);// for each client should be a replybuffer , in order to reply later when the channel becomes writable

    boolean commandSent = true;  // the client send data(the message in the email) or commands : in the beginning the client sends me the helo command , thats why in the beginning commandsent is true
    // if DATA command was send : then commandSent will be changed to false :
    boolean isDataOnTheWay = false;

    SocketChannel clientSock;

    String reversePath;
    //String forwardPath ;
    List<String> forwardPath = new ArrayList<String>();
    String mailData = ""; // Buffer f�r die Mail-Daten
    int progress = 0; // 0..5 -> 0 - Verbindung aufgebaut; 1 - HELO; 2 - MAIL FROM; 3 - RCPT TO; 4 - DATA; 5 - QUIT


    ClientState(SocketChannel clientSock) throws Exception{
        this.clientSock = clientSock;
        reply(220);
    }


    public void reply(int replynumber) throws Exception{
        replyBuffer.clear();
        String replymeaning = ReplyCode.getInstance().listreplies.get(replynumber) ;
        replyBuffer.put(String.format("%d %s\r\n",replynumber, replymeaning).getBytes(SMTPServer.messageCharset));
        replyBuffer.flip();


    }


}