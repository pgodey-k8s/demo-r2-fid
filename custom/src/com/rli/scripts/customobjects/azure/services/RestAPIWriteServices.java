package com.rli.scripts.customobjects.azure.services;

import java.io.StringWriter;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rli.scripts.customobjects.azure.exception.CustomAzureException;
import com.rli.vds.util.InterceptParam;



/**
 * This class facilitates all the write functionalities to the REST Endpoint
 * such as creating, updating, deleting user objects, adding an user to a group/role,
 * deleting an user from a group/role.
 *
 */
public class RestAPIWriteServices {
		

	
	/**
	 * This method creates a new user.
	 * @param request The httpservletrequest object that contains the description
	 * of the new object.
	 * @return 
	 * @throws CustomAzureException if the operation can not be successfully created.
	 */
	public static String createUser(InterceptParam request, String sessionKey) throws CustomAzureException{		
		
		/**
		 * Send the http Post request to the appropriate url and
		 * using an appropriate message body.
		 */
		return HttpRequestHandler.handlRequestPost(
				"/Users", 
				null, 
				createXMLData(request,"createuser"), 
				"createUser",
				sessionKey);
	}

	/**
	 * This method creates a new group.
	 * @param request The httpservletrequest object that contains the description
	 * of the new object.
	 * @return 
	 * @throws CustomAzureException if the operation can not be successfully created.
	 */
	public static String createGroup(InterceptParam request, String sessionKey) throws CustomAzureException{		
		
		/**
		 * Send the http Post request to the appropriate url and
		 * using an appropriate message body.
		 */
		return HttpRequestHandler.handlRequestPost(
				"/Groups", 
				null, 
				createXMLDataGroup(request,"creategroup"), 
				"createGroup",
				sessionKey);
	}

    /**
     * This method would create a string consisting of a xml document with all the necessary elements
     * set from the HttpServletRequest request.	
     * @param request The HttpServletRequest
     * @return the string containing the xml document.
     * @throws CustomAzureException If there is any error processing the request.
     */
	private static String createXMLData(InterceptParam request,String type) throws CustomAzureException {
		try{
			/**
			 * Setup the necessary operations to build the xml document.
			 */
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();;
			Document xmlDoc = docBuilder.newDocument();
			
			/**
			 * Create a root element entry and set the appropriate namespace declarations.
			 */
			Element rootElement = xmlDoc.createElement("entry");
			rootElement.setAttribute("xmlns", CustomObjectParameter.xmlNameSpaceforEntry);
			rootElement.setAttribute("xmlns:d", CustomObjectParameter.xmlNameSpaceforD);
			rootElement.setAttribute("xmlns:m", CustomObjectParameter.xmlNameSpaceforM);
			xmlDoc.appendChild(rootElement);
			
			/**
			 * Create the content node and set the appropriate type attribute.
			 */
			Element content = xmlDoc.createElement("content");
			content.setAttribute("type", CustomObjectParameter.contentTypeXML);
			rootElement.appendChild(content);
			
			
			Element properties = xmlDoc.createElement("m:properties");
			content.appendChild(properties);
			if(type.equalsIgnoreCase("createuser")){
				Attributes attrs = request.getAttributes();
			   
			   
			      NamingEnumeration attributesEnu = attrs.getAll();
			      String idValue=null;
			      while (attributesEnu.hasMore()) {
						Attribute attr = (Attribute) attributesEnu.next();
						
						 if(!attr.getID().equalsIgnoreCase("ObjectId")){
							 if( attr.getID().equalsIgnoreCase("UserPrincipalName"))
							 {
								 Element element = xmlDoc.createElement(String.format("d:%s", "MailNickname"));//adding mandatory MailNickname based on UPN given by user
								 String upn=(String) attr.get(0);
								 String mailNickName=null;
								if(upn.contains("@")){
								 mailNickName=upn.substring(0,upn.indexOf("@"));
								}else{
									mailNickName=upn;
								}
								element.appendChild(xmlDoc.createTextNode(mailNickName));
								properties.appendChild(element); 
							 }
						Element element = xmlDoc.createElement(String.format("d:%s", attr.getID()));
						  Object[] values1 = new Object[attr.size()];
							for (int i = 0; i < attr.size(); i++) {
								element.appendChild(xmlDoc.createTextNode( (String) attr.get(i)));
								
							}
							properties.appendChild(element);
						 }
			      }
				
			}else if(type.equalsIgnoreCase("updateuser")){
				
	            List<ModificationItem> modificationAttrs = request.getModifications();
	          
	            if (modificationAttrs != null) {
	            
					for (ModificationItem modification : modificationAttrs) {
						Attribute attr = (Attribute) modification.getAttribute();
						
						
						Element element = xmlDoc.createElement(String.format("d:%s",  attr.getID()));
						  Object[] values1 = new Object[attr.size()];
							for (int i = 0; i < attr.size(); i++) {
								element.appendChild(xmlDoc.createTextNode( (String) attr.get(i)));
								
							}
							properties.appendChild(element);
						
					
					}
	            }
	       
			}
			
			
			/**
			 * Convert the xml document in a string and return.
			 */
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StringWriter writer = new StringWriter();
			DOMSource source = new DOMSource(xmlDoc);
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
			writer.flush();
			System.out.println(writer.toString());
			return writer.toString();
			
			
		}catch(Exception e){
			throw new CustomAzureException(CustomObjectParameter.ErrorCreatingXML,e.getMessage(), e);
			
		}
	}

	
	/**
	 * This method deletes an user identified by its ObjectId.
	 * @param objectId The ObjectId of the user to be deleted.
	 * @throws CustomAzureException If the operation can not be done successfully.
	 */
	public static String deleteUser(String objectId, String sessionKey) throws CustomAzureException{
		return HttpRequestHandler.handleRequestDelete( 
				String.format("/Users('User_%s')", objectId), 
				null,
				sessionKey);
		
	}
	
	
	/**
	 * This method deletes an user identified by its ObjectId.
	 * @param objectId The ObjectId of the user to be deleted.
	 * @throws CustomAzureException If the operation can not be done successfully.
	 */
	public static String deleteGroup(String objectId, String sessionKey) throws CustomAzureException{
		return HttpRequestHandler.handleRequestDelete( 
				String.format("/Groups('Group_%s')", objectId), 
				null,
				sessionKey);
		
	}

	/**
	 * This method updates an user.
	 * @param request The HttpServletRequest request
	 * @throws CustomAzureException if there is an error while doing this operation.
	 */
	
	public static String updateUser(InterceptParam request,String objectID, String sessionKey) throws CustomAzureException {
		
		/**
		 * Send a patch request to the appropriate url with the request body
		 * as data.
		 */
		return HttpRequestHandler.handlRequestPost(
				String.format("/Users('%s')",objectID), 
				null, 
				createXMLData(request,"updateuser"), 
				"updateUser",
				sessionKey);			
	}

	
	/**
	 * This method updates an Group.
	 * @param request The HttpServletRequest request
	 * @throws CustomAzureException if there is an error while doing this operation.
	 */
	
	public static String updateGroup(InterceptParam request,String objectID, String sessionKey) throws CustomAzureException {
		
		/**
		 * Send a patch request to the appropriate url with the request body
		 * as data.
		 */
		return HttpRequestHandler.handlRequestPost(
				String.format("/Groups('%s')",objectID), 
				null, 
				createXMLDataGroup(request,"updategroup"), 
				"updateGroup",
				sessionKey);			
	}

	/**
	 * This method adds or removes a role or group to a particular user 
	 * depending on the parameters.
	 * @param userId The Id of the user who should be added to the group/role.
	 * @param objectName Is it Group or Role?
	 * @param opName Whether delete or add
	 * @param objectId The object Id of the Group or the Role.
	 * @throws CustomAzureException 
	 */
	public static String updateLink(String userId, String objectName,
			String opName, String groupId, String sessionKey) throws CustomAzureException {
		
		String newKey = null;
			/**
			 * If the operation is add.
			 */
		if(opName.equalsIgnoreCase("add")){		
			newKey =  addUserToGroup(userId, groupId, objectName, sessionKey);
		}
		
		/**
		 * If the operation is delete.
		 */
		if(opName.equalsIgnoreCase("delete")){
			String path = String.format("/%ss('%s_%s')/$links/Members('User_%s')", 
					objectName, 
					objectName,
					groupId,
					userId);
			
			newKey = HttpRequestHandler.handleRequestDelete(path, null, sessionKey);
		}
		
		return newKey;		
	}


	/**
	 * This method adds an user to a group/role.
	 * @param userId The ObjectId of the user to be added.
	 * @param groupId The ObjectId of the group/role object where to be added.
	 * @param objectName Whether user to be added in a group or a role.
	 * @throws CustomAzureException If the operation can not be successfully carried out.
	 */	
	private static String addUserToGroup(
			String userId, 
			String groupId, 
			String objectName,
			String sessionKey) throws CustomAzureException {
		
		String newKey = null;
		try{				
			
			/**
			 * Setup the necessary operations to build the xml document.
			 */
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();;
			Document xmlDoc = docBuilder.newDocument();
		
			/**
			 * Create a root element uri and set the appropriate namespace declarations.
			 */
			Element rootElement = xmlDoc.createElement("uri");
			rootElement.setAttribute("xmlns", CustomObjectParameter.xmlNameSpaceforM);
			xmlDoc.appendChild(rootElement);
			
			/**
			 * Create the content of uri tag.
			 */
			rootElement.appendChild(xmlDoc.createTextNode(String.format("%s://%s/%s/ReferencedObjects('User_%s')", 
					CustomObjectParameter.PROTOCOL_NAME, CustomObjectParameter.getRestServiceHost(), CustomObjectParameter.getTenantContextId(), userId)));
			
			/**
			 * Convert the xml document in a string and return.
			 */
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StringWriter writer = new StringWriter();
			DOMSource source = new DOMSource(xmlDoc);
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
			writer.flush();
			String data = writer.toString();
			
			
			newKey = HttpRequestHandler.handlRequestPost(
					String.format("/%ss('%s_%s')/$links/Members", objectName, objectName, groupId), 
					null, 
					data,
					"addUserToGroup",
					sessionKey);

			
		
		}catch(Exception e){
			throw new CustomAzureException( CustomObjectParameter.ErrorCreatingXML, e.getMessage(), e);
		} 
		
		return newKey;
		
	}
	
	
	/**
     * This method would create a string consisting of a xml document with all the necessary elements
     * set from the HttpServletRequest request.	
     * @param request The HttpServletRequest
     * @return the string containing the xml document.
     * @throws CustomAzureException If there is any error processing the request.
     */
	private static String createXMLDataGroup(InterceptParam request,String type) throws CustomAzureException {
		try{
			/**
			 * Setup the necessary operations to build the xml document.
			 */
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();;
			Document xmlDoc = docBuilder.newDocument();
			
			/**
			 * Create a root element entry and set the appropriate namespace declarations.
			 */
			Element rootElement = xmlDoc.createElement("entry");
			rootElement.setAttribute("xmlns", CustomObjectParameter.xmlNameSpaceforEntry);
			rootElement.setAttribute("xmlns:d", CustomObjectParameter.xmlNameSpaceforD);
			rootElement.setAttribute("xmlns:m", CustomObjectParameter.xmlNameSpaceforM);
			xmlDoc.appendChild(rootElement);
			
			/**
			 * Create the content node and set the appropriate type attribute.
			 */
			Element content = xmlDoc.createElement("content");
			content.setAttribute("type", CustomObjectParameter.contentTypeXML);
			rootElement.appendChild(content);
			
			
			Element properties = xmlDoc.createElement("m:properties");
			content.appendChild(properties);
			if(type.equalsIgnoreCase("creategroup")){
				Attributes attrs = request.getAttributes();
			   
			   
			      NamingEnumeration attributesEnu = attrs.getAll();
			      String idValue=null;
			      while (attributesEnu.hasMore()) {
						Attribute attr = (Attribute) attributesEnu.next();
						
						 if(!attr.getID().equalsIgnoreCase("ObjectId")){
							 if( attr.getID().equalsIgnoreCase("DisplayName"))
							 {
							 Element element = xmlDoc.createElement(String.format("d:%s", "MailNickname"));//adding mandatory MailNickname based on UPN given by user
							
							 String mailNickName=(String) attr.get(0);
							
							element.appendChild(xmlDoc.createTextNode(mailNickName));
							properties.appendChild(element); 
							 }
						Element element = xmlDoc.createElement(String.format("d:%s", attr.getID()));
						  Object[] values1 = new Object[attr.size()];
							for (int i = 0; i < attr.size(); i++) {
								element.appendChild(xmlDoc.createTextNode( (String) attr.get(i)));
								
							}
							properties.appendChild(element);
						 }
			      }
				
			}else if(type.equalsIgnoreCase("updategroup")){
				
	            List<ModificationItem> modificationAttrs = request.getModifications();
	          
	            if (modificationAttrs != null) {
	            
					for (ModificationItem modification : modificationAttrs) {
						Attribute attr = (Attribute) modification.getAttribute();
						
						
						Element element = xmlDoc.createElement(String.format("d:%s",  attr.getID()));
						  Object[] values1 = new Object[attr.size()];
							for (int i = 0; i < attr.size(); i++) {
								element.appendChild(xmlDoc.createTextNode( (String) attr.get(i)));
								
							}
							properties.appendChild(element);
						
					
					}
	            }
	       
			}
			
			
			/**
			 * Convert the xml document in a string and return.
			 */
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StringWriter writer = new StringWriter();
			DOMSource source = new DOMSource(xmlDoc);
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
			writer.flush();
			System.out.println(writer.toString());
			return writer.toString();
			
			
		}catch(Exception e){
			throw new CustomAzureException(CustomObjectParameter.ErrorCreatingXML,e.getMessage(), e);
			
		}
	}


}
