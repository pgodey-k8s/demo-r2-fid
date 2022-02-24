/*
 * Created on Dec 31, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.rli.scripts.intercept;

import javax.naming.NamingEnumeration;

import com.rli.jndi.vdap.runtime.misc.JNDI2XSD;
import com.rli.util.djava.ScriptHelper;
import com.rli.vds.util.UserDefinedInterception;
import com.rli.vds.util.XMLOperation;

/**
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class fulljointest1 implements UserDefinedInterception {

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#select(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	@SuppressWarnings("unchecked")
	public void select(XMLOperation src, XMLOperation dest) {
  		// TODO Auto-generated method stub
  		try {
  			System.out.println(src.exportXML());
            String strDN = src.getValue("dn");
            System.out.println("..DN=" + strDN);
            String strRDNValue = GetRDNValue(strDN);
            String RDNName=src.getValue("typename"); // rdn name to return 
            String strObjectClass = src.getValue("objectclass");
            String strScope = src.getValue("scope").toLowerCase();
            String IRLtojoin="";
            if(strScope.equals("base"))
            {	// put rdn and do base search
          		IRLtojoin="LDAP:///Employee="+strRDNValue+",table=employees,dv=accessnorthwind,o=vds??base?(objectclass=*);LDAP:///Employee="+strRDNValue+",Table=employees,dv=northwind,o=vds??base?(objectclass=*)";
            	
            }
            else
            {	// search obe level
          		IRLtojoin="LDAP:///table=employees,dv=accessnorthwind,o=vds??one?(objectclass=*);LDAP:///Table=employees,dv=northwind,o=vds??one?(objectclass=*)";

            }
  	  	System.out.println("irls to join:"+IRLtojoin);
  		if (IRLtojoin!=null && IRLtojoin.length()>0)
  		{
  			ScriptHelper scr=new ScriptHelper();
  			JNDI2XSD tx=new JNDI2XSD();
  			NamingEnumeration nm= scr.Buildfulljoin(IRLtojoin,RDNName,null,false,100);
  			String resultXML=tx.toXSD(nm,"select");
  		  	System.out.println("result xml:"+resultXML);			
  			dest.importXML(resultXML);
  			dest.firstInstance();
  		}
  		else
  		{
  		  	System.out.println("!!! No join found");
  		  	dest.setMessageType("resultcode");
  		    // copy the input connectionstring property to the output
  		    dest.copyAttribute(src,"command","command");
  		    dest.copyAttribute(src,"connectionstring","connectionstring");
  		      	
  		  	dest.setValue("status","proceed");
  		  	dest.setValue("errorcode","0");
  		  	
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
	 * @see com.rli.vds.util.UserDefinedInterception#insert(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void insert(XMLOperation src, XMLOperation dest) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#delete(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void delete(XMLOperation src, XMLOperation dest) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#update(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void update(XMLOperation src, XMLOperation dest) {
		// TODO Auto-generated method stub

	} 

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#authenticate(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void authenticate(XMLOperation src, XMLOperation dest) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.rli.vds.util.UserDefinedInterception#invoke(com.rli.vds.util.XMLOperation, com.rli.vds.util.XMLOperation)
	 */
	public void invoke(XMLOperation src, XMLOperation dest) {
		// TODO Auto-generated method stub

	}
    public String GetRDNValue(String strDN)
    {
        String val = "";
        int index = strDN.indexOf("=");
        if( index > 0 )
        {
        	int index1=strDN.indexOf(",");
        	if (index1 >0)        		
        		val = strDN.substring(index+1,index1 );
        	else
        		val = strDN.substring(index+1,index1 );
        	val = val.trim();
        }
        return val;
    }

	public static void main(String[] args) {
	}
}
