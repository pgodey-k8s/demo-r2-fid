package com.rli.scripts.customobjects.rsa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.rli.slapd.server.LDAPException;
import com.rli.vds.util.InterceptParam;
import com.rli.web.http.service.twofactor.UserDefinedAuthenticator;

public class RsaRestAuthenticator implements UserDefinedAuthenticator {

	@Override
	public void authenticate(InterceptParam ip) {

		try {
			// connection to service
			String serviceUrl = ip.getConnectionstringUrl();
			String serviceUsername = ip.getConnectionstringUsername(); // not used
			String servicePassword = ip.getConnectionstringPassword(); // not used
			// parameters to authenticate / verify passcode
			String username = ip.getUserid();
			String password = ip.getPassword(); //full password unparsed
			String userDN=ip.getBindDN();
			String token = ip.getProperty(UserDefinedAuthenticator.PROP_TOKENXTRACT); // get token /pass code
			String passwordxtract = ip.getProperty(UserDefinedAuthenticator.PROP_PASSWORDXTRACT); // get password after parsing
			ip.setStatusProceed();
			ip.setErrorcodeZero();
			// verify input parameter to handle the authentication
			if ("".equals(username)||"".equals(token) ){
				//cannot pass identification of user or passcode for the authentication
				// if we impose to have it  we stop
				// we may  extract it from the userDN and continue (if we have the passcode)
				// or we may have also another way to get the passcode and continue
				
				ip.setStatusFailed();
				ip.setErrorcode(LDAPException.INAPPROPRIATE_AUTHENTICATION);	
				return;
			}
						
			String resultString;
			CloseableHttpClient httpClient = null;
			try {

				httpClient = getHttpClient();

				HttpPost post = new HttpPost(serviceUrl);
				post.setHeader("Content-Type", "application/xml");

				String body = getPostBody(username, token);
				HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
				post.setEntity(entity);
				String client_key = ip.getProperty("client_key");
				if(client_key != null)
					post.addHeader("client_key", client_key);

				CloseableHttpResponse response = null;
				try {
					response = httpClient.execute(post);

					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode != 200) {
						ip.setErrormessage("HttpCode: "
								+ Integer.toString(statusCode));
						
						ip.setStatusFailed();
						ip.setErrorcode(LDAPException.INVALID_CREDENTIALS);				
						return;
					}

					BufferedReader rd = new BufferedReader(
							new InputStreamReader(response.getEntity()
									.getContent()));

					StringBuffer result = new StringBuffer();
					String line;
					while ((line = rd.readLine()) != null) {
						result.append(line);
					}
					resultString = result.toString();

				} finally {
					if (response != null) {
						response.close();
					}
				}
			} finally {
				if (httpClient != null) {
					httpClient.close();
				}
			}

			if (isAuthenticated(resultString)) {
				ip.setPassword(passwordxtract); // set password extract part
				ip.setStatusProceed(); // indicate we want to do VDS authentication
			} else {
				ip.setStatusFailed();
				ip.setErrorcode(LDAPException.INVALID_CREDENTIALS);
				ip.setErrormessage("Invalid UserID or Passcode ");				
			}

		} catch (Exception e) {
			ip.setErrorcode(LDAPException.INAPPROPRIATE_AUTHENTICATION);
			ip.setErrormessage(e.toString());
			ip.setStatusFailed();

		}
	}

	private static String getPostBody(String userId, String password) {
		return "<Authentication type=\"token\"><token userID=\"" + userId
				+ "\" passcode=\"" + password + "\"/></Authentication>";

	}

	// <?xml version="1.0" encoding="UTF-8"
	// standalone="no"?><authenticationResult><authenticated>false</authenticated><code>1</code><failed>true</failed><message>ACCESS_DENIED</message></authenticationResult>
	private static boolean isAuthenticated(String xmlResponse) {

		int begin = xmlResponse.indexOf("<authenticated>");
		int end = xmlResponse.indexOf("</authenticated>");

		String authenticatedString = xmlResponse.substring(begin
				+ "<authenticated>".length(), end);

		return authenticatedString.equals("true");
	}

	private static CloseableHttpClient getHttpClient()
			throws NoSuchAlgorithmException, KeyStoreException,
			KeyManagementException {

		SSLContextBuilder builder = SSLContexts.custom();
		builder.loadTrustMaterial(null, new TrustStrategy() {
			@Override
			public boolean isTrusted(X509Certificate[] chain, String authType)
					throws CertificateException {
				return true;
			}
		});

		SSLContext sslContext = builder.build();

		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslContext, new X509HostnameVerifier() {

					@Override
					public void verify(String host, SSLSocket sslSocket)
							throws IOException {
					}

					@Override
					public void verify(String host, X509Certificate cert)
							throws SSLException {
					}

					@Override
					public void verify(String host, String[] cns,
							String[] subjectAlts) throws SSLException {
					}

					@Override
					public boolean verify(String host, SSLSession sslSession) {
						return true;
					}

				});

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
				.<ConnectionSocketFactory> create().register("https", sslsf)
				.register("http", new PlainConnectionSocketFactory()).build();

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(
				socketFactoryRegistry);
		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(cm).build();

		return httpClient;
	}

}
