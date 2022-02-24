/*
 * Created on Jan 1, 2005
 *
 */
package com.rli.scripts.intercept;

import com.rli.vds.util.UserDefinedInterception;
import com.rli.vds.util.XMLOperation;

/**
 * Template for Interception Script
 * 
 * TODO To change the template 
 */
public class UserDefinedInterceptionImpl implements UserDefinedInterception {

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#select(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void select(XMLOperation src, XMLOperation dest) {
		try

		  {
		    /* 'select' operation is performed here
		     * and result is populated in "dest"
		     * The following example returns
		     * always the same entry 
		     */ 	
		    dest.setMessageType("resultset");
		    String rdn=src.getValue("typename");
		    dest.setValue("dn", rdn+"="+"VPerson");
		    dest.setValue("cn", "Virtual Person");
		    dest.setValue("uid", "VPerson");
		    dest.setValuesFromString("objectclass", src.getValue("objectclass"));
		  }
		  catch(Exception iex) 
		  {
		  	System.out.println("!!! We got an Exception: "+iex);
		  	dest.setMessageType("resultcode");
		  	dest.setValue("status","failed");
		  	dest.setValue("errorcode","1");
		  }

	}

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#insert(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void insert(XMLOperation src, XMLOperation dest) {
		try
		{
		    /* 'insert' operation is performed here
		     * status, errorcode and other parameters are populated accordingly
		     * The following code returns status "proceed" with errorcode "0" :
		     *   	The command will be executed after returning from the script 
		     */
		    String command = src.getValue("command");
		    // You may change the command here if needed
		    // ...
		    // Set the command in the output attributes  
		    dest.setValue("command", command);
		    // copy the input connectionstring property to the output
		    dest.copyAttribute(src,"connectionstring","connectionstring");
		    dest.setValue("status", "proceed");
		    dest.setValue("errorcode", "0");
		}
		catch(Exception iex) 
		{
		  	System.out.println("!!! We got an Exception: "+iex);
		  	dest.setMessageType("resultcode");
		  	dest.setValue("status","failed");
		  	dest.setValue("errorcode","1");
		}

	}

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#delete(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void delete(XMLOperation src, XMLOperation dest) {
	  try
	  {	
	    /* 'delete' operation is performed here
	     * status, errorcode and other parameters are populated accordingly
	     * The following code returns status "ok" with errorcode "0" :
	     * 	 In this sample , the delete will never do anyhing
	     */
	    dest.setValue("status", "ok");
	    dest.setValue("errorcode", "0");
	  }
	  catch(Exception iex) 
	  {
	  	System.out.println("!!! We got an Exception: "+iex);
	  	dest.setMessageType("resultcode");
	  	dest.setValue("status","failed");
	  	dest.setValue("errorcode","1");
	  }

	}

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#update(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void update(XMLOperation src, XMLOperation dest) {
		 try
		 {	
		    /* 'update' operation is performed here
		     * status, errorcode and other parameters are populated accordingly
		     * 	 In this sample , the command will be executed if the 
		     *   rdn value in dn(distinguished name) does not start with 'v'
		     */
		  	String dn= src.getValue("dn");
		    String rdn=src.getValue("typename").toLowerCase()+"=v";
	  		System.out.println(">>> dn: "+dn);
		    
		  	if (dn.toLowerCase().startsWith(rdn))
		  	{
		  		// do nothing
		  		System.out.println(">>> No update");
			  	dest.setValue("status", "ok");
		  		dest.setValue("errorcode", "0");
		  	}
		  	else
		  	{
		  		// status set to 'proceed' in order to execute the command
		  		System.out.println(">>> Do update");
			    dest.copyAttribute(src,"command","command");
			    dest.copyAttribute(src,"connectionstring","connectionstring");
		  		dest.setValue("status", "proceed");
		  		dest.setValue("errorcode", "0");
		  	}
		 }
		 catch(Exception iex) 
		 {
		  	System.out.println("!!! We got an Exception: "+iex);
		  	dest.setMessageType("resultcode");
		  	dest.setValue("status","failed");
		  	dest.setValue("errorcode","1");
		 }
	}

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#authenticate(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void authenticate(XMLOperation src, XMLOperation dest) {
	  try
	  {	
		    /* 'authentication' operation is performed here
		     * status, errorcode and other parameters are populated accordingly
		     * The following code returns status "ok" with errorcode "0" :
		     * 	 In this sample , the authentication will always succeed
		     */
	  		dest.setValue("status", "YES");
	  		dest.setValue("userid", src.getValue("userid"));
	  		dest.setValue("password", src.getValue("password"));
	  		dest.setValue("errorcode", "0");		  
	  }
	  catch(Exception iex) 
	  {
		  	System.out.println("!!! We got an Exception: "+iex);
		  	dest.setMessageType("resultcode");
		  	dest.setValue("status","failed");
		  	dest.setValue("errorcode","1");
	  }
		
	}

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#invoke(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void invoke(XMLOperation src, XMLOperation dest) {
		// Reserved for future use
	    dest.setValue("status", "ok");
	    dest.setValue("errorcode", "0");
	}

	public static void main(String[] args) {
	}
}
