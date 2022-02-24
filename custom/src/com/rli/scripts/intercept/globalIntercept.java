package com.rli.scripts.intercept;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.rli.vds.util.UserDefinedInterception2;

import com.rli.util.djava.ScriptHelper;
import com.rli.vds.util.InterceptParam;
import com.rli.slapd.server.*;

public class globalIntercept implements UserDefinedInterception2 {
	private final static Logger log4jLogger = LogManager.getLogger(globalIntercept.class);
	
		/**
		 * @param prop - InterceptParam - Input /Output parameters
		 **/
		public void select(InterceptParam prop) {
			/* 'select' operation associated with 'On Search' event
			 * 
			 */
			try
			  {
				  	String dn= prop.getDn();
				  	log(">>> "+prop.getAction());
			  		log(">>> dn: "+dn);			    
				  	prop.setStatus("proceed");
				  	prop.setErrorcode("0");			
					// prop.list(System.out);
					String operation = prop.getOperationID();
					log(">>> Operation ID: " +operation);
			  }
			  catch(Exception iex) 
			  {
				  	log("!!! We got an Exception: "+iex);
				  	prop.setStatus("failed");
				  	prop.setErrorcode("1");
			  }
		}

		/**
		 * @param prop - InterceptParam - Input /Output parameters
		 **/
		public void insert(InterceptParam prop) {
			try
			{
			    /* 'insert' operation associated with 'On Add' event
			     * Status, errorcode and other parameters are populated accordingly
			     * The following code returns status "proceed" with errorcode "0" :
			     *  The LDAP Add operation will be executed after returning from the script 
			     */			    
			  	String dn= prop.getDn();
			  	log(">>> "+prop.getAction());
		  		log(">>> dn: "+dn);		
		  		String operation = prop.getOperationID();
				log(">>> Operation ID: " +operation);
				Attributes toadd=prop.getAttributes();
				try {
					// add an attribute to insert
//					toadd.put("description","This attribute is auto-generated");
				} catch (Exception e) {}
			    prop.setStatus("proceed");
			    prop.setErrorcode("0");
				// prop.list(System.out);
			}
			catch(Exception iex) 
			{
			  	log("!!! We got an Exception: "+iex);
			  	prop.setStatus("failed");
			  	prop.setErrorcode("1");		    
			}

		}

		/**
		 * @param prop - InterceptParam - Input /Output parameters
		 **/
		public void delete(InterceptParam prop) {
		  try
		  {	
		    /* 'delete' operation associated with 'On delete' event
		     * status, errorcode and other parameters are populated accordingly
		     * The following code returns status "proceed" with errorcode "0" :
		     * The LDAP Delete operation will be executed after returning from the script
		     */
			  	String dn= prop.getDn();
			  	log(">>> "+prop.getAction());
		  		log(">>> dn: "+dn);			    
			    prop.setStatus("proceed");
			    prop.setErrorcode("0");
				// prop.list(System.out);
		  }
		  catch(Exception iex) 
		  {
			  	log("!!! We got an Exception: "+iex);
			  	prop.setStatus("failed");
			  	prop.setErrorcode("1");		    
		  }

		}

		/**
		 * @param prop - InterceptParam - Input /Output parameters
		 **/
		public void update(InterceptParam prop) {
			try
			{	
			    /* 'update' operation associated with 'On modify' event
			     * status, errorcode and other parameters are populated accordingly
			     * The following code returns status "proceed" with errorcode "0" :
			     * The LDAP Delete operation will be executed after returning from the script
			     */
			  	String dn= prop.getDn();
			  	log(">>> Do update");
		  		log(">>> dn: "+dn);			    
			  	// status set to 'proceed' in order to execute the command
				prop.setStatus("proceed");
				prop.setErrorcode("0");
				// prop.list(System.out);
			}
			catch(Exception iex) 
			{
			  	log("!!! We got an Exception: "+iex);
			  	prop.setStatus("failed");
			  	prop.setErrorcode("1");		    
			}
		}
			
		/**
		 * @param prop - InterceptParam - Input /Output parameters
		 **/
		public void compare (InterceptParam prop) {
			 try
			 {					 
				 /* 'compare' operation associated with 'On compare' event
				  * status, errorcode and other parameters are populated accordingly
				  * The following code returns status "proceed" with errorcode "0" :
				  * The LDAP Compare operation will be executed after returning from the script
				  */
			  	String dn= prop.getDn();
				log(">>> "+prop.getAction());
			  	log(">>> dn: "+dn);			    
				String filter=prop.getFilter();
			  	log(">>> filter: "+filter);			    				 
				prop.setStatus("proceed");
				prop.setErrorcode("0");
				// prop.list(System.out);
			 }
			 catch(Exception iex) 
			 {
			  	log("!!! We got an Exception: "+iex);
			  	prop.setStatus("failed");
			  	prop.setErrorcode("1");		    
			 }
		}
			
		/**
		 * @param prop - InterceptParam - Input /Output parameters
		 **/
		public void authenticate(InterceptParam prop) {
		  try
		  {	
			    /* 'authenticate' operation associated with 'On bind' event 
			     * status, errorcode and other parameters are populated accordingly
			     * The following code returns status "proceed" with errorcode "0" :
				 * The LDAP Bind operation will be executed after returning from the script
			     */
			  	String dn= prop.getDn();
				log(">>> "+prop.getAction());
			  	log(">>> dn: "+dn);			    
			  	String userdn= prop.getUserid();
				log(">>> BindDn: "+userdn);
				log(">>> getBaseDN "+prop.getBaseDn());
				log(">>> getOperationID "+prop.getOperationID());

//			  	String password= prop.getPassword();
			    prop.setStatus("proceed");
			    prop.setErrorcode("0");
//				// prop.list(System.out);
		  }
		  catch(Exception iex) 
		  {
			  	log("!!! We got an Exception: "+iex);
			  	prop.setStatus("failed");
			  	prop.setErrorcode("49"); // invalid credentials		    
		  }
			
		}

		public void invoke(InterceptParam prop) {
			
			// interception that may be used to support new custom commands like action=xxx, 
			// to use with extreme caution !
		    prop.setStatus("proceed");
		    prop.setErrorcode("0");
		}
		
		
		/**
		 * Post processing after the target entry is added by LDAPAddRequest
		 * @param prop - InterceptParam - Input /Output parameters
		 */
		public void postInsert(InterceptParam prop)
		{
			
			log(">>> In postInsert() .......... ");

			try{
				/*
				 * Process the added entry -- added_entry
				 * For example:
				 *    1) to send an event about the action of adding of this entry
				 *    2) to log this added entry to some where else for audit purpose
				 *    3) ...
				 */
			  	String action = prop.getAction();
				log(">>> action: " + action);

				// get the dn of the added entry .
				String dn = prop.getDn();
				log(">>> dn: " + dn);
				
				// get the attributes of the added entry .
				Attributes attrs = prop.getAttributes();
				log(">>> attrs: " + attrs.toString());		
				
				// How is the operation ended?
			  	int result_code = Integer.parseInt(prop.getErrorcode());
				log(">>> result_code: " + result_code);
			  	String err_message = prop.getErrormessage();
				log(">>> err_message: " + err_message);
			  	
				/*
				 * Your logic ...
				 * For example:
				 *    1) to send an event about the action of adding of this entry
				 *    2) to log this added entry to some where else for audit purpose
				 *    3) ...
				 */
				
				prop.setStatus("proceed");
				prop.setErrorcode("0");
			}
			catch(Exception e) 
			{
				prop.setStatus("failed");
				prop.setErrorcode("1");		    
			}
		}
		
		/**
		 * Post processing after the target entry is deleted by LDAPDeleteRequest
		 * @param prop - InterceptParam - Input /Output parameters
		 */
		public void postDelete(InterceptParam prop)
		{
			
			log(">>> In postDelete() .......... ");

			try{
				
			  	String action = prop.getAction();
				log(">>> action: " + action);

				// get dn of the deleted entry.
				String dn = prop.getDn();
				log(">>> dn: " + dn);
				
				// How is the operation ended?
			  	int result_code = Integer.parseInt(prop.getErrorcode());
				log(">>> result_code: " + result_code);
			  	String err_message = prop.getErrormessage();
				log(">>> err_message: " + err_message);
			  	
				/*
				 * Your logic ...
				 * For example:
				 *    1) to delete the dependencies of this entry
				 *    2) ...
				 */
				
				
				prop.setStatus("proceed");
				prop.setErrorcode("0");
			}
			catch(Exception e) 
			{
				prop.setStatus("failed");
				prop.setErrorcode("1");		    
			}
		}
		
		/**
		 * Post processing after the operation by LDAPModifyRequest
		 * @param prop - InterceptParam - Input /Output parameters
		 */
		public void postUpdate(InterceptParam prop)
		{
			
			log(">>> In postUpdate() .......... ");

			try{
				
			  	String action = prop.getAction();
				log(">>> action: " + action);

				// get dn of the updated entry.
				String dn = prop.getDn();
				log(">>> dn: " + dn);
		    	List mods = prop.getModifications();          	
		    	for (int i = 0; i <mods.size(); i++) 
		    	{
		    		javax.naming.directory.ModificationItem mod = (javax.naming.directory.ModificationItem)mods.get(i);
					log(">>> mod: " + mod.toString());
		    	}            	    
				
				// How is the operation ended?
			  	int result_code = Integer.parseInt(prop.getErrorcode());
				log(">>> result_code: " + result_code);
			  	String err_message = prop.getErrormessage();
				log(">>> err_message: " + err_message);
			  	
				/*
				 *  Your logic ...
				 */
				
		    	
				prop.setStatus("proceed");
				prop.setErrorcode("0");
			}
			catch(Exception e) 
			{
				prop.setStatus("failed");
				prop.setErrorcode("1");		    
			}
		}
		
		/**
		 * Post processing after the operation by LDAPCompareRequest
		 * @param prop - InterceptParam - Input /Output parameters
		 */
		public void postCompare(InterceptParam prop)
		{
			
			log(">>> In postCompare() .......... ");

			try{
				
			  	String action = prop.getAction();
				log(">>> action: " + action);

				// get the dn 
				String dn = prop.getDn();
				log(">>> dn: " + dn);
				
				// get the assertion 
				String filter = prop.getFilter();
				log(">>> filter: " + filter);
				
				// How is the operation ended?
			  	int result_code = Integer.parseInt(prop.getErrorcode());
				log(">>> result_code: " + result_code);
			  	String err_message = prop.getErrormessage();
				log(">>> err_message: " + err_message);
			  	
				/*
				 *  Your logic ...
				 */
				
				
				prop.setStatus("proceed");
				prop.setErrorcode("0");
			}
			catch(Exception e) 
			{
				prop.setStatus("failed");
				prop.setErrorcode("1");		    
			}
		}
		
		
		/**
		 * Post processing after the bind operation by LDAPBindRequest
		 * @param prop - InterceptParam - Input /Output parameters
		 */
		public void postAuthenticate(InterceptParam prop)
		{
			
			log(">>> In postAuthenticate() .......... ");

			try{
				
			  	String action = prop.getAction();
				log(">>> action: " + action);
				log(">>> post-bind:getBaseDN "+prop.getBaseDn());
				log(">>> post-bind:getDn "+prop.getDn());
				log(">>> post-bind:getBindDN "+prop.getBindDN());
				log(">>> post-bind:getUserid "+prop.getUserid());
				log(">>> post-bind:getOperationID "+prop.getOperationID());

				
				// get the dn 
				String dn = prop.getDn();
				
				// How is the operation ended?
			  	int result_code = Integer.parseInt(prop.getErrorcode());
				log(">>> result_code: " + result_code);
			  	String err_message = prop.getErrormessage();
				log(">>> err_message: " + err_message);
			  	
				/*
				 *  Your logic ...
				 *  For example:
				 *    1) If the bind operation fails, you can add your own policy here 
				 *    2) ...
				 */
				
				prop.setStatus("proceed");
				prop.setErrorcode("0");
			}
			catch(Exception e) 
			{
				prop.setStatus("failed");
				prop.setErrorcode("1");		    
			}
		}
		
		
		
	 /**
	 * @param prop - InterceptParam - Input /Output parameters
	 * @param anEntry - javax.naming.directory.SearchResult - entry  
	 * @return - javax.naming.directory.SearchResult modified entry or null
	 */
	public static SearchResult processresult(InterceptParam prop,
			SearchResult anEntry) {
		// !!!!!!!!!!!!!!!!! not called for global interception script
		// 
		// stayed defined to comply with interface UserDefinedInterception2
		// LDAPEntry processresult(InterceptParam prop,LDAPEntry anEntry) is triggered instead
		return null;
	}

	/**
	 * @param prop - InterceptParam - Input /Output parameters
	 * @param anEntry - com.rli.slapd.server.LDAPEntry - entry  
	 * @return - com.rli.slapd.server.LDAPEntry modified entry or null(to ignore entry in the result) 
	 */
	public static LDAPEntry processresult(InterceptParam prop, LDAPEntry anEntry) {
		/* post processing of the result of a search (global level)
		 * The function is called for each entry of the result 
		 * 
		 */
		String dn = prop.getDn();
		log(">>> " + prop.getAction());
		log(">>> dn: " + dn);
		String operation = prop.getOperationID();
		log(">>> Operation ID: " +operation);
		
		//	   prop.list(System.out);

		LDAPEntry lde = anEntry;
		/*
		// get the set of attributes  of the entry .
		LDAPAttributeSet attrs = lde.getAttributeSet();
		// you may manipulate the entry here
		// to skip this entry , if you do not want he entry to be part of the result, return null
		try {
			// build attributes on the fly....

			// for any object, add the 'ou' attribute or add a value if it already exist

			LDAPAttribute attr = lde.getAttribute("ou");
			if (attr != null) { // attribute exists , add a value
				attr.addValue("added value");
			} else { // attribute does not exist, add it
				attr = new LDAPAttribute("ou", "Added Attribute");
				attrs.add(attr);
			}
			// let's add othermail  attribute when objectclass is person and has sn and givenName defined
			if (ScriptHelper.matchFilter(lde,
					"(&(objectclass=person)(sn=*)(givenName=*))")) {
				String Firstname = lde.getAttribute("givenName")
						.getStringValueArray()[0];
				String Lastname = lde.getAttribute("sn").getStringValueArray()[0];
				String othermail = Firstname + "." + Lastname
						+ "@mycompany.com";
				attrs.add(new LDAPAttribute("othermail", othermail));
			}

			// lets change the value of an attribute
			attr = lde.getAttribute("l");
			if (attr != null) {
				String[] vals = attr.getStringValueArray();
				if (vals != null && vals.length > 0)
					if (vals[0].equalsIgnoreCase("san francisco")) {
						vals[0] = "SFO"; // change san francisco to SFO
						attr.setValues(vals);
					}
			}

			// >>> hiding an attribute : remove the nTSecurityDescriptor attribute for entries with objectclass 'user'

			if (ScriptHelper.matchFilter(lde,
					"(&(objectclass=user)(nTSecurityDescriptor=*)")) {
				// attribute exists , remove it
				attrs.remove("nTSecurityDescriptor");
			}
		} catch (Exception exx) {
			log("exception:" + exx);
		}
		if (dn.toLowerCase().endsWith("o=companydirectory")) // avoid join with itself         
			prop.setCalljoin("no"); // do not join 
		else
			prop.setCalljoin("yes"); // do the external join (if any)

		//	    log(">>LDIF of the entry after entry manipulation");
		//	    String att=lde.toLDIF();
		//	    log(att); // display in ldif format    
		//	    log(">>> Entry:");
		//	    log(lde.toString());
*/
		return lde;
	}

	// method to intercept the 'resultcode' message returned by a search

	public void processresultcode(InterceptParam prop) {
		// Get result info
		String operation = prop.getOperationID();
		String code = prop.getErrorcode();
		log(">>> " + prop.getAction());
		log(">>> clientIP: " + prop.getClientIP());
		log(">>> Operation ID: " + operation);
		log(">>> Resultcode : " + code);
		log(">>> ErrorMessage : " + prop.getErrormessage());
		log(">>> Matched DN : " + prop.getDn());
		log(">>> # entries returned: " + prop.getResultcount());

		// example : change resultcode and error message message
		/*
		if (code.equals("9")) {
			prop.setErrorcode("1");
			prop.setErrormessage("One or more DataSource(s) could not be reached");
			log(">>> Resultcode Changed to: " + prop.getErrorcode());
		}
		*/
	} 



		private static  void log(String message)
		{
			
			log4jLogger.info(message);
			//System.out.println(message);
		}


		public static void main(String[] args) {
		}
	}


