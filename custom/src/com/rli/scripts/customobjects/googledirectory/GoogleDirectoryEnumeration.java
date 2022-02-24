package com.rli.scripts.customobjects.googledirectory;

import java.lang.reflect.Method;
import java.util.List;

import javax.naming.directory.SearchResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.json.GenericJson;
import com.google.api.services.admin.directory.Directory;
import com.rli.scripts.customobjects.api.WSEnumeration;

public class GoogleDirectoryEnumeration extends WSEnumeration {

	private final static Logger logger = LogManager.getLogger(GoogleDirectoryEnumeration.class);

	private GoogleObject gObject;
	private Object googleList;
	private Method setPageToken;
	private Method execute;
	private Method getNextPageToken;
	private Method getInstanceObjects;
	private String token = null;
	private boolean first_page = true;

	/*
	 * All Google Directory.?.List objects have the methods: setPageToken,
	 * execute. And all Google ? model objects have the methods:
	 * getNextPageToken, get+? (e.g. getGroups) But there are no Generic
	 * Interfaces. Based on this observation we can call them dynamically
	 * anyway.
	 * Except for Orgunits which is the special child.
	 * 
	 * javadoc example Directory.Groups.List:
	 * https://developers.google.com/resources
	 * /api-libraries/documentation/admin/directory_v1/java/latest/
	 */

	public GoogleDirectoryEnumeration(GoogleObject handler, Object list,
			String filter, String[] attributes, int pageSize)
					throws NoSuchMethodException, SecurityException {
		super(filter, attributes, pageSize);
		this.gObject = handler;
		googleList = list;
		// Directory.Orgunits.List has no pagination
		setPageToken = googleList instanceof Directory.Orgunits.List ? null
				: googleList.getClass().getMethod("setPageToken", String.class);
		execute = googleList.getClass().getMethod("execute");
	}

	@Override
	protected void getNextPage() throws Exception {
		// TODO Fields mask and Filter

		// quit if end of request reached
		if (!first_page && token == null)
			return;

		Object responseObjects = null;
		// get page
		try{
			responseObjects = execute.invoke(googleList);
		}catch (Exception e){
			logger.error("error in getNextPage()", e);
			throw e;
		}
		if (responseObjects != null) {
			// set getters methods on response objects if not set already
			if (getInstanceObjects == null) {
				Class<?> objectclass = responseObjects.getClass();
				// Orgunits Class does not follow the pattern
				if (googleList instanceof Directory.Orgunits.List) {
					getInstanceObjects = objectclass
							.getMethod("getOrganizationUnits");
				} else {
					getNextPageToken = objectclass
							.getMethod("getNextPageToken");
					getInstanceObjects = objectclass.getMethod("get"
							+ objectclass.getSimpleName());
				}
			}
			// get next page token and update request if pagination is used
			if (setPageToken != null) {
				Object t = getNextPageToken.invoke(responseObjects);
				token = t == null ? null : t.toString();
				googleList = setPageToken.invoke(googleList, token);
			}
			// process response objects
			for (Object instance : (List<?>) getInstanceObjects
					.invoke(responseObjects)) {
				SearchResult sr = gObject
						.generate((GenericJson) instance, filter, attributes);
				if (sr != null) {
					page.add(sr);
				}
			}
		}
		first_page = false;
	}
}
