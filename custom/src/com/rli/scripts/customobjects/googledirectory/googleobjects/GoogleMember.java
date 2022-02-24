package com.rli.scripts.customobjects.googledirectory.googleobjects;

import java.io.IOException;
import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import com.google.api.client.json.GenericJson;
import com.google.api.services.admin.directory.model.Member;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryEnumeration;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryHandler;
import com.rli.scripts.customobjects.googledirectory.GoogleObject;
import com.rli.util.djava.ScriptHelper;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.service.adap.core.exception.POSTParsingException;

public class GoogleMember extends GoogleObject {

	public GoogleMember(GoogleDirectoryHandler handler) {
		super(handler);
	}

	@Override
	public SearchResult lookupEntry(String dn, String filter,
			String[] attributesRequested) throws Exception {
		String memberKey = ScriptHelper.getRDNValue(dn);
		String groupKey = ScriptHelper
				.getRDNValue(ScriptHelper.getParentDN(dn));
		return generate(dir.members().get(groupKey, memberKey).execute(),
				filter, attributesRequested);
	}

	@Override
	public GoogleDirectoryEnumeration search(String baseDN, String filter, String[] attributes)
			throws Exception {
		return new GoogleDirectoryEnumeration(this, dir.members()
				.list(ScriptHelper.getRDNValue(baseDN)).setMaxResults(pageSize), filter,
				attributes, pageSize);
	}

	@Override
	public void delete(String dn) throws Exception {
		String memberKey = ScriptHelper.getRDNValue(dn);
		String groupKey = ScriptHelper
				.getRDNValue(ScriptHelper.getParentDN(dn));
		deleteMember(groupKey, memberKey);
	}

	@Override
	public void insert(String dn, Attributes attrs) throws Exception {
		String memberKey = ScriptHelper.getRDNValue(dn);
		String groupKey = ScriptHelper
				.getRDNValue(ScriptHelper.getParentDN(dn));
		insertMember(groupKey, memberKey);
	}

	@Override
	public void update(String dn, List<ModificationItem> modificationAttrs)
			throws Exception {
		// Only role update is enabled
		String memberKey = ScriptHelper.getRDNValue(dn);
		String groupKey = ScriptHelper
				.getRDNValue(ScriptHelper.getParentDN(dn));
		Member m = dir.members().get(groupKey, memberKey).execute();
		for (ModificationItem mod : modificationAttrs) {
			if (mod.getAttribute().getID().equals("role")
					&& mod.getModificationOp() == DirContext.REPLACE_ATTRIBUTE) {
				String newRole = ScriptHelper.getAttributeValue(mod
						.getAttribute()).toUpperCase();
				m.setRole(newRole);
				dir.members().update(groupKey, memberKey, m).execute();
			}
		}
	}

	@Override
	public SearchResult generate(GenericJson model, String filter,
			String[] attributes) throws JSONException, POSTParsingException,
			IOException {
		Member m = (Member) model;
		return new SearchResult(getDN(m.getEmail()), null, getAttributes(model));
	}

}
