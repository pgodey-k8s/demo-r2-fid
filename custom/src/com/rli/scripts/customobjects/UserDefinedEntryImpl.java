/*
 * Created on Dec 27, 2004
 *
 */
package com.rli.scripts.customobjects;

import com.rli.vds.util.UserDefinedEntry;
import com.rli.vds.util.XMLOperation;

public class UserDefinedEntryImpl implements UserDefinedEntry {
	
	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedEntry#authenticate(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	
	int curcounter;
	
	public void authenticate(XMLOperation src, XMLOperation dest) {
		  try
		  {
		    //authenticate operation is performed here; status, and errorcode attributes are populated accordingly
		    //...
		    //for example following code returns status "YES" with errorcode "0"
		    dest.setValue("status", "YES");
		    dest.setValue("userid", src.getValue("userid"));
		    dest.setValue("password", src.getValue("password"));
		    dest.setValue("errorcode", "0");
		  }
		  catch(Exception iex) {}
	}
	
	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedEntry#select(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void select(XMLOperation src, XMLOperation dest) {
		  try
		  {
		    //select operation is performed here and result is populated in "xsdResult"
		    //...	
		    System.out.println(src.exportXML());
		    
		    
		    curcounter=1;
		    dest.setMessageType("resultset");
		    String rdn=src.getValue("typename");
		    dest.setValue("dn", rdn+"="+curcounter);
		    dest.setValue("cn", "myvalue "+curcounter);		    
		    dest.setValue("uid", (new Integer(curcounter)).toString());
		    dest.setValuesFromString("objectclass",src.getValue("objectclass"));
		    
		  }
		  catch(Exception iex) {}
	}
	
	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedEntry#update(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void update(XMLOperation src, XMLOperation dest) {
		  try
		  {
		    //update operation is performed here; status, and errorcode attributes are populated accordingly
		    String commandstr = src.getValue("command");
		    XMLOperation command=new XMLOperation();
			// display command
			System.out.println(commandstr);	    
		    command.importXML(commandstr);
		    dest.setValue("status", "ok");
		    dest.setValue("errorcode", "0");
		  }
		  catch(Exception iex) {}
	}	

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedEntry#delete(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void delete(XMLOperation src, XMLOperation dest) {
		  try
		  {
		    //delete operation is performed here; status, and errorcode attributes are populated accordingly
		    //...
		    //for example following code returns status "proceed" with errorcode "0"
		    dest.setValue("status", "proceed");
		    String command = src.getValue("command");
		    // change command here if needed
		    dest.setValue("command", command);
		    dest.setValue("connectionstring", src.getValue("connectionstring"));
		    dest.setValue("errorcode", "0");
		  }
		  catch(Exception iex) {}
	}
	
	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedEntry#insert(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void insert(XMLOperation src, XMLOperation dest) {
		  try
		  {
		    //insert operation is performed here; status, and errorcode attributes are populated accordingly
		    //...
		    //for example following code returns status "proceed" with errorcode "0"
		    String commandstr = src.getValue("command");
		    XMLOperation command=new XMLOperation();
			// display command
			System.out.println(commandstr);	    
		    command.importXML(commandstr);
		    dest.setValue("status", "ok");
		    dest.setValue("errorcode", "0");
		  }
		  catch(Exception iex) {}
	}
	
	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedEntry#invoke(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void invoke(XMLOperation src, XMLOperation dest)
	{
		  try
		  {
		    //invoke operation is performed here; status, and errorcode attributes are populated accordingly
		    //...
		    //for example following code returns status "proceed" with errorcode "0"
		    dest.setValue("status", "proceed");
		    String command = src.getValue("command");
		    // change command here if needed
		    dest.setValue("command", command);
		    dest.setValue("connectionstring", src.getValue("connectionstring"));
		    dest.setValue("errorcode", "0");
		  }
		  catch(Exception iex) {}
	}
	
		private static void TestSelect(UserDefinedEntry ud)
	{
		XMLOperation src=new XMLOperation();
		XMLOperation dest=new XMLOperation();
		ud.select(src,dest);
		// display result
		System.out.println("Result:");			
		System.out.println(dest.exportXML());
	
	}
	private static void TestUpdate(UserDefinedEntry ud)
	{
		XMLOperation src=new XMLOperation();
		XMLOperation dest=new XMLOperation();
		XMLOperation command=new XMLOperation();
		src.setValue("command",command.exportXML());
		ud.update(src,dest);		
		System.out.println("Resultcode:");			
		System.out.println(dest.exportXML());	
	}
	public static void main(String[] args) {
		UserDefinedEntry ud= new UserDefinedEntryImpl();
		TestSelect(ud);
		TestUpdate(ud);
	}
}
