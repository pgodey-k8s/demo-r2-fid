package com.rli.scripts.customobjects.azure.tokenhandler;

import java.net.URI;
import java.net.URISyntaxException;

import com.rli.scripts.customobjects.azure.exception.CustomAzureException;
import com.rli.scripts.customobjects.azure.services.CustomObjectParameter;


public class TokenGenerator {


	
	/**
	 * This method generates an access token for the caller.
	 * @param tenantContextId The context Id of the tenant.
	 * @param appPrincipalId Application Principal Id.
	 * @param stsUrl The Url of ACS (STS).
	 * @param acsPrincipalId Principal Id of ACS.
	 * @param symmetricKey Symmetric key for generating the self signed token.
	 * @param protectedResourcePrincipalId The Principal Id of the protected Resource.
	 * @param protectedResourceHostName The host name of the protected Resource host name.
	 * @return 
	 * @throws CustomAzureException If the operation can't be successfully completed.
	 */
	public static String generateToken(String tenantContextId, String appPrincipalId,
			String stsUrl, String acsPrincipalId, String symmetricKey, String protectedResourcePrincipalId,
			String protectedResourceHostName) throws CustomAzureException{
		
		JsonWebToken webToken;
		try {
			webToken = new JsonWebToken(appPrincipalId, tenantContextId, (new URI(stsUrl)).getHost(), acsPrincipalId, JWTTokenHelper.getCurrentDateTime(), 60*60);
		} catch (URISyntaxException e) {
			throw new CustomAzureException(CustomObjectParameter.ErrorGeneratingToken, CustomObjectParameter.ErrorGeneratingTokenMessage, e);
		}
		String assertion = JWTTokenHelper.generateAssertion(webToken, symmetricKey);
		String resource = String.format("%s/%s@%s", protectedResourcePrincipalId, protectedResourceHostName, tenantContextId);
		return JWTTokenHelper.getOAuthAccessTokenFromACS(stsUrl, assertion, resource);
	
	}
	
	
}
