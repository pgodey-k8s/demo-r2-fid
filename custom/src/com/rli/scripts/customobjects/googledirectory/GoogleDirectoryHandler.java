package com.rli.scripts.customobjects.googledirectory;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.rli.scripts.customobjects.googledirectory.googleobjects.GoogleChromeosdevices;
import com.rli.scripts.customobjects.googledirectory.googleobjects.GoogleGroup;
import com.rli.scripts.customobjects.googledirectory.googleobjects.GoogleMember;
import com.rli.scripts.customobjects.googledirectory.googleobjects.GoogleMobiledevices;
import com.rli.scripts.customobjects.googledirectory.googleobjects.GoogleOrgunits;
import com.rli.scripts.customobjects.googledirectory.googleobjects.GoogleUser;
import com.rli.synsvcs.common.ConnString2;
import com.rli.tools.view.core.ViewOperations;
import com.rli.util.djava.ScriptHelper;
import com.rli.util.jndi.vdap.compound.CompoundObject;
import com.rli.vds.util.InterceptParam;

/**
 * The GoogleDirectoryHandler is a router for GoogleObject. An LDAP request is
 * transformed into a Google request by the corresponding GoogleObject.
 * GoogleDirectoryHandler checks the credentials, connects to the Google
 * Directory API and calls the request method of the proper GoogleObject for
 * execution.
 * 
 * @author bdamour
 *
 */
public class GoogleDirectoryHandler implements Handler {
	private final static Logger log4jLogger = LogManager
			.getLogger(GoogleDirectoryHandler.class);

	// custom datasource connection string properties
	private static final String serv_email_prop = "service_account_email";
	private static final String serv_certif_prop = "service_account_p12_certificate";
	private static final String serv_user_prop = "service_account_user";
	private static final String domain_prop = "domain";
	private static final String SERVICE_NAME = "radiantlogic-vds";

	// virtual object class
	private final static String USER_CLASS = "vdgduser";
	private final static String GROUP_CLASS = "vdgdgroup";
	private final static String MEMBER_CLASS = "vdgdmember";
	private final static String MOBILEDEVICES_CLASS = "vdgdmobiledevice";
	private final static String CHROMEOSDEVICES_CLASS = "vdgdchromeosdevice";
	private final static String ORGUNITS_CLASS = "vdgdorgunit";

	private final static String DEFAULT_DN_WHEN_CO_NULL = "ou=objectclass,dv=viewname";

	// pagination parameter
	private final static int pageSize = 50;

	public static final String proxyProperty = "proxy";

	// the data store connection with the required credentials
	private Directory dir;
	private String domain;
	private CompoundObject co;
	private GoogleObject objectHandler;

	public GoogleDirectoryHandler(CompoundObject co, InterceptParam ip) throws Exception {
		this.co = co;
		// Get the required properties from the datasource
		ConnString2 cs2 = ip.getConnectionstringObject();
		String serviceAccountEmailAddress = cs2.getProperty(serv_email_prop);
		String serviceAccountP12Certificate = cs2.getProperty(serv_certif_prop);
		String serviceAccountUser = cs2.getProperty(serv_user_prop);
		domain = cs2.getProperty(domain_prop);

		// BUG 34339 - when called from control panel to test connection, domain is treated as a special property 
		if (domain == null){
			domain = cs2.getCustomProperty(domain_prop);
		}

		if (serviceAccountEmailAddress == null
				|| serviceAccountP12Certificate == null
				|| serviceAccountUser == null)
			throw new Exception(
					"A service account must be setup and provided"
							+ " to successfully connect to the Google Directory."
							+ " Please fill in the following properties in the datasource "
							+ cs2.getDatasourceName() + ": " + serv_email_prop
							+ ", " + serv_certif_prop + ", " + serv_user_prop
							+ ", " + domain_prop);
		if (domain == null) {
			throw new Exception("The domain name is required."
					+ " Please fill in the property " + domain_prop
					+ " in the datasource " + cs2.getDatasourceName() + ".");
		}
		File certif = new File(serviceAccountP12Certificate);
		if (!certif.exists())
			throw new Exception(
					"Cannot find the service account certificate file."
							+ " Please check the property " + serv_certif_prop
							+ " in the datasource " + cs2.getDatasourceName()
							+ ".");

		// Instantiate the data store
		try {
			JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
			HttpTransport httpTransport;
			String[] proxy = getProxy(ip);
			if(proxy==null){
				httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			}else{
				NetHttpTransport.Builder builder = new NetHttpTransport.Builder();
				builder.trustCertificates(GoogleUtils.getCertificateTrustStore());
				String host = proxy[0];
				int port = Integer.parseInt(proxy[1]);
				log4jLogger.info("using proxy server: " + host+":"+port);
				builder.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
				httpTransport = builder.build();
			}
			Collection<String> scopes = Arrays.asList(new String[] {
					DirectoryScopes.ADMIN_DIRECTORY_USER,
					DirectoryScopes.ADMIN_DIRECTORY_GROUP,
					DirectoryScopes.ADMIN_DIRECTORY_DEVICE_CHROMEOS,
					DirectoryScopes.ADMIN_DIRECTORY_DEVICE_MOBILE,
					DirectoryScopes.ADMIN_DIRECTORY_ORGUNIT });
			GoogleCredential credential = new GoogleCredential.Builder()
			.setTransport(httpTransport).setJsonFactory(JSON_FACTORY)
			.setServiceAccountId(serviceAccountEmailAddress)
			.setServiceAccountUser(serviceAccountUser)
			.setServiceAccountPrivateKeyFromP12File(certif)
			.setServiceAccountScopes(scopes).build();
			dir = new Directory.Builder(httpTransport, JSON_FACTORY, credential)
			.setApplicationName(SERVICE_NAME)
			.setHttpRequestInitializer(credential).build();
		} catch (Exception e) {
			logError("Error while trying to connect to the Google Directory. The service account information"
					+ " in the datasource "
					+ cs2.getDatasourceName()
					+ " must be wrong: "
					+ serv_email_prop
					+ ", "
					+ serv_certif_prop
					+ ", "
					+ serv_user_prop
					+ ", "
					+ domain_prop);
			throw e;
		}

		log("Connection to google directory established.");

		// Now that the connection is set up we can set the Object Handler
		String objectName = ip.getName();
		this.objectHandler = getGoogleObjectHandler(objectName);
		if (objectHandler == null)
			throw new Exception("The compound object types are not handled: "
					+ objectName);

		log("Google request created.");
	}

	public SearchResult lookupEntry(String dn, String filter,
			String[] attributesRequested) throws Exception {
		String key = objectHandler instanceof GoogleMember ? dn
				: getGoogleKeyFromDN(dn);
		return objectHandler.lookupEntry(key, filter, attributesRequested);
	}

	public GoogleDirectoryEnumeration search(String baseDN, String filter,
			String[] attributes) throws Exception {
		return objectHandler.search(baseDN, filter, attributes);
	}

	public void delete(String dn) throws Exception {
		String key = objectHandler instanceof GoogleMember ? dn
				: getGoogleKeyFromDN(dn);
		// deleting a group/user proc deletion of linked memberships
		objectHandler.delete(key);
	}

	public void insert(String dn, Attributes attrs) throws Exception {
		String key = objectHandler instanceof GoogleMember ? dn
				: getGoogleKeyFromDN(dn);
		// normalize attributes before insertion
		objectHandler.insert(key, normalizeAttributes(attrs));
	}

	/**
	 * Remove brackets around composite attribute names.
	 * 
	 * @param attrs
	 * @throws NamingException
	 */
	private Attributes normalizeAttributes(Attributes attrs)
			throws NamingException {
		Attributes normalizedAttributes = new BasicAttributes();
		// for all attributes
		NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();
		while (allAttrs.hasMore()) {
			Attribute att = allAttrs.next();
			String id = att.getID();
			// if name surrounded by brackets, remove those
			id = id.startsWith("[") && id.endsWith("]") ? id.substring(1,
					id.length() - 1) : id;
			normalizedAttributes.put(ScriptHelper.createAttribute(id, att));
		}
		allAttrs.close();
		return normalizedAttributes;
	}

	@Override
	public void update(String dn, List<ModificationItem> modificationAttrs)
			throws Exception {
		String key = objectHandler instanceof GoogleMember ? dn
				: getGoogleKeyFromDN(dn);
		objectHandler.update(key, modificationAttrs);
	}

	public String getMetaDN() {
		if (co==null){
			return DEFAULT_DN_WHEN_CO_NULL;
		}

		return co.getMetaDN();
	}

	public Directory getDir() {
		return dir;
	}

	public int getPageSize() {
		return pageSize;
	}

	public String getDomain() {
		return domain;
	}

	public GoogleObject getGoogleObject(){
		return objectHandler;
	}

	public String getGroupsMetaDN() {
		return getMetaDN(GROUP_CLASS);
	}

	/**
	 * The GoogleDirectory view follow this pattern:
	 * uid=objectkey,ou=objectclass,dv=viewname
	 * 
	 * @param target_objectclass
	 *            the objectclass of object of DN:
	 *            uid=objectkey,ou=objectclass,dv=viewname
	 * @return ou=objectclass,dv=viewname
	 */
	private String getMetaDN(String target_objectclass) {

		if (co==null){
			return DEFAULT_DN_WHEN_CO_NULL;
		}

		CompoundObject node = co;
		// go up till DV node (DN: dv=viewname)
		while (!ScriptHelper.contains(node.getVirtualObjectClass(),
				ViewOperations.STR_CLASS_DV))
			node = node.getParent();
		// find root node among DV children
		for (CompoundObject target : node.getChilds()) {
			// find root node whose child container is of target_objectclass
			if (target.getChildCount() == 1
					&& ScriptHelper.contains(target.getChilds().get(0)
							.getVirtualObjectClass(), target_objectclass))
				return target.getChilds().get(0).getMetaDN();
		}
		return null;
	}

	private GoogleObject getGoogleObjectHandler(String objectName) {
		if ("gduser".equals(objectName))
			return new GoogleUser(this);
		if ("gdgroup".equals(objectName))
			return new GoogleGroup(this);
		if ("gdmember".equals(objectName))
			return new GoogleMember(this);
		if ("gdmobiledevice".equals(objectName))
			return new GoogleMobiledevices(this);
		if ("gdchromeosdevice".equals(objectName))
			return new GoogleChromeosdevices(this);
		if ("gdorgunit".equals(objectName))
			return new GoogleOrgunits(this);
		return null;
	}

	private String getGoogleKeyFromDN(String dn) {
		return ScriptHelper.getRDNValue(dn);
	}

	private void log(String message) {
		log4jLogger.debug("GoogleDirectoryHandler> " + message);
	}

	private void logError(String message) {
		log4jLogger.warn("GoogleDirectoryHandler> " + message);
	}

	private String[] getProxy(InterceptParam ip){
		String proxyS = ip.getConnectionstringObject().getProperty(proxyProperty);
		if(proxyS==null)
			return null;
		String[] split = proxyS.split(":");
		if(split.length!=2){
			log4jLogger.error("invalid proxy property value (host:port) = " + proxyS);
			return null;
		}
		try{
			Integer.parseInt(split[1]);
		}catch (Exception e){
			log4jLogger.error("invalid proxy property value (host:port) = " + proxyS + ", " + e.toString());
			return null;
		}
		return split;
	}
}
