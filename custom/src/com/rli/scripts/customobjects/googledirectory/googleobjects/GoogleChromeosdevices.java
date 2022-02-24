package com.rli.scripts.customobjects.googledirectory.googleobjects;

import java.io.IOException;
import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import com.google.api.client.json.GenericJson;
import com.google.api.services.admin.directory.model.ChromeOsDevice;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryEnumeration;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryHandler;
import com.rli.scripts.customobjects.googledirectory.GoogleObject;
import com.rli.slapd.server.LDAPException;
import com.rli.util.djava.ScriptHelper;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.service.adap.core.exception.POSTParsingException;

public class GoogleChromeosdevices extends GoogleObject {

	private final static String[] ATTRIBUTES_TYPE_LIST_ARRAY = new String[] {
			"recentUsers", "activeTimeRanges" };

	@SuppressWarnings("unchecked")
	public GoogleChromeosdevices(GoogleDirectoryHandler handler) {
		super(handler);
		attributes_type_list.addAll(ScriptHelper
				.convertArrayStrToList(ATTRIBUTES_TYPE_LIST_ARRAY));
	}

	@Override
	public SearchResult lookupEntry(String key, String filter,
			String[] attributesRequested) throws Exception {
		return generate(dir.chromeosdevices().get(customerId, key).execute(),
				filter, attributesRequested);
	}

	@Override
	public GoogleDirectoryEnumeration search(String baseDN, String filter, String[] attributes)
			throws NoSuchMethodException, SecurityException, IOException {
		return new GoogleDirectoryEnumeration(this, dir.chromeosdevices()
				.list("my_customer").setMaxResults(pageSize), filter,
				attributes, pageSize);
	}

	@Override
	public void delete(String key) throws Exception {
		throw new LDAPException(
				"ChromeOS devices cannot be deleted via the Google Admin API.");
	}

	@Override
	public void insert(String key, Attributes attrs) throws Exception {
		throw new LDAPException(
				"ChromeOS devices cannot be created via the Google Admin API.");
	}

	@Override
	public void update(String key, List<ModificationItem> modificationAttrs)
			throws Exception {
		ChromeOsDevice c = dir.chromeosdevices().get(customerId, key).execute();
		for (ModificationItem mod : modificationAttrs) {
			updateAttribute(c, mod);
		}
		dir.chromeosdevices().update(customerId, key, c).execute();
	}

	@Override
	public SearchResult generate(GenericJson model, String filter,
			String[] attributes) throws JSONException, POSTParsingException, IOException {
		ChromeOsDevice device = (ChromeOsDevice) model;
		return new SearchResult(getDN(device.getDeviceId()), null,
				getAttributes(model));
	}

}
