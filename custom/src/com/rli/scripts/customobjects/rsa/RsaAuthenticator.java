package com.rli.scripts.customobjects.rsa;

import com.rli.slapd.server.LDAPException;
import com.rli.vds.util.InterceptParam;
import com.rli.web.http.service.twofactor.UserDefinedAuthenticator;
import com.rsa.command.CommandTarget;
import com.rsa.command.CommandTargetPolicy;
import com.rsa.command.ConnectionFactory;

public class RsaAuthenticator implements UserDefinedAuthenticator {

	private static final String PROP_JKS = "weblogic.security.SSL.trustedCAKeyStore";
	private static final String PROP_URL = "java.naming.provider.url";
	private static final String PROP_CMD_USER = "com.rsa.cmdclient.user";
	private static final String PROP_CMD_PWD = "com.rsa.cmdclient.user.password";

	
	private boolean initialized = false;
	private CommandTarget target;

	@Override
	public void authenticate(InterceptParam ip) {
		try {
			// predefined params
			String serviceUrl = ip.getConnectionstringUrl();
			String serviceUsername = ip.getConnectionstringUsername();
			String servicePassword = ip.getConnectionstringPassword();

			// flexible param
			String truststore = ip.getProperty("truststore");

			// initialize system properties and command target
			if (!initialized) {

				System.setProperty(PROP_JKS, truststore);
				System.setProperty(PROP_URL, serviceUrl);
				System.setProperty(PROP_CMD_USER, serviceUsername);
				System.setProperty(PROP_CMD_PWD, servicePassword);

				target = ConnectionFactory.getTarget();
				CommandTargetPolicy.setDefaultCommandTarget(target);

				initialized = true;
			}

			// do authentication with the rsa service
			// credentials
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
			}

			boolean authResult = SecurIDAuthentication.doAuthentication(target,
					username, token);

			if (authResult) {
				ip.setPassword(passwordxtract); // set password extract part
				ip.setStatusProceed(); // indicate we want to do VDS authentication
			} else {
				ip.setErrorcode(LDAPException.INVALID_CREDENTIALS);
				ip.setErrormessage("Invalid UserID or Passcode ");				
				ip.setStatusFailed();
				
			}

		} catch (Throwable t) {
			ip.setErrormessage(t.toString());
			ip.setErrorcode(LDAPException.INVALID_CREDENTIALS);
			ip.setStatusFailed();
		}

	}

}