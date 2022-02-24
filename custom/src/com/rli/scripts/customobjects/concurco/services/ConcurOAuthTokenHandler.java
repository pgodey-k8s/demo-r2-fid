package com.rli.scripts.customobjects.concurco.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.rli.scripts.customobjects.concurco.utils.ConcurParameters;

/**
 *
 *This class is used to get the Concur Oauth token for a given username and password
 * and also used to get expiry date and refreshtocken
 *
 */
public class ConcurOAuthTokenHandler {
	
	private String username;
	private String password;
	private String url;
	private String authorizationKey;

	/**
	 * 
	 * @param username
	 * @param password
	 * @param url
	 */
	public ConcurOAuthTokenHandler(String username,String password,String url,String auString){
		this.username=username;
		this.password=password;
		this.url=url;
		this.authorizationKey=auString;
		
		
	}
	
	/**
	 * @return
	 * @throws Exception 
	 */
	public String getOAuthToken() throws Exception{
		String token="";
		String response=handleRequest("",null);
		DocumentBuilderFactory dbf =
            DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(response));
        Document doc = db.parse(is);
        NodeList nodes = doc.getElementsByTagName("Access_Token");
        
        if (nodes != null) {
			
        	NodeList childn = nodes.item(0).getChildNodes();

			if (childn != null) {
				for (int j = 0; j < childn.getLength(); j++) {
				Node chilnode = childn.item(j);
				if (chilnode.getNodeName().equalsIgnoreCase("Token")) {
					
					ConcurParameters.setToken(chilnode.getTextContent());
				}else if (chilnode.getNodeName().equalsIgnoreCase("Expiration_date")) {
					
					ConcurParameters.setExpiryDate(chilnode.getTextContent());
					
				}if (chilnode.getNodeName().equalsIgnoreCase("Refresh_Token")) {
					
					ConcurParameters.setRefresh_Token(chilnode.getTextContent());
				}
			}
			}
        }
		return token;
	}
	
	
	/**
	 * This method sends a http or https GET request to the rest end point.
	 * 
	 * @param path
	 *            The additional path part of the URL.
	 * @param queryOption
	 *            The queries of the URL.
	 * @return The response of the request sent.
	 * @throws Exception
	 */
	public String handleRequest(String path, String queryOption)
			throws Exception {
		BufferedReader reader = null;
		HttpURLConnection conn = null;
	

		try {

			String userPassword = username + ":"
					+ password;
			String encoding = Base64.getEncoder().encodeToString(userPassword
					.getBytes());
			/**
			 * Get the url and open an http connection.
			 */
			String urlString = url + path;
			if (queryOption != null) {

				urlString = urlString + "?" + queryOption;
			}

			URL url = new URL(this.url);
			URI uri = new URI(url.getProtocol(), url.getUserInfo(),
					url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), url.getRef());
			url = uri.toURL();
			conn = (HttpURLConnection) url.openConnection();

			conn.setRequestProperty("Authorization",
					"Basic " + encoding);

			conn.setRequestProperty("X-ConsumerKey",
					authorizationKey);

			/**
			 * Get the reader for this connection.
			 */
			reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			/**
			 * Read the response stream and return the equivalent string.
			 */

			StringBuffer stringBuf = new StringBuffer();
			String inputLine;

			while ((inputLine = reader.readLine()) != null) {
				stringBuf.append(inputLine);
			}
			return stringBuf.toString();

		} catch (IOException e) {

			throw new Exception(e);

		} catch (Exception e) {
			throw new Exception(e);
		}

	}
	public static void main(String args[]){
		
		ConcurOAuthTokenHandler cth= new ConcurOAuthTokenHandler("praveennandi@gmail.com", "pra101980", "https://www.concursolutions.com/net2/oauth2/accesstoken.ashx", "bfPF4Y1nBKsSNNk6RnexOd");
		try {
			System.out.println(cth.getOAuthToken());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
