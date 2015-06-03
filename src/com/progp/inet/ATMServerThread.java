package com.progp.inet;
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
   @author Viebrapadata
*/
public class ATMServerThread extends Thread {
    private Socket socket = null;
    private BufferedReader in;
    PrintWriter out;
    
    final static String DATABASEURL = "jdbc:sqlite:database.db";
    
    private Language currentLanguage = Language.SWEDISH;
    
    public static final byte loginCode = 0b00000000;
    public static final byte statusCode = 0b00100000;
    public static final byte withdrawCode = 0b01000000;
    public static final byte depositCode = 0b01100000;
    public static final byte exitCode =  (byte) 0b11100000;
    
    public static final int SUCCESS = 1;
    public static final int FAIL = 0;
    
    public static final int CREDITCARDLENGTH = 16;
    public static final int PINLENGTH = 2;
	private static final int TIMEOUT = 5;
	
	
    
    
    public ATMServerThread(Socket socket) {
        super("ATMServerThread");
        this.socket = socket;
    }

    private String readLine() throws IOException {
        String str = in.readLine();
        System.out.println(""  + socket + " : " + str);
        return str;
    }
    
    public void sendMessage(ServerMessage message)
    {
    	String sql = "SELECT word FROM " + currentLanguage.getName() + " WHERE " +
    			" id=" + (message.ordinal() + 1);
    	
    	ArrayList<String> result = queryDatabase(sql);
    	
    	if(result.size() != 0)
    		out.println(result.get(0));
    }
    
    public void sendMessage(String message)
    {
    	out.println(message);
    }
    
    public void sendCode(int code)
    {
    	out.println(code);
    }

    public byte[] readBytes() throws IOException {
        // Again, probably better to store these objects references in the support class
        InputStream in = socket.getInputStream();
        DataInputStream dis = new DataInputStream(in);
        
        int len = dis.readInt();
        byte[] data = new byte[len];
        if (len > 0) {
            dis.readFully(data);
        }
        return data;
    }
    
  //What makes the SQL work
    public boolean updateDatabase(String query)
    {
    	Connection conn = null;
		try {
			conn = DriverManager.getConnection(DATABASEURL);
		} catch (SQLException e) {
			System.out.println("CANT CONNECT!");
			e.printStackTrace();
		}
        System.out.println("Connected to database");
        try {
            Statement stmt = conn.createStatement();
            System.out.println("Created statement");
            try 
            {
                stmt.setQueryTimeout(TIMEOUT);
                stmt.executeUpdate(query);
            } 
            finally 
            {
            	try { stmt.close(); } catch (Exception ignore) {}
            }
        } 
        catch (SQLException e) 
        {
			System.out.println("SQL ERROR BOYS");
			e.printStackTrace();
			return false;
		} 
        finally 
        {
        	try { conn.close(); } catch (Exception ignore) {}
        }
        return true;
    }
    
    //ArrayList for parsing SQL queries
    public ArrayList<String> queryDatabase(String query)
    {
    	Connection conn = null;
    	ResultSet rs = null;
    	
    	ArrayList<String> result = new ArrayList<String>();
    	
		try {
			conn = DriverManager.getConnection(DATABASEURL);
		} catch (SQLException e) {
			System.out.println("CANT CONNECT!");
			e.printStackTrace();
		}
        System.out.println("Connected to database");
        try {
            Statement stmt = conn.createStatement();
            System.out.println("Created statement");
            try {
                stmt.setQueryTimeout(TIMEOUT);
                System.out.println("Set timeout");
                rs = stmt.executeQuery(query);
                
                ResultSetMetaData rsmd = rs.getMetaData();
                
                int columns = rsmd.getColumnCount();
                
                while(rs.next())
                {
                	for(int i = 0; i < columns; i++)
                	{
                		result.add(rs.getString(i+1));
                	}
                }
                try 
                {
                } finally {
                    try { rs.close(); } catch (Exception ignore) {}
                }
            } finally {
                try { stmt.close(); } catch (Exception ignore) {}
            }
        } catch (SQLException e) 
        {
			System.out.println("SQL ERROR BOYS");
			e.printStackTrace();
		} finally {
            try { conn.close(); } catch (Exception ignore) {}
        }
        
        return result;
    }

    private boolean validateUser() throws IOException 
    {
    	byte[] result = readBytes();
    	
    	int paddingBytes = 1;
    	
    	StringBuilder creditcardSB = new StringBuilder();
    	
    	StringBuilder pinSB = new StringBuilder();
    	
    	for(int i = 0; i < CREDITCARDLENGTH / 2; i++)
    	{
    		Byte b = (byte) (result[i+paddingBytes] & 0xff);
			Byte highNibble = (byte) ((byte)(b >> 4) & 0x0f);
			Byte lowNibble = ((byte)(b & 0x0f));
    		creditcardSB.append(String.valueOf((int)highNibble));
    		creditcardSB.append(String.valueOf((int)lowNibble));
    	}
    	
    	for(int i = 0; i < PINLENGTH / 2; i++)
    	{
    		Byte b = (byte) (result[i+paddingBytes + CREDITCARDLENGTH / 2] & 0xff);
    		
    		Byte highNibble = (byte) ((byte)(b >> 4) & 0x0f);
			Byte lowNibble = ((byte)(b & 0x0f));
			pinSB.append(String.valueOf((int)highNibble));
			pinSB.append(String.valueOf((int)lowNibble));
    	}
    	
    	String sql = "SELECT * FROM User WHERE creditCardNumber="+creditcardSB.toString() + " AND pinCode=" + pinSB.toString();
    	
    	ArrayList<String> queryResult = queryDatabase(sql);
    	
    	if(queryResult.size() == 0)
    	{
    		sendCode(0);
    		return false;
    	}
    		
    	sendCode(1);
        return true;
    }

    public void run(){
         
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader
                (new InputStreamReader(socket.getInputStream()));
            
            String inputLine, outputLine;
	
            int balance = 1000;
            int value;
            
            //If we can't find the user
            if(validateUser() == false)
            {
            	sendMessage(ServerMessage.INVALIDLOGIN);
            	return;
            }
            
            sendMessage(ServerMessage.VALIDLOGIN);
            
            out.println("Welcome to Bank! (1)Balance, (2)Withdrawal, (3)Deposit, (4)Exit"); 
            inputLine = readLine();
            int choice = Integer.parseInt(inputLine);
            while (choice != 4) {
                int deposit = 1;
                switch (choice) {
                case 2:
                    deposit = -1;
                case 3:
                    out.println("Enter amount: ");	
                    inputLine= readLine();
                    value = Integer.parseInt(inputLine);
                    balance += deposit * value;
                case 1:
                    out.println("Current balance is " + balance + " dollars");
                    out.println("(1)Balance, (2)Withdrawal, (3)Deposit, (4)Exit");
                    inputLine=readLine();
                    choice = Integer.parseInt(inputLine);
                    break;
                case 4:
                    break;
                default: 
                    break;
                }
            }
            out.println("Good Bye");
            out.close();
            in.close();
            socket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    
    }
}
