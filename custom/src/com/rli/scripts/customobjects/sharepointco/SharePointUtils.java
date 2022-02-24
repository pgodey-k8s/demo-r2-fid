package com.rli.scripts.customobjects.sharepointco;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.rli.vds.util.InterceptParam;

public class SharePointUtils {
	
	 public static String getCookie(InterceptParam param)
	throws XPathExpressionException, SAXException,
	ParserConfigurationException, IOException, ParseException {
String userid = param.getConnectionstringObject().getProperty(
		"username");
String pwd = param.getConnectionstringObject().getProperty("password");
String url = param.getConnectionstringObject().getProperty("url");
String endpoint = param.getProperty("endpoint");

String cookie = "";
String formatString = "yyyy-mm-dd'T'HH:mm:ss'Z'";
Date todaydate = new Date();
SimpleDateFormat df = new SimpleDateFormat(formatString);
Date expdate;
if (LoginManager.expiryDate != null)
	expdate = df.parse(LoginManager.expiryDate);
else
	expdate = todaydate;

if (expdate.compareTo(todaydate) > 0) {
	cookie = LoginManager.cookie;
} else {
	LoginManager loginManager = new LoginManager(userid, pwd, url,endpoint);
	cookie = loginManager.login();
}
return cookie;
}

	
	 public static void doTrustToCertificates() throws Exception {
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) throws CertificateException {
				return;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) throws CertificateException {
				return;
			}

		} };

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
					System.out.println("Warning: URL host '" + urlHostName
							+ "' is different to SSLSession host '"
							+ session.getPeerHost() + "'.");
				}
				return true;
			}

		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}

}
