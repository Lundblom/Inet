package com.progp.inet;

import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;
import java.util.Scanner;

/**
 * @author Snilledata
 */
public class ATMClient {
	private static int connectionPort = 8989;
	private static Socket ATMSocket = null;

	private static BufferedReader in;
	
	public static boolean sendRequest(byte[] myByteArray) throws IOException {
		return sendRequest(myByteArray, 0, myByteArray.length);
	}

	
	/**
	 * This method sends a specific byte pattern to the ATMServer. This is converted into
	 * some request by the server and some response is received and printed on the client's screen.
	 * @param myByteArray The array to be sent, the first byte should match one of the command codes in ATMServerThread.
	 * @param start where to start(usually 0).
	 * @param len Length of the array (usually myByteArray.length).
	 * @return Returns false if the request failed.
	 * @throws IOException
	 */
	public static boolean sendRequest(byte[] myByteArray, int start, int len)
			throws IOException {
		if (len < 0)
			throw new IllegalArgumentException("Negative length not allowed");
		if (start < 0 || start >= myByteArray.length)
			throw new IndexOutOfBoundsException("Out of bounds: " + start);
		// Other checks if needed.

		// May be better to save the streams in the support class;
		// just like the socket variable.
		OutputStream out = ATMSocket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		
		if(Debug.ON)
			System.out.println("In sendRequest() with array" + myByteArray + " and start " + start + " and len " + len);

		dos.writeInt(len);
		if (len > 0) {
			dos.write(myByteArray, start, len);
		}
		boolean code = getAnswerCode();
		getMessage();	
		return code;
	}
	
	
	/**
	 * Receives a boolean code that the server sends via the socket connection
	 * @return The sent code, true or false
	 * @throws IOException
	 */
	public static boolean getAnswerCode() throws IOException 
	{
		String code = in.readLine();
		if(Debug.ON)
			System.out.println("Got code: " + code);
		if (code.equals("0"))
			return false;
		return true;
	}
	
	/**
	 * Gets a string that the server is sending over the socket and prints it to console.
	 * @throws IOException
	 */
	public static void getMessage() throws IOException 
	{
		String message = in.readLine();
		if(Debug.ON)
			System.out.println("Received message: " + message);
		System.out.println(message);
	}

	/** 
	 * A method for converting a String consisting of only integer characters to a byte value 
	 * ranging from 0 to 9. Will crash if the String contains non numerical characters.
	 * @param string The string to be converted.
	 * @return The converted array.
	 */
	public static byte[] convertIntegerStringToBytes(String string) {
		byte[] result = new byte[string.length()];

		for (int i = 0; i < string.length(); i++) {
			int integerFound = Integer.parseInt(string.substring(i, i + 1));

			result[i] = (byte) integerFound;
		}

		return result;
	}

	/**
	 * Requests one of the predefined ServerMessages contained in the enum ServerMessage and
	 * prints it on the console. The message received varies depending on which language is set in ATMServerThread.
	 * @param message The message ID.
	 * @throws IOException
	 */
	public static void getServerMessage(ServerMessage message) throws IOException
	{
		if(Debug.ON)
			System.out.println("Received message: " + message);
		
		byte[] convertedStringBytes = convertIntegerStringToBytes(String.valueOf(message.ordinal()));
		
		byte[] messageBytes = new byte[10];
		messageBytes[0] = ATMServerThread.messageCode;
		
		for(int i = 0; i < convertedStringBytes.length; i++)
		{
			messageBytes[i+1] += convertedStringBytes[i];
		}
		
		sendRequest(messageBytes);
	}
	
	/**
	 * Creates a byte array containing the a credit card number of 16 characters
	 * and a pin containing 2 characters. 
	 * @param ccn The credit card number to be added to the byte array.
	 * @param pin The pin to be added to the byte array.
	 * @return The converted array.
	 */
	public static byte[] createLoginBytes(String ccn, String pin) {
		
		//Converts the user info the byte-form.
		byte[] creditCardBytes = convertIntegerStringToBytes(ccn);
		byte[] creditPinBytes = convertIntegerStringToBytes(pin);

		return new byte[] { ATMServerThread.loginCode,
				(byte) (creditCardBytes[0] * 16 + creditCardBytes[1]),
				(byte) (creditCardBytes[2] * 16 + creditCardBytes[3]),
				(byte) (creditCardBytes[4] * 16 + creditCardBytes[5]),
				(byte) (creditCardBytes[6] * 16 + creditCardBytes[7]),
				(byte) (creditCardBytes[8] * 16 + creditCardBytes[9]),
				(byte) (creditCardBytes[10] * 16 + creditCardBytes[11]),
				(byte) (creditCardBytes[12] * 16 + creditCardBytes[13]),
				(byte) (creditCardBytes[14] * 16 + creditCardBytes[15]),
				(byte) (creditPinBytes[0] * 16 + creditPinBytes[1]) };
	}
	
	
	public static byte[] createStatusBytes()
	{
		return new byte[] {ATMServerThread.statusCode};
	}
	
	/**
	 * Creates the byte array used when sending a withdraw request to 
	 * the server.
	 * @param amount Amount to withdraw
	 * @param safetyCode The predefined security code. Has to match the codes in the SQL database.
	 * @return
	 */
	public static byte[] createWithdrawalBytes(String amount, String safetyCode)
	{
		
		//Makes sure that the withdrawal amount fills the entire array
		while(amount.length() < 16)
		{
			amount = "0" + amount;
		}
		//Same thing here
		while(safetyCode.length() < 2)
		{
			safetyCode = "0" + safetyCode;
		}
		
		byte[] amountBytes = convertIntegerStringToBytes(amount);
		
		
		byte[] safetyCodeBytes = convertIntegerStringToBytes(safetyCode);
		
		return new byte[] { ATMServerThread.withdrawCode,
				(byte) (amountBytes[0] * 16 + amountBytes[1]),
				(byte) (amountBytes[2] * 16 + amountBytes[3]),
				(byte) (amountBytes[4] * 16 + amountBytes[5]),
				(byte) (amountBytes[6] * 16 + amountBytes[7]),
				(byte) (amountBytes[8] * 16 + amountBytes[9]),
				(byte) (amountBytes[10] * 16 + amountBytes[11]),
				(byte) (amountBytes[12] * 16 + amountBytes[13]),
				(byte) (amountBytes[14] * 16 + amountBytes[15]),
				(byte) (safetyCodeBytes[0] * 16 + safetyCodeBytes[1]) };		
	}
	
	public static byte[] createLanguageBytes(int languageCode)
	{
		if(languageCode > 127)
			throw new InvalidParameterException();
		
		byte[] languageBytes = new byte[2];
		
		languageBytes[0] = ATMServerThread.setLanguageCode;
		languageBytes[1] = (byte)languageCode;
		
		return languageBytes;
	}
	
	public static byte[] createDepositBytes(String amount, String safetyCode)
	{
		//Makes sure that the Deposit amount fills the entire array
		while(amount.length() < 16)
		{
			amount = "0" + amount;
		}

		while(safetyCode.length() < 2)
		{
			safetyCode = "0" + safetyCode;
		}
		
		byte[] amountBytes = convertIntegerStringToBytes(amount);
		byte[] safetyCodeBytes = convertIntegerStringToBytes(safetyCode);
		
		
		return new byte[] { ATMServerThread.depositCode,
				(byte) (amountBytes[0] * 16 + amountBytes[1]),
				(byte) (amountBytes[2] * 16 + amountBytes[3]),
				(byte) (amountBytes[4] * 16 + amountBytes[5]),
				(byte) (amountBytes[6] * 16 + amountBytes[7]),
				(byte) (amountBytes[8] * 16 + amountBytes[9]),
				(byte) (amountBytes[10] * 16 + amountBytes[11]),
				(byte) (amountBytes[12] * 16 + amountBytes[13]),
				(byte) (amountBytes[14] * 16 + amountBytes[15]),
				(byte) (safetyCodeBytes[0] * 16 + safetyCodeBytes[1]) };		
	}
	

	public static void main(String[] args) throws IOException {

		programloop:
		while(true)
		{
			PrintWriter out = null;
			String adress = "";
			Scanner scanner = new Scanner(System.in);
			while (true) 
			{
				//This part creates a connection the the IP.
				try 
				{
					adress = args[0];
				} 
				catch (ArrayIndexOutOfBoundsException e) 
				{
					System.err.println("Missing argument ip-adress");
					System.exit(1);
				}
				
				try 
				{
					ATMSocket = new Socket(adress, connectionPort);
					out = new PrintWriter(ATMSocket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(
							ATMSocket.getInputStream()));
				} 
				catch (UnknownHostException e) 
				{
					System.err.println("Unknown host: " + adress);
					System.exit(1);
				} 
				catch (IOException e) 
				{
					System.err.println("Couldn't open connection to " + adress);
					System.exit(1);
				}
	
				//Loop for handling credit card number
				String creditCardNumber;
				while (true) 
				{
					System.out.print("Enter your credit card number: ");
					creditCardNumber = scanner.nextLine();
					if (creditCardNumber.length() != 16) 
					{
						getServerMessage(ServerMessage.LOGINFORMATERROR);
						continue;
					}
					break;
				}
	
				//Loop for handling pin code
				String pinCode;
				while (true) {
					System.out.print("Enter your pin code: ");
					pinCode = scanner.nextLine();
					if (pinCode.length() != 2) {
						getServerMessage(ServerMessage.PINFORMATERROR);
						continue;
					}
					break;
				}
	
				//Gets the confirmation code from the server
				//Loops again if the credentials are invalid
				boolean code = sendRequest(createLoginBytes(creditCardNumber, pinCode));
				if (code)
					break;
	
			}
	
			
			String menuOption;
	
			while (true) 
			{
				getServerMessage(ServerMessage.GREETING);
				
				while(true)
				{
					System.out.print("> ");
					menuOption = scanner.nextLine();
					if(!menuOption.matches("[0-9]?") || menuOption.equals("") || menuOption == null)
					{
						getServerMessage(ServerMessage.INVALIDINPUT);
						continue;
					}
					break;
				}
				
				if (menuOption.equals("1")) 
				{
					sendRequest(createStatusBytes());
				}
				else if(menuOption.equals("2"))
				{
					
					getServerMessage(ServerMessage.ENTERAMOUNT);
					scanner.nextLine();
					String withdrawAmount = scanner.nextLine();
					
					getServerMessage(ServerMessage.ENTERCODE);
					int safetyCode = scanner.nextInt();
					System.out.println();
					
									
					sendRequest(createWithdrawalBytes(String.valueOf(withdrawAmount), String.valueOf(safetyCode)));
				}
				else if(menuOption.equals("3"))
				{
					getServerMessage(ServerMessage.ENTERAMOUNT);
					int depositAmount = scanner.nextInt();
					System.out.println();
					
					getServerMessage(ServerMessage.ENTERCODE);
					int safetyCode = scanner.nextInt();
					System.out.println();
					
									
					sendRequest(createDepositBytes(String.valueOf(depositAmount), String.valueOf(safetyCode)));
	
				}
				
				else if(menuOption.equals("4"))
				{
					Language[] languages = Language.values();
					getServerMessage(ServerMessage.AVAILABLELANGUAGES);
				 	
					for(int i = 0; i < languages.length; i++)
					{
						System.out.println("(" + (i+1) + ") " + languages[i].toString());
					}
					
					getServerMessage(ServerMessage.CHOOSELANGUAGE);
					int selectedLanguage = scanner.nextInt() - 1;
				 	
					
					sendRequest(createLanguageBytes(selectedLanguage));
				}
				//Log out
				else if(menuOption.equals("5"))
				{
					break;
				}
				else 
				{
					break programloop;
				}
			}
	
			out.close();
			in.close();
			ATMSocket.close();
		}
	
	in.close();
	ATMSocket.close();
	}
}
