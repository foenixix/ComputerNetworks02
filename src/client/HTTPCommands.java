
package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.plaf.synth.SynthSeparatorUI;

enum HTTPCommands {
	

	GET{
		/**
		 * Checks if the given type is GET.
		 * 
		 * returns true if the given type equals GET without case sensitivity, else false.
		 */
		@Override
		protected boolean isCorrectType(String type) {
			return type.equalsIgnoreCase("GET");
		}

		@Override
		protected void executeRequest(Request request) throws IllegalArgumentException, IllegalStateException{
			Socket socket= getSocket(request);
			String host = prompt("Your host name: ");
			PrintWriter writer = sendRequest(request, socket, host); // includes the fileWriter
			FileWriter fw = initiateFileWriter("out.html"); // initiate the fileWriter with given fileName
			BufferedReader br = initBuffReader(socket); // initiate the BufferedReader
			System.out.println("RESULT: "); // Format info
			System.out.println("");		    // Format info
			manageOutput(fw, br,request.getURIHost(),writer,host,br); 	   // gets and writes the output of the GET command
			closeReaderWriter(fw, br); // Closes used writer and reader
			closeSocket(socket);
		}
		
		/**
		 * Closes the given FileWriter and BufferedReader.
		 * 
		 * @param fw
		 * 			The FileWriter to close.
		 * @param br
		 * 			The BufferedReader to close.
		 */
		private void closeReaderWriter(FileWriter fw, BufferedReader br) {
			try {
				br.close();
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Manages the result of the server by writing it to a file called out.html and printing
		 * it in the console.
		 * 
		 * @param fw
		 * 			FileWriter used to write the result of the server.
		 * @param br
		 * 			BufferedReader used to read the answer of the server.
		 * @param uriHost
		 * 			The host you are connecting to.
		 * @param writerToHost
		 * 			PrintWriter used to send information to the server.
		 * @param hostName
		 * 			String used as host for HTTP/1.1
		 * @param socketReader
		 * 			TODO not sure/ Same as br.
		 */
		private void manageOutput(FileWriter fw, BufferedReader br, String uriHost, PrintWriter writerToHost, String hostName, BufferedReader socketReader) {
			try {
				ArrayList<String> relativeImagePaths = new ArrayList<>();
				String line;
				int i=0;
				boolean headDone = false;
				while((line = br.readLine()) != null){
					System.out.println(line);
					if(line.isEmpty()){
						headDone=true;
					}
					if(headDone){
						fw.write(line+"\r\n");
					}
					relativeImagePaths.addAll(getRelativeImagePathsFromLine(line, uriHost));
					i++;
				}
				System.out.println("images: "+relativeImagePaths);
				//getFiles(relativeImagePaths,writerToHost, hostName,socketReader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 *TODO
		 * @param relativeFilePaths
		 * @param writerToHost
		 * @param hostName
		 * @param socketReader
		 * @throws IOException
		 */
		private void getFiles(ArrayList<String> relativeFilePaths, PrintWriter writerToHost, String hostName,BufferedReader socketReader) throws IOException {
			for (String relativePath : relativeFilePaths) {
				sendGetRequest(writerToHost, relativePath, hostName);
				saveFile(relativePath, socketReader);
			}
		}

		/**
		 * TODO
		 * @param relativePath
		 * @param socketReader
		 * @throws IOException
		 */
		private void saveFile(String relativePath, BufferedReader socketReader) throws IOException {
			if(relativePath.contains("/")){
				File newFile = new File("output/"+relativePath.substring(0,relativePath.indexOf("/")));
				Files.createDirectory(newFile.toPath());
			}
			FileWriter writer = initiateFileWriter(relativePath);
			String line;
			while((line = socketReader.readLine()) != null){
				writer.write(line+"\r\n");
			}
			writer.close();
		}

		/**
		 * Initiates the FileWriter with the given file name.
		 * 
		 * @param fileName
		 * 			Name for the output file.
		 * @return A FileWriter which writes to the the file with the given file name.
		 */
		private FileWriter initiateFileWriter(String fileName) {
			try {
				FileWriter result = new FileWriter("output/"+fileName);
				return result;
			} catch (IOException e1) {
				System.out.println("could not create the fileWriter for: "+fileName);
				e1.printStackTrace();
				throw new IllegalStateException();
			}
		}

		/**
		 * Initiates a PrintWriter that sends data to the server that this client
		 * is connected to and activates the actual request.
		 * 
		 * @param request
		 * 			This request contains the method, URI and port number.
		 * @param socket
		 * 			The socket that is connected to the server.
		 * @param host
		 * 			The host used in HTTP/1.1
		 * @return	A PrintWriter which sends data to the connected server.
		 */
		private PrintWriter sendRequest(Request request, Socket socket, String host){
			PrintWriter pw;
			try {
				pw = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException();
			}
			sendGetRequest(pw,request.getURIFile(), host);
			return pw;
		}
		
		/**
		 * Sends the actual GET request to the connected server.
		 * 
		 * @param writer
		 * 			The PrintWriter that sends the data to the connected server.
		 * @param filePath
		 * 			The path of the file the client wants to get.
		 * @param host
		 * 			The host used for HTTP/1.1.
		 */
		private void sendGetRequest(PrintWriter writer,String filePath, String host){
			writer.println("GET "+filePath+ " HTTP/1.1");
			writer.println("Host: "+host);
			writer.println("");
			writer.flush();
		}
		
		/**
		 * Gets the relative path of the images in the given line.
		 * 
		 * @param line
		 * 			A String of HTML possibly containing the path of images.
		 * @param uriHost
		 * 			The host of the URI this client is connected to.
		 * @return An ArrayList<String> with all the relative paths of all images found in the given line.
		 */
		private ArrayList<String> getRelativeImagePathsFromLine(String line, String uriHost){
			ArrayList<String> result = new ArrayList<>();
			int index =line.indexOf("<img");
			while(index!=-1){
				//System.out.print("index of <img: "+index);
				index = line.indexOf("src", index);
				//System.out.print("\t index of src: "+index);
				index = line.indexOf("\"",index);
				//System.out.print("\t index of \": "+index);
				int endIndex =line.indexOf("\"",index+1);
				//System.out.println("\t endIndex: "+endIndex);
				String cutImage =  removeHost(line.substring(index+1,endIndex),uriHost);
				if(!isAbsolutePath(cutImage)){
					result.add(cutImage);
				}
				index = line.indexOf("<img",index);
			}
			return result;
		}

		/**
		 * Checks if the given cutImage is an absolute path.
		 * 
		 * @param cutImage
		 * 			The String to check.
		 * @return True if the given cutImage contains :// or it has a length larger than 2 and the
		 * 			first three characters are www, else false.
		 */
		private boolean isAbsolutePath(String cutImage) {
			return cutImage.contains("://") || (cutImage.length()>2 && cutImage.substring(0,3).equals("www"));
		}
		/**
		 * Removes the host from the given URL. 
		 * 
		 * @param url
		 * 			The URL to remove the host from.
		 * @param host
		 * 			The host to remove from the given URL.
		 * @return A string which contains the given URL without the given host.
		 */
		private String removeHost(String url, String host){
			if(! url.contains(host))
				return url;
			return url.substring(url.indexOf(host)+host.length(), url.length());
		}
		
	},
	HEAD{
		/**
		 * Checks if the given type is HEAD.
		 * 
		 * returns true if the given type equals HEAD without case sensitivity, else false.
		 */
		@Override
		protected boolean isCorrectType(String type) {
			return type.equalsIgnoreCase("HEAD");
		}

		@Override
		public void executeRequest(Request request) throws IllegalArgumentException, IllegalStateException{
			Socket socket= getSocket(request);
			String host = prompt("Your host name: ");
			sendRequest(request, socket, host); // includes the fileWriter
			BufferedReader br = initBuffReader(socket); // initiate the BufferedReader
			System.out.println("RESULT: "); // Format info
			System.out.println("");		    // Format info
			manageOutput(br); 	   // gets and writes the output of the GET command
			closeReader(br); // Closes used writer and reader
			closeSocket(socket);	
		}
		
		private void manageOutput(BufferedReader br) {
			try {
				String line;
				while((line = br.readLine()) != null){
					System.out.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Initiates a PrintWriter that sends data to the server that this client
		 * is connected to and activates the actual request.
		 * 
		 * @param request
		 * 			This request contains the method, URI and port number.
		 * @param socket
		 * 			The socket that is connected to the server.
		 * @param host
		 * 			The host used in HTTP/1.1
		 */
		private void sendRequest(Request request, Socket socket, String host){
			PrintWriter pw;
			try {
				pw = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException();
			}
			sendHeadRequest(pw,request.getURIFile(), host);
		}
		
		/**
		 * Sends the actual HEAD request to the connected server.
		 * 
		 * @param writer
		 * 			The PrintWriter that sends the data to the connected server.
		 * @param filePath
		 * 			The path of the file the client wants to get.
		 * @param host
		 * 			The host used for HTTP/1.1.
		 */
		private void sendHeadRequest(PrintWriter writer,String filePath, String host){
			writer.println("HEAD "+filePath+ " HTTP/1.1");
			writer.println("Host: "+host);
			writer.println("");
			writer.flush();
		}
				
	},
	PUT{
		/**
		 * Checks if the given type is PUT.
		 * 
		 * returns true if the given type equals PUT without case sensitivity, else false.
		 */
		@Override
		protected boolean isCorrectType(String type) {
			return type.equalsIgnoreCase("PUT");
		}

		@Override
		protected void executeRequest(Request request) {
			Socket socket= getSocket(request);
			String host = prompt("Your host name: ");
			// standaard body heeft de vorm: param=value
			String body = promptLines("Your message: ");
			System.out.println("body with promptPUTBody is: " + body); // TODO debug info
			sendRequest(request, socket, host, body); // includes the fileWriter
			BufferedReader br = initBuffReader(socket); // initiate the BufferedReader
			System.out.println("RESULT: "); // Format info
			System.out.println("");		    // Format info
			manageOutput(br); 	   // gets and writes the output of the GET command
			closeReader(br); // Closes used writer and reader
			closeSocket(socket);
		}
		/*
		 * ik ben niet zeker of dit text/http of application/x-www-form-urlencoded moet zijn
		 */
		
		private final String textType = "text/http";
		
		private void manageOutput(BufferedReader br) {
			try {
				String line;
				while((line = br.readLine()) != null){
					System.out.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void sendRequest(Request request, Socket socket, String host, String body){
			PrintWriter pw;
			try {
				pw = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException();
			}
			// ik weet niet zeker of de filepath nu (request.getURIHost()+request.getURIFile() )
			// moet zijn of gewoon (request.getURIFile)
			sendPutRequest(pw,request.getURIFile(), host, body);
		}
		
		/*
		 * Vergeet zeker de blanco lijnen niet!
		 */
		private void sendPutRequest(PrintWriter writer,String filePath, String host, String body){
			System.out.println("PUT " +filePath+ " HTTP/1.1");//TODO debug info
			System.out.println("Host: " + host);//TODO debug info
			System.out.println("Content-Type: " + textType);//TODO debug info
			System.out.println("Content-Length: " + body.length());//TODO debug info
			System.out.println("");//TODO debug info
			System.out.println(body);//TODO debug info
			System.out.println("");//TODO debug info
			writer.println("PUT "+filePath+ " HTTP/1.1");
			writer.println("Host: "+host);
			writer.println("Content-Type: " + textType);
			writer.println("Content-Length: " + body.length());
			writer.println("");
			writer.println(body);
			writer.println("");
			writer.flush();
		}
	},
	POST{
		/**
		 * Checks if the given type is POST.
		 * 
		 * returns true if the given type equals POST without case sensitivity, else false.
		 */
		@Override
		protected boolean isCorrectType(String type) {
			return type.equalsIgnoreCase("POST");
		}
		
		@Override
		protected void executeRequest(Request request) {
			Socket socket= getSocket(request);
			String host = prompt("Your host name: ");
			// standaard body heeft de vorm: param=value
			String body = prompt("Your message body: ");
			sendRequest(request, socket, host, body); // includes the fileWriter
			BufferedReader br = initBuffReader(socket); // initiate the BufferedReader
			System.out.println("RESULT: "); // Format info
			System.out.println("");		    // Format info
			manageOutput(br); 	   // gets and writes the output of the GET command
			closeReader(br); // Closes used writer and reader
			closeSocket(socket);
		}
		/*
		 * ik ben niet zeker of dit text/http of application/x-www-form-urlencoded moet zijn
		 */
		private final String textType = "text/http";
		
		private void manageOutput(BufferedReader br) {
			try {
				String line;
				while((line = br.readLine()) != null){
					System.out.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void sendRequest(Request request, Socket socket, String host, String body){
			PrintWriter pw;
			try {
				pw = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException();
			}
			// ik weet niet zeker of de filepath nu (request.getURIHost()+request.getURIFile() )
			// moet zijn of gewoon (request.getURIFile)
			sendPostRequest(pw,request.getURIFile(), host, body);
		}
		
		/*
		 * Vergeet zeker de blanco lijnen niet!
		 */
		private void sendPostRequest(PrintWriter writer,String filePath, String host, String body){
			System.out.println("POST " +filePath+ " HTTP/1.1");//TODO debug info
			System.out.println("Host: " + host);//TODO debug info
			System.out.println("Content-Type: " + textType);//TODO debug info
			System.out.println("Content-Length: " + body.length());//TODO debug info
			System.out.println("");//TODO debug info
			System.out.println(body);//TODO debug info
			System.out.println("");//TODO debug info
			writer.println("POST "+filePath+ " HTTP/1.1");
			writer.println("Host: "+host);
			writer.println("Content-Type: " + textType);
			writer.println("Content-Length: " + body.length());
			writer.println("");
			writer.println(body);
			writer.println("");
			writer.flush();
		}		
	};
	/**
	 * This scanner is used for extra input from the user.
	 */
	protected static Scanner scanner = new Scanner(System.in);

	/**
	 * Checks if the given type if correct.
	 * 
	 * @param type
	 * 			The type to check.
	 * @return True if the type is correct, else false.
	 */
	protected abstract boolean isCorrectType(String type);
	
	/**
	 * Executes the given request.
	 * 
	 * @param request
	 * 			The request to execute.
	 * @throws IllegalArgumentException
	 * 			When an IOException occurs when trying to create a PrintWriter.
	 * 			When an IOException occurs when trying to create a BufferedReader.
	 * 			When an IOException occurs when trying to get the Socket.
	 * 			When an UnknownHostException occurs when trying to get the Socket.
	 * 			
	 * @throws IllegalStateException
	 * 			When an IOException occurs when trying to close the socket.
	 * 		    When an IOException occurs when trying create a FileWriter.
	 */
	protected abstract void executeRequest(Request request) throws IllegalArgumentException, IllegalStateException;

	private void sendRequest(Request request, Socket socket, String host){
		PrintWriter pw;
		try {
			pw = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException();
		}
		sendHeadRequest(pw,request.getURIFile(), host);
	}
	private void sendHeadRequest(PrintWriter writer,String filePath, String host){
		writer.println("HEAD "+filePath+ " HTTP/1.1");
		writer.println("Host: "+host);
		writer.println("");
		writer.flush();
	}

	protected static HTTPCommands getType(String type){
		for (HTTPCommands command : HTTPCommands.values()) {
			if(command.isCorrectType(type)){
				return command;
			}
		}
		return null;
	}
	
	/**
	 * Prints the given message and returns the first word input of the user.
	 * All other input will be ignored and deleted.
	 * 
	 * @param message
	 * 			What the scanner asks as input from the user.
	 * @return The first word input of the user.
	 */
	protected static String prompt(String message){
		System.out.print(message);
    	String result = scanner.next();
    	if (scanner.hasNext()){
    		scanner.nextLine();
    	}
	    return result;
	}
	
	/**
	 * Prints the given message and returns the input of the user.
	 * 
	 * @param message
	 * 			What the scanner asks as input from the user.
	 * @return The input of the user.
	 */
	protected static String promptLines(String message){
		System.out.print(message);
    	String result = scanner.next();
    	if (scanner.hasNextLine()){
	    	result += scanner.nextLine();
    	}
	    return result;
	}
	
	protected static Socket getSocket(Request request){
		try {
			return new Socket(request.getURIHost(), request.getPort());
		} catch (UnknownHostException e) {
			System.out.println("The given uri isn't a valid host.");
			throw new IllegalArgumentException();
		} catch (IOException e) {
			System.out.println("Unable to connect to: "+request.getURIHost()+":"+request.getPort());
			throw new IllegalArgumentException();
		}
	}
	
	protected static void closeSocket(Socket socket){
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("could not close the socket");
			throw new IllegalStateException();
		}
	}
	
	private static BufferedReader initBuffReader(Socket socket) throws IllegalArgumentException{
		try {			
			BufferedReader result = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			return result;
		} catch (IOException e) {
			System.out.println("could not get the inputStream of the socket");
			throw new IllegalArgumentException();
		}
	}
	
	
	/**
	 * Closes the given BufferedReader.
	 * 
	 * @param br
	 * 			The BufferedReader to close.
	 */
	protected static void closeReader(BufferedReader br) {
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
