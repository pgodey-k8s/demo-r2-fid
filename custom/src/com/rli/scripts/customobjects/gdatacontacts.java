package com.rli.scripts.customobjects;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchResult;

import com.rli.scripts.customobjects.api.WSHandler;
import com.rli.scripts.customobjects.gcontacts.GdataHandler;
import com.rli.slapd.server.LDAPException;
import com.rli.vds.util.InterceptParam;
import com.rli.vds.util.UserDefinedInterception2;

public class gdatacontacts implements UserDefinedInterception2{

	@Override
	public void select(InterceptParam prop) {

		// REQUESTED ATTRIBUTES
		String[] requestedAttributes=null;
		String strAttrs = prop.getAttrs();
//		if (strAttrs != null && !strAttrs.equals(""))
//			requestedAttributes = strAttrs.split(",");
		
		try{
			WSHandler gConnection=new GdataHandler(prop.getCompoundObject());
			
			if (prop.getScope().equals("base")) {
				SearchResult entry=gConnection.lookupEntry(prop.getDn(),prop.getFilter(), requestedAttributes);
				List<SearchResult> result=new ArrayList<SearchResult>();
				if(entry!=null)
					result.add(entry);
				prop.setResultSet(result);
			}
			else{
				NamingEnumeration enu=gConnection.search(prop.getFilter(), requestedAttributes);
				prop.setResultSet_Object(enu);
			}
		}
		catch(Throwable e){
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
			prop.setStatusFailed();
			prop.setErrormessage(e.getMessage());
		}
		
	}

	@Override
	public void insert(InterceptParam prop) {
		try{
			WSHandler gConnection=new GdataHandler(prop.getCompoundObject());
			
			gConnection.insert(prop.getDn(), prop.getAttributes());
		}
		catch(Throwable e){
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
			prop.setStatusFailed();
			prop.setErrormessage(e.getMessage());
		}
		
	}

	@Override
	public void delete(InterceptParam prop) {
		try{
			WSHandler gConnection=new GdataHandler(prop.getCompoundObject());
			
			gConnection.delete(prop.getDn());
		}
		catch(Throwable e){
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
			prop.setStatusFailed();
			prop.setErrormessage(e.getMessage());
		}
		
	}

	@Override
	public void update(InterceptParam prop) {
		try{
			WSHandler gConnection=new GdataHandler(prop.getCompoundObject());
			
			gConnection.update(prop.getDn(), prop.getModifications());
		}
		catch(Throwable e){
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
			prop.setStatusFailed();
			prop.setErrormessage(e.getMessage());
		}
		
	}

	@Override
	public void authenticate(InterceptParam prop) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void compare(InterceptParam prop) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invoke(InterceptParam prop) {
		// TODO Auto-generated method stub
		
	}

}
