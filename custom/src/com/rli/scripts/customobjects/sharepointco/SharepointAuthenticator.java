package com.rli.scripts.customobjects.sharepointco;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/*
 * class used to athenticate user with credentials provided by auuthenticate
 * method this class is implementation of Authenticator for NTML or Basic
 * authentication using jre 1.6
 */
public class SharepointAuthenticator extends Authenticator {

	private String username, password;

	public SharepointAuthenticator(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public PasswordAuthentication getPasswordAuthentication() {

		return (new PasswordAuthentication(username, password.toCharArray()));

	}

}