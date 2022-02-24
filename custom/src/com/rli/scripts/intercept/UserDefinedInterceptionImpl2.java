package com.rli.scripts.intercept;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import com.rli.vds.util.UserDefinedInterception2;
import com.rli.vds.util.InterceptParam;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.rli.slapd.server.LDAPException;
import com.rli.synsvcs.common.ConnString2;
import com.rli.util.BasicAttributeIndexed;
import com.rli.util.djava.ScriptHelper;
import static com.rli.util.djava.ScriptHelper.*;

/**
 * 
 *         <pre>
 * This template implements the interception operations (using the new InterceptParam Class for passing parameters)
 * On Bind event (Authenticate)
 * On Search events  ( Select)
 * On Add event (insert)
 * On modify event (update)
 * On delete event (delete) 
 * On compare event (compare)
 * On ProcessResult (processresult) - called before each entry of a search result is returned 
 * 
 * The operations must be implemented in the corresponding functions. The Input/output class InterceptParam extends a 
 * java 'Properties'  object and adds getter /setter for each property. It contains all the Meta information properties like 
 * Connection String, Filter, Attributes, etc.
 * It is used also as an output structure to hold the result status of the interception (and may contain modified input parameters)
 * 
 * Note on Select operation interception:
 * 	If the result of the select is set inside the interception , it is returned in a list property of SearchResult entries
 * </pre>
 **/

public class UserDefinedInterceptionImpl2 implements UserDefinedInterception2 {
	private final static Logger log4jLogger = LogManager.getLogger(UserDefinedInterceptionImpl2.class);

	/**
	 * The function to implement search operation.
	 * @param prop
	 *            - InterceptParam - Input /Output parameters
	 **/
	public void select(InterceptParam prop) {

		/**
		 * <pre>
		 * Default values received in select function:
		 * 
		 * Constants:
		 * - Status=proceed
		 * - Errorcode=0
		 * - Calljoin=YES
		 * 
		 * Meta variables:
		 * - ConnectionstringObject: connection information for backend
		 * - Associationurls: xjoin definition
		 * 
		 * Request variables:
		 * - OperationID: operation id for current VDS request.
		 * - Dn: virtual DN requested
		 * - ClientIP: IP adress of remote VDS client
		 * - Userid: username of remote VDS client
		 * - Password: password of remote VDS client
		 * - Scope: LDAP: scope to be sent to backend, RDBMS/CUSTOM: scope for current virtual query
		 * - BaseDn: base DN to search on backend (ldap backend only)
		 * - Filter: LDAP: filter to be sent to LDAP backend; RDBMS/CUSTOM: filter received for current query
		 * - Sizelimit: size limit to be sent to backend
		 * - Attrs: requested attributes for current query (separated by ',')
		 * - Command: command to be executed on backend (SQL for SQL backends, LDAP query for LDAP/Custom backends)
		 * 
		 * <pre>
		 */
		
		/*
		 * 'select' operation associated with 'On Search' event
		 */
		try {
			String dn = prop.getDn();
			log(">>> " + prop.getAction());
			log(">>> dn: " + dn);
			
			// proceed means that current request will be sent to backend (with
			// parameters eventually modified by interception)
			prop.setStatusProceed();
			
			
			//START - CODE TEMPLATE: reading/updating backend query
			/*
			//for LDAP change base dn, filter and scope
			log("command received: "+prop.getCommand());
			prop.setBaseDn("o=mynewbasedn");
			prop.setFilter("objectclass=mynewfilter");
			prop.setScopeInt(SearchControls.SUBTREE_SCOPE);
			prop.setSizelimit(10);
			//for SQL backend, update SQL
			prop.setCommand("SELECT ...");
			prop.setSizelimit(10);
			*/
			//END - CODE TEMPLATE: reading/updating backend query
			
			
			//START - CODE TEMPLATE: reading/updating backend connection information
			/*
			prop.setHostName("newhost.domain.com");
			prop.setUserid("newuser");
			prop.setPassword("newpassword");
			//for advanced connection information use ConnString2 object
			//ConnString2 connStr = prop.getConnectionstringObject();
			//log("host to connect to: " + prop.getHostName());
			//connStr.setDb_driverClassName("jdbc.mydriver...");
			//save modified ConnString2 object
			//prop.setConnectionstring(connStr.toString());
			 */
			//END - CODE TEMPLATE: reading/updating backend connection information
			
			
			//START - CODE TEMPLATE: producing results 
			/*
			//set a list of result
			int scope=prop.getScopeInt();
			if (scope == SearchControls.ONELEVEL_SCOPE
					|| scope == SearchControls.SUBTREE_SCOPE) {
					
				//please select one of the following method: list of enumeration results
					
				//1 - using a list
				List<SearchResult> listResults=new ArrayList<SearchResult>();
				//... produce some results
				prop.setResultSet(listResults);
				
				//2 - or using an enumeration
				//using enumeration could be more efficient since it allows result to be produced on the fly
				NamingEnumeration<SearchResult> enuResults = new NamingEnumeration<SearchResult>() {
					public void close() throws NamingException {
						// to implement
					}
					public boolean hasMore() throws NamingException {
						// to implement
						return false;
					}
					public SearchResult next() throws NamingException {
						// to implement
						//... produce some result
						return null;
					}
					public boolean hasMoreElements() {
						try {
							return hasMore();
						} catch (NamingException e) {
							return false;
						}
					}
					public SearchResult nextElement() {
						try {
							return next();
						} catch (NamingException e) {
							return null;
						}
					}
				};
				prop.setResultSet_Object(enuResults);
				//when setting a result inside select interception, put status ok to not go to backend
				prop.setStatusOk();
			}else{
				//base search: set a list with one entry
				//prop.setResultSet(listResults);
			}
			*/
			//END - Code Template: producing results
			
			
			//START - CODE TEMPLATE: xjoin operation
			/*
			//read/set new xjoin
			String xjoins=prop.getAssociationurls();
			prop.setAssociationurls("ldap://[VDS]/...");
			//cancel xjoin execution for current request
			prop.cancelExternalJoin();
			*/
			//END - CODE TEMPLATE: xjoin operation
			
			// display content of interception in current system.out
			prop.list(System.out);
			
		} catch (Exception iex) {
			log("!!! We got an Exception: " + iex);
			//failed status stops current request and returns an error
			prop.setStatusFailed();
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
		}
	}
	

	/**
	 * post processing of the result of a search The function is called for each
	 * entry of the result
	 * 
	 * @param prop
	 *            - InterceptParam - Input /Output parameters
	 * @param anEntry
	 *            - javax.naming.directory.SearchResult - entry
	 * @return - javax.naming.directory.SearchResult modified entry or null
	 */
	public static SearchResult processresult(InterceptParam prop,
			SearchResult anEntry) {

		/**
		 * <pre>
		 * Default values received in processresult function:
		 * 
		 * Constants:
		 * - Status=proceed
		 * - Errorcode=0
		 * - Calljoin=YES
		 * 
		 * Meta variables:
		 * - ConnectionstringObject: connection information for backend
		 * - Associationurls: xjoin definition
		 * 
		 * Request variables:
		 * - Dn: virtual DN requested
		 * - OperationID: operation id for current VDS request.
		 * - ClientIP: IP adress of remote VDS client
		 * - Userid: username of remote VDS client
		 * - Password: password of remote VDS client
		 * - Filter: filter received for current query
		 * - Sizelimit: size limit to be sent to backend
		 * - Attrs: requested attributes for current query (separated by ',')
		 * 
		 * <pre>
		 */
		
		try {
			// START - CODE TEMPLATE: discarding entry
			//returning null discard current entry
			//if (condition) return null;
			// END - CODE TEMPLATE: discarding entry
			
			
			// START - CODE TEMPLATE: updating entry
			/*
			Attribute attr=findAttribute(anEntry, "description", true);
			if(attr==null){
				attr=new BasicAttributeIndexed("description");
			}
			//add value to current attribute (clear it before for replacing value)
			//attr.clear();
			attr.add("new value");
			*/
			// END - CODE TEMPLATE: updating entry
			
			
			// START - CODE TEMPLATE: xjoin operation
			/*
			// read/set new xjoin
			String xjoins = prop.getAssociationurls();
			prop.setAssociationurls("ldap://[VDS]/...");
			// cancel xjoin execution for current request
			prop.cancelExternalJoin();
			// executing xjoin (entry manipulated by xjoin process result has
			// not
			// been joined yet, calling this method will do the join)
			anEntry = InterceptParam.doXjoinAndGlobalComputed(prop, anEntry);
			*/
			// END - CODE TEMPLATE: xjoin operation
			
			
			// START - CODE TEMPLATE: stopping search enumeration	
			/*
			//setting result code to "OK" will stop current enumeration 
			//(current entry will be the latest one returned for enumeration currently processing)
			prop.setStatusOk();
			*/
			// END - CODE TEMPLATE: stopping search enumeration		

		} catch (Exception iex) {
			log("!!! We got an Exception: " + iex);
			// failed status stops current request and returns an error
			prop.setStatusFailed();
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
		}
		
		return anEntry;
	}



	/**
	 * 'authenticate' operation associated with 'On bind' event status,
	 * errorcode and other parameters are populated accordingly The following
	 * code returns status "proceed" with errorcode "0" : The LDAP Bind
	 * operation will be executed after returning from the script
	 * 
	 * @param prop
	 *            - InterceptParam - Input /Output parameters
	 **/
	public void authenticate(InterceptParam prop) {

		/**
		 * <pre>
		 * Default values received in authenticate function:
		 * 
		 * Constants:
		 * - Status=proceed
		 * - Errorcode=0
		 * 
		 * Meta variables:
		 * - ConnectionstringObject: connection information for backend
		 * 
		 * Request variables:
		 * - ClientIP: IP adress of remote VDS client
		 * - Userid: username for bind
		 * - Password: password for bind
		 * 
		 * <pre>
		 */
		
		try {

			//START - CODE TEMPLATE: reading/updating bind information
			/*
			String user=prop.getUserid();
			String password=prop.getPassword();
			//... more code here
			//to bypass backend authentication, change status
			//fail authentication
			prop.setStatusFailed();
			//succeed authentication
			prop.setStatusOk();
			//default status is proceed and bind is going to be done on backend
			//update user/password	
			prop.setUserid("newuser");
			prop.setPassword("newpwd");
			*/
			//END - CODE TEMPLATE: reading/updating bind information
			
		} catch (Exception iex) {
			log("!!! We got an Exception: " + iex);
			// failed status stops current request and returns an error
			prop.setStatusFailed();
			prop.setErrorcode(LDAPException.INVALID_CREDENTIALS);
		}

	}
	
	/**
	 * 'insert' operation associated with 'On Add' event Status, errorcode and
	 * other parameters are populated accordingly The following code returns
	 * status "proceed" with errorcode "0" : The LDAP Add operation will be
	 * executed after returning from the script
	 * 
	 * @param prop
	 *            - InterceptParam - Input /Output parameters
	 **/
	public void insert(InterceptParam prop) {

		/**
		 * <pre>
		 * Default values received in insert function:
		 * 
		 * Constants:
		 * - Status=proceed
		 * - Errorcode=0
		 * 
		 * Meta variables:
		 * - ConnectionstringObject: connection information for backend
		 * 
		 * Request variables:
		 * - Dn: virtual DN requested
		 * - ClientIP: IP adress of remote VDS client
		 * - Userid: username of remote VDS client
		 * - Attributes: attributes to insert
		 * 
		 * <pre>
		 */
		
		try {
			
			log("Insert: " + prop.getDn() + " values: " + prop.getAttributes());
			
			//START - CODE TEMPLATE: reading/updating backend connection information
			/*
			prop.setHostName("newhost.domain.com");
			prop.setUserid("newuser");
			prop.setPassword("newpassword");
			//for advanced connection information use ConnString2 object
			//ConnString2 connStr = prop.getConnectionstringObject();
			//log("host to connect to: " + prop.getHostName());
			//connStr.setDb_driverClassName("jdbc.mydriver...");
			//save modified ConnString2 object
			//prop.setConnectionstring(connStr.toString());
			 */
			//END - CODE TEMPLATE: reading/updating backend connection information
			
			
			// START - CODE TEMPLATE: reading/updating attributes to insert
			/*
			Attributes attrs = p.getAttributes();
			NamingEnumeration<Attribute> attrEnu = (NamingEnumeration<Attribute>) attrs
					.getAll();
			while (attrEnu.hasMoreElements()) {
				Attribute attr = attrEnu.nextElement();
				String attrName = attr.getID();
				log("attribute to insert: " + attrName);
				// operation on attribute
				// ...
			}
			// add new attribute
			attrs.put(new BasicAttributeIndexed("newattr", "neavalue"));
			// remove attribute
			attrs.remove("attrname");
			// access an attribute by its name
			Attribute attr = prop.getAttributeToInsert("attrname");
			//cancel insert on backend by returning OK (if script already did the insert)
			//prop.setStatusOk();
			 */
			// END - CODE TEMPLATE: reading/updating attributes to insert
			
		} catch (Exception iex) {
			log("!!! We got an Exception: " + iex);
			// failed status stops current request and returns an error
			prop.setStatusFailed();
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
		}
	}


	/**
	 * 'update' operation associated with 'On modify' event status, errorcode
	 * and other parameters are populated accordingly The following code returns
	 * status "proceed" with errorcode "0" : The LDAP Delete operation will be
	 * executed after returning from the script
	 * 
	 * @param prop
	 *            - InterceptParam - Input /Output parameters
	 **/
	public void update(InterceptParam prop) {
		
		/**
		 * <pre>
		 * Default values received in update function:
		 * 
		 * Constants:
		 * - Status=proceed
		 * - Errorcode=0
		 * 
		 * Meta variables:
		 * - ConnectionstringObject: connection information for backend
		 * 
		 * Request variables:
		 * - Dn: virtual DN requested
		 * - ClientIP: IP adress of remote VDS client
		 * - Userid: username of remote VDS client
		 * - Modifications: attributes to modify
		 * 
		 * <pre>
		 */
		try {
			
			log("Update: " + prop.getDn() + " values: " + prop.getModifications());
			
			//START - CODE TEMPLATE: reading/updating backend connection information
			/*
			prop.setHostName("newhost.domain.com");
			prop.setUserid("newuser");
			prop.setPassword("newpassword");
			//for advanced connection information use ConnString2 object
			//ConnString2 connStr = prop.getConnectionstringObject();
			//log("host to connect to: " + prop.getHostName());
			//connStr.setDb_driverClassName("jdbc.mydriver...");
			//save modified ConnString2 object
			//prop.setConnectionstring(connStr.toString());
			 */
			//END - CODE TEMPLATE: reading/updating backend connection information
			
			
			// START - CODE TEMPLATE: reading/updating attributes to update
			/*
			List<ModificationItem> modifs = prop.getModifications();
			for (ModificationItem modif : modifs) {
				String attrName=modif.getAttribute().getID();
				int modifOp=modif.getModificationOp();
				log("attribute to update: " + attrName);
			}
			// add new attribute to modif
			modifs.add(new ModificationItem(
					DirContext.REPLACE_ATTRIBUTE,
					new BasicAttributeIndexed("newattr", "neavalue")));
			// remove attribute
			modifs.remove(0);//replace 0 with attribute index
			// access an attribute by its name
			ModificationItem modif=prop.getModificationItem("attrname");
			
			//cancel update on backend by returning OK (if script already did the insert)
			//prop.setStatusOk();
			 */
			// END - CODE TEMPLATE: reading/updating attributes to update
			
		} catch (Exception iex) {
			log("!!! We got an Exception: " + iex);
			// failed status stops current request and returns an error
			prop.setStatusFailed();
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
		}
	}
	
	/**
	 * 'delete' operation associated with 'On delete' event status, errorcode
	 * and other parameters are populated accordingly The following code returns
	 * status "proceed" with errorcode "0" : The LDAP Delete operation will be
	 * executed after returning from the script
	 * 
	 * @param prop
	 *            - InterceptParam - Input /Output parameters
	 **/
	public void delete(InterceptParam prop) {

		/**
		 * <pre>
		 * Default values received in delete function:
		 * 
		 * Constants:
		 * - Status=proceed
		 * - Errorcode=0
		 * 
		 * Meta variables:
		 * - ConnectionstringObject: connection information for backend
		 * 
		 * Request variables:
		 * - Dn: virtual DN requested
		 * - ClientIP: IP adress of remote VDS client
		 * - Userid: username of remote VDS client
		 * 
		 * <pre>
		 */

		try {

			log("Delete: " + prop.getDn());
			// script operation ...

			// if insert operation should not be sent to backend, change status
			// to ok
			// prop.setStatusOk();
			
			//START - CODE TEMPLATE: reading/updating backend connection information
			/*
			prop.setHostName("newhost.domain.com");
			prop.setUserid("newuser");
			prop.setPassword("newpassword");
			//for advanced connection information use ConnString2 object
			//ConnString2 connStr = prop.getConnectionstringObject();
			//log("host to connect to: " + prop.getHostName());
			//connStr.setDb_driverClassName("jdbc.mydriver...");
			//save modified ConnString2 object
			//prop.setConnectionstring(connStr.toString());
			 */
			//END - CODE TEMPLATE: reading/updating backend connection information
		} catch (Exception iex) {
			log("!!! We got an Exception: " + iex);
			// failed status stops current request and returns an error
			prop.setStatusFailed();
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
		}
	}

	/**
	 * 'compare' operation associated with 'On compare' event status, errorcode
	 * and other parameters are populated accordingly The following code returns
	 * status "proceed" with errorcode "0" : The LDAP Compare operation will be
	 * executed after returning from the script
	 * 
	 * @param prop
	 *            - InterceptParam - Input /Output parameters
	 **/
	public void compare(InterceptParam prop) {
		try {
			String dn = prop.getDn();
			log(">>> " + prop.getAction());
			log(">>> dn: " + dn);
			String filter = prop.getFilter();
			log(">>> filter: " + filter);
			prop.setStatusProceed();
			prop.setErrorcodeZero();
		} catch (Exception iex) {
			log("!!! We got an Exception: " + iex);
			// failed status stops current request and returns an error
			prop.setStatusFailed();
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
		}
	}

	/**
	 * invoke interception
	 */
	public void invoke(InterceptParam prop) {
		// Reserved for future use
		prop.setStatusOk();
		prop.setErrorcodeZero();
	}

	/**
	 * log into rli.log file
	 * @param message
	 */
	private static void log(String message) {
		log4jLogger.info(message);
	}
}
