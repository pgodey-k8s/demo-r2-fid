package com.rli.scripts.customobjects.gcontacts;

import javax.naming.directory.SearchResult;

import com.google.gdata.client.Query;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.contacts.ContactGroupEntry;
import com.google.gdata.data.contacts.ContactGroupFeed;
import com.rli.scripts.customobjects.api.WSEnumeration;

public class GdataEnumeration extends WSEnumeration {

	GdataHandler gdataHandler;
	
	public GdataEnumeration(GdataHandler operationHandler, String filter,
			String[] attributes, int pageSize) {
		super(filter, attributes, pageSize);
		this.gdataHandler=operationHandler;
	}

	protected void getNextPage() throws Exception{
		Query query = new Query(gdataHandler.getFeedUrl());
		query.setStartIndex(cursor);
		query.setMaxResults(pageSize);
		
		
		if (gdataHandler.isGroup()) {
			ContactGroupFeed groupFeed = gdataHandler.getService().query(query, ContactGroupFeed.class);
		    for (ContactGroupEntry entry : groupFeed.getEntries()) {
		    	SearchResult sr=gdataHandler.generate(entry, attributes);
		    	if(sr!=null){
			    	page.add(sr);
			    	cursor++;
		    	}
		    }
		} else {
			// WE CAN POST FILTER ONLY ON GROUP OF A USER
			// TODO FOR OPIMIZATION
			
			ContactFeed resultFeed = gdataHandler.getService().query(query, ContactFeed.class);
	        for (ContactEntry entry : resultFeed.getEntries()) {
	        	SearchResult sr=gdataHandler.generate(entry, attributes);
		    	if(sr!=null){
		        	page.add(sr);
		        	cursor++;
		    	}
	        }
		}
		
	}

}
