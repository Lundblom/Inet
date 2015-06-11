package com.progp.inet;
import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.lang.reflect.Array;
import java.net.*;
import java.security.InvalidParameterException;
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

    /**
     * Sends a message specified in the enum ServerMessage to the client
     */
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
    
    /**
     * Sends a message that is not predefined in the database
     * @param message
     */
    public void sendStringMessage(String message)
    {
    	out.println(message);
    }
    
    /**
     * Gets a message from the SQL database
     * @param message The message ID to be retrieved
     * @return The message as a string
     */
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
    
    /**
     * Sends a confirmation code for request to the user. 1 is success, 0 is fail
     * @param code The code to be sent, has to be 1 or 0.
     */
    public void sendCode(int code)
    {
    	if(code > 1 || code < 0)
    	{
    		throw new InvalidParameterException();
    	}
    		
    	if(Debug.ON)
    		System.out.println("Sending code " + code + " to socket " + socket);
    	out.println(code);
    }

    /**
     * Reads a byte array sent by the connected client
     * @return The read data
     * @throws IOException
     */
    public byte[] readBytes() throws IOException, SocketException {
        
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
    
  /**
   * Updates the SQL database with the specified query
   * @param query 
   * @return Returns false if an error has occurred when contacting the database.
   */
    public boolean updateDatabase(String query)
    {
    	Connection conn = null;
		try {
			conn = DriverManager.getConnection(DATABASEURL);
		} catch (SQLException e) {
			System.out.println("CANT CONNECT!");
			e.printStackTrace();
		}
        try {
            Statement stmt = conn.createStatement();
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
    
    /**
     * Queries the database with the specified SQL query and returns the response as
     * an arraylist
     * @param query
     * @return All matching entries, in found order.
     */
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
        try {
            Statement stmt = conn.createStatement();
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

    /**
     * Method that connects the client with the sent credentials.
     * If the credentails are not found the user can't connect
     * @return Returns false if the credentials are not in the database
     * @throws IOException
     */
    private boolean validateUser() throws IOException, SocketException
    {
    	byte[] result;

    	result = readBytes();
    	
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
    
    /**
     * Gets the connected user's balance from the SQL database as a String.
     * @return
     */
    private String getBalance()
    {
    	String sql = "SELECT balance FROM User WHERE creditCardNumber=" + userCreditCard + " AND pinCode=" + userPinCode;
    	
    	ArrayList<String> result = queryDatabase(sql);
    	
    	return result.get(0);
    }

    /**
     * Gets the connects user's balance and sends a success code and the complete status message
     * to the user.
     */
    private void getStatus()
    {
    	String balanceStart = getMessageAsString(ServerMessage.BALANCESTATUS);
    	
    	String sql = "SELECT balance FROM User WHERE creditCardNumber=" + userCreditCard + " AND pinCode=" + userPinCode;
    	
    	String value = queryDatabase(sql).get(0);
    	
    	String currency = getMessageAsString(ServerMessage.CURRENCY);
    	
    	sendCode(1);
    	
    	sendStringMessage(balanceStart + " " + value + " " + currency);
    }
    
    private void setLanguage(Language l)
    {
    	currentLanguage = l;
    }
    
    /** 
     * Handler for when the client sends a withdraw request
     * @param withdrawAmount Amount of money to be withdrawn
     * @param pinCode The user's security code for transfers
     * @throws NumberFormatException
     * @throws IOException
     */
    private void withdraw(long withdrawAmount, String pinCode) throws NumberFormatException, IOException
    {	
    	String sql = "SELECT * FROM SecurityCodes WHERE code=" + pinCode;
    	
    	ArrayList<String> result = queryDatabase(sql);
    	
    	if(result.size() == 0)
    	{
    		sendCode(0);
    		sendMessage(ServerMessage.PINFAIL);
    	}
    	
    	long balance = Long.parseLong(getBalance());   
    	
    	if(Debug.ON)
    	{
    		System.out.println("Balance in withdraw is: " + balance);
    		System.out.println("Withdraw amount in withdraw is: " + withdrawAmount);
    	}
    	
    	
    	if(balance < withdrawAmount)
    	{
    		sendCode(0);
    		sendMessage(ServerMessage.WITHDRAWFAIL);
    		return;
    	}
    	
    	String updateQuery = "UPDATE User SET balance=" + (balance-withdrawAmount) + " WHERE creditCardNumber=" + userCreditCard + " AND pinCode=" + userPinCode;
    	
    	updateDatabase(updateQuery);
    	sendCode(1);
    	sendMessage(ServerMessage.WITHDRAWSUCCESS);
    }
    
    /**
     * Handler for when the client sends a deposit request.
     * @param depositAmount Amount of money to be deposited.
     * @param pinCode The user's security code for transfers
     * @throws NumberFormatException
     * @throws IOException
     */
    private void deposit(long depositAmount, String pinCode) throws NumberFormatException, IOException
    {
    	String sql = "SELECT * FROM SecurityCodes WHERE code=" + pinCode;
    	
    	ArrayList<String> result = queryDatabase(sql);
    	
    	if(result.size() == 0)
    	{
    		sendCode(0);
    		sendMessage(ServerMessage.PINFAIL);
    	}
    	
    	long balance = Long.parseLong(getBalance());   
    	
    	if(Debug.ON)
    	{
    		System.out.println("Balance in withdraw is: " + balance);
    		System.out.println("Withdraw amount in withdraw is: " + depositAmount);
    	}   
    	
    	
    	String updateQuery = "UPDATE User SET balance=" + (balance+depositAmount) + " WHERE creditCardNumber=" + userCreditCard + " AND pinCode=" + userPinCode;
    	
    	sendCode(0);
    	sendMessage(ServerMessage.DEPOSITSUCCESS);
    	updateDatabase(updateQuery);
    }

    
    /**
     * Gets the withdraw/deposit value and the pincode from the data
     * that the client has sent
     * @param data The data sent b the client
     * @return An array with 2 elements: 0 is the value and 1 is the pin
     */
    public ArrayList<String> withdrawAndDepositDataGetter(byte[] data)
    {
    	StringBuilder amountAsString = new StringBuilder();
		int loopLength = 8;
		
		String pinCode = "";
		
		int pinOne = Byte.valueOf((byte) (data[9] >> 4));
		int pinTwo = Byte.valueOf((byte) (data[9] & 0xf));
		
		pinCode += (String.valueOf(pinOne) + String.valueOf(pinTwo));
		
		
		for(int i = 1; i < loopLength; i++)
    	{
    		Byte b = (byte) (data[i+1] & 0xff);
			Byte highNibble = (byte) ((byte)(b >> 4) & 0x0f);
			Byte lowNibble = ((byte)(b & 0x0f));
			amountAsString.append(String.valueOf((int)highNibble));
			amountAsString.append(String.valueOf((int)lowNibble));
    	}
		
		ArrayList<String> result = new ArrayList<String>();
		result.add(amountAsString.toString());
		result.add(pinCode);
		
		return result;
		
    }
    
    /**
     * Main loop for the thread.
     */
    public void run(){
         
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader
                (new InputStreamReader(socket.getInputStream()));
            
            //If we can't find the use
            if(Debug.ON)
            	System.out.println("Validating User");
            try{
	            if(validateUser() == false)
	            {
	            	if(Debug.ON)
	            		System.out.println("Sending invalid Message");
	            	sendMessage(ServerMessage.INVALIDLOGIN);
	            	return;
	            }
            }
            catch(SocketException e)
            {
            	System.out.println(socket + " closed unexpectedly.");
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
            	byte[] data;
            	try
            	{
            		data = readBytes();
            	}
            	catch(SocketException e)
            	{
            		System.out.println("Socket " + socket + " closed unexpectedly.");
            		return;
            	}
            	
            	byte code = data[0];
            	
            	switch(code)
            	{
            	case statusCode:
            		if(Debug.ON)
                		System.out.println("Entering status");
            		getStatus();
            		break;
            	case withdrawCode:
            		if(Debug.ON)
                		System.out.println("Entering withdraw");
            		
            		ArrayList<String> withdrawData = withdrawAndDepositDataGetter(data);
            		
            		withdraw(Integer.parseInt(withdrawData.get(0)), withdrawData.get(1));
            		
            		
            		break;
            	case depositCode:
            		if(Debug.ON)
                		System.out.println("Entering deposit");
            		
            		ArrayList<String> depositData = withdrawAndDepositDataGetter(data);
            		
            		deposit(Integer.parseInt(depositData.get(0)), depositData.get(1));
            		break;
            		
            	case messageCode:
            		long message = 0;
            		
            		for(int i = 8; i < Long.SIZE / Byte.SIZE ; i--)
            		{
            			message += data[i] * 8 * Math.pow(2, i-1);
            		}
            		
            		ServerMessage[] messages = ServerMessage.values();
            		
            		sendCode(1);
            		sendMessage(messages[(int)message]);
            		break;
            		
            	case setLanguageCode:
            		int languageCode = data[1];
            		
            		Language l = Language.values()[languageCode];
            		
            		setLanguage(l);
            		sendCode(1);
            		sendMessage(ServerMessage.LANGUAGESUCCESS);
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
