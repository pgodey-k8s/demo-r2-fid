package com.rli.scripts.customobjects.concurco.utils;

public class ConcurParameters {

	private static String url = "";

	private static String userName = "";

	private static String password = "";
	
	public static String expiryDate;
	public static String token;
	public static String refresh_Token;

	public static String getUrl() {
		return url;
	}

	public static void setUrl(String url) {
		ConcurParameters.url = url;
	}

	public static String getUserName() {
		return userName;
	}

	public static void setUserName(String userName) {
		ConcurParameters.userName = userName;
	}

	public static String getPassword() {
		return password;
	}

	public static void setPassword(String password) {
		ConcurParameters.password = password;
	}

	public static String getExpiryDate() {
		return expiryDate;
	}

	public static void setExpiryDate(String expiryDate) {
		ConcurParameters.expiryDate = expiryDate;
	}

	public static String getToken() {
		return token;
	}

	public static void setToken(String token) {
		ConcurParameters.token = token;
	}

	public static String getRefresh_Token() {
		return refresh_Token;
	}

	public static void setRefresh_Token(String refresh_Token) {
		ConcurParameters.refresh_Token = refresh_Token;
	}

}
