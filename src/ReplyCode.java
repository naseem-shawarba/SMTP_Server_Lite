import java.util.HashMap;

public class ReplyCode {

    private static ReplyCode instance = null;

    HashMap<Double, String> listreplies = new HashMap<Double, String>(); // all potential replies are stored here . to get the value of the key use hmap.get(replyNumber)

    ReplyCode(){

        listreplies.put(500.0 ,"Syntax error, command unrecognized");
        listreplies.put(501.0 ,"Syntax error in parameters or arguments");
        listreplies.put(502.0 ,"Command not implemented");
        listreplies.put(503.0 ,"Bad sequence of commands");
        listreplies.put(504.0 ,"Command parameter not implemented");
        listreplies.put(211.0 ,"System status, or system help reply");
        listreplies.put(214.0 ,"HELO MAIL RCPT DATA HELP QUIT");      // the meaning of 214 can be edited ; the meaning should talk about  the commands and thier usage
        listreplies.put(220.0 ,"localhost Simple Mail Transfer Service ready");
        listreplies.put(221.0 ,"localhost Service closing transmission channel");
        listreplies.put(421.0 ,"<domain> Service not available,");
        listreplies.put(250.1 ,"Requested mail action okay, completed");
        listreplies.put(250.2 ,"localhost");
        listreplies.put(251.0 ,"User not local; will forward to <forward-path>");
        listreplies.put(450.0 ,"Requested mail action not taken: mailbox unavailable");
        listreplies.put(550.0 ,"Requested action not taken: mailbox unavailable");
        listreplies.put(451.0 ,"Requested action aborted: error in processing");
        listreplies.put(551.0 ,"User not local; please try <forward-path>");
        listreplies.put(452.0 ,"Requested action not taken: insufficient system storage");
        listreplies.put(552.0 ,"Requested mail action aborted: exceeded storage allocation");
        listreplies.put(553.0 ,"Requested action not taken: mailbox name not allowed");
        listreplies.put(354.0 ,"Start mail input; end with <CRLF>.<CRLF>");
        listreplies.put(554.0 ,"Transaction failed");
    }
    public static synchronized ReplyCode getInstance(){
        if(instance == null){
            instance = new ReplyCode();
        }
        return instance;
    }



}
