import java.nio.ByteBuffer;
import java.util.ArrayList;

import java.util.List;

public class ClientState {


    public ByteBuffer replyBuffer = ByteBuffer.allocate(1024);// for each client should be a replybuffer , in order to reply later when the channel becomes writable

    boolean commandSent = true;  // the client send data(the message in the email) or commands : in the beginning the client sends me the helo command , thats why in the beginning commandsent is true
    // if DATA command was send : then commandSent will be changed to false :

    String reversePath = "" ;
    List<String> forwardPath = new ArrayList<String>();
    String mailData = "";
    //StringBuilder mailData = new StringBuilder("");
    boolean heloSent = false;
    boolean closeChannel = false;
    boolean helpsent = false;
    int message_id ;



    ClientState(){

        reply(220);
    }


    public void reply(double replynumber) {
        replyBuffer.clear();

        String replymeaning;
        replymeaning = ReplyCode.getInstance().listreplies.get(replynumber);
        replyBuffer.put(String.format("%d %s\r\n", (int) Math.floor(replynumber), replymeaning).getBytes(SMTPServer.messageCharset));

        if(helpsent){
            replymeaning = ReplyCode.getInstance().listreplies.get(214);
            replyBuffer.put(String.format("%d %s\r\n", (int) Math.floor(214), replymeaning).getBytes(SMTPServer.messageCharset));
        }





        if(replynumber == 214.0){
            helpsent = true;
        }


        replyBuffer.flip();
    }


}























