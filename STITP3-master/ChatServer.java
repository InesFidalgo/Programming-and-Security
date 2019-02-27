	import javax.crypto.*;
	import javax.crypto.KeyGenerator;
	import javax.crypto.SecretKey;
	import javax.crypto.spec.SecretKeySpec;
	import java.net.*;
	import java.io.*;
	import java.util.*;
	import java.security.*;
	import java.security.cert.*;
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.List;
	import java.util.Base64;


	public class ChatServer implements Runnable
	{  
		public ChatServerThread clients[] = new ChatServerThread[20];
		private ServerSocket server_socket = null;
		private Thread thread = null;
		public int clientCount = 0;
	    protected KeyPair serverSigKeyPair    = null;
	    public PublicKey server_pubKey  = null;
	    public PrivateKey server_privKey  = null;
	    public MenuThread menuT = null;
	    public Thread menuThread = null;
	    public ArrayList<User> users =null;


		public ChatServer(int port)
	    	{  

				     users =new ArrayList<User>();
				     readUsers();
			try
	      		{  
	            		// Binds to port and starts server
						System.out.println("Binding to port " + port);
	            		server_socket = new ServerSocket(port);  
	            		System.out.println("Server started: " + server_socket);

			    		menuT = new MenuThread(this);
			    		menuThread=new Thread(menuT);
			    		menuThread.start();
	            		start();
	        	}
	      		catch(IOException ioexception)
	      		{  
	            		// Error binding to port
	            		System.out.println("Binding error (port=" + port + "): " + ioexception.getMessage());
	        	}
	    	}
	    
	    	public void run()
	    	{  

	        	while (thread != null)
	        	{  
	            		try
	            		{  
	                		// Adds new thread for new client
	                		System.out.println("Waiting for a client ..."); 
	                		addThread(server_socket.accept()); 
	            		}
	            		catch(IOException ioexception)
	            		{
	                		System.out.println("Accept error: " + ioexception); stop();
	            		}
	        	}
	    	}


       
        public void readUsers(){
        	try{
	        	FileInputStream fin = new FileInputStream("users");
				ObjectInputStream ois = new ObjectInputStream(fin);
				users = (ArrayList<User>) ois.readObject();

				fin.close();
			}
			catch( Exception e){
	        	users=new ArrayList<User>();

			}
        }

        public void writeUsers(){
        	try{
	        	FileOutputStream fin = new FileOutputStream("users");
				ObjectOutputStream ois = new ObjectOutputStream(fin);
				ois.writeObject(users);
			}
			catch( Exception e){
	        	users=new ArrayList<User>();

			}
        }
	    
	   	public void start()
	    	{  
	        	if (thread == null)
	        	{  
	            		// Starts new thread for client
	            		thread = new Thread(this); 
	            		thread.start();

	        	}
	    	}
	    
	    	public void stop()
	    	{  
	        	if (thread != null)
	        	{
	            		// Stops running thread for client
	            		thread.stop(); 
	            		menuThread.stop(); 
	            		thread = null;
	        	}
	    	}
	   
	    	public int findClient(int ID)
	    	{  
	        	// Returns client from id
	        	for (int i = 0; i < clientCount; i++)
	            		if (clients[i].getID() == ID)
	                		return i;
	        	return -1;
	    	}
	    
	    	public synchronized void handle(int ID, String username, String msg)
	    	{  	



	        	if (msg.equals(".quit"))
	            	{  
	                	int leaving_id = findClient(ID);
	                	// Client exits
	                	clients[leaving_id].send(".quit");
	                	// Notify remaing users
	                	for (int i = 0; i < clientCount; i++)
	                    		if (i!=leaving_id)
	                        		clients[i].send("Client " +ID + " exits..");
	                	remove(ID);
	            	}




	        	else if (msg.equals(".refresh"))
            	{  

	                	int refreshing_id = findClient(ID);
	                	try{
	                		System.out.println("Refreshing key for client "+ID);
		                	clients[refreshing_id].secondHandShake();
		                	clients[refreshing_id].thirdHandShake();

	                	}
	                	catch (Exception e){}
            	}
	        	else{
	            		// Brodcast message for every other client online
	            		for (int i = 0; i < clientCount; i++)
	            			if(menuT.blacklist.contains(clients[i].username)){ 
	            				clients[i].send("broo, you are blacklisted");
	            				remove(clients[i].ID);
	                	
	            			}
							else	            			
	                			clients[i].send("("+ID+")"+username + ": " + msg);
	                		
	                
	            }
	    	}
	    
	    	public synchronized void remove(int ID)
	    	{  
	        	int pos = findClient(ID);
	      
	       	 	if (pos >= 0)
	        	{  
	            		// Removes thread for exiting client
	            		ChatServerThread toTerminate = clients[pos];
	            		System.out.println("Removing client thread " + ID + " at " + pos);
	            		if (pos < clientCount-1)
	                		for (int i = pos+1; i < clientCount; i++)
	                    			clients[i-1] = clients[i];
	            		clientCount--;
	         
	            		try
	            		{  
	                		toTerminate.close(); 
	            		}
	         
	            		catch(IOException ioe)
	            		{  
	                		System.out.println("Error closing thread: " + ioe); 
	            		}
	         
	            		toTerminate.stop(); 
	        	}
	    	}


	    
	    
	    	private void addThread(Socket socket)
	    	{  
	    	    	if (clientCount < clients.length)
	        	{  
	            		// Adds thread for new accepted client
	            		System.out.println("Client accepted: " + socket);

	            		clients[clientCount] = new ChatServerThread(this, socket);
  
	         
	           		try
	            		{  
	                		clients[clientCount].open(); 
	                		clients[clientCount].start();  
	                		clientCount++; 
	            		}
	            		catch(IOException ioe)
	            		{  
	               			System.out.println("Error opening thread: " + ioe); 
	            		}
	       	 	}
	        	else
	            		System.out.println("Client refused: maximum " + clients.length + " reached.");
	    	}
	    
	    
		public static void main(String args[])
	   	{  
	        	ChatServer server = null;
	        
	        	if (args.length != 1)
	            		// Displays correct usage for server
	            		System.out.println("Usage: java ChatServer port");
	        	else
	            		// Calls new server
	            		server = new ChatServer(Integer.parseInt(args[0]));
	    	}

	}

	class ChatServerThread extends Thread
	{  
	    private ChatServer       server    = null;
	    private Socket           socket    = null;
	    public int              ID        = -1;
	    private ObjectInputStream  streamIn  =  null;
	    private ObjectOutputStream streamOut = null;
		private PublicKey client_pubKey 			= null;
		private SecretKey client_symetricKey 			= null;
        public String username  = null;
        public SecretKey tempKey=null;

	   
	    public ChatServerThread(ChatServer _server, Socket _socket)
	    {  
	        super();
	        server = _server;
	        socket = _socket;
	        ID     = socket.getPort();
	    }
	    


        public int firstHandShake() throws IOException, Exception{


			KeyPairGenerator keypairgenerator = KeyPairGenerator.getInstance("RSA");
			keypairgenerator.initialize(2048);


			server.serverSigKeyPair = keypairgenerator.generateKeyPair();

            server.server_pubKey=server.serverSigKeyPair.getPublic();
            server.server_privKey=server.serverSigKeyPair.getPrivate();

			ByteArrayOutputStream bo = new ByteArrayOutputStream();
             ObjectOutputStream so = new ObjectOutputStream(bo);
             so.writeObject(server.server_pubKey);
             so.flush();
             String redisString = new String(Base64.getEncoder().encode(bo.toByteArray()));      				



            streamOut.writeUTF(redisString);






            streamOut.flush();



            String msg = streamIn.readUTF();


			byte b[] = Base64.getDecoder().decode(msg.getBytes()); 
             ByteArrayInputStream bi = new ByteArrayInputStream(b);
             ObjectInputStream si = new ObjectInputStream(bi);
             client_pubKey = (PublicKey)si.readObject();


            return 0;
        }

        public int secondHandShake() throws IOException, Exception{


        	byte[] b=null;
            String msg = streamIn.readUTF();



			 b = Base64.getDecoder().decode(msg.getBytes());


            Cipher myCipher=null;
            byte[] data= null;
            try {
                myCipher = Cipher.getInstance("RSA");
                myCipher.init(Cipher.DECRYPT_MODE, client_pubKey);
                
                data = myCipher.doFinal(b);

            } catch (Exception e) {
                e.printStackTrace();
            }


            tempKey = new SecretKeySpec(data, 0, data.length, "AES");

            return 0;
        }


        public int thirdHandShake() throws IOException, Exception{
        

                 //byte[] signatureBytes=data;
        		byte[] signatureBytes=tempKey.getEncoded();





                 String msg = streamIn.readUTF();





                 byte[] b=null;

				 b = Base64.getDecoder().decode(msg.getBytes()); 
                 
				 byte[] data=null;



                Cipher myCipher = Cipher.getInstance("AES");
		        myCipher.init(Cipher.DECRYPT_MODE, tempKey);
		        
		        data = myCipher.doFinal(b);
                




	            Signature sig = Signature.getInstance("SHA256withRSA");
	            sig.initVerify(client_pubKey);
	            sig.update(signatureBytes);


               	if(sig.verify(data)){
	                client_symetricKey=tempKey;
       			     return 0;
               	}


        		return 1;



        }


	    public int fourthHandShake() throws IOException, Exception{


                 //byte[] signatureBytes=data;
    		byte[] signatureBytes=tempKey.getEncoded();

    		String received=streamIn.readUTF();

            byte[]   b=null;

             b = Base64.getDecoder().decode(received.getBytes()); 
             
             byte[]  data=null;
             Cipher myCipher=null;

             try{
	             myCipher = Cipher.getInstance("AES");
	            myCipher.init(Cipher.DECRYPT_MODE, client_symetricKey);
	            
	            data = myCipher.doFinal(b);
            }
            catch (Exception e){}

            
            
            username=new String(data);

           







                 String msg = streamIn.readUTF();





                  b=null;

				b = Base64.getDecoder().decode(msg.getBytes()); 
                  
                



	            Signature sig = Signature.getInstance("SHA256withRSA");
	            sig.initVerify(client_pubKey);
	            sig.update(username.getBytes());

               	if(sig.verify(b)){
               	}
               	else{
   			         return 1;
               	}








    		 received=streamIn.readUTF();

               b=null;

             b = Base64.getDecoder().decode(received.getBytes()); 
             
             data=null;


             try{
	             myCipher = Cipher.getInstance("AES");
	            myCipher.init(Cipher.DECRYPT_MODE, client_symetricKey);
	            
	            data = myCipher.doFinal(b);
            }
            catch (Exception e){}
            
            String password=new String(data);








                  msg = streamIn.readUTF();





                  b=null;

				b = Base64.getDecoder().decode(msg.getBytes()); 
                  
                



	             sig = Signature.getInstance("SHA256withRSA");
	            sig.initVerify(client_pubKey);
	            sig.update(password.getBytes());

               	if(sig.verify(b)){
               	}
               	else{
   			         return 1;
               	}









            for(int i=0;i<server.users.size();i++){
            	if(server.users.get(i).getUsername().equals(username)&&server.users.get(i).getPassword().equals(password)){
            		
            		return 0;
            	}  
	          	else if(server.users.get(i).getUsername().equals(username)&&!server.users.get(i).getPassword().equals(password)){
            		return 1;
            	}
            	


            }
    		server.users.add(new User(username,password));
    	

            return 2;

        
        }



		 public void handshake(){
	         try
	           {  


	               

            		System.out.println("1st handshake");
                    firstHandShake();


            		System.out.println("2nd handshake");
                    secondHandShake();

            		System.out.println("3rd handshake");
            		int j=thirdHandShake();
                    if(j==1){
                    	send("something went wrong");
              		  	server.remove(ID);

                    }

            		


            		System.out.println("4th handshake (login)");
                    int i=fourthHandShake();
                    if(i==0){
                    	send("broo, you are cool");
                    }
                    else if(i==1){
                    	send("broo, your password is wrong");
                    	server.remove(ID);
                    }
                    else{
                    	send("broo, you are new");


                    }


               		System.out.println("Authentication successfull");


	     			if(server.menuT.blacklist.contains(username)){
	               		send("broo, you are blacklisted");
		            	server.remove(ID);
	               	}

	               	server.writeUsers();
	                				                 			


	               	




					



	                  

	 	



	 				
	             
	           }
	         
	           catch(IOException ioexception)
	           {  
	               System.out.println("Error sending string to server: " + ioexception.getMessage());
	               stop();
	           }
	            catch (ClassNotFoundException e) {
	                e.printStackTrace();
	            }
	            
	            catch (Exception e1){
	                e1.printStackTrace();
	                stop();
	                return;
	            }

	    }


        public void sendSignature( String message){
            
        	try{

            byte[] signatureBytes=null;

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(server.server_privKey);
            sig.update(message.getBytes());
            signatureBytes = sig.sign();



            String redisString2 = new String(signatureBytes); 
             
             streamOut.writeUTF(redisString2);
             streamOut.flush();

         	}
         	catch(Exception e){
            

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
	                    myCipher.init(Cipher.ENCRYPT_MODE, client_symetricKey);
	                    msg_data = myCipher.doFinal(msg.getBytes());

	                } catch (Exception e) {
	                    e.printStackTrace();
	                }





	                 String redisString2 = new String(Base64.getEncoder().encode(msg_data)); 
	                 
	                 streamOut.writeUTF(redisString2);
	                 streamOut.flush();


	                 sendSignature( msg);

	        }
	       
	        catch(IOException ioexception)
	        {  
	            System.out.println(ID + " ERROR sending message: " + ioexception.getMessage());
	            server.remove(ID);
	            stop();
	        }
	    }
	    
	    // Gets id for client
	    public int getID()
	    {  
	        return ID;
	    }


	    // Runs thread
	    public void run()
	    {  
	        System.out.println("Server Thread " + ID + " running.");

	        handshake();

	        while (true)
	        {  
	            try
	            {  

		    		String input;




		    		String received=(String) streamIn.readUTF();




		             byte[] b=null;

		             b = Base64.getDecoder().decode(received.getBytes()); 
		             
		             byte[] data=null;


		             try{
			            Cipher myCipher = Cipher.getInstance("AES");
			            myCipher.init(Cipher.DECRYPT_MODE, client_symetricKey);
			            
			            data = myCipher.doFinal(b);
		            }
		            catch (Exception e){}

		            input=new String(data);




					if(checkSignature(data)){

                        continue;
                    }
                	server.handle(ID, username, input);



		            
	            }
	         
	            catch(IOException ioe)
	            {  
	                System.out.println(ID + " ERROR reading: " + ioe.getMessage());
	                server.remove(ID);
	                stop();
	            }
	            catch (Exception e){

	            }
	        }
	    }

	    public boolean checkSignature( byte[] match){

	    	try{


	              String msg = streamIn.readUTF();


	            byte[] b = Base64.getDecoder().decode(msg.getBytes()); 
	              
	            
	            Signature sig = Signature.getInstance("SHA256withRSA");
	            sig.initVerify(client_pubKey);
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
	    
	    
	    // Opens thread
	    public void open() throws IOException
	    {  
	        streamIn = new ObjectInputStream(socket.getInputStream());
			streamOut = new ObjectOutputStream(socket.getOutputStream());
	    }
	    
	    // Closes thread
	    public void close() throws IOException
	    {  
	        if (socket != null)    socket.close();
	        if (streamIn != null)  streamIn.close();
	        if (streamOut != null) streamOut.close();
	    }
	    
	}


    class MenuThread extends Thread
    {  
        public ArrayList<String> blacklist=null;
	    private ChatServer       server    = null;
	    DataInputStream console   = null;

        public MenuThread(ChatServer server)
        {  
        	this.server=server;

            console   = new DataInputStream(System.in);
        }
       
        public void readFromFile(){
        	try{
	        	FileInputStream fin = new FileInputStream("blacklist");
				ObjectInputStream ois = new ObjectInputStream(fin);
				blacklist = (ArrayList<String>) ois.readObject();

				fin.close();
			}
			catch( Exception e){
	        	blacklist=new ArrayList<String>();

			}
        }

        public void writeToFile(){
        	try{
	        	FileOutputStream fin = new FileOutputStream("blacklist");
				ObjectOutputStream ois = new ObjectOutputStream(fin);
				ois.writeObject(blacklist);
			}
			catch( Exception e){
	        	blacklist=new ArrayList<String>();

			}
        }

        public boolean checkIfInClients(String stuff){
    		for(int i=0;i<server.clientCount;i++){

    			if(server.clients[i].username.equals(stuff))
    				return true;

    		}
    		return false;
        }
        
        public void run()
        {  
        	readFromFile();

        	while(true){
        		System.out.println("\n\n\n\nChoose the username to blacklist (you can press enter to refresh)");
        		for(int i=0;i<server.clientCount;i++){
        			
        			if(!blacklist.contains(server.clients[i].username)){
	        			System.out.println(" - "+server.clients[i].username);
	        		}



        		}
        		try{
		    		String input=console.readLine();
		    		if(checkIfInClients(input)){

	        			blacklist.add(input);
	        			for(int j=0;j<server.clientCount;j++){
	        				if(server.clients[j].username.equals(input)){
	        					server.clients[j].send( "broo, you are blacklisted");
	        				}
	        			}
	        			writeToFile();
		    		}

        		}
        		catch(Exception e){
        			System.out.println("Can't do this");
        		}

        		System.out.println("\n\n\n\n");

        	}

        }

    }