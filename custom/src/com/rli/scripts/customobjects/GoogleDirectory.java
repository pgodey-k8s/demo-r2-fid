package com.rli.scripts.customobjects;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchResult;

import com.google.api.services.admin.directory.model.User;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryHandler;
import com.rli.scripts.customobjects.googledirectory.GoogleObject;
import com.rli.slapd.server.LDAPException;
import com.rli.vds.util.InterceptParam;
import com.rli.vds.util.UserDefinedInterception2;

public class GoogleDirectory implements UserDefinedInterception2 {

	private GoogleDirectoryHandler gConnection = null;

	private void setHandler(InterceptParam prop) throws Exception {
		if (gConnection == null)
			gConnection = new GoogleDirectoryHandler(prop.getCompoundObject(), prop);
	}

	private void reportError(InterceptParam prop, Throwable e) {
		prop.setErrorcode(LDAPException.OPERATION_ERROR);
		prop.setStatusFailed();
		StringBuilder errorMessage = new StringBuilder("Error type: " + e.getClass().getName() + "; message: { "
				+ e.getMessage() + " }" + System.lineSeparator() + "Stack trace: ");
		for (StackTraceElement s : e.getStackTrace()) {
			errorMessage.append(s.toString());
			errorMessage.append(System.lineSeparator());
		}
		prop.setErrormessage(errorMessage.toString());
	}

	@Override
	public void select(InterceptParam prop) {

		// REQUESTED ATTRIBUTES
		String[] requestedAttributes = null;
		try {
			setHandler(prop);

			if (prop.getScope().equals("base")) {
				SearchResult entry = gConnection.lookupEntry(prop.getDn(), prop.getFilter(), requestedAttributes);
				List<SearchResult> result = new ArrayList<SearchResult>();
				if (entry != null)
					result.add(entry);
				prop.setResultSet(result);
			} else if ("gduser".equals(prop.getName()) && prop.getSizelimit() == 1) {
				String domain = gConnection.getDomain();
				List<User> users = gConnection.getDir().users().list().setDomain(domain).setMaxResults(1).execute()
						.getUsers();
				GoogleObject go = gConnection.getGoogleObject();
				SearchResult sr = go.generate(users.get(0), null, null);

				List<SearchResult> srs = new ArrayList<>();
				srs.add(sr);
				prop.setResultSet(srs);
			} else {

				NamingEnumeration<?> enu = gConnection.search(prop.getVirtualBaseDn(), prop.getFilter(),
						requestedAttributes);
				prop.setResultSet_Object(enu);
			}
		} catch (Throwable e) {
			reportError(prop, e);
		}
	}

	@Override
	public void insert(InterceptParam prop) {
		try {
			setHandler(prop);

			gConnection.insert(prop.getDn(), prop.getAttributes());
		} catch (Throwable e) {
			reportError(prop, e);
		}
	}

	@Override
	public void delete(InterceptParam prop) {
		try {
			setHandler(prop);

			gConnection.delete(prop.getDn());
		} catch (Throwable e) {
			reportError(prop, e);
		}
	}

	@Override
	public void update(InterceptParam prop) {
		try {
			setHandler(prop);

			gConnection.update(prop.getDn(), prop.getModifications());
		} catch (Throwable e) {
			reportError(prop, e);
		}
	}

	@Override
	public void authenticate(InterceptParam prop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compare(InterceptParam prop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void invoke(InterceptParam prop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void testConnectToBackend(InterceptParam ip) {
		ip.setScope("sub");
		ip.setSizelimit(1);
		ip.setName("gduser");

		select(ip);
	}
}
