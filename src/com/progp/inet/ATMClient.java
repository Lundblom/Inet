package com.progp.inet;
import java.io.*;   
import java.net.*;  
import java.util.Scanner;

/**
   @author Snilledata
*/
public class ATMClient {
    private static int connectionPort = 8989;
    private static Socket ATMSocket = null;
    
    public static void sendBytes(byte[] myByteArray) throws IOException {
        sendBytes(myByteArray, 0, myByteArray.length);
    }

    public static void sendBytes(byte[] myByteArray, int start, int len) throws IOException {
        if (len < 0)
            throw new IllegalArgumentException("Negative length not allowed");
        if (start < 0 || start >= myByteArray.length)
            throw new IndexOutOfBoundsException("Out of bounds: " + start);
        // Other checks if needed.

        // May be better to save the streams in the support class;
        // just like the socket variable.
        OutputStream out = ATMSocket.getOutputStream(); 
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeInt(len);
        if (len > 0) {
            dos.write(myByteArray, start, len);
        }
    }
    
    public static boolean getAnswerCode(BufferedReader in) throws IOException
    {
    	if (in.readLine().equals("0"))
    		return false;
    	return true;
    }
    
    public static void getMessage(BufferedReader in) throws IOException
    {
    	System.out.println(in.readLine());
    }
    
    public static byte[] convertStringToBytes(String string)
    {
    	byte[] result = new byte[string.length()];
    	
    	for(int i = 0; i < string.length(); i++)
    	{
    		int integerFound = Integer.parseInt(string.substring(i, i+1));
    		
    		result[i] = (byte)integerFound;
    	}
    	
    	return result;
    }
    
    public static byte[] createLoginBytes(String ccn, String pin)
    {
    	byte[] creditCardBytes = convertStringToBytes(ccn);
        byte[] creditPinBytes = convertStringToBytes(pin);
        
        return new byte[] { ATMServerThread.loginCode, 
        		(byte)(creditCardBytes[0] * 16 + creditCardBytes[1]), 
        		(byte)(creditCardBytes[2] * 16 + creditCardBytes[3]),
        		(byte)(creditCardBytes[4] * 16 + creditCardBytes[5]), 
        		(byte)(creditCardBytes[6] * 16 + creditCardBytes[7]), 
        		(byte)(creditCardBytes[8] * 16 + creditCardBytes[9]), 
        		(byte)(creditCardBytes[10] * 16 + creditCardBytes[11]), 
        		(byte)(creditCardBytes[12] * 16 + creditCardBytes[13]),
        		(byte)(creditCardBytes[14] * 16 + creditCardBytes[15]),
        		(byte)(creditPinBytes[0] * 16 + creditPinBytes[1])
        };
    }
    
    public static void main(String[] args) throws IOException {
        
        
        PrintWriter out = null;
        BufferedReader in = null;
        String adress = "";
        Scanner scanner = new Scanner(System.in);
        while(true)
        {
	        try {
	            adress = args[0];
	        } catch (ArrayIndexOutOfBoundsException e) {
	            System.err.println("Missing argument ip-adress");
	            System.exit(1);
	        }
	        try {
	            ATMSocket = new Socket(adress, connectionPort); 
	            out = new PrintWriter(ATMSocket.getOutputStream(), true);
	            in = new BufferedReader(new InputStreamReader
	                                    (ATMSocket.getInputStream()));
	        } catch (UnknownHostException e) {
	            System.err.println("Unknown host: " +adress);
	            System.exit(1);
	        } catch (IOException e) {
	            System.err.println("Couldn't open connection to " + adress);
	            System.exit(1);
	        }
	        
	        String creditCardNumber;
	        while(true)
	        {
		        System.out.print("Enter your credit card number: ");
		        creditCardNumber = scanner.nextLine();
		        if(creditCardNumber.length() != 16)
		        {
		        	System.out.println("Please enter a number of length 16");
		        	continue;
		        }
		        break;
	        }
	        System.out.println();
	        
	        
	        String pinCode; 
	        
	        while(true)
	        {
	        	System.out.print("Enter your pin code: ");
		        pinCode = scanner.nextLine();
		        if(pinCode.length() != 2)
		        {
		        	System.out.println("Please enter a number of length 2");
		        	continue;
		        }
		        break;
	        }
	        
	
	        sendBytes(createLoginBytes(creditCardNumber, pinCode));
	        if(getAnswerCode(in))
	        {
	        	getMessage(in);
	        	break;
	        }
	        else
	        	getMessage(in);
	        
        
        }

        System.out.print("> ");
        int menuOption = scanner.nextInt();
        int userInput;
        out.println(menuOption);
        while(menuOption < 4) {
                if(menuOption == 1) {
                        System.out.println(in.readLine()); 
                        System.out.println(in.readLine());
                        System.out.print("> ");
                        menuOption = scanner.nextInt();
                        out.println(menuOption);           
                } else if (menuOption > 3) {
                    break;
                }	
                else {
                    System.out.println(in.readLine()); 
                    userInput = scanner.nextInt();
                    out.println(userInput);
                    String str;
                    do {
                        str = in.readLine();
                        System.out.println(str);
                    } while (! str.startsWith("(1)"));
                    System.out.print("> ");
                    menuOption = scanner.nextInt();
                    out.println(menuOption);           
                }	
        }		
		
        out.close();
        in.close();
        ATMSocket.close();
    }
}   
