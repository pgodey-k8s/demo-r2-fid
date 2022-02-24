package com.rli.scripts.customobjects.azure.tokenhandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.rli.scripts.customobjects.azure.exception.CustomAzureException;
import com.rli.scripts.customobjects.azure.services.CustomObjectParameter;
import com.rli.web.http.json.JSONObject;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;



/**
 * Facilitates minting a test token.
 *
 */
public class JWTTokenHelper {
	
	
    /**
     * Grant type claim
     */ 
    private static final String claimTypeGrantType = "grant_type";

    /**
     * Assertion Claim.
     */
    private static final String claimTypeAssertion = "assertion";

    /**
     * Resource Claim.
     */
    private static final String claimTypeResource = "resource";

    /**
     * Prefix for bearer tokens.
     */
    private static final String bearerTokenPrefix = "Bearer ";

	
    /**
     * Get the formatted Service Principal Name
     * @param principalName Principal Identifier
     * @param hostName Service Host Name
     * @param realm Tenant Realm.
     * @return The formatted SPN.
     */
	public static String getFormattedPrincipal(String principalName, String hostName, String realm){

		if((hostName != null) && (realm != null)){
			return String.format("%s/%s@%s", principalName, hostName, realm);
		}else if((realm == null) || (realm.isEmpty()) || (realm.trim().isEmpty())){
			return String.format("%s/%s", principalName, hostName);
		}else{
			return String.format("%s@%s", principalName, realm);
		}		
		
	}
	
	/**
	 * Returns the starting Unix Epoch Time.
	 * @return the Unix Epoch Time.
	 */
	public static Date getUnixEpochDateTime() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(1970, 0, 1, 0, 0, 0);
		return calendar.getTime();
	}
	
	/**
	 * Returns the current Date Time in UTC.
	 * @return The current time in UTC.
	 */
	public static Date getCurrentDateTime() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		return calendar.getTime();
	}

	/**
	 * Add seconds to an existing date time object.
	 * @param seconds Seconds to be added.
	 * @return The new Date Time Object.
	 */
	public static Date addSecondsToCurrentTime(int seconds) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.SECOND, seconds);
		return calendar.getTime();
		
	}


	/**
	 * Generate access token with a symmetric signing key.
	 * @param webToken JSON Web Token.
	 * @param signingKey Symmetric signing key.
	 * @return Self Signed Assertion.
	 * @throws CustomAzureException If the operation is not successful.
	 */
	public static String generateAssertion(JsonWebToken webToken,
			String signingKey) throws CustomAzureException {

		TokenHeader tokenHeaderContract = new TokenHeader("HS256", "");
		String tokenHeader = Base64Utils.encode(tokenHeaderContract.encodeToJson());				
		String tokenBody = Base64Utils.encode(webToken.encodeToJson());
		
		String rawToken = String.format("%s.%s", tokenHeader, tokenBody);		
		String signature = Base64Utils.encode(JWTTokenHelper.signData(signingKey, rawToken));				
		String accessToken = String.format("%s.%s", rawToken, signature);		
		return accessToken;
	}

	
	/**
	 * Sign the text with the symmetric key.
	 * @param signingKey The Signing Key.
	 * @param rawToken The rawToken that needs to be signed.
	 * @return Signed byte array.
	 * @throws CustomAzureException
	 */
	private static byte[] signData(String signingKey, String rawToken) throws CustomAzureException {
		SecretKeySpec secretKey = null;
		try {
			secretKey = new SecretKeySpec(Base64.decode(signingKey), "HmacSHA256");
		} catch (Base64DecodingException e1) {
			throw new CustomAzureException(CustomObjectParameter.ErrorGeneratingToken, CustomObjectParameter.ErrorGeneratingTokenMessage, e1);
		}
		Mac mac;
		byte[] signedData = null;
		
		try {
			mac = Mac.getInstance("HmacSHA256");
			mac.init(secretKey);
			mac.update(rawToken.getBytes("UTF-8"));
			signedData = mac.doFinal();
			
		} catch (Exception e) {
			throw new CustomAzureException(CustomObjectParameter.ErrorGeneratingToken, CustomObjectParameter.ErrorGeneratingTokenMessage, e);
		}		
		return signedData;
	}


	/**
	 * Get an access token from ACS (STS).
	 * @param stsUrl ACS STS Url.
	 * @param assertion Assertion Token.
	 * @param resource ExpiresIn name.
	 * @return The OAuth access token.
	 * @throws CustomAzureException If the operation can not be completed successfully.
	 */
	public static String getOAuthAccessTokenFromACS(String stsUrl,
			String assertion, String resource) throws CustomAzureException {
		
		String accessToken = "";
				
		URL url = null;
		
		String data = null;
		
		try {
			data = URLEncoder.encode(JWTTokenHelper.claimTypeGrantType, "UTF-8") + "=" + URLEncoder.encode("http://oauth.net/grant_type/jwt/1.0/bearer", "UTF-8");
			data += "&" + URLEncoder.encode(JWTTokenHelper.claimTypeAssertion, "UTF-8") + "=" + URLEncoder.encode(assertion, "UTF-8");
			data += "&" + URLEncoder.encode(JWTTokenHelper.claimTypeResource, "UTF-8") + "=" + URLEncoder.encode(resource, "UTF-8");
			
			url = new URL(stsUrl);
			
			URLConnection conn = url.openConnection();
			
			conn.setDoOutput(true);
			
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(data);
			wr.flush();
			
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			String line, response = "";
			
			while((line=rd.readLine()) != null){
				response += line;
			}
			
			wr.close();
			rd.close();
			
			
			accessToken = (new JSONObject(response)).optString("access_token");						
			return String.format("%s%s", JWTTokenHelper.bearerTokenPrefix, accessToken);
			
		} catch (Exception e2) {
			throw new CustomAzureException(CustomObjectParameter.ErrorGeneratingToken, CustomObjectParameter.ErrorGeneratingTokenMessage, e2);
		}
	}



}
