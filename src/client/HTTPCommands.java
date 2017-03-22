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

public enum HTTPCommands {
	

	GET{

		@Override
		public boolean isCorrectType(String type) {
			return type.equalsIgnoreCase("GET");
		}

		@Override
		public void execute(Request request) throws IllegalArgumentException, IllegalStateException{
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

		private void closeReaderWriter(FileWriter fw, BufferedReader br) {
			try {
				br.close();
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

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

		private void getFiles(ArrayList<String> relativeFilePaths, PrintWriter writerToHost, String hostName,BufferedReader socketReader) throws IOException {
			for (String relativePath : relativeFilePaths) {
				sendGetRequest(writerToHost, relativePath, hostName);
				saveFile(relativePath, socketReader);
			}
		}

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

		private BufferedReader initBuffReader(Socket socket) throws IllegalArgumentException{
			try {			
				BufferedReader result = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				return result;
			} catch (IOException e) {
				System.out.println("could not get the inputStream of the socket");
				throw new IllegalArgumentException();
			}
		}

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
		
		private void sendGetRequest(PrintWriter writer,String filePath, String host){
			writer.println("GET "+filePath+ " HTTP/1.1");
			writer.println("Host: "+host);
			writer.println("");
			writer.flush();
		}
		
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

		private boolean isAbsolutePath(String cutImage) {
			return cutImage.contains("://") || (cutImage.length()>2 && cutImage.substring(0,3).equals("www"));
		}
		
		private String removeHost(String url, String host){
			if(! url.contains(host))
				return url;
			return url.substring(url.indexOf(host)+host.length(), url.length());
		}
		
	},
	HEAD{

		@Override
		public boolean isCorrectType(String type) {
			return type.equalsIgnoreCase("HEAD");
		}

		@Override
		public void execute(Request request) throws IllegalArgumentException, IllegalStateException{
			Socket socket= getSocket(request);
			String host = prompt("Your host name: ");
			PrintWriter writer = sendRequest(request, socket, host); // includes the fileWriter
			FileWriter fw = initiateFileWriter("outHead.html"); // initiate the fileWriter with given fileName
			BufferedReader br = initBuffReader(socket); // initiate the BufferedReader
			System.out.println("RESULT: "); // Format info
			System.out.println("");		    // Format info
			manageOutput(fw, br,request.getURIHost(),writer,host,br); 	   // gets and writes the output of the GET command
			closeReaderWriter(fw, br); // Closes used writer and reader
			closeSocket(socket);	
		}
		
		private void manageOutput(FileWriter fw, BufferedReader br, String uriHost, PrintWriter writerToHost, String hostName, BufferedReader socketReader) {
			try {
				ArrayList<String> relativeImagePaths = new ArrayList<>();
				String line;
				int i=0;
				while((line = br.readLine()) != null){
					System.out.println(line);
					if(i>6){
						fw.write(line+"\r\n");
					}
					//relativeImagePaths.addAll(getRelativeImagePathsFromLine(line, uriHost));
					i++;
				}
				//System.out.println("images: "+relativeImagePaths);
				//getFiles(relativeImagePaths,writerToHost, hostName,socketReader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private PrintWriter sendRequest(Request request, Socket socket, String host){
			PrintWriter pw;
			try {
				pw = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException();
			}
			sendHeadRequest(pw,request.getURIFile(), host);
			return pw;
		}
		
		private void saveFile(String relativePath, BufferedReader socketReader) throws IOException {
			if(relativePath.contains("/")){
				File newFile = new File("outputHead/"+relativePath.substring(0,relativePath.indexOf("/")));
				Files.createDirectory(newFile.toPath());
			}
			FileWriter writer = initiateFileWriter(relativePath);
			String line;
			while((line = socketReader.readLine()) != null){
				writer.write(line+"\r\n");
			}
			writer.close();
		}
		
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

		private BufferedReader initBuffReader(Socket socket) throws IllegalArgumentException{
			try {			
				BufferedReader result = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				return result;
			} catch (IOException e) {
				System.out.println("could not get the inputStream of the socket");
				throw new IllegalArgumentException();
			}
		}
		
		private void sendHeadRequest(PrintWriter writer,String filePath, String host){
			writer.println("HEAD "+filePath+ " HTTP/1.1");
			writer.println("Host: "+host);
			writer.println("");
			writer.flush();
		}
		
		private String removeHost(String url, String host){
			if(! url.contains(host))
				return url;
			return url.substring(url.indexOf(host)+host.length(), url.length());
		}
		
		private void closeReaderWriter(FileWriter fw, BufferedReader br) {
			try {
				br.close();
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	},
	PUT{

		@Override
		public boolean isCorrectType(String type) {
			return type.equalsIgnoreCase("PUT");
		}

		@Override
		public void execute(Request request) {
			// TODO Auto-generated method stub
			
		}
		
	},
	POST{

		@Override
		public boolean isCorrectType(String type) {
			return type.equalsIgnoreCase("POST");
		}

		@Override
		public void execute(Request request) {
			// TODO Auto-generated method stub
			
		}
		
	};
	public static Scanner scanner = new Scanner(System.in);

	public abstract boolean isCorrectType(String type);
	public abstract void execute(Request request) throws IllegalArgumentException, IllegalStateException;

	public static HTTPCommands getType(String type){
		for (HTTPCommands command : HTTPCommands.values()) {
			if(command.isCorrectType(type)){
				return command;
			}
		}
		return null;
	}
	
	public static String prompt(String message){
		System.out.print(message);
    	String result = scanner.next();
	    return result;
	}
	
	public static Socket getSocket(Request request){
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
	
	public static void closeSocket(Socket socket){
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("could not close the socket");
			throw new IllegalStateException();
		}
	}
}
