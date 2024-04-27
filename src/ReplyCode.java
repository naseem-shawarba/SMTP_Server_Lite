import java.net.InetAddress;
import java.util.HashMap;

public class ReplyCode {

    private static ReplyCode instance = null;

    HashMap<Integer, String> listreplies = new HashMap<Integer, String>(); // all potential replies are stored here . to get the value of the key use hmap.get(replyNumber)
    HashMap<String, String> helpRepliesWithCommand = new HashMap<String, String>(); // HELP mit Argument
    HashMap<Integer, String> helpRepliesWOCommand = new HashMap<Integer, String>(); // HELP ohne Argument

    ReplyCode() throws Exception{

        listreplies.put(500 ,"Syntax error, command unrecognized");
        listreplies.put(501 ,"Syntax error in parameters or arguments");
        listreplies.put(502 ,"Command not implemented");
        listreplies.put(503 ,"Bad sequence of commands");
        listreplies.put(504 ,"Command parameter not implemented");
        listreplies.put(211 ,"System status, or system help reply");
        listreplies.put(214 ,"Help message");      // the meaning of 214 can be edited ; the meaning should talk about  the commands and thier usage
        listreplies.put(220 , InetAddress.getLocalHost().getCanonicalHostName() + " Simple Mail Transfer Service ready"); // Local-Hostname wird in die Nachricht eingef�gt
        listreplies.put(221 , InetAddress.getLocalHost().getCanonicalHostName() + " Service closing transmission channel"); // NEW
        listreplies.put(421 ,"<domain> Service not available,");
        listreplies.put(250 ,"Requested mail action okay, completed");
        listreplies.put(251 ,"User not local; will forward to <forward-path>");
        listreplies.put(450 ,"Requested mail action not taken: mailbox unavailable");
        listreplies.put(550 ,"Requested action not taken: mailbox unavailable");
        listreplies.put(451 ,"Requested action aborted: error in processing");
        listreplies.put(551 ,"User not local; please try <forward-path>");
        listreplies.put(452 ,"Requested action not taken: insufficient system storage");
        listreplies.put(552 ,"Requested mail action aborted: exceeded storage allocation");
        listreplies.put(553 ,"Requested action not taken: mailbox name not allowed");
        listreplies.put(354 ,"Start mail input; end with <CRLF>.<CRLF>");
        listreplies.put(554 ,"Transaction failed");

        initialiseHelp();
    }
    public static synchronized ReplyCode getInstance() throws Exception{
        if(instance == null){
            instance = new ReplyCode();
        }
        return instance;
    }

    private void initialiseHelp() {
        String rep = "214 " + listreplies.get(214) + "\r\n";
        // Initialisierung der HELP-Antworten mit dem Command:
        helpRepliesWithCommand.put("HELO", rep + "HELO <SP> <domain> <CRLF>");
        helpRepliesWithCommand.put("MAIL", rep + "MAIL <SP> FROM:<reverse-path> <CRLF>");
        helpRepliesWithCommand.put("RCPT", rep + "RCPT <SP> TO:<forward-path> <CRLF>");
        helpRepliesWithCommand.put("DATA", rep + "DATA <CRLF>\n End of data: <CRLF>.<CRLF>");
        helpRepliesWithCommand.put("QUIT", rep + "QUIT <CRLF>");

        // Initialisierung der HELP-Antworten ohne Command je nach dem Stand der Kommunikation:
        helpRepliesWOCommand.put(0, rep + "HELO <SP> <domain> <CRLF>");
        helpRepliesWOCommand.put(1, rep + "MAIL <SP> FROM:<reverse-path> <CRLF>");
        helpRepliesWOCommand.put(2, rep + "RCPT <SP> TO:<forward-path> <CRLF>");
        helpRepliesWOCommand.put(3, rep + "DATA <CRLF>");
        helpRepliesWOCommand.put(4, rep + "Enter you mail data. End of mail data indication with <CRLF>.<CRLF>");
        helpRepliesWOCommand.put(5, rep + "Close connetion with QUIT <CRLF>");

    }

}