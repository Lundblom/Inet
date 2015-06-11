package com.progp.inet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;

public class ServerInput implements Runnable {

	
	/**
	   * Updates the SQL database with the specified query
	   * @param query 
	   * @return Returns false if an error has occurred when contacting the database.
	   */
	    public boolean updateDatabase(String query)
	    {
	    	Connection conn = null;
			try {
				conn = DriverManager.getConnection(ATMServerThread.DATABASEURL);
			} catch (SQLException e) {
				System.out.println("CANT CONNECT!");
				e.printStackTrace();
			}
	        try {
	            Statement stmt = conn.createStatement();
	            try 
	            {
	                stmt.setQueryTimeout(ATMServerThread.TIMEOUT);
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
				conn = DriverManager.getConnection(ATMServerThread.DATABASEURL);
			} catch (SQLException e) {
				System.out.println("CANT CONNECT!");
				e.printStackTrace();
			}
	        try {
	            Statement stmt = conn.createStatement();
	            try {
	                stmt.setQueryTimeout(ATMServerThread.TIMEOUT);
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
	
	@Override
	public void run() 
	{
		Scanner input = new Scanner(System.in);
		mainloop:
		while(true)
		{
			System.out.println("(1) Change messages sent to client (2) Add new messages");
			System.out.print("> ");
			String in = input.nextLine();
			
			
			if(in.equals("1"))
			{
				Language[] languages = Language.values();
				System.out.println("Choose language to change ");
				for(int i = 0; i < languages.length; i++)
				{
					System.out.println("(" + (i+1) + ") " + languages[i]);
				}
				int choice = 0;
				while(true)
					{
						in = input.nextLine();
					try
					{
						if(in.equals("exit"))
							continue mainloop;
						choice = Integer.parseInt(in);
						if(choice < 1 || choice > languages.length)
							throw new NumberFormatException();
					}
					catch(NumberFormatException e)
					{
						System.out.println("Enter valid number.");
						continue;
					}
					break;
				}
				
				//Make choice zero-based
				choice--;
				
				String query = "SELECT * FROM " + languages[choice].getName();
				
				ArrayList<String> result = queryDatabase(query);
				
				System.out.println("Select message to change, exit to exit");
				
				for(int i = 0; i < result.size(); i++)
				{
					System.out.println("(" + result.get(i) + ")" + result.get(++i));
				}
				
				System.out.print("> ");
				int wordChoice = 0;
				while(true)
				{
					in = input.nextLine();
					try
					{
						if(in.equals("exit"))
							continue mainloop;
						wordChoice = Integer.parseInt(in);
					}
					catch(NumberFormatException e)
					{
						System.out.println("Enter valid number.");
						continue;
					}
					break;
				}
				
				System.out.print("Enter new message: ");
				
				
				String newMessage = input.nextLine();
				
				if(newMessage.equals("exit"))
					continue mainloop;
				
				String insertQuery = "UPDATE " + languages[choice].getName() + " SET word='" + newMessage + "' WHERE id=" + wordChoice;
				
				updateDatabase(insertQuery);
				
				System.out.println("Message updated.");
			}
			
			if(in.equals("2"))
			{
				Language[] languages = Language.values();
				String messageId = null;
				
				while(true)
				{
					System.out.print("Enter new message id, exit to exit: ");
					messageId = input.nextLine();
					
					if(messageId.equals("exit"))
						continue mainloop;
					
					String messageTestQuery = "SELECT id FROM English WHERE id=" + messageId;
					
					if(queryDatabase(messageTestQuery).size() != 0)
					{
						System.out.println("Id already exists.");
						continue;
					}
					break;
				}
				
				String[] messages = new String[languages.length];
				
				for(int i = 0; i < languages.length; i++)
				{
					System.out.println("Enter new message in " + languages[i].getName() + ": ");
					messages[i] = input.nextLine();
				}
				
				for(int i = 0; i < messages.length; i++)
				{
					String query = "INSERT INTO " + languages[i] + " VALUES ('" + messageId + "', '" + messages[i] + "')";
					updateDatabase(query);
				}
				
				System.out.println("New message created.");
			}
		
		}
	}

}
