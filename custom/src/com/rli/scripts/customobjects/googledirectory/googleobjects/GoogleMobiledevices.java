package com.rli.scripts.customobjects.googledirectory.googleobjects;

import java.io.IOException;
import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import com.google.api.client.json.GenericJson;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.admin.directory.model.MobileDeviceAction;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryEnumeration;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryHandler;
import com.rli.scripts.customobjects.googledirectory.GoogleObject;
import com.rli.slapd.server.LDAPException;
import com.rli.util.djava.ScriptHelper;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.service.adap.core.exception.POSTParsingException;

public class GoogleMobiledevices extends GoogleObject {

	private final static String[] ATTRIBUTES_TYPE_LIST_ARRAY = new String[] {
			"name", "email", "applications" };

	@SuppressWarnings("unchecked")
	public GoogleMobiledevices(GoogleDirectoryHandler handler) {
		super(handler);
		attributes_type_list.addAll(ScriptHelper
				.convertArrayStrToList(ATTRIBUTES_TYPE_LIST_ARRAY));
	}

	@Override
	public SearchResult lookupEntry(String key, String filter,
			String[] attributesRequested) throws Exception {
		return generate(dir.mobiledevices().get(customerId, key).execute(),
				filter, attributesRequested);
	}

	@Override
	public GoogleDirectoryEnumeration search(String baseDN, String filter, String[] attributes)
			throws NoSuchMethodException, SecurityException, IOException {
		return new GoogleDirectoryEnumeration(this, dir.mobiledevices()
				.list("my_customer").setMaxResults(pageSize), filter,
				attributes, pageSize);
	}

	@Override
	public void delete(String key) throws Exception {
		dir.mobiledevices().delete(customerId, key);
	}

	@Override
	public void insert(String key, Attributes attrs) throws Exception {
		throw new LDAPException(
				"Mobile devices cannot be created via the Google Admin API.");
	}

	@Override
	public void update(String key, List<ModificationItem> modificationAttrs)
			throws Exception {
		String action = null;
		for (ModificationItem mod : modificationAttrs) {
			if (mod.getAttribute().getID().equalsIgnoreCase("action")
					&& (mod.getModificationOp() == DirContext.ADD_ATTRIBUTE || mod
							.getModificationOp() == DirContext.REPLACE_ATTRIBUTE))
				action = ScriptHelper.getAttributeValue(mod.getAttribute());
		}
		if (action != null)
			dir.mobiledevices()
					.action(customerId, key,
							new MobileDeviceAction().setAction(action))
					.execute();
	}

	@Override
	public SearchResult generate(GenericJson model, String filter,
			String[] attributes) throws JSONException, POSTParsingException, IOException {
		MobileDevice device = (MobileDevice) model;
		return new SearchResult(getDN(device.getDeviceId()), null,
				getAttributes(model));
	}

}
