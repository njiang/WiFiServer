package com.WiFiServer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

class PacketHandler extends Thread {
	WiFiServer server;
    
    private Socket clientSocket;
    private String rootFilePath = ".\\";
    private DataInputStream ins;
    private DataOutputStream outstream;

    public PacketHandler(WiFiServer server, Socket clientSocket)
    {
        this.server = server;
        this.clientSocket = clientSocket;
        this.rootFilePath = server.getDataPath();
        if (clientSocket != null) {
        	try {
        		this.ins = new DataInputStream(clientSocket.getInputStream());
        		this.outstream = new DataOutputStream(clientSocket.getOutputStream());
        	}
        	catch (Exception e) {
        		System.out.println("Failed to get input stream of client socket " + e.getMessage());
        	}
        }
    }


    public void run()
    {
    	if (this.ins == null)
    		return;
    	
    	while (true) {
    		try {
    			String input = this.ins.readUTF();
    			System.out.println("Received " + input);
    			if (input != null) {
    				if (input.contains("GetPassword")) {
    					String[] splitted = input.split(";;;");
    					String bssid = splitted[1];
    					System.out.println("BSSID: " + bssid);
    					if (bssid != null) {
    						String password = this.server.getPassword(bssid);
    						System.out.println("Got password " + password);
    						if (password != null)
    							this.outstream.writeUTF(password);
    						else
    							this.outstream.writeUTF("");
    						this.outstream.flush();
    					}
    				}
    			}
    		}
    		catch (Exception e) {
    			System.out.println("Failed to read data from socket client " + e.getMessage());
    			break;
    		}
    	}
    }
}

public class WiFiServer extends Thread {
	private String dataFilePath = "/home/ubuntu/WiFiServerData/wifi_info.txt";
	private int myPort = 5999;
	private ServerSocket serverSocket = null;
	private HashMap<String, String> BSSID_Password = new HashMap<String, String>();
	
	public WiFiServer(String[] args) {
		try {
            serverSocket = new ServerSocket(myPort);
        }
        catch (Exception e) {
            System.out.println("Failed to open server socket " + e.getMessage());
        }
		
		try {
			FileReader fileReader = new FileReader(this.dataFilePath);
            BufferedReader reader = new BufferedReader(fileReader);
            
            do {
                String line = reader.readLine();
                if (line == null)
                	break;
                String[] splitted = line.split("\t");
                String bssid = splitted[0];
                String password = splitted[1];
                System.out.println("BSSID: " + bssid + " Password: " + password);
                BSSID_Password.put(bssid, password);
            }
            while (true);
            reader.close();
            fileReader.close();
		}
		catch (Exception e) {
			System.out.println("Failed to read bssid password file " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public String getDataPath() { return dataFilePath; }
	public String getPassword(String bssid) {
		System.out.println("Checking password for " + bssid);
		if (this.BSSID_Password.containsKey(bssid)) {
			return this.BSSID_Password.get(bssid);
		}
		return null;
	}
	
	public void run() {
		while(true)
        {
            try {
                new PacketHandler(this, serverSocket.accept()).start();
            }
            catch (Exception e) {
                System.out.println("Failed to accept connections " + e.getMessage());
                e.printStackTrace();
            }
        }
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		WiFiServer server = new WiFiServer(args);
		server.start();
	}

}
