package com.progp.inet;

public enum Language
{
	ENGLISH("English"),
	SWEDISH("Swedish");
	
	private String prettyName;
	
	private Language(String prettyName)
	{
		this.prettyName = prettyName;
	}
	
	public String getName()
	{
		return prettyName;
	}
}
