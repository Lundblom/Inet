package com.progp.inet;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * @author Snilledata
 */
public class ATMClient {
	private static int connectionPort = 8989;
	private static Socket ATMSocket = null;

	public static boolean sendBytes(byte[] myByteArray, BufferedReader in) throws IOException {
		return sendBytes(myByteArray, 0, myByteArray.length, in);
	}
	
	public static void sendBytes(byte[] myByteArray) throws IOException {
		sendBytes(myByteArray, 0, myByteArray.length);
	}

	public static boolean sendBytes(byte[] myByteArray, int start, int len, BufferedReader in)
			throws IOException {
		sendBytes(myByteArray, start, len);
		return getAnswerCode(in);
	}
	
	public static void sendBytes(byte[] myByteArray, int start, int len)
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
			System.out.println("In sendBytes() with array" + myByteArray + " and start " + start + " and len " + len);

		dos.writeInt(len);
		if (len > 0) {
			dos.write(myByteArray, start, len);
		}
		
	}
	
	

	public static boolean getAnswerCode(BufferedReader in) throws IOException 
	{
		String code = in.readLine();
		if(Debug.ON)
			System.out.println("Got code: " + code);
		if (code.equals("0"))
			return false;
		return true;
	}

	public static void getMessage(BufferedReader in) throws IOException 
	{
		String message = in.readLine();
		if(Debug.ON)
			System.out.println("Received message: " + message);
		System.out.println(message);
	}

	public static byte[] convertIntegerStringToBytes(String string) {
		byte[] result = new byte[string.length()];

		for (int i = 0; i < string.length(); i++) {
			int integerFound = Integer.parseInt(string.substring(i, i + 1));

			result[i] = (byte) integerFound;
		}

		return result;
	}
	
	public static void recieveResponse(BufferedReader in) throws IOException
	{
		String response = in.readLine();
		
		System.out.println(response);
	}
	
	public static void getServerMessage(ServerMessage message, BufferedReader in) throws IOException
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
		
		sendBytes(messageBytes, in);
	}

	public static byte[] createLoginBytes(String ccn, String pin) {
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
	
	public static byte[] createWithdrawalBytes(String amount, String safetyCode)
	{
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
	
	public static byte[] createDepositBytes(String amount, String safetyCode)
	{
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

		PrintWriter out = null;
		BufferedReader in = null;
		String adress = "";
		Scanner scanner = new Scanner(System.in);
		while (true) {
			try {
				adress = args[0];
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println("Missing argument ip-adress");
				System.exit(1);
			}
			try {
				ATMSocket = new Socket(adress, connectionPort);
				out = new PrintWriter(ATMSocket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(
						ATMSocket.getInputStream()));
			} catch (UnknownHostException e) {
				System.err.println("Unknown host: " + adress);
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Couldn't open connection to " + adress);
				System.exit(1);
			}

			String creditCardNumber;
			while (true) {
				System.out.print("Enter your credit card number: ");
				creditCardNumber = scanner.nextLine();
				if (creditCardNumber.length() != 16) {
					System.out.println("Please enter a number of length 16");
					continue;
				}
				break;
			}
			System.out.println();

			String pinCode;

			while (true) {
				System.out.print("Enter your pin code: ");
				pinCode = scanner.nextLine();
				if (pinCode.length() != 2) {
					System.out.println("Please enter a number of length 2");
					continue;
				}
				break;
			}

			sendBytes(createLoginBytes(creditCardNumber, pinCode));
			if (getAnswerCode(in)) {
				getMessage(in);
				break;
			} else
				getMessage(in);

		}

		
		int menuOption;

		while (true) 
		{
			getServerMessage(ServerMessage.GREETING, in);
			
			System.out.print("> ");
			menuOption = scanner.nextInt();
			
			if (menuOption == 1) 
			{
				sendBytes(createStatusBytes());
				recieveResponse(in);
			}
			else if(menuOption == 3)
			{
				System.out.print("Enter amount: ");
				int depositAmount = scanner.nextInt();
				System.out.println();
				
				System.out.print("Enter code: ");
				int safetyCode = scanner.nextInt();
				System.out.println();
				
								
				sendBytes(createDepositBytes(String.valueOf(depositAmount), String.valueOf(safetyCode)), in);
				getServerMessage(ServerMessage.DEPOSITSUCCESS, in);

			}
			else if(menuOption == 2)
			{
				
				System.out.print("Enter amount: ");
				scanner.nextLine();
				String withdrawAmount = scanner.nextLine();
				
				System.out.print("Enter code: ");
				int safetyCode = scanner.nextInt();
				System.out.println();
				
								
				boolean response = sendBytes(createWithdrawalBytes(String.valueOf(withdrawAmount), String.valueOf(safetyCode)), in);
				if(response)
				{
					getServerMessage(ServerMessage.WITHDRAWSUCCESS, in);
				}
				else
				{
					getServerMessage(ServerMessage.WITHDRAWFAIL, in);
				}
			}
			else 
			{
				break;
			}
		}

		out.close();
		in.close();
		ATMSocket.close();
	}
}
