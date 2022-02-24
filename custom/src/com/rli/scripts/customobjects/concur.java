package com.rli.scripts.customobjects;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;

import com.rli.scripts.customobjects.concurco.data.UserProfile;
import com.rli.scripts.customobjects.concurco.services.ConcurOAuthTokenHandler;
import com.rli.scripts.customobjects.concurco.services.ConcurUserServices;
import com.rli.scripts.customobjects.concurco.utils.ConcurParameters;
import com.rli.slapd.server.LDAPException;
import com.rli.util.RLIConstants;
import com.rli.util.djava.ScriptHelper;
import com.rli.vds.util.InterceptParam;
import com.rli.vds.util.UserDefinedInterception2;

/**
 * Customobject sample for the concur webservice client<br>
 * association between the ORX and the javaclass in VDS)<br>
 * Orx is defined using custom datasource with properties 
 * url, username, password, authorizationkey
 * <br>
 * Schema overview:<br>
 * objectclass: userpfoile<br>
 * key name: LoginId<br>
 * key value: LoginId of the user <br>
 * The schema defined in the ORX can be added (necessary to add a new user through VDS) 
 * with the Administration Console in the Configuration tab.<br> 
 * <br>
 * Supported operations are:<br>
 * <li>select</li>
 * <li>insert</li>
 * <li>update</li>
 * <li>delete</li>
 * <br>
 * <br>
 * 
 * @author: Praveen Nandi
 * @since 06-Feb-2012
 * @version 1.0
 */
public class concur implements UserDefinedInterception2 {

	private static final String ATTR_ID = "LoginId";

	private static final String METHOD_GET = "get";

	private static final String TOKEN_URL = "https://www.concursolutions.com/net2/oauth2/accesstoken.ashx";

	private String getToken(InterceptParam param) throws Exception {
		String formatString = "dd/mm/yyyy hh:mm:ss a";

		String userid = param.getConnectionstringObject().getProperty("username");
		String pwd = param.getConnectionstringObject().getProperty("password");
		String authorizationKey = param.getConnectionstringObject()
				.getProperty("authorizationKey");
		Date todaydate = new Date();
		SimpleDateFormat df = new SimpleDateFormat(formatString);
		String token = "";
		Date expdate;
		if (ConcurParameters.getExpiryDate() != null)
			expdate = df.parse(ConcurParameters.getExpiryDate());
		else
			expdate = todaydate;

		if (expdate.compareTo(todaydate) > 0) {
			token = ConcurParameters.getToken();
		} else {
			ConcurOAuthTokenHandler coth = new ConcurOAuthTokenHandler(userid,
					pwd, TOKEN_URL, authorizationKey);
			token = coth.getOAuthToken();
		}
		return token;
	}

	@Override
	public void select(InterceptParam param) {

		try {
			String response = "";
			String url = param.getConnectionstringObject().getProperty("url");
			String userid = param.getConnectionstringObject().getProperty(
					"username");
			ConcurParameters.setUrl(url);
			// create the entries vector
			Vector entries = new Vector();
			String idToLookup = null;
			// get a suitable size limit
			int sizelimit = 100;
			String token = getToken(param);
			if (param.getScope().equals("base")) {

				sizelimit = 1;
				String dnStr = param.getVirtualBaseDn();
				idToLookup = getId(dnStr);

			} else {

				sizelimit = param.getSizelimit();
			}
			if (idToLookup == null) {
				idToLookup = userid;
			}

			if (idToLookup != null) {

				UserProfile userprofile = ConcurUserServices
						.getUserProfile(idToLookup);

				entries.add(buildSearchResult(userprofile, param));

				param.setResultSet(entries);

			}
		} catch (Exception e) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public void insert(InterceptParam param) {
		try {

			boolean results = ConcurUserServices.createUserProfile(param);
			if (!results) {
				param.setErrorcode(LDAPException.OPERATION_ERROR);
				param.setStatusFailed();
				param.setErrormessage("Unable to create User .");
			}

		} catch (Exception e) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
		}

	}

	@Override
	public void delete(InterceptParam param) {
		try {
			String dnStr = param.getVirtualBaseDn();
			String id = getId(dnStr);
			UserProfile up = ConcurUserServices.getUserProfile(id);
			boolean results = ConcurUserServices.deactivateUserProfile(up);
			if (!results) {
				param.setErrorcode(LDAPException.OPERATION_ERROR);
				param.setStatusFailed();
				param.setErrormessage("Unable to Deactivate User .");
			}

		} catch (Exception e) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
		}

	}

	@Override
	public void update(InterceptParam param) {

		try {
			String dnStr = param.getVirtualBaseDn();
			String id = getId(dnStr);
			UserProfile up = ConcurUserServices.getUserProfile(id);
			String pwd = param.getConnectionstringObject().getProperty("password");
			up.setPassword(pwd);
			up.setFeedRecordNumber("1");
			boolean results = ConcurUserServices.updateUserProfile(param,up);
			if (!results) {
				param.setErrorcode(LDAPException.OPERATION_ERROR);
				param.setStatusFailed();
				param.setErrormessage("Unable to Deactivate User .");
			}

		} catch (Exception e) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
		}
	}

	private SearchResult buildSearchResult(UserProfile user,
			InterceptParam param) throws Exception {

		// prepare attributes...
		Attributes attributes = new BasicAttributes();
		String loginId = user.getLoginId();
		// ID
		Attribute attribute = new BasicAttribute(ATTR_ID, loginId);
		attributes.put(attribute);

		Class c = user.getClass();
		String attrs = param.getAttrs();
		StringTokenizer st = new StringTokenizer(attrs, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();

			Method m = c.getMethod(METHOD_GET + token, null);
			attribute = new BasicAttribute(token, m.invoke(user, null));
			if(attribute.get(0)!=null)
			if (!(attribute.get(0).equals("null"))) {

				attributes.put(attribute);
			}

		}

		String dn = param.getTypename() + RLIConstants.EQUAL + loginId;

		return new SearchResult(dn, null, attributes);
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

	/**
	 * get the contact ID from the given DN
	 * 
	 * @param dn
	 * @return The RDN value
	 * @throws Exception
	 */
	private String getId(String dn) throws Exception {

		String id = ScriptHelper.getRDNValue(dn);
		if (id.indexOf("{") > 0 && id.indexOf("}") > 0) {
			id = id.substring(id.indexOf("{") + 1, id.indexOf("}"));

		}
		if (id.contains("\\\\"))
			id = id.replace("\\\\", "");
		if (id.contains("\\"))
			id = id.replace("\\", "");

		return id;
	}

}
