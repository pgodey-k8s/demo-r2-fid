package com.rli.scripts.customobjects.googledirectory.googleobjects;

import java.io.IOException;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import com.google.api.client.json.GenericJson;
import com.google.api.services.admin.directory.model.Group;
import com.google.api.services.admin.directory.model.Member;
import com.google.api.services.admin.directory.model.Members;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryEnumeration;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryHandler;
import com.rli.scripts.customobjects.googledirectory.GoogleObject;
import com.rli.util.djava.ScriptHelper;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.service.adap.core.exception.POSTParsingException;

public class GoogleGroup extends GoogleObject {

	private final static String[] ATTRIBUTES_TYPE_LIST_ARRAY = new String[] { "aliases", "nonEditableAliases" };

	@SuppressWarnings("unchecked")
	public GoogleGroup(GoogleDirectoryHandler handler) {
		super(handler);
		attributes_type_list.addAll(ScriptHelper.convertArrayStrToList(ATTRIBUTES_TYPE_LIST_ARRAY));
	}

	@Override
	public SearchResult lookupEntry(String key, String filter, String[] attributesRequested) throws Exception {
		return generate(dir.groups().get(key).execute(), filter, attributesRequested);
	}

	@Override
	public GoogleDirectoryEnumeration search(String baseDN, String filter, String[] attributes)
			throws NoSuchMethodException, SecurityException, IOException {
		return new GoogleDirectoryEnumeration(this, dir.groups().list().setDomain(domain).setMaxResults(pageSize),
				filter, attributes, pageSize);
	}

	@Override
	public void delete(String key) throws Exception {
		dir.groups().delete(key).execute();
	}

	@Override
	public void insert(String key, Attributes attrs) throws Exception {
		Group g = new Group();
		g.setEmail(key);
		fillAttributes(attrs, g);
		dir.groups().insert(g).execute();
	}

	@Override
	public void update(String key, List<ModificationItem> modificationAttrs) throws Exception {
		Group g = dir.groups().get(key).execute();
		for (ModificationItem mod : modificationAttrs) {
			String attributeId = mod.getAttribute().getID();
			attributeId = attributeId.startsWith("[") && attributeId.endsWith("]")
					? attributeId.substring(1, attributeId.length() - 1) : attributeId;
			if ("uniqueMember".equalsIgnoreCase(attributeId)) {
				handleUniqueMemberModif(key, mod);
			} else {
				updateAttribute(g, mod);
			}
		}
		dir.groups().update(key, g).execute();
	}

	private void handleUniqueMemberModif(String groupEmail, ModificationItem mod) throws NamingException, IOException {
		switch (mod.getModificationOp()) {
		case DirContext.REMOVE_ATTRIBUTE:
			if (mod.getAttribute().size() > 0) {
				NamingEnumeration<?> userDns = mod.getAttribute().getAll();
				while (userDns.hasMore()) {
					Object userDn = userDns.next();
					String userKey = userDn instanceof byte[] ? new String((byte[]) userDn, "UTF-8")
							: userDn.toString();
					deleteMember(groupEmail, userKey);
				}
			} else {
				// Find all members of the group, and delete them.
				deleteAllMembers(groupEmail);
			}
			break;
		case DirContext.REPLACE_ATTRIBUTE: // remove all + add
			deleteAllMembers(groupEmail);
		case DirContext.ADD_ATTRIBUTE:
			NamingEnumeration<?> userDns = mod.getAttribute().getAll();
			while (userDns.hasMore()) {
				Object userDn = userDns.next();
				String userKey = userDn instanceof byte[] ? new String((byte[]) userDn, "UTF-8") : userDn.toString();
				insertMember(groupEmail, userKey);
			}
			break;
		}
	}

	private void deleteAllMembers(String groupEmail) throws IOException {
		Members members = dir.members().list(groupEmail).execute();
		if (members != null) {
			List<Member> membersList = members.getMembers();
			if (membersList != null) {
				for (Member m : membersList) {
					deleteMember(groupEmail, m.getEmail());
				}
			}
		}
	}

	@Override
	public SearchResult generate(GenericJson model, String filter, String[] attributes)
			throws JSONException, POSTParsingException, IOException {
		Group group = (Group) model;
		Attributes returnAttributes = getAttributes(model);
		if (group.getDirectMembersCount() > 0) {
			// The uniqueMember attribute is not part of the attributes for a
			// member in Google directory.
			// We add it to the return attributes by issuing a separate request
			// for the list of members with the current group email.
			Attribute uniqueMemberAttribute = new BasicAttribute("uniqueMember");
			Members members = dir.members().list(group.getEmail()).execute();
			if (members != null) {
				List<Member> membersList = members.getMembers();
				if (membersList != null) {
					for (Member m : membersList) {
						uniqueMemberAttribute.add(m.getEmail());
					}
					returnAttributes.put(uniqueMemberAttribute);
				}
			}
		}
		return new SearchResult(getDN(group.getEmail()), null, returnAttributes);
	}
}
