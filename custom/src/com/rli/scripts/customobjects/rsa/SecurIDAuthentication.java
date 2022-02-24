package com.rli.scripts.customobjects.rsa;

import java.io.IOException;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import com.rsa.authmgr.common.SecurIDAuthenticationConstants;
import com.rsa.command.CommandTarget;
import com.rsa.command.CommandTargetPolicy;
import com.rsa.command.ConnectionFactory;
import com.rsa.common.AuthenticationConstants;

/**
 * Sample code illustrating how to perform SecurID authentications via the
 * command API. Prompting for user and passcode information is handled via stdin
 * and stdout.
 *
 * <p>
 * Like the password example, the particular method is explicitly requested,
 * allowing for simpler programming. However, as SecurID authentications may
 * require additional information from the user, the client must handle the
 * possibility of additional challenges. In this example, only the
 * SecurID-specific prompts are handled but the example code could be extended
 * to support the full range of responses from the server.
 * </p>
 * 
 * <p>
 * Note that there are some important differences between the command API and
 * the ACE authentication protocol. Most notably, the command API does not make
 * use of agents. This means that features such as restricted agents,
 * time-restricted access, contact lists, aliases, and trusted realm
 * authentications do not apply to authentications performed through the command
 * API. In addition, the communications channel for the command API is secured
 * with SSL rather than the agent node secret.
 * </p>
 * 
 * <p>
 * Please refer to the developer documentation for additional details about the
 * <code>LoginCommand</code> and the different authentication methods available.
 * </p>
 */
public class SecurIDAuthentication {

	/** Mapping of message IDs of the expect fields to English text. */
	private static ResourceBundle sidBundle = new ListResourceBundle() {
		@Override
		protected Object[][] getContents() {
			return new Object[][] {
					new Object[] { AuthenticationConstants.PRINCIPAL_ID,
							"User ID: " },
					new Object[] {
							SecurIDAuthenticationConstants.PromptKeys.SYSGEN_PIN,
							"System generated PIN: {0}" },
					new Object[] {
							SecurIDAuthenticationConstants.PromptKeys.USER_NEW_PIN,
							"New PIN: " },
					new Object[] {
							SecurIDAuthenticationConstants.PromptKeys.USER_NEW_PIN_CONFIRM,
							"New PIN (confirm): " },
					new Object[] {
							SecurIDAuthenticationConstants.PromptKeys.NEXT_TOKENCODE,
							"Next tokencode: " },
					new Object[] {
							SecurIDAuthenticationConstants.PromptKeys.PASSCODE,
							"Passcode: " }, };
		}
	};

	/**
	 * Performs the SecurID authentication, including prompts for additional
	 * data such as new PIN and next tokencode modes. Under normal conditions
	 * authentications should complete in one pass. But in the event that
	 * additional challenges are required, the code the simply loops until a
	 * success or failure is encountered, processing whatever challenges the
	 * server presents. This strategy not only works for SecurID
	 * authentications, but for other multi-stage methods as well as
	 * policy-driven authentications as well.
	 *
	 * @param target
	 *            the command target to use
	 * @param username
	 * @param passcode
	 *
	 * @return True if authentication successful, false if credentials are
	 *         invalid
	 * @throws Exception
	 *             if unable to correctly process request
	 */
	public static boolean doAuthentication(CommandTarget target,
			String username, String passcode) throws Exception {

		StreamParameterHandler handler = new StreamParameterHandler(sidBundle);

		// start the authentication
		AuthenticatedTarget session = new AuthenticatedTarget(target);
		session.login(handler, null, SecurIDAuthenticationConstants.METHOD_ID,
				null, username, passcode);

		// replace the default command target now so we can use this session
		CommandTargetPolicy.setDefaultCommandTarget(session);
		return session.getUserGuid() != null;
	}
}
