

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SMTPServer {

    public static Charset messageCharset = Charset.forName("US-ASCII");
    public static CharsetDecoder decoder = messageCharset.newDecoder();
    public static Random random = new Random();

    public static void runServer(int portnumber) throws IOException{
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        


        Selector selector = Selector.open();
        ServerSocketChannel servSock = ServerSocketChannel.open();
        servSock.configureBlocking(false);
        servSock.socket().bind(new InetSocketAddress(portnumber));
        servSock.register(selector, SelectionKey.OP_ACCEPT);

        while(true){


            if(selector.select() == 0){
                continue;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while(iter.hasNext()){ SelectionKey key = iter.next();
                iter.remove();
                readBuffer.clear();

                if(key.isAcceptable()){
                    ServerSocketChannel sock = (ServerSocketChannel) key.channel();
                    SocketChannel client = sock.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE ,new ClientState());







                }
                if(key.isReadable()){

                    SocketChannel channel =(SocketChannel) key.channel();
                    ClientState client = (ClientState) key.attachment();
                    readBuffer.clear();
                    try{
                        channel.read(readBuffer);
                    }catch (Exception e){

                    }
                    //int readBytes =channel.read(readBuffer);
                    /*if (readBytes == -1){
                        channel.close();
                        continue;
                    }*/

                    readBuffer.flip();

                    //further processing of the data or command
                    String messageRecieved = decoder.decode(readBuffer).toString();

                    int num= 3;
                    if(client.commandSent){//command was sent from the client
                        String command = messageRecieved.substring(0,messageRecieved.length()-2);
                        handleCommand(client , command);


                    }else{//data was sent from the client
                        client.mailData += messageRecieved ;

                        //String str =client.mailData ;

                        if(messageTerminated(client.mailData)){
                            client.commandSent = true;
                            client.mailData = client.mailData.substring(0,client.mailData.length()-5); // delete "\r\n.\r\n" from the message
                            saveMessage(client );
                            client.reply(250.1);
                        }

                    }


                }
                if(key.isWritable()){
                    SocketChannel channel =(SocketChannel) key.channel();
                    ClientState client = (ClientState) key.attachment();

                    /*if( client.replyBuffer.limit() == client.replyBuffer.position()){
                        channel.close();
                        System.out.println("exit loop");
                        System.out.println(client.replyBuffer.position());
                        System.out.println(client.replyBuffer.limit());
                        continue;
                    }*/
                    int writeBytes = channel.write(client.replyBuffer);
                    if( writeBytes == -1){
                        channel.close();
                        continue;
                    }
                    //client.replyBuffer.clear();
                    //channel.write(client.replyBuffer);


                    if(client.closeChannel){
                        channel.close();
                    }


                }





            }
        }
    }

    private static boolean messageTerminated(String message) {
        String mes = message.toString();
        if (message.length()<5){
            return false;
        }

        if(mes.indexOf("\r\n.\r\n",message.length()-5) == message.length()-5){
            return true;
        }
        return false ;


    }

    private static void handleCommand(ClientState client, String messageRecieved) {

        String commandCode = messageRecieved.substring(0,4);
        commandCode= commandCode.toUpperCase();


        if (commandCode.equals("HELO")){
            //TODO
            client.heloSent = true;
            client.reply(250.2);
            
        } else if (commandCode.equals("MAIL")){ //data should be extracted and saved in client buffers ; writebuffer should be filles with the corresponding message
            //TODO
            // clear state of all buffers
            if (!client.heloSent){
                client.reply(503);
                return;
            }
            client.reversePath = "";
            client.forwardPath.clear();
            client.mailData = "";


            String patternString1 = "MAIL FROM: (.*@.+)";
            Pattern pattern = Pattern.compile(patternString1);
            Matcher matcher = pattern.matcher(messageRecieved);
            while(matcher.find()) {
                 client.reversePath += matcher.group(1);
            }
            String dtr = client.reversePath;

            client.reply(250.1);



        }else if (commandCode.equals("RCPT")){
            //TODO
            if (!client.heloSent || client.reversePath.isEmpty()){
                client.reply(503);
                return;
            }



            String patternString1 = "RCPT TO: (.*@.+)";
            Pattern pattern = Pattern.compile(patternString1);
            Matcher matcher = pattern.matcher(messageRecieved);
            while(matcher.find()) {
                client.forwardPath.add(matcher.group(1));
            }


            client.reply(250.1);



        } else if (commandCode.equals("DATA")){
            //
            if (!client.heloSent || client.reversePath.isEmpty() || client.forwardPath.isEmpty()){
                client.reply(503);
                return;
            }
            client.commandSent = false ;
            client.reply(354);



        }else if (commandCode.equals("QUIT")){
            //TODO
            client.closeChannel = true;
            client.reply(221);



        }else if (commandCode.equals("HELP")){
            //TODO


            client.reply(214);



        }else if (commandCode.equals("RSET")|| commandCode.equals("NOOP")){
            // sind nicht Teil der Hausaufgabe
            client.reply(502);

        }else{
            client.reply(500);
        }
    }
    private static void saveMessage(ClientState client) throws IOException { // creat files for the RCPT and save the message for each RCPT
        //TODO
        System.out.println(client.reversePath);
        for (String msg:client.forwardPath){
            System.out.println(msg);
        }
        System.out.println(client.mailData);

        //TODO
        client.message_id = random.nextInt(10000); //this will give an random number from 0 to 9999

        for(String reciever : client.forwardPath) {
            String recieverFolderPath = String.format("/home/naseem/Desktop/SMTPServer_database/%s",reciever );
            File recieverFolder = new File(String.format(recieverFolderPath));
            recieverFolder.mkdirs();

            String recievedMessageFilePath = String.format("%s/%s-%d.txt",recieverFolderPath,client.reversePath,client.message_id );
            File recievedMessageFile = new File(recievedMessageFilePath);
            recievedMessageFile.createNewFile();
            FileOutputStream f = new FileOutputStream(recievedMessageFilePath);
            FileChannel ch = f.getChannel();


            ByteBuffer saveBuffer = ByteBuffer.allocate(1024);
            saveBuffer.put(client.mailData.getBytes(messageCharset));
            saveBuffer.flip();
            ch.write(saveBuffer);








            ch.close();
            //System.out.println(recievedMessageFile.exists());

        }

    }
























    public static void main(String[] args) throws IOException {
        int portNumber = 0;
        if (args.length != 1) {
            System.err.println("Usage: java SMTPServer.class <port>");
            System.exit(1);
        }
        else{
            try {
                portNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                //System.err.println("Argument : " + args[0] + " must be an integer.");
                System.exit(1);
            }
        }

        runServer(portNumber);

    }
}
