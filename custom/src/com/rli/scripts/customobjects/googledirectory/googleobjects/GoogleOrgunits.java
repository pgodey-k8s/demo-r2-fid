package com.rli.scripts.customobjects.googledirectory.googleobjects;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import com.google.api.client.json.GenericJson;
import com.google.api.services.admin.directory.model.OrgUnit;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryEnumeration;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryHandler;
import com.rli.scripts.customobjects.googledirectory.GoogleObject;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.service.adap.core.exception.POSTParsingException;

public class GoogleOrgunits extends GoogleObject {

	public GoogleOrgunits(GoogleDirectoryHandler handler) {
		super(handler);
	}

	@Override
	public SearchResult lookupEntry(String key, String filter,
			String[] attributesRequested) throws Exception {
		return generate(
				dir.orgunits().get(customerId, Collections.singletonList(key))
						.execute(), filter, attributesRequested);
	}

	@Override
	public GoogleDirectoryEnumeration search(String baseDN, String filter, String[] attributes)
			throws NoSuchMethodException, SecurityException, IOException {
		return new GoogleDirectoryEnumeration(this, dir.orgunits()
				.list("my_customer").setType("all").setOrgUnitPath("/"),
				filter, attributes, pageSize);
	}

	@Override
	public void delete(String key) throws Exception {
		dir.orgunits().delete(customerId, Collections.singletonList(key))
				.execute();
	}

	@Override
	public void insert(String key, Attributes attrs) throws Exception {
		OrgUnit o = new OrgUnit();
		o.setOrgUnitPath(key);
		fillAttributes(attrs, o);
		dir.orgunits().insert(customerId, o).execute();
	}

	@Override
	public void update(String key, List<ModificationItem> modificationAttrs)
			throws Exception {
		OrgUnit o =dir.orgunits().get(customerId, Collections.singletonList(key))
				.execute();
		for (ModificationItem mod : modificationAttrs) {
			updateAttribute(o, mod);
		}
		dir.orgunits().update(customerId, Collections.singletonList(key), o).execute();
	}

	@Override
	public SearchResult generate(GenericJson model, String filter,
			String[] attributes) throws JSONException, POSTParsingException, IOException {
		OrgUnit unit = (OrgUnit) model;
		return new SearchResult(getDN(unit.getOrgUnitPath()), null,
				getAttributes(model));
	}

}
