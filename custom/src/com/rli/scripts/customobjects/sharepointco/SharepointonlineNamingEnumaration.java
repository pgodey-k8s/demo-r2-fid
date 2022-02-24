package com.rli.scripts.customobjects.sharepointco;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rli.slapd.server.LDAPException;
import com.rli.util.RLIConstants;
import com.rli.vds.util.InterceptParam;

/**
 * This class used to get the users or groups list from the scim server
 *
 *
 */
public class SharepointonlineNamingEnumaration implements NamingEnumeration<SearchResult> {

	private List<SearchResult> searchResults = new ArrayList<SearchResult>();

	private static final String ATTR_OBJECTCLASS = "objectclass";

	private static String NAMESPACE = "http://schemas.microsoft.com/sharepoint/soap/directory";

	private int sizelimit;

	private int currentIndex = 1;

	private InterceptParam param;

	private static final int MAX_PAGE_SIZE = 100;

	private int resultsSize = 0;

	private boolean isClosed = false;

	private static final String ATTR_ID = "id";

	private String scimfilter = null;

	ArrayList<String> attlist = new ArrayList<String>();

	public SharepointonlineNamingEnumaration(int sizelimit, InterceptParam param) throws Exception {

		this.sizelimit = sizelimit;
		this.param = param;

		buildFilter();
		getNextPage();
	}

	/**
	 * Used to generate filter used in the query to scim server
	 */
	private void buildFilter() {

	}

	@Override
	public void close() throws NamingException {
		isClosed = true;
		searchResults.clear();

	}

	@Override
	public boolean hasMore() throws NamingException {
		return searchResults.size() > 0;
	}

	@Override
	public SearchResult next() throws NamingException {

		if (searchResults.size() > 0) {

			SearchResult sr = searchResults.remove(0);

			return sr;
		} else
			return null;
	}

	@Override
	public boolean hasMoreElements() {

		try {
			return hasMore();
		} catch (NamingException e) {
			return false;
		}
	}

	@Override
	public SearchResult nextElement() {
		try {
			return next();
		} catch (NamingException e) {
			return null;
		}
	}

	/**
	 *
	 * Used to generate the next page results based on the requirements
	 *
	 * @throws Exception
	 */
	private void getNextPage() throws Exception {
		if (isClosed)
			return;

		String url = param.getConnectionstringObject().getProperty("url");
		String cookie = "";
		String userid = param.getConnectionstringObject().getProperty("username");
		String pwd = param.getConnectionstringObject().getPassword();
		String endpoint = param.getConnectionstringObject().getProperty("endpoint");

		String formatString = "yyyy-mm-dd'T'HH:mm:ss.s'Z'";
		Date todaydate = new Date();
		SimpleDateFormat df = new SimpleDateFormat(formatString);
		Date expdate;
		if (LoginManager.expiryDate != null)
			expdate = df.parse(LoginManager.expiryDate);
		else
			expdate = todaydate;

		if (expdate.compareTo(todaydate) > 0) {
			cookie = LoginManager.cookie;
		} else {
			LoginManager loginManager = new LoginManager(userid, pwd, url, endpoint);
			cookie = loginManager.login();
		}
		String id = "";
		url = url + "/_vti_bin/usergroup.asmx";
		int count = MAX_PAGE_SIZE;
		if (sizelimit > 0) {
			count = sizelimit - resultsSize;
			if (count > MAX_PAGE_SIZE)
				count = MAX_PAGE_SIZE;
		}
		try {

			String nextValue = "-1";

			do {
				if (sizelimit > 0)
					if (sizelimit < resultsSize) {
						isClosed = true;
						break;
					}

				SOAPConnectionFactory scf = SOAPConnectionFactory.newInstance();
				SOAPConnection connection = scf.createConnection();
				SOAPFactory sf = SOAPFactory.newInstance();

				// Create the message
				MessageFactory mf = MessageFactory.newInstance();
				SOAPMessage message = mf.createMessage();

				// Create objects for the message parts
				SOAPPart soapPart = (SOAPPart) message.getSOAPPart();
				SOAPEnvelope envelope = (SOAPEnvelope) soapPart.getEnvelope();
				SOAPBody body = envelope.getBody();

				// Populate the body of the message
				Name bodyName = sf.createName("GetUserCollectionFromSite", "", NAMESPACE);
				SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
				Name name = sf.createName("index");
				SOAPElement accname = bodyElement.addChildElement(name);

				accname.addTextNode(nextValue);

				// Set the destination
				URL endpointurl = new URL(url);
				if (url.contains("https"))
					SharePointUtils.doTrustToCertificates();
				MimeHeaders hd = message.getMimeHeaders();
				hd.addHeader("SOAPAction", NAMESPACE + "/GetUserCollectionFromSite");
				hd.addHeader("Content-Type", "text/xml; charset=UTF-8");
				hd.addHeader("cookie", cookie);
				System.out.println("Calling Sharepoint Service...");
				message.saveChanges();
				message.writeTo(System.out);
				SOAPMessage response = connection.call(message, endpointurl);
				System.out.println(hd.getHeader("Content-Type"));
				response.writeTo(System.out);

				// prepare attributes...
				Attributes attributes = new BasicAttributes();

				// objectclass
				Attribute attribute = new BasicAttribute(ATTR_OBJECTCLASS, "top");
				attribute.add(param.getObjectclass());
				attributes.put(attribute);

				if (response.getSOAPBody().hasFault()) {
					SOAPFault newFault = response.getSOAPBody().getFault();
					QName code = newFault.getFaultCodeAsQName();

					String string = newFault.getFaultString();
					String actor = newFault.getFaultActor();

					param.setErrorcode(LDAPException.OPERATION_ERROR);
					param.setStatusFailed();
					param.setErrormessage("Select Operation :SOAP Fault Contains :" + string);

				} else {
					/*
					 * Reading the output from soap response Logic to fetch the data from soap
					 * response to propertydata arraylist
					 */
					SOAPBody sbody = response.getSOAPBody();
					NodeList users = sbody.getElementsByTagName("User");
					if (users != null) {
						for (int i = 0; i < users.getLength(); i++) {
							NamedNodeMap attrs = users.item(i).getAttributes();
							String userId = null;
							Attributes userAttrs = new BasicAttributes();
							if (attrs != null) {
								for (int j = 0; j < attrs.getLength(); j++) {
									Node attr = attrs.item(j);
									String attrName = attr.getNodeName();
									String attrVal = attr.getNodeValue();

									if (attrName.equalsIgnoreCase("LoginName"))
										userId = attrVal;
								}

								if (userId != null) {
									userAttrs.put(new BasicAttribute(ATTR_ID, userId));
									String dn = param.getTypename() + RLIConstants.EQUAL + userId;
									searchResults.add(new SearchResult(dn, null, userAttrs));
									resultsSize++;
								}
							}
						}
					}
				}
			} while (!nextValue.equalsIgnoreCase("-1"));
		} catch (Exception e) {
			System.out.println("Exception in select :" + e.getMessage());
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
			e.printStackTrace();
		} finally {
		}

	}

}
