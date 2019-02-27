
    import javax.crypto.KeyGenerator;
    import javax.crypto.SecretKey;
    import javax.crypto.*;
    import java.net.*;
    import java.io.*;
    import java.util.*;
    import java.security.*;
    import java.security.cert.*;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;
    import java.util.Scanner;
    import java.util.Base64;

    public class ChatClient implements Runnable
    {  
        private Socket socket              = null;
        private Thread thread              = null;
        private DataInputStream  console   = null;
        public ObjectInputStream  streamIn   = null;
        public ObjectOutputStream streamOut = null;
        private ChatClientThread client    = null;
        public SecretKey symetricKey           = null;
        private SecretKey next_sKey      = null;
        private PublicKey server_pubKey  = null;
        private PublicKey server_sigKey  = null;
        private KeyPair clientSigKeyPair    = null;
        private PublicKey client_pubKey  = null;
        public PrivateKey client_privKey  = null;
        private String username  = null;
        private int counter=0;

        private UserKeys userKeys=null;

        public ChatClient(String serverName, int serverPort)
        {  

            try {
                KeyPairGenerator keypairgenerator = KeyPairGenerator.getInstance("RSA");

                
                keypairgenerator.initialize(2048);
                clientSigKeyPair = keypairgenerator.genKeyPair();

                client_pubKey=clientSigKeyPair.getPublic();
                client_privKey=clientSigKeyPair.getPrivate();
                console   = new DataInputStream(System.in);

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }



            System.out.println("Establishing connection to server...");
            
            try
            {
                // Establishes connection with server (name and port)
                socket = new Socket(serverName, serverPort);
                System.out.println("Connected to server: " + socket);
                start();
            }
            
            catch(UnknownHostException uhe)
            {  
                // Host unkwnown
                System.out.println("Error establishing connection - host unknown: " + uhe.getMessage()); 
            }
          
            catch(IOException ioexception)
            {  
                // Other error establishing connection
                System.out.println("Error establishing connection - unexpected exception: " + ioexception.getMessage()); 
            }
            
       }
        
       public void run()
       {  
           while (thread != null) 
           {  
             try
           {
               String msg = console.readLine();
               // Sends message from console to server




              counter++;
               if(counter==9||msg.equals(".refresh")){
                System.out.println("Refreshing Key");
                   client.send(".refresh");


                try{
                    counter=0;
                    secondHandShake();
                    thirdHandShake();
                    System.out.println("Key Refreshed, enjoy");
                }
                catch (Exception e){}

               }
               else{
                   client.send(msg);

               }


           }
         
               catch(IOException ioexception)
               {
                   if(thread != null) {
                       System.out.println("Error sending string to server: " + ioexception.getMessage());
                       stop();
                   }
               }
              
           }
        }

        public int firstHandShake() throws IOException, Exception{


            //recebe e envia as public keys
            String msg = streamIn.readUTF();


            byte b[] = Base64.getDecoder().decode(msg.getBytes()); 
             ByteArrayInputStream bi = new ByteArrayInputStream(b);
             ObjectInputStream si = new ObjectInputStream(bi);
             server_pubKey = (PublicKey)si.readObject();


            ByteArrayOutputStream bo = new ByteArrayOutputStream();
             ObjectOutputStream so = new ObjectOutputStream(bo);
             so.writeObject(client_pubKey);
             so.flush();
             String redisString = new String(Base64.getEncoder().encode(bo.toByteArray()));                      
            streamOut.writeUTF(redisString);

            streamOut.flush();

            return 0;

        }


        public int secondHandShake() throws IOException, Exception{


    
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            symetricKey = kg.generateKey();


             byte[] msg_data = null;
            
            Cipher myCipher=null;
            try {
                myCipher = Cipher.getInstance("RSA");
                myCipher.init(Cipher.ENCRYPT_MODE, client_privKey);
                myCipher.update(symetricKey.getEncoded());
                msg_data = myCipher.doFinal();

            } catch (Exception e) {
                e.printStackTrace();
            }



             String redisString2 = new String(Base64.getEncoder().encode(msg_data));                      
            streamOut.writeUTF(redisString2);
            

             streamOut.flush();



            return 0;
        }


        public int thirdHandShake() throws IOException, Exception{


             byte[] signatureBytes=null;
            byte[] msg_data=null;       
            try {



                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(client_privKey);
                sig.update(symetricKey.getEncoded());
                signatureBytes = sig.sign();

            } catch (Exception e) {
                e.printStackTrace();
            }





            try {
                Cipher myCipher = Cipher.getInstance("AES");

                myCipher.init(Cipher.ENCRYPT_MODE, symetricKey);
                msg_data = myCipher.doFinal(signatureBytes);

            } catch (Exception e) {
                e.printStackTrace();
            }








             String redisString2 = new String(Base64.getEncoder().encode(msg_data)); 
             
             streamOut.writeUTF(redisString2);

             streamOut.flush();

            return 0;
             
        }


        public int login() throws IOException, Exception{


            System.out.println("Insert your username");

            String msg = console.readLine();
             username=msg;
            




             byte[] msg_data=null;
            Cipher myCipher =null;

            try {
                 myCipher = Cipher.getInstance("AES");
                myCipher.init(Cipher.ENCRYPT_MODE, symetricKey);
                msg_data = myCipher.doFinal(username.getBytes());

            } catch (Exception e) {
                e.printStackTrace();
            }




             String redisString2 = new String(Base64.getEncoder().encode(msg_data)); 
             
             streamOut.writeUTF(redisString2);
             streamOut.flush();




            byte[] signatureBytes=null;

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(client_privKey);
            sig.update(username.getBytes());
            signatureBytes = sig.sign();



             redisString2 = new String(Base64.getEncoder().encode(signatureBytes)); 
             
             streamOut.writeUTF(redisString2);
             streamOut.flush();




            System.out.println("Insert your password");

    

              msg = console.readLine();

    

             String password=msg;
            



             msg_data=null;
            try {
                 myCipher = Cipher.getInstance("AES");
                myCipher.init(Cipher.ENCRYPT_MODE, symetricKey);
                msg_data = myCipher.doFinal(password.getBytes());

            } catch (Exception e) {
                e.printStackTrace();
            }




              redisString2 = new String(Base64.getEncoder().encode(msg_data)); 
             
             streamOut.writeUTF(redisString2);
             streamOut.flush();




            signatureBytes=null;

             sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(client_privKey);
            sig.update(password.getBytes());
            signatureBytes = sig.sign();



             redisString2 = new String(Base64.getEncoder().encode(signatureBytes)); 
             
             streamOut.writeUTF(redisString2);
             streamOut.flush();







             byte[] b=null;

            
             msg = streamIn.readUTF();

             byte[] data=null;
             b = Base64.getDecoder().decode(msg.getBytes()); 


             myCipher = Cipher.getInstance("AES");
            myCipher.init(Cipher.DECRYPT_MODE, symetricKey);
            
            data = myCipher.doFinal(b);
            
            String stuff=new String(data);




              msg = streamIn.readUTF();






            if(stuff.equals("broo, your password is wrong")){
                try{
                    System.out.println("Wrong password");
                    stop();
                }
                catch (Exception e){

                }                
            }
            if(stuff.equals("broo, you are new")){
                System.out.println("User created");
               
            }
            
            if(stuff.equals("broo, you are cool")){
                System.out.println("User authenticated");
                
            }
            
            
            if (stuff.equals("broo, you are blacklisted"))
            {  
                // blacklisted, quit command

                System.out.println("You are blacklisted");
                try{
                    stop();
                }
                catch (Exception e){

                }
            }     

             if(stuff.equals("something went wrong")){
                try{
                    System.out.println("Connection refused");
                    stop();
                }
                catch (Exception e){

                }                
            }






            return 0;

        }


        public void handshake(){
             try
               {  

                    System.out.println("1st handshake");
                    firstHandShake();


                    System.out.println("2nd handshake");
                    secondHandShake();


                    System.out.println("3rd handshake");
                    thirdHandShake();


                    System.out.println("4th handshake (login)");
                    login();
                                  
               }
             
               catch(IOException ioexception)
               {  
                   System.out.println("Error sending string to server: " + ioexception.getMessage());
                   stop();
               }
                
                catch (Exception e1){
                    e1.printStackTrace();
                    client.stop();
                    return;
                }
        }
        
        
        public void handle(String msg)
        {  




             byte[] b=null;

             b = Base64.getDecoder().decode(msg.getBytes()); 
             
             byte[] data=null;


             try{
                Cipher myCipher = Cipher.getInstance("AES");
                myCipher.init(Cipher.DECRYPT_MODE, symetricKey);
                
                data = myCipher.doFinal(b);
            }
            catch (Exception e){}


            String received= new String(data);


            // Receives message from server
            
            if (received.equals("broo, you are blacklisted"))
            {  
                // Leaving, quit command
                System.out.println("The server has blacklisted you.");
                stop();

            }            

            else if (received.equals(".quit"))
            {  
                // Leaving, quit command
                System.out.println("Exiting...Please press RETURN to exit ...");
                stop();
            }
            else
                // else, writes message received from server to console
                System.out.println(received);
        }
        
        // Inits new client thread
        public void start() throws IOException
        {  

            streamOut = new ObjectOutputStream(socket.getOutputStream());
            streamIn = new ObjectInputStream(socket.getInputStream());
           
            if (thread == null)
            {  

            
                client = new ChatClientThread(this, socket);
                thread = new Thread(this);                   
                thread.start();

            }
        
        }


        public void sendSignature(String message){
            
            try{
            byte[] signatureBytes=null;

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(client_privKey);
            sig.update(message.getBytes());
            signatureBytes = sig.sign();



            String redisString2 = new String(signatureBytes); 
             
             streamOut.writeUTF(redisString2);
             streamOut.flush();
            }
            catch (Exception e){
            }
        }




      public boolean checkSignature( byte[] match){

            try{
                  String msg = streamIn.readUTF();


                byte[] b = Base64.getDecoder().decode(msg.getBytes()); 
                  
                
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(server_pubKey);
                sig.update(match);

                if(sig.verify(b)){
                    
                     return false;
                }
                else{
                    System.out.println("Assignature not checked");
                         
                     return true;
                }

            }
            catch(Exception e){}
            return false;

        }




        
        // Stops client thread
        public void stop()
        {  
            if (thread != null)
            {  
                thread.stop();  
                thread = null;
            }
            try
            {  
                if (console   != null)  console.close();
                if (streamOut != null)  streamOut.close();
                if (socket    != null)  socket.close();
            }
          
            catch(IOException ioe)
            {  
                System.out.println("Error closing thread..."); }
                client.close();  
                client.stop();
            }
       
        
        public static void main(String args[])
        {  
            ChatClient client = null;
            if (args.length != 2)
                // Displays correct usage syntax on stdout
                System.out.println("Usage: java ChatClient host port");
            else
                // Calls new client
                client = new ChatClient(args[0], Integer.parseInt(args[1]));
        }
        
    }

    class ChatClientThread extends Thread
    {  
        private Socket           socket   = null;
        private ChatClient       client   = null;

        public ChatClientThread(ChatClient _client, Socket _socket)
        {  
            client   = _client;
            socket   = _socket;

            

            client.handshake();
            System.out.println("\nHandshake terminated\n");
            start();
        }
       


        
        public void close()
        {  
            try
            {  
                if (client.streamIn != null) client.streamIn.close();
            }
          
            catch(IOException ioe)
            {  
                System.out.println("Error closing input stream: " + ioe);
            }
        }
        
        public void run()
        {  

            while (true)
            {   
                try
                {  
                    String stuff=client.streamIn.readUTF();
                    byte[] b=null;

                     b = Base64.getDecoder().decode(stuff.getBytes());
                    if(client.checkSignature(b)){
                        continue;
                    }
                    client.handle(stuff);

                }
                catch(Exception ioe)
                {  
                    System.out.println("Listening error: " + ioe.getMessage());
                    client.stop();
                }

                
            }
        }


        // Sends message to client
        public void send(String msg)
        {   
            try
            {  

                      byte[] msg_data=null;

                    try {
                         Cipher myCipher = Cipher.getInstance("AES");
                        myCipher.init(Cipher.ENCRYPT_MODE, client.symetricKey);
                        msg_data = myCipher.doFinal(msg.getBytes());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }




                     String redisString2 = new String(Base64.getEncoder().encode(msg_data)); 
                     
                     client.streamOut.writeUTF(redisString2);
                     client.streamOut.flush();


                    client.sendSignature(msg);


            }
           
            catch(IOException ioexception)
            {  
                stop();
            }
        }
    }

