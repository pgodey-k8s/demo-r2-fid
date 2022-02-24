package com.rli.scripts.customobjects;

import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;

import com.rli.logging.LoggingUtils;
import com.rli.scripts.customobjects.azure.dto.Group;
import com.rli.scripts.customobjects.azure.dto.User;
import com.rli.scripts.customobjects.azure.exception.CustomAzureException;
import com.rli.scripts.customobjects.azure.services.CustomObjectParameter;
import com.rli.scripts.customobjects.azure.services.RestAPIGroupServices;
import com.rli.scripts.customobjects.azure.services.RestAPIUserServices;
import com.rli.scripts.customobjects.azure.services.RestAPIWriteServices;
import com.rli.scripts.customobjects.azure.utils.AzureNamingEnumaration;
import com.rli.scripts.customobjects.azure.tokenhandler.TokenGenerator;
import com.rli.slapd.server.LDAPException;
import com.rli.util.RLIConstants;
import com.rli.util.djava.ScriptHelper;
import com.rli.vds.util.InterceptParam;
import com.rli.vds.util.UserDefinedInterception2;

/**
 * Customobject sample for the Azure AD Restful webservice client<br>
 * association between the ORX and the javaclass in VDS)<br>
 * Orx is defined using custom datasource with properties 
 * DataContractVersion,TenantContextId, TenantDomainName,ProtectedResourcePrincipalId
 * ,ProtectedResourceHostName,RestServiceHost,AppPrincipalId,StsUrl,SymmetricKey, AcsPrincipalId
 * <br>
 * Schema overview:<br>
 * objectclass: azureuser or group<br>
 * key name: ObjectId<br>
 * key value: ObjectId of the user or group in Azure AD<br>
 * The schema defined in the ORX can be added (necessary to add a new user or group through VDS) 
 * with the Administration Console in the Configuration tab.<br> 
 * <br>
 * Supported operations are:<br>
 * <li>select</li>
 * <li>insert</li>
 * <li>update</li>
 * <li>delete</li>
 * <br>
 * <br>
 * Examples below assume the view is mounted in the o=azuread naming context<br>
 * [the ObjectId] is the ObjectId of user or group<br> 
 * <br>
 *
 * @author: Praveen Nandi
 * @since 06-Feb-2012
 * @version 1.0
 */
public class azuread implements UserDefinedInterception2 {

	private static final String ATTR_ID = "ObjectId";

	private static final String METHOD_GET = "get";
	

	private void createToken(InterceptParam param) {
		/**
		 * Load the initial parameters from the xml configuration file to the
		 * appropriate fields in the CustomObjectParameter class.
		 */
		CustomObjectParameter
				.setDataContractVersion(param.getConnectionstringObject()
						.getProperty("DataContractVersion"));
		CustomObjectParameter.setAcsPrincipalId(param
				.getConnectionstringObject().getProperty("AcsPrincipalId"));
		CustomObjectParameter.setAppPrincipalId(param
				.getConnectionstringObject().getProperty("AppPrincipalId"));
		CustomObjectParameter.setProtectedResourceHostName(param
				.getConnectionstringObject().getProperty(
						"ProtectedResourceHostName"));
		CustomObjectParameter.setRestServiceHost(param
				.getConnectionstringObject().getProperty("RestServiceHost"));
		CustomObjectParameter.setSymmetricKey(param.getConnectionstringObject()
				.getProperty("SymmetricKey"));
		CustomObjectParameter.setTenantDomainName(param
				.getConnectionstringObject().getProperty("TenantDomainName"));
		CustomObjectParameter.setTenantContextId(param
				.getConnectionstringObject().getProperty("TenantContextId"));
		CustomObjectParameter.setStsUrl(param.getConnectionstringObject()
				.getProperty("StsUrl"));
		CustomObjectParameter.setProtectedResourcePrincipalId(param
				.getConnectionstringObject().getProperty(
						"ProtectedResourcePrincipalId"));

		// If there is no predefined Access Token, generate an access token and
		// set it to the accessToken field in CustomObjectParameter.
		if (CustomObjectParameter.getAccessToken() == null) {
			try {
				CustomObjectParameter.setAccessToken(TokenGenerator
						.generateToken(CustomObjectParameter
								.getTenantContextId(), CustomObjectParameter
								.getAppPrincipalId(), CustomObjectParameter
								.getStsUrl(), CustomObjectParameter
								.getAcsPrincipalId(), CustomObjectParameter
								.getSymmetricKey(), CustomObjectParameter
								.getProtectedResourcePrincipalId(),
								CustomObjectParameter
										.getProtectedResourceHostName()));
			} catch (CustomAzureException e) {
				param.setErrorcode(LDAPException.OPERATION_ERROR);
				param.setStatusFailed();
				param.setErrormessage(e.getMessage());
				e.printStackTrace();
				e.getCause().printStackTrace();
				System.out.println("Can not generate Access Token");
				System.exit(1);
			}
		}

	}

	@Override
	public void authenticate(InterceptParam arg0) {

	}

	@Override
	public void compare(InterceptParam arg0) {

	}

	@Override
	public void delete(InterceptParam param) {
		if (CustomObjectParameter.getAccessToken() == null)
			createToken(param);
		String dn = param.getVirtualBaseDn();
		String objectID = null;
		try {
			objectID = getId(dn);
		} catch (Exception e1) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e1.getMessage());

			e1.printStackTrace();
		}
		try {
			if (param.getName().contains("user")) {
				RestAPIWriteServices.deleteUser(objectID, null);
			} else {
				RestAPIWriteServices.deleteGroup(objectID, null);
			}
			
		} catch (CustomAzureException e) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public void insert(InterceptParam param) {
		if (CustomObjectParameter.getAccessToken() == null)
			createToken(param);
		try {
			if (param.getName().contains("user")) {
				RestAPIWriteServices.createUser(param,null);
			} else {
				RestAPIWriteServices.createGroup(param,null);
			}
			
		} catch (CustomAzureException e) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void invoke(InterceptParam arg0) {

	}

	@Override
	public void select(InterceptParam param) {

		try {
			String response = "";
			System.out.println(param.getFilter());
			if (CustomObjectParameter.getAccessToken() == null)
				createToken(param);
			// create the entries vector
			Vector entries = new Vector();
			String idToLookup = null;
			// get a suitable size limit
			int sizelimit = 100;

			if (param.getScope().equals("base")) {

				sizelimit = 1;
				String dnStr = param.getVirtualBaseDn();
				idToLookup = getId(dnStr);

			} else {

				sizelimit = param.getSizelimit();
			}

			if (idToLookup != null) {

				if (param.getName().contains("user")) {
					User user = RestAPIUserServices.getUser(idToLookup, null);

					entries.add(buildSearchResult(user, param));
				} else {
					Group group = RestAPIGroupServices.getGroup(idToLookup,
							null);

					entries.add(buildSearchResult(group, param));
				}

				param.setResultSet(entries);

			} else {

				param.setResultSet_Object(new AzureNamingEnumaration(sizelimit,
						param));
			}
		} catch (Exception e) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public void update(InterceptParam param) {
		String dn = param.getVirtualBaseDn();
		String objectID = null;
		try {
			objectID = getId(dn);
		} catch (Exception e1) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e1.getMessage());

			e1.printStackTrace();
		}
		try {
			if (!param.getName().contains("group")) {
				RestAPIWriteServices.updateUser(param, objectID,null);
			} else {
				RestAPIWriteServices.updateGroup(param, objectID,null);
			}
			
		} catch (CustomAzureException e) {
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
			e.printStackTrace();

		}

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
		return id;
	}

	private SearchResult buildSearchResult(Object object, InterceptParam param)
			throws Exception {
		User userobj = null;
		Group groupobj = null;
		String id = null;

		if (param.getName().contains("user")) {
			userobj = (User) object;
			id = userobj.getObjectId();
		} else {
			groupobj = (Group) object;
			id = groupobj.getObjectId();
		}

		// prepare attributes...
		Attributes attributes = new BasicAttributes();

		// ID
		Attribute attribute = new BasicAttribute(ATTR_ID, id);
		attributes.put(attribute);

		Class c = object.getClass();
		String attrs = param.getAttrs();
		StringTokenizer st = new StringTokenizer(attrs, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();

			Method m = c.getMethod(METHOD_GET + token, null);
			attribute = new BasicAttribute(token, m.invoke(object, null));
			if (!(attribute.get(0).equals("null"))) {

				attributes.put(attribute);
			}

		}

		String dn = param.getTypename() + RLIConstants.EQUAL + id;

		return new SearchResult(dn, null, attributes);
	}

}
