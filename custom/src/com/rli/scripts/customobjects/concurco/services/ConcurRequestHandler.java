package com.rli.scripts.customobjects.concurco.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import com.rli.scripts.customobjects.concurco.utils.ConcurParameters;


public class ConcurRequestHandler {

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
	public static String handleRequest(String urlString) throws Exception {
		BufferedReader reader = null;
		HttpURLConnection conn = null;

		try {

			URL url = new URL(urlString);
			URI uri = new URI(url.getProtocol(), url.getUserInfo(),
					url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), url.getRef());
			url = uri.toURL();
			conn = (HttpURLConnection) url.openConnection();

			conn.setRequestProperty("Authorization", "OAuth "
					+ ConcurParameters.getToken());

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

	/**
	 * This method would handle the http POST/PATCH request to the REST
	 * Endpoint.
	 * 
	 * @param path
	 *            The additional path part of the URL
	 * @param queryOption
	 *            The response of the request sent
	 * @param data
	 *            Data that would be put to the http request POST/PATCH body.
	 * @param opName
	 *            The name of the operation that is invoking this method.
	 * @return
	 * @throws Exception
	 */
	public static String handlRequestPost(String urlString, String data)
			throws Exception {

		HttpURLConnection conn = null;

		urlString = urlString + "s";
		try {

			URL url = new URL(urlString);
			URI uri = new URI(url.getProtocol(), url.getUserInfo(),
					url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), url.getRef());
			url = uri.toURL();

			/**
			 * Open an URL Connection.
			 */

			conn = (HttpURLConnection) url.openConnection();

			conn.setRequestProperty("Content-Type", "application/xml");
			conn.setRequestProperty("Authorization", "OAuth "
					+ ConcurParameters.getToken());

			conn.setRequestMethod("POST");

			/**
			 * Send the http message payload to the server.
			 */
			conn.setDoOutput(true);
			System.out.println("Data sent is : " + data);
			OutputStreamWriter wr = new OutputStreamWriter(
					conn.getOutputStream());
			wr.write(data);
			wr.flush();

			/**
			 * Get the session key from the http response header.
			 */
			// AppParameter.sessionKeyValue =
			// conn.getHeaderField(AppParameter.sessionKeyHeader);

			/**
			 * Get the message response from the server.
			 */
			/*
			 * BufferedReader rd = new BufferedReader(new
			 * InputStreamReader(conn.getInputStream())); String line, response
			 * = ""; while((line=rd.readLine()) != null){ response += line; }
			 */

			/**
			 * Close the streams.
			 */
			wr.close();
			// rd.close();
			/**
			 * Get the reader for this connection.
			 */
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			/**
			 * Read the response stream and return the equivalent string.
			 */

			StringBuffer stringBuf = new StringBuffer();
			String inputLine;

			while ((inputLine = reader.readLine()) != null) {
				stringBuf.append(inputLine);
			}

			int responseCode = conn.getResponseCode();
			System.out.println("Response Code: " + responseCode);
			System.out.println(stringBuf.toString());
			return stringBuf.toString();

		} catch (Exception e2) {

			try {
				int responseCode = conn.getResponseCode();
				System.out.println("Response Code: " + responseCode);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				throw new Exception(e1);
			}

			/**
			 * Get the error stream.
			 */
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getErrorStream()));
			StringBuffer stringBuf = new StringBuffer();
			String inputLine;
			try {
				while ((inputLine = reader.readLine()) != null) {
					stringBuf.append(inputLine);
				}
			} catch (IOException e) {
				throw new Exception(e);

			}
			String response = stringBuf.toString();
			System.out.println(response);
			return response;
		}
		// SCIMParameters.sessionKeyHeader);

	}

	public static void main(String ars[]) {
		ConcurOAuthTokenHandler cth = new ConcurOAuthTokenHandler(
				"praveennandi@gmail.com", "pra101980",
				"https://www.concursolutions.com/net2/oauth2/accesstoken.ashx",
				"bfPF4Y1nBKsSNNk6RnexOd");
		try {
			String token = cth.getOAuthToken();
			System.out.println(token);
			ConcurRequestHandler cr = new ConcurRequestHandler();
			String loginname = URLEncoder.encode("praveennandi@gmail.com",
					"UTF-8");
			// String
			// response=cr.handleRequest("https://www.concursolutions.com/api/user/v1.0/User/?loginID="+loginname);
			String response = cr
					.handleRequest("https://www.concursolutions.com/api/user/v1.0/User");
			System.out.println(response);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
