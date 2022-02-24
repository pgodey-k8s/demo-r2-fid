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
public class SharepointNamingEnumaration implements
		NamingEnumeration<SearchResult> {

	private List<SearchResult> searchResults = new ArrayList<SearchResult>();

	private static final String ATTR_OBJECTCLASS = "objectclass";

	private static String NAMESPACE = "http://microsoft.com/webservices/SharePointPortalServer/UserProfileService";

	private int sizelimit;

	private int currentIndex = 1;

	private InterceptParam param;

	private static final int MAX_PAGE_SIZE = 100;

	private int resultsSize = 0;

	private boolean isClosed = false;

	private static final String ATTR_ID = "id";

	private String scimfilter = null;

	ArrayList<String> attlist = new ArrayList<String>();

	public SharepointNamingEnumaration(int sizelimit, InterceptParam param)
			throws Exception {

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
		
		String userid = param.getConnectionstringObject().getProperty(
				"username");
		String pwd = param.getConnectionstringObject().getProperty("password");

		
		String id = "";
		
		int count = MAX_PAGE_SIZE;
		if (sizelimit > 0) {
			count = sizelimit - resultsSize;
			if (count > MAX_PAGE_SIZE)
				count = MAX_PAGE_SIZE;
		}
		try {

			String nextValue = "-1";

			do {
				if(sizelimit>0)
					if(sizelimit < resultsSize){
						isClosed=true;
						break;
					}
				
			
				SOAPConnectionFactory scf = SOAPConnectionFactory.newInstance();
				SOAPConnection connection = scf.createConnection();
				SOAPFactory sf = SOAPFactory.newInstance();
				java.net.Authenticator.setDefault(new SharepointAuthenticator(
						userid, pwd));
				if (url.contains("https")) {
					java.lang.System.setProperty(
							"sun.security.ssl.allowUnsafeRenegotiation", "true");
					System.out.println("In Https certificate trust");
					SharePointUtils.doTrustToCertificates();
				}
				// Create the message
				MessageFactory mf = MessageFactory.newInstance();
				SOAPMessage message = mf.createMessage();

				// Create objects for the message parts
				SOAPPart soapPart = (SOAPPart) message.getSOAPPart();
				SOAPEnvelope envelope = (SOAPEnvelope) soapPart.getEnvelope();
				SOAPBody body = envelope.getBody();

				// Populate the body of the message
				Name bodyName = sf.createName("GetUserProfileByIndex", "",
						NAMESPACE);
				SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
				Name name = sf.createName("index");
				SOAPElement accname = bodyElement.addChildElement(name);

				accname.addTextNode(nextValue);

				// Set the destination
				URL endpoint = new URL(url);
				if (url.contains("https"))
					SharePointUtils.doTrustToCertificates();
				MimeHeaders hd = message.getMimeHeaders();
				hd.addHeader("SOAPAction", NAMESPACE + "/GetUserProfileByIndex");
				hd.addHeader("Content-Type", "text/xml; charset=UTF-8");
				System.out.println("Calling Sharepoint Service...");
				message.saveChanges();
				message.writeTo(System.out);
				SOAPMessage response = connection.call(message, endpoint);
				System.out.println(hd.getHeader("Content-Type"));
				response.writeTo(System.out);

				// prepare attributes...
				Attributes attributes = new BasicAttributes();

				// objectclass
				Attribute attribute = new BasicAttribute(ATTR_OBJECTCLASS,
						"top");
				attribute.add(param.getObjectclass());
				attributes.put(attribute);

				if (response.getSOAPBody().hasFault()) {
					SOAPFault newFault = response.getSOAPBody().getFault();
					QName code = newFault.getFaultCodeAsQName();

					String string = newFault.getFaultString();
					String actor = newFault.getFaultActor();

					param.setErrorcode(LDAPException.OPERATION_ERROR);
					param.setStatusFailed();
					param.setErrormessage("Select Operation :SOAP Fault Contains :"
							+ string);

				} else {
					/*
					 * Reading the output from soap response Logic to fetch the
					 * data from soap response to propertydata arraylist
					 */
					SOAPBody sbody = response.getSOAPBody();
					NodeList nv = sbody.getElementsByTagName("NextValue");
					Node nvalue = nv.item(0);
					nextValue = nvalue.getTextContent();
					NodeList n = sbody.getElementsByTagName("PropertyData");
					if (n != null) {
						if(n.getLength()>0){

						System.out
								.println(" Reading Sharepoint service returned attributes");
						/*
						 * getting the list of attributes from the schema using
						 * param
						 */

						String attribname = null;
						String attribvalue = null;
						ArrayList<String> valueArray = new ArrayList<String>();
						for (int i = 0; i < n.getLength(); i++) {

							NodeList childn = n.item(i).getChildNodes();

							if (childn != null) {
								for (int j = 0; j < childn.getLength(); j++) {
									Node nch = childn.item(j);

									if (nch.getNodeName().equalsIgnoreCase(
											"Name")) {

										attribname = nch.getTextContent();
									} else if (nch.getNodeName()
											.equalsIgnoreCase("Values")) {
										if (nch.getChildNodes() != null) {

											NodeList cnodelist = nch
													.getChildNodes();
											for (int k = 0; k < cnodelist
													.getLength(); k++) {
												Node valuen = cnodelist.item(k);
												if (valuen.getNodeName()
														.equalsIgnoreCase(
																"ValueData")) {

													Node v = valuen
															.getFirstChild();
													attribvalue = v
															.getTextContent();
												}
												if (k == 0) {
													attribute = new BasicAttribute(
															attribname,
															attribvalue);
													if (attribname
															.equalsIgnoreCase("AccountName"))
														id = attribvalue;
												} else
													attribute.add(attribvalue);
											}

										} else {

											attribvalue = nch.getTextContent();
											if (attribname
													.equalsIgnoreCase("AccountName"))
												id = attribvalue;
											attribute = new BasicAttribute(
													attribname, attribvalue);
										}
									}

								}
								attributes.put(attribute);
							}
						}
						// ID
						attribute = new BasicAttribute(ATTR_ID, id);
						attributes.put(attribute);
						String dn = param.getTypename() + RLIConstants.EQUAL
								+ id;
						searchResults
								.add(new SearchResult(dn, null, attributes));
						resultsSize++;
						System.out
								.println("After placing sharepoint attributes into resultset");
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
