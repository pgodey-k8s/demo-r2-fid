package com.rli.scripts.customobjects.sharepointco;

import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.Document;
import org.xml.sax.*;

public class LoginManager {
	private final String sts = "https://login.microsoftonline.com/extSTS.srf";
	private final String login = "/_forms/default.aspx?wa=wsignin1.0";
	private String user="";
	private String password="";
	private String url="";
	private String endpoint="";
	public static String expiryDate;
	public static String cookie;
	// SAML.xml from https://github.com/lstak/node-sharepoint
	private final String reqXML = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\" xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><s:Header><a:Action s:mustUnderstand=\"1\">http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue</a:Action><a:ReplyTo><a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address></a:ReplyTo><a:To s:mustUnderstand=\"1\">https://login.microsoftonline.com/extSTS.srf</a:To><o:Security s:mustUnderstand=\"1\" xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><o:UsernameToken><o:Username>[username]</o:Username><o:Password>[password]</o:Password></o:UsernameToken></o:Security></s:Header><s:Body><t:RequestSecurityToken xmlns:t=\"http://schemas.xmlsoap.org/ws/2005/02/trust\"><wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\"><a:EndpointReference><a:Address>[endpoint]</a:Address></a:EndpointReference></wsp:AppliesTo><t:KeyType>http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey</t:KeyType><t:RequestType>http://schemas.xmlsoap.org/ws/2005/02/trust/Issue</t:RequestType><t:TokenType>urn:oasis:names:tc:SAML:1.0:assertion</t:TokenType></t:RequestSecurityToken></s:Body></s:Envelope>";

	private String generateSAML() {
		String saml = reqXML.replace("[username]", user);
		saml = saml.replace("[password]", password);
		saml = saml.replace("[endpoint]", url);
		return saml;
	}
	
	public String login() throws XPathExpressionException, SAXException, ParserConfigurationException, IOException {
		String token;
		token = requestToken();
		String cookie = submitToken(token);
		return cookie;
	}
	
	public LoginManager(String userid, String pwd, String url2,String endpoint) {
		this.user=userid;
		this.password=pwd;
		this.url=url2;
		this.endpoint=endpoint;
	}
	
	private String requestToken() throws XPathExpressionException, SAXException,
		ParserConfigurationException, IOException {

		String saml = generateSAML();

		URL u = new URL(sts);
		URLConnection uc = u.openConnection();
		HttpURLConnection connection = (HttpURLConnection) uc;
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		
		connection.addRequestProperty("Content-Type",
				"text/xml; charset=utf-8");
	
		OutputStream out = connection.getOutputStream();
		Writer wout = new OutputStreamWriter(out);
		wout.write(saml);

		wout.flush();
		wout.close();

		InputStream in = connection.getInputStream();
		int c;
		StringBuilder sb = new StringBuilder("");
		while ((c = in.read()) != -1)
			sb.append((char) (c));
		in.close();
		String result = sb.toString();
		String token = extractToken(result);
		LoginManager.expiryDate = extractExpiryDate(result);
		System.out.println(expiryDate);
		System.out.println(token);
		return token;
	}
	
	private String extractToken(String result) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		//http://stackoverflow.com/questions/773012/getting-xml-node-text-value-with-java-dom
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document document = db.parse(new InputSource(new StringReader(result)));

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        String token = xp.evaluate("//BinarySecurityToken/text()", document.getDocumentElement());
        
        if (token == null || token.isEmpty()) {
        	String errorMsg = xp.evaluate("/", document.getDocumentElement());
        	throw new IOException("Failed to fetch token: " + errorMsg);
        }
        
        //handle error  S:Fault:
        //http://social.microsoft.com/Forums/en-US/crmdevelopment/thread/df862099-d9a1-40a4-b92e-a107af5d4ca2
        //System.out.println(token);
        return token;
	}
	
	private String extractExpiryDate(String result) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		//http://stackoverflow.com/questions/773012/getting-xml-node-text-value-with-java-dom
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document document = db.parse(new InputSource(new StringReader(result)));

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        String expiryDate = xp.evaluate("//Expires/text()", document.getDocumentElement());
        
        if (expiryDate == null || expiryDate.isEmpty()) {
        	String errorMsg = xp.evaluate("/", document.getDocumentElement());
        	throw new IOException("Failed to fetch expiration date: " + errorMsg);
        }
        //handle error  S:Fault:
        //http://social.microsoft.com/Forums/en-US/crmdevelopment/thread/df862099-d9a1-40a4-b92e-a107af5d4ca2
        //System.out.println(token);
        return expiryDate;
	}
	
	private String submitToken(String token) throws IOException {
		
		
		String url = this.endpoint+ login;
		URL u = new URL(url);
		URLConnection uc = u.openConnection();
		HttpURLConnection connection = (HttpURLConnection) uc;
		connection.setInstanceFollowRedirects(false);
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		//connection.addRequestProperty("Accept", "application/x-www-form-urlencoded");
		//connection.addRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
		connection.addRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0)");
		
		// http://stackoverflow.com/questions/12294274/mobile-app-for-sharepoint/12295224#12295224
		// connection.addRequestProperty("SOAPAction", sts);
		//connection.addRequestProperty("Content-Type",
		//		"text/xml; charset=utf-8");
		//connection.addRequestProperty("Content-Length", token.length() + "");

		
		
		
		// connection.addRequestProperty("Expect", "100-continue");
		// connection.addRequestProperty("Connection", "Keep-Alive");
		// connection.addRequestProperty("Content-Length", saml.length() +
		// "");

		OutputStream out = connection.getOutputStream();
		Writer wout = new OutputStreamWriter(out);
		wout.write(token);

		wout.flush();
		wout.close();
		InputStream in = connection.getInputStream();
		//http://www.exampledepot.com/egs/java.net/GetHeaders.html
		StringBuilder cookieValue=new StringBuilder();
	    for (int i=0; ; i++) {
	        String headerName = connection.getHeaderFieldKey(i);
	        String headerValue = connection.getHeaderField(i);
	      //  System.out.println("header: " + headerName + " : " + headerValue);
	        if(headerName!=null)
	        if(headerName.equalsIgnoreCase("set-cookie")){
	        String	cv=headerValue;
	        	if(cv.contains(";"))
	        		cookieValue.append(cv.substring(0,cv.indexOf(";")+1));
	        	
	        }
	        if (headerName == null && headerValue == null) {
	            // No more headers
	            break;
	        }
	       
	    }
		
		int c;
		StringBuilder sb = new StringBuilder("");
		while ((c = in.read()) != -1)
			sb.append((char) (c));
		in.close();
		String result = sb.toString();
		System.out.println(result);
		cookie=cookieValue.toString();
		return cookieValue.toString();
	}
}