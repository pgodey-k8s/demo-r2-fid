package com.rli.scripts.customobjects.concurco.services;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.rli.scripts.customobjects.azure.exception.CustomAzureException;
import com.rli.scripts.customobjects.azure.services.CustomObjectParameter;
import com.rli.scripts.customobjects.concurco.data.UserProfile;
import com.rli.scripts.customobjects.concurco.utils.ConcurParameters;
import com.rli.vds.util.InterceptParam;

public class ConcurUserServices {
	
	private static UserProfile userprofile=null;
	private static final String METHOD_SET = "set";
	public static UserProfile getUserProfile(String id) throws Exception{
		UserProfile userProfile = null;
		
		String newurl=ConcurParameters.getUrl();
		String response= ConcurRequestHandler.handleRequest(newurl);
		JAXBContext jaxbContext = JAXBContext.newInstance("com.rli.concur.services");
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		StringReader sr = new StringReader(response);
		Document XMLDoc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(response)));
		Node node=XMLDoc.getFirstChild();
		userProfile=(UserProfile)unmarshaller.unmarshal(node);
				
		return userProfile;
	}
	
	
	public static boolean updateUserProfile(InterceptParam param,UserProfile user){
		
		try{
			
			userprofile=user;
			
			String response=ConcurRequestHandler.handlRequestPost(ConcurParameters.getUrl(), createXMLData(param, "updateuser"));
			

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (response);
            NodeList nl=doc.getChildNodes();
            NodeList l1=nl.item(0).getChildNodes();
            for(int i=0;i<l1.getLength();i++){
            	Node child=l1.item(i);
            	if(child.getNodeName().equalsIgnoreCase("Errors")){
            		NodeList nn=child.getChildNodes();
            		for(int j=0;j<nn.getLength();j++){
            			Node gchild=nn.item(j);
            			if(gchild.getNodeName().equalsIgnoreCase("message"))
            				System.out.println("Error in Create : "+gchild.getTextContent());
            			return false;
            		}
            			
            	}
            	
            }
            
			return true;
		}catch(Exception e){
			return false;
			
		}
		
		
	}
	
	
	public static boolean createUserProfile(InterceptParam param){
		try{
			String response=ConcurRequestHandler.handlRequestPost(ConcurParameters.getUrl(), createXMLData(param, "createuser"));
			

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (response);
            NodeList nl=doc.getChildNodes();
            NodeList l1=nl.item(0).getChildNodes();
            for(int i=0;i<l1.getLength();i++){
            	Node child=l1.item(i);
            	if(child.getNodeName().equalsIgnoreCase("Errors")){
            		NodeList nn=child.getChildNodes();
            		for(int j=0;j<nn.getLength();j++){
            			Node gchild=nn.item(j);
            			if(gchild.getNodeName().equalsIgnoreCase("message"))
            				System.out.println("Error in Create : "+gchild.getTextContent());
            			return false;
            		}
            			
            	}
            	
            }
            
			return true;
		}catch(Exception e){
			return false;
			
		}
		
		
	}
	public static boolean deactivateUserProfile(UserProfile up){
		userprofile=up;
		userprofile.setActive("false");
		
		return false;
		
	}
	public static void main(String args[]){
		
		
		ConcurOAuthTokenHandler cth= new ConcurOAuthTokenHandler("praveennandi@gmail.com", "pra101980", "https://www.concursolutions.com/net2/oauth2/accesstoken.ashx", "bfPF4Y1nBKsSNNk6RnexOd");
		try {
			String token=cth.getOAuthToken();
			ConcurParameters.setUrl("https://www.concursolutions.com/api/user/v1.0/User");
			ConcurUserServices cus=new ConcurUserServices();
			UserProfile up=cus.getUserProfile("praveennandi@gmail.com");
			System.out.println(up.getFirstName());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
			Element rootElement = xmlDoc.createElement("batch");
			rootElement.setAttribute("xmlns", "http://www.concursolutions.com/api/user/2011/02");
			xmlDoc.appendChild(rootElement);
			
			
			
			
			
			if(type.equalsIgnoreCase("createuser")){
				Element properties = xmlDoc.createElement("UserProfile");
				rootElement.appendChild(properties);
				Attributes attrs = request.getAttributes();
			   
			   
			      NamingEnumeration attributesEnu = attrs.getAll();
			      String idValue=null;
			      while (attributesEnu.hasMore()) {
						Attribute attr = (Attribute) attributesEnu.next();
						
						
							
						Element element = xmlDoc.createElement( attr.getID());
						  Object[] values1 = new Object[attr.size()];
							for (int i = 0; i < attr.size(); i++) {
								element.appendChild(xmlDoc.createTextNode( (String) attr.get(i)));
								
							}
							properties.appendChild(element);
						 
			      }
				
			}else if(type.equalsIgnoreCase("updateuser")){
				
	            List<ModificationItem> modificationAttrs = request.getModifications();
	            Class c = userprofile.getClass();
	            if (modificationAttrs != null) {
	            
					for (ModificationItem modification : modificationAttrs) {
						Attribute attr = (Attribute) modification.getAttribute();
						
						Method m = c.getMethod(METHOD_SET + attr.getID(), new Class[]{String.class});
						
						  Object[] values1 = new Object[attr.size()];
							for (int i = 0; i < attr.size(); i++) {
								m.invoke(userprofile,(String) attr.get(i));
								
							}
							
						
					
					}
					
					
	            }
	       JAXBContext context = JAXBContext.newInstance(UserProfile.class);
	          Marshaller marshaller = context.createMarshaller();
	            
	            // Create a stringWriter to hold the XML
	           StringWriter stringWriter = new StringWriter();
	           marshaller.marshal(userprofile, stringWriter);
	           
	           Node element = docBuilder.parse (new InputSource(new StringReader(stringWriter.toString()))).getDocumentElement();
	            Document doc = rootElement.getOwnerDocument();
	            element=doc.importNode(element, true);
	      rootElement.appendChild(element);
	            
			}else if(type.equalsIgnoreCase("deactivateuser")){
				JAXBContext context = JAXBContext.newInstance(UserProfile.class);
		          Marshaller marshaller = context.createMarshaller();
		            
		            // Create a stringWriter to hold the XML
		           StringWriter stringWriter = new StringWriter();
		           marshaller.marshal(userprofile, stringWriter);
		           
		            Node element = docBuilder.parse (new InputSource(new StringReader(stringWriter.toString()))).getDocumentElement();
		            Document doc = rootElement.getOwnerDocument();
		            element=doc.importNode(element, true);
		      rootElement.appendChild(element);
		     
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
