package com.rli.scripts.customobjects;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
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

import com.rli.scripts.customobjects.sharepointco.PropertyData;
import com.rli.scripts.customobjects.sharepointco.SharePointUtils;
import com.rli.scripts.customobjects.sharepointco.SharepointAuthenticator;
import com.rli.scripts.customobjects.sharepointco.SharepointNamingEnumaration;
import com.rli.slapd.filter.JDAPFilter;
import com.rli.slapd.server.LDAPException;
import com.rli.util.RLIConstants;
import com.rli.util.djava.ScriptHelper;
import com.rli.util.jndi.vdap.LDAPFilter;
import com.rli.vds.util.InterceptParam;
import com.rli.vds.util.UserDefinedInterception2;

/**
 * Customobject sample for the Sharepoint UserProfile webservice client<br>
 * association between the ORX and the javaclass in VDS)<br>
 * Orx is defined using custom datasource with properties 
 * username ,password and url
 * <br>
 * Schema overview:<br>
 * objectclass: azureuser or group<br>
 * key name: id<br>
 * key value: accountname of the user <br>
 * The schema defined in the ORX can be added (necessary to add a new user or group through VDS) 
 * with the Administration Console in the Configuration tab.<br> 
 * <br>
 * Supported operations are:<br>
 * <li>select</li>
 * <li>update</li>
 * <br>
 * <br>
 * Examples below assume the view is mounted in the o=sharepoint naming context<br>
 * [the Accountname] is the Accountname of user <br> 
 * <br>
 *
 * @author: Praveen Nandi
 * @since 06-Feb-2012
 * @version 1.0
 */
public class sharepoint implements UserDefinedInterception2 {

	private static String NAMESPACE = "http://microsoft.com/webservices/SharePointPortalServer/UserProfileService";

	private static final String ATTR_ID = "Id";
	private static final String ATTR_OBJECTCLASS = "objectclass";

	

	@Override
	public void authenticate(InterceptParam param) {

	}

	@Override
	public void compare(InterceptParam arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(InterceptParam arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void insert(InterceptParam arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void invoke(InterceptParam arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void select(InterceptParam param) {
		try {
			String idToLookup = null;
			System.out.println("In sharepoint custom object Search ...");
			// create the entries vector
			Vector entries = new Vector();
			String userid = param.getConnectionstringObject().getProperty(
					"username");
			String pwd = param.getConnectionstringObject().getProperty(
					"password");
			String url = param.getConnectionstringObject().getProperty("url");
			
			// get a suitable size limit
			int sizelimit = 0;

			if (param.getScope().equals("base")) {

				sizelimit = 1;
				String dnStr = param.getVirtualBaseDn();
				idToLookup = getId(dnStr);

			} else {

				sizelimit = param.getSizelimit();

			}
			
			if (!param.getFilter().equalsIgnoreCase(LDAPFilter.DEFAULT_FILTER)) {

				String filter = param.getFilter();

				System.out.println("In Filter case" + filter);

				JDAPFilter jdapFilter = JDAPFilter.getFilter(filter);

				if (jdapFilter.hasAttribute("Id")) {
					List arl = jdapFilter.retrieveValues("Id");
					System.out.println(arl.get(0).toString());
					idToLookup = arl.get(0).toString();
				} else if (jdapFilter.hasAttribute("UserName")) {
					List arl = jdapFilter.retrieveValues("UserName");
					System.out.println(arl.get(0).toString());
					idToLookup = arl.get(0).toString();
				} else if (jdapFilter.hasAttribute("AccountName")) {
					List arl = jdapFilter.retrieveValues("AccountName");
					System.out.println(arl.get(0).toString());
					idToLookup = arl.get(0).toString();
				}

			}
			if (url.contains("https")) {
				java.lang.System.setProperty(
						"sun.security.ssl.allowUnsafeRenegotiation", "true");
				System.out.println("In Https certificate trust");
				SharePointUtils.doTrustToCertificates();
			}
			java.net.Authenticator.setDefault(new SharepointAuthenticator(
					userid, pwd));

			System.out.println("Authentication Successful");
			if (idToLookup != null) {
				if(idToLookup.contains("\\\\")){
					idToLookup=idToLookup.replace("\\\\", "\\");
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
			Name bodyName = sf
					.createName("GetUserProfileByName", "", NAMESPACE);
			SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
			Name name = sf.createName("AccountName");
			SOAPElement accname = bodyElement.addChildElement(name);
			String dnStr = param.getVirtualBaseDn();
			

			accname.addTextNode(idToLookup);

			// Set the destination
			URL endpoint = new URL(url);

			MimeHeaders hd = message.getMimeHeaders();
			hd.addHeader("SOAPAction", NAMESPACE + "/GetUserProfileByName");
			hd.addHeader("Content-Type", "text/xml; charset=UTF-8");

			System.out.println("Calling Sharepoint Service...");
			SOAPMessage response = connection.call(message, endpoint);

			// prepare attributes...
			Attributes attributes = new BasicAttributes();

			// ID
			Attribute attribute = new BasicAttribute(ATTR_ID, idToLookup);
			attributes.put(attribute);
			// objectclass
			attribute = new BasicAttribute(ATTR_OBJECTCLASS, "top");
			attribute.add(param.getObjectclass());
			attributes.put(attribute);

			if (response.getSOAPBody().hasFault()) {
				SOAPFault newFault = response.getSOAPBody().getFault();
				QName code = newFault.getFaultCodeAsQName();

				String string = newFault.getFaultString();
				String actor = newFault.getFaultActor();
				System.out.println("SOAP fault contains: ");
				System.out.println("  Fault code = " + code.toString());
				System.out.println("  Local name = " + code.getLocalPart());
				System.out.println("  Namespace prefix = " + code.getPrefix()
						+ ", bound to " + code.getNamespaceURI());
				System.out.println("  Fault string = " + string);
				if (param.getScope().equals("base")) {
					param.setErrorcode(LDAPException.OPERATION_ERROR);
					param.setStatusFailed();
					param.setErrormessage("Select Operation :SOAP Fault Contains :"
							+ string);
				}
				if (actor != null) {
					System.out.println("  Fault actor = " + actor);
				}
			} else {
				/*
				 * Reading the output from soap response Logic to fetch the data
				 * from soap response to propertydata arraylist
				 */
				SOAPBody sbody = response.getSOAPBody();

				NodeList n = sbody.getElementsByTagName("PropertyData");
				if (n != null) {

					System.out
							.println(" Reading Sharepoint service returned attributes");
					/*
					 * getting the list of attributes from the schema using
					 * param
					 */

					String attribname = null;
					String attribvalue = null;
					for (int i = 0; i < n.getLength(); i++) {

						NodeList childn = n.item(i).getChildNodes();

						if (childn != null) {
							for (int j = 0; j < childn.getLength(); j++) {
								Node nch = childn.item(j);

								if (nch.getNodeName().equalsIgnoreCase("Name")) {
									attribname = nch.getTextContent();
								} else if (nch.getNodeName().equalsIgnoreCase(
										"Values")) {
									if (nch.getChildNodes() != null) {
										NodeList cnodelist = nch
												.getChildNodes();
										for (int k = 0; k < cnodelist
												.getLength(); k++) {
											Node valuen = cnodelist.item(k);
											if (valuen.getNodeName()
													.equalsIgnoreCase(
															"ValueData")) {

												Node v = valuen.getFirstChild();
												attribvalue = v
														.getTextContent();
											}
											if (k == 0)
												attribute = new BasicAttribute(
														attribname, attribvalue);
											else
												attribute.add(attribvalue);
										}

									} else {
										attribvalue = nch.getTextContent();
										attribute = new BasicAttribute(
												attribname, attribvalue);
									}
								}

							}
							attributes.put(attribute);
						}
					}

					String dn = param.getTypename() + RLIConstants.EQUAL
							+ idToLookup;
					entries.add(new SearchResult(dn, null, attributes));

					param.setResultSet(entries);
					System.out
							.println("After placing sharepoint attributes into resultset");
				} else {
					System.out.println("No data returned for Account Name :"
							+ idToLookup);
					if (param.getScope().equals("base")) {
						param.setErrorcode(LDAPException.OPERATION_ERROR);
						param.setStatusFailed();
						param.setErrormessage("No data returned for Account Name :"
								+ idToLookup);

					}

				}
			}
			}else{
				param.setResultSet_Object(new SharepointNamingEnumaration(sizelimit, param));
			}
		} catch (Exception e) {
			System.out.println("Exception in select :" + e.getMessage());
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
			e.printStackTrace();
		} finally {
		}

	}

	@Override
	public void update(InterceptParam param) {

		try {

			System.out.println("In Sharepoint custom object Update");
			String userid = param.getConnectionstringObject().getProperty(
					"username");
			String pwd = param.getConnectionstringObject().getProperty(
					"password");
			String url = param.getConnectionstringObject().getProperty("url");

			if (url.contains("https"))
				SharePointUtils.doTrustToCertificates();

			java.net.Authenticator.setDefault(new SharepointAuthenticator(
					userid, pwd));

			String dnStr = param.getVirtualBaseDn();

			String username = getId(dnStr);
			if(username.contains("\\\\")){
				username=username.replace("\\\\", "\\");
			}
			ArrayList<PropertyData> propList = new ArrayList<PropertyData>();
			propList = getUserProfileAttributes(param, username);

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
			Name bodyName = sf.createName("ModifyUserPropertyByAccountName",
					"", NAMESPACE);
			envelope.setAttribute("xmlns:xsi",
					"http://www.w3.org/2001/XMLSchema-instance");
			envelope.setAttribute("xmlns:xsd",
					"http://www.w3.org/2001/XMLSchema");
			SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
			Name name = sf.createName("accountName");
			SOAPElement accname = bodyElement.addChildElement(name);
			accname.addTextNode(username);
			Name newDataName = sf.createName("newData");
			SOAPElement newData = bodyElement.addChildElement(newDataName);
			System.out.println(propList.size());
			for (int prop = 0; prop < propList.size(); prop++) {
				PropertyData propertyData = propList.get(prop);
				Name propdataName = sf.createName("PropertyData");
				SOAPElement propData = newData.addChildElement(propdataName);

				Name ispcName = sf.createName("IsPrivacyChanged");
				SOAPElement ispc = propData.addChildElement(ispcName);
				ispc.addTextNode(String.valueOf(propertyData
						.isIsPrivacyChanged()));

				Name isvcName = sf.createName("IsValueChanged");
				SOAPElement isvc = propData.addChildElement(isvcName);
				isvc.addTextNode(String.valueOf(propertyData.isIsValueChanged()));

				Name nameName = sf.createName("Name");
				SOAPElement nameV = propData.addChildElement(nameName);
				nameV.addTextNode(propertyData.getName());

				Name privacyName = sf.createName("Privacy");
				SOAPElement privacy = propData.addChildElement(privacyName);
				privacy.addTextNode(propertyData.getPrivacy());

				Name valuesName = sf.createName("Values");
				SOAPElement values = propData.addChildElement(valuesName);
				for (String svalue : propertyData.getValues()) {
					Name valuedataName = sf.createName("ValueData");
					SOAPElement valueData = values
							.addChildElement(valuedataName);
					Name valueName = sf.createName("Value");
					SOAPElement value = valueData.addChildElement(valueName);
					value.setAttribute("xsi:type", "xsd:string");
					value.addTextNode((String) svalue);
				}

			}
			MimeHeaders hd = message.getMimeHeaders();
			hd.addHeader(
					"SOAPAction",
					"http://microsoft.com/webservices/SharePointPortalServer/UserProfileService/ModifyUserPropertyByAccountName");
			hd.addHeader("Content-Type", "text/xml; charset=utf-8");

			message.writeTo(System.out);
			// Set the destination
			URL endpoint = new URL(url);
			System.out.println("Calling  Sharepoint Service for update... ");

			SOAPMessage response = connection.call(message, endpoint);
			if (response.getSOAPBody().hasFault()) {
				SOAPFault newFault = response.getSOAPBody().getFault();
				QName code = newFault.getFaultCodeAsQName();

				String string = newFault.getFaultString();
				String actor = newFault.getFaultActor();
				System.out.println("SOAP fault contains: ");
				System.out.println("  Fault code = " + code.toString());
				System.out.println("  Local name = " + code.getLocalPart());
				System.out.println("  Namespace prefix = " + code.getPrefix()
						+ ", bound to " + code.getNamespaceURI());
				System.out.println("  Fault string = " + string);
				param.setErrorcode(LDAPException.OPERATION_ERROR);
				param.setStatusFailed();
				param.setErrormessage("Select Operation :SOAP Fault Contains :"
						+ string);
				if (actor != null) {
					System.out.println("  Fault actor = " + actor);
				}
			} else {
				System.out.println("Update Successful for Account Name : "
						+ username);
			}
		} catch (Exception e) {
			System.out.println("Exception in update :" + e.getMessage());
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
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

	public ArrayList<PropertyData> getUserProfileAttributes(
			InterceptParam param, String username) {

		try {
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
			Name bodyName = sf
					.createName("GetUserProfileByName", "", NAMESPACE);
			SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
			Name name = sf.createName("AccountName");
			SOAPElement accname = bodyElement.addChildElement(name);

			accname.addTextNode(username);

			String url = param.getConnectionstringObject().getProperty("url");
			// Set the destination
			URL endpoint = new URL(url);

			if (url.contains("https"))
				SharePointUtils.doTrustToCertificates();
			MimeHeaders hd = message.getMimeHeaders();
			hd.addHeader("SOAPAction", NAMESPACE + "/GetUserProfileByName");
			hd.addHeader("Content-Type", "text/xml; charset=UTF-8");

			SOAPMessage response = connection.call(message, endpoint);

			/*
			 * Reading the output from soap response Logic to fetch the data
			 * from soap response to propertydata arraylist
			 */
			SOAPBody sbody = response.getSOAPBody();
			NodeList n = sbody.getElementsByTagName("PropertyData");
			if (n != null) {
				ArrayList<PropertyData> propertyDataArray = new ArrayList<PropertyData>();

				List<ModificationItem> modificationAttrs = param
						.getModifications();
				System.out
						.println("Reading the modifications and preparing soap request");
				/*
				 * Logic to add changed values into the propertydata bean for
				 * update
				 */
				if (modificationAttrs != null) {
					for (ModificationItem modification : modificationAttrs) {

						Attribute attr = modification.getAttribute();

						String attribute = attr.getID();
						String[] attrvalue = new String[attr.size()];
						for (int i = 0; i < attr.size(); i++)
							attrvalue[i] = (String) attr.get(i);

						for (int i = 0; i < n.getLength(); i++) {

							NodeList childn = n.item(i).getChildNodes();
							PropertyData pd = new PropertyData();
							if (childn != null) {
								if (attribute.equals(childn.item(2)
										.getTextContent())) {
									for (int j = 0; j < childn.getLength(); j++) {
										Node nch = childn.item(j);

										if (nch.getNodeName() == "Name") {
											pd.setName(nch.getTextContent());
											System.out.println(nch
													.getTextContent());
										} else if (nch.getNodeName() == "IsPrivacyChanged") {
											pd.setIsPrivacyChanged(Boolean
													.parseBoolean(nch
															.getTextContent()));
										} else if (nch.getNodeName() == "IsValueChanged") {
											pd.setIsValueChanged(true);
										} else if (nch.getNodeName() == "Privacy") {

											pd.setPrivacy(nch.getTextContent());
										} else if (nch.getNodeName() == "Values") {

											pd.setValues(attrvalue);// adding
																	// the
																	// changed
																	// value for
																	// specific
																	// variable

										}

									}
									propertyDataArray.add(pd);
								}
							}
						}

					}
				}
				return propertyDataArray;
			}

		} catch (Exception e) {
			System.out.println("Exception : " + e.getMessage());
			param.setErrorcode(LDAPException.OPERATION_ERROR);
			param.setStatusFailed();
			param.setErrormessage(e.getMessage());
		}
		return null;
	}

	
}
