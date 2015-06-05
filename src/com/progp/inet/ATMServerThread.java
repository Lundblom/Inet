package com.progp.inet;
import java.io.*;
import java.io.ObjectInputStream.GetField;
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
    
    private Language currentLanguage = Language.ENGLISH;
    
    public static final byte loginCode = 0b00000000;
    public static final byte statusCode = 0b00100000;
    public static final byte withdrawCode = 0b01000000;
    public static final byte depositCode = 0b01100000;
    public static final byte messageCode = (byte)0b10000000;
    public static final byte setLanguageCode = (byte) 0b10100000;
    public static final byte exitCode =  (byte) 0b11100000;
    
    public static final int SUCCESS = 1;
    public static final int FAIL = 0;
    
    public static final int CREDITCARDLENGTH = 16;
    public static final int PINLENGTH = 2;
	private static final int TIMEOUT = 5;
	
	private static String userCreditCard = "";
	private static String userPinCode = "";
	
    
    
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
    	{
    		if(Debug.ON)
        		System.out.println("Sending message " + result.get(0) + " to socket " + socket);
    		out.print(result.get(0));
    	}
    	else
    		if(Debug.ON)
        		System.out.println("Sending empty string to socket " + socket);
    		out.println("");
    }
    
    public void sendStringMessage(String message)
    {
    	out.println(message);
    }
    
    public String getMessageAsString(ServerMessage message)
    {
    	String sql = "SELECT word FROM " + currentLanguage.getName() + " WHERE " +
    			" id=" + (message.ordinal() + 1);
    	
    	ArrayList<String> result = queryDatabase(sql);
    	if(result.size() != 0)
    	{
    		return result.get(0);
    	}
    	else
    		return "";
    }
    
    public void sendCode(int code)
    {
    	if(Debug.ON)
    		System.out.println("Sending code " + code + " to socket " + socket);
    	out.println(code);
    }

    public byte[] readBytes() throws IOException {
        
        InputStream in = socket.getInputStream();
        DataInputStream dis = new DataInputStream(in);
        
        int len = dis.readInt();
        if(Debug.ON)
        	System.out.println("In readBytes() with len " + len);
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
    	
    	userCreditCard = creditcardSB.toString();
    	userPinCode = pinSB.toString();
    		
    	sendCode(1);
        return true;
    }
    
    private String getBalance()
    {
    	String sql = "SELECT balance FROM User WHERE creditCardNumber=" + userCreditCard + " AND pinCode=" + userPinCode;
    	
    	ArrayList<String> result = queryDatabase(sql);
    	
    	return result.get(0);
    }

    private void getStatus()
    {
    	String balanceStart = getMessageAsString(ServerMessage.BALANCESTATUS);
    	
    	String sql = "SELECT balance FROM User WHERE creditCardNumber=" + userCreditCard + " AND pinCode=" + userPinCode;
    	
    	String value = queryDatabase(sql).get(0);
    	
    	String currency = getMessageAsString(ServerMessage.CURRENCY);
    	sendStringMessage(balanceStart + value + currency);
    }
    
    private void withdraw() throws NumberFormatException, IOException
    {	
    	
    	long withdrawAmount = Long.parseLong(in.readLine());
    	
    	long balance = Long.parseLong(getBalance());    
    	
    	if(balance < withdrawAmount)
    	{
    		sendCode(0);
    		return;
    	}
    	
    	String updateQuery = "UPDATE User SET balance=" + (balance-withdrawAmount) + " WHERE creditCardNumber=" + userCreditCard + " AND pinCode=" + userPinCode;
    	
    	updateDatabase(updateQuery);
    	sendCode(1);
    }
    
    private void deposit() throws NumberFormatException, IOException
    {
    	long depositAmount = Long.parseLong(in.readLine());
    	
    	long balance = Long.parseLong(getBalance());    
    	
    	
    	String updateQuery = "UPDATE User SET balance=" + (balance+depositAmount) + " WHERE creditCardNumber=" + userCreditCard + " AND pinCode=" + userPinCode;
    	
    	updateDatabase(updateQuery);
    }

    public void run(){
         
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader
                (new InputStreamReader(socket.getInputStream()));
            
            //If we can't find the use
            if(Debug.ON)
            	System.out.println("Validating User");
            if(validateUser() == false)
            {
            	if(Debug.ON)
            		System.out.println("Sending invalid Message");
            	sendMessage(ServerMessage.INVALIDLOGIN);
            	return;
            }
            
            if(Debug.ON)
        		System.out.println("Sending valid Message");
            sendMessage(ServerMessage.VALIDLOGIN);
            
            if(Debug.ON)
        		System.out.println("Entering main loop");
            
            mainloop:
            while(true)
            {
            	byte[] data = readBytes();
            	
            	byte code = data[0];
            	
            	switch(code)
            	{
            	case withdrawCode:
            		withdraw();
            		break;
            	case depositCode:
            		deposit();
            		break;
            		
            	case messageCode:
            		long message = 0;
            		
            		for(int i = 8; i < Long.SIZE / Byte.SIZE ; i--)
            		{
            			message += data[i] * 8 * Math.pow(2, i-1);
            		}
            		
            		ServerMessage[] messages = ServerMessage.values();
            		
            		sendMessage(messages[(int)message]);
            		sendCode(1);
            		break;
            		
            	case setLanguageCode:
            		break;
            		
            	case exitCode:
            		break mainloop;
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
