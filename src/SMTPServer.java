import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.io.*;

public class SMTPServer {

    public static Charset messageCharset = Charset.forName("US-ASCII");
    public static CharsetDecoder decoder = messageCharset.newDecoder();
    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private Selector selector;
    ServerSocketChannel servSock;
    public static boolean help = false;

    // Constructor with initialization of the Server Socket Channel:
    public SMTPServer(int portnumber) throws Exception {
        selector = Selector.open();
        servSock = ServerSocketChannel.open();
        servSock.configureBlocking(false);
        servSock.socket().bind(new InetSocketAddress(portnumber));
        servSock.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void runServer() throws Exception {

        while (true) {
            if (selector.select() == 0) {
                continue;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                // Check if the key's channel is ready to accept a new connection
                if (key.isAcceptable()) {
                    ServerSocketChannel sock = (ServerSocketChannel) key.channel();
                    SocketChannel clientSock = sock.accept();
                    clientSock.configureBlocking(false);
                    ClientState clientState = new ClientState(clientSock);
                    clientSock.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, clientState);
                }

                // Check if the key's channel is ready for reading
                if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    ClientState client = (ClientState) key.attachment();
                    readBuffer.clear();

                    // Read data from the channel into the buffer
                    int readBytes = channel.read(readBuffer);
                    readBuffer.flip();

                    // Decode the received message
                    String messageReceived = decoder.decode(readBuffer).toString();

                    // Determine whether the message is a command or data
                    if (client.commandSent) {
                        handleCommand(client, messageReceived, readBytes);
                    } else {
                        saveMessage(client, messageReceived);
                    }

                    // If there is no more data expected from the client or if help is requested,
                    // set interest in writing
                    if (!client.isDataOnTheWay || help) {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }

                // Check if the key's channel is ready for writing
                if (key.isWritable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    ClientState client = (ClientState) key.attachment();

                    // Write data from the client's reply buffer to the channel
                    channel.write(client.replyBuffer);
                    key.interestOps(SelectionKey.OP_READ);

                    // If the client's progress reaches a certain point, cancel the key and close
                    // the channel
                    if (client.progress == 6) {
                        key.cancel();
                        channel.close();
                    }
                }

                // Remove the current key from the iterator
                iter.remove();
                readBuffer.clear();
            }
        }
    }

    private static void handleCommand(ClientState client, String messageReceived, int readBytes) throws Exception {

        // Check if the received message is empty or too short
        if (messageReceived.length() == 0 || messageReceived.length() < 4)
            return;

        // Extract the command from the received message and convert it to uppercase
        String command = messageReceived.substring(0, 4).toUpperCase();
        System.out.println(messageReceived); // Print the received message

        // Perform actions based on the command
        if (command.equals("HELO")) {
            // HELO command
            if (client.progress != 0) {
                // Already in progress, send error reply
                client.reply(503);
                return;
            }
            // Prepare and send the reply
            client.replyBuffer.clear();
            String myHost = InetAddress.getLocalHost().getCanonicalHostName();
            client.replyBuffer.put(String.format("%d %s\r\n", 250, myHost).getBytes(SMTPServer.messageCharset));
            client.replyBuffer.flip();
            client.progress = 1; // Update progress

        } else if (command.equals("MAIL")) {
            // MAIL command
            if (client.progress != 1) {
                // Incorrect progress, send error reply
                client.reply(503);
                return;
            }
            // Check if the command format is correct
            if (!messageReceived.toUpperCase().startsWith("MAIL FROM:")) {
                client.reply(502);
                return;
            }
            // Extract reverse path and prepare reply
            int k = (messageReceived.charAt(readBytes - 2) != '\r') ? 1 : 0; // Adjustment for netcat
            client.reversePath = messageReceived.substring(10, readBytes - 2 + k).replaceAll("\\s", "");
            client.replyBuffer.clear();
            client.replyBuffer.put(String.format("250 OK\r\n").getBytes(SMTPServer.messageCharset));
            client.replyBuffer.flip();
            client.progress = 2; // Update progress

        } else if (command.equals("RCPT")) {
            // RCPT command
            if (client.progress < 2 || client.progress > 3) {
                // Incorrect progress, send error reply
                client.reply(503);
                return;
            }
            // Check if the command format is correct
            if (!messageReceived.toUpperCase().startsWith("RCPT TO:")) {
                client.reply(502);
                return;
            }
            // Extract forward path and prepare reply
            int k = (messageReceived.charAt(readBytes - 2) != '\r') ? 1 : 0; // Adjustment for netcat
            client.forwardPath.add(messageReceived.substring(8, readBytes - 2 + k).replaceAll("\\s", ""));
            client.replyBuffer.clear();
            client.replyBuffer.put(String.format("250 OK\r\n").getBytes(SMTPServer.messageCharset));
            client.replyBuffer.flip();
            client.progress = 3; // Update progress

        } else if (command.equals("DATA")) {
            // DATA command
            if (client.progress != 3) {
                // Incorrect progress, send error reply
                client.reply(503);
                return;
            }
            // Prepare for data reception
            client.commandSent = false;
            client.reply(354); // Send intermediate reply

        } else if (command.equals("QUIT")) {
            // QUIT command
            System.out.println("OK");
            client.reply(221); // Send reply
            client.progress = 6; // Update progress

        } else if (command.equals("HELP")) {
            // HELP command
            String reply;
            if (messageReceived.length() == 6 || messageReceived.length() == 5) {
                // Reply without command
                reply = ReplyCode.getInstance().helpRepliesWOCommand.get(client.progress) + "\r\n";
            } else {
                // Reply with specific command help
                String helpCommand = messageReceived.substring(4, readBytes - 2).toUpperCase().replaceAll("\\s", "");
                if (ReplyCode.getInstance().helpRepliesWithCommand.get(helpCommand) == null) {
                    reply = "214 " + ReplyCode.getInstance().listreplies.get(214) + "\r\n" + "bad command\r\n";
                } else {
                    reply = ReplyCode.getInstance().helpRepliesWithCommand.get(helpCommand) + "\r\n";
                }
            }
            // Prepare and send reply
            client.replyBuffer.clear();
            client.replyBuffer.put(reply.getBytes(SMTPServer.messageCharset));
            client.replyBuffer.flip();

        } else {
            // Unknown command, send error reply
            client.reply(500);
        }
    }

    private static void saveMessage(ClientState client, String messageReceived) {

        System.out.println(messageReceived); // Print received message

        // Check if the message is a HELP command
        if (messageReceived.toUpperCase().equals("HELP\r\n")) {
            help = true; // Set help flag to true

            try {
                // Get help reply based on client progress
                String reply = ReplyCode.getInstance().helpRepliesWOCommand.get(client.progress + 1) + "\r\n";

                // Prepare reply buffer
                client.replyBuffer.clear();
                client.replyBuffer.put(reply.getBytes(SMTPServer.messageCharset));
                client.replyBuffer.flip();
            } catch (Exception e) {
                // Handle exception (currently empty)
            }

            return; // Exit method
        }

        // Append received message to client's mail data
        client.mailData += messageReceived;

        int lenMess = client.mailData.length(); 

        // Check if mail data ends with "\r\n.\r\n"
        if (lenMess >= 5) {
            String endOfMessage = client.mailData.substring(lenMess - 5, lenMess);
            if (!endOfMessage.equals("\r\n.\r\n")) {
                // If message is not complete, set data on the way flag and return
                client.isDataOnTheWay = true;
                return;
            }

            // Remove "\r\n.\r\n" from mail data if message is complete
            client.mailData = client.mailData.substring(0, client.mailData.length() - 5);
        } else if (messageReceived.equals("\r\n.\r\n")) {
            // If message is only "\r\n.\r\n", remove it from mail data
            client.mailData = client.mailData.substring(0, client.mailData.length() - 5);
        } else {
            // If message is not complete, set data on the way flag and return
            client.isDataOnTheWay = true;
            return;
        }

        // If message is complete, process it
        client.isDataOnTheWay = false; // Reset data on the way flag
        client.commandSent = true; // Set command sent flag
        client.progress = 5; // Update client progress

        // Save message for each recipient
        for (String rcpt : client.forwardPath) {
            String orderName = "./SMTPServer_database/" + rcpt; // Directory for recipient
            File file = new File(orderName); // Create directory if not exists
            if (!file.isDirectory()) {
                file.mkdirs();
            }

            String filename = orderName + "/" + client.reversePath + "_" + ((new Random()).nextInt(10000)) + ".txt"; // Filename  for message

            try {
                // Write message to file
                FileWriter myWriter = new FileWriter(filename);
                Timestamp time = new Timestamp(System.currentTimeMillis()); // Get current timestamp
                myWriter.write(time + "\n\n" + client.mailData); // Write timestamp and message
                myWriter.close();
            } catch (IOException e) {
                // Handle file writing exception
                System.out.println("Couldn't write file! Stopping...");
            }

            try {
                // Send reply code 250
                client.reply(250);
            } catch (Exception e) {
                // Handle reply exception (currently empty)
            }
        }
    }

    // Main method to start the server
    public static void main(String[] args) throws Exception {
        int portNumber = 0;
        if (args.length != 1) {
            System.err.println("Usage: java SMTPServer <port>");
            System.exit(1);
        } else {
            try {
                portNumber = Integer.parseInt(args[0]);
                System.out.println("Server started on port: " + portNumber); // Notify server startup with port
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number"); // Error message for invalid port number
                System.exit(1);
            }
        }
        SMTPServer server = new SMTPServer(portNumber);
        server.runServer();
    }
}