package com.rli.scripts.customobjects.googledirectory.googleobjects;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import com.google.api.client.json.GenericJson;
import com.google.api.services.admin.directory.model.Group;
import com.google.api.services.admin.directory.model.Groups;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryEnumeration;
import com.rli.scripts.customobjects.googledirectory.GoogleDirectoryHandler;
import com.rli.scripts.customobjects.googledirectory.GoogleObject;
import com.rli.util.djava.ScriptHelper;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.service.adap.core.exception.POSTParsingException;

public class GoogleUser extends GoogleObject {

	private final static String attribute_groups = "groups";
	private final static String[] ATTRIBUTES_TYPE_LIST_ARRAY = new String[] {
			"ims", "emails", "externalIds", "relations", "addresses",
			"organizations", "phones", "aliases", "nonEditableAliases" };
	private final static String name = "name";
	private final static String familyName = "familyName";
	private final static String givenName = "givenName";
	private HashSet<String> attributes_name = new HashSet<String>();
	private String groups_metaDN;

	@SuppressWarnings("unchecked")
	public GoogleUser(GoogleDirectoryHandler handler) {
		super(handler);
		groups_metaDN = handler.getGroupsMetaDN();
		attributes_type_list.addAll(ScriptHelper
				.convertArrayStrToList(ATTRIBUTES_TYPE_LIST_ARRAY));
		attributes_name.addAll(ScriptHelper.convertArrayStrToList(new String[] {
				name + subdocument_token,
				name + subdocument_token + familyName,
				name + subdocument_token + givenName }));
	}

	@Override
	public SearchResult lookupEntry(String key, String filter,
			String[] attributesRequested) throws Exception {
		return generate(dir.users().get(key).execute(), filter,
				attributesRequested);
	}

	@Override
	public GoogleDirectoryEnumeration search(String baseDN, String filter,
			String[] attributes) throws IOException, NoSuchMethodException,
			SecurityException {
		return new GoogleDirectoryEnumeration(this, dir.users().list()
				.setDomain(domain).setMaxResults(pageSize), filter, attributes,
				pageSize);
	}

	@Override
	public void delete(String key) throws Exception {
		dir.users().delete(key).execute();
	}

	@Override
	public void insert(String key, Attributes attrs) throws Exception {
		User u = new User();
		u.setPrimaryEmail(key);
		// password is mandatory, we set a dummy one in case no one is provided
		u.setPassword("password");
		// name property does not follow mapping pattern
		u.setName(removeUserName(attrs));
		// remove group attribute
		Attribute groups = attrs.remove(attribute_groups);
		fillAttributes(attrs, u);
		// create new user
		dir.users().insert(u).execute();
		// create group membership
		if (groups != null)
			handleGroupModif(key, new ModificationItem(
					DirContext.ADD_ATTRIBUTE, groups));
	}

	@Override
	public void update(String key, List<ModificationItem> modificationAttrs)
			throws Exception {
		User u = dir.users().get(key).execute();
		for (ModificationItem mod : modificationAttrs) {
			String attributeId = mod.getAttribute().getID();
			attributeId = attributeId.startsWith("[")
					&& attributeId.endsWith("]") ? attributeId.substring(1,
					attributeId.length() - 1) : attributeId;
			// intercept username modifications
			if (attributes_name.contains(attributeId))
				handleNameModif(u, mod);
			else if (attributeId.equals(attribute_groups))
				handleGroupModif(key, mod);
			else
				updateAttribute(u, mod);
		}
		dir.users().update(key, u).execute();
	}

	@Override
	public SearchResult generate(GenericJson model, String filter,
			String[] attributes) throws JSONException, POSTParsingException,
			IOException {
		User user = (User) model;
		Attributes attrs = getAttributes(model);
		// get groups
		Attribute groups = new BasicAttribute(attribute_groups);
		for (Group g : getGroups(user.getPrimaryEmail())) {
			groups.add(getDN(groups_metaDN, g.getEmail()));
		}
		attrs.put(groups);
		return new SearchResult(getDN(user.getPrimaryEmail()), null, attrs);
	}

	private void handleNameModif(User u, ModificationItem mod)
			throws JsonParseException, JsonMappingException,
			UnsupportedEncodingException, IOException {
		String attributeId = mod.getAttribute().getID();
		attributeId = attributeId.startsWith("[")
				&& attributeId.endsWith("]") ? attributeId.substring(1,
				attributeId.length() - 1) : attributeId;
		
		if (mod.getModificationOp() == DirContext.REPLACE_ATTRIBUTE
				|| mod.getModificationOp() == DirContext.ADD_ATTRIBUTE) {
			switch (attributeId) {
			case name + subdocument_token:
				Object nameobject = normalizeValueObject(ScriptHelper
						.getAttributeValue(mod.getAttribute()));
				if (nameobject != null && nameobject instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, String> namemap = (Map<String, String>) nameobject;
					u.setName(new UserName().setFamilyName(
							namemap.get(familyName)).setGivenName(
							namemap.get(givenName)));
				}
				break;
			case name + subdocument_token + givenName:
				u.setName(u.getName().setGivenName(
						ScriptHelper.getAttributeValue(mod.getAttribute())));
				break;
			case name + subdocument_token + familyName:
				u.setName(u.getName().setFamilyName(
						ScriptHelper.getAttributeValue(mod.getAttribute())));
				break;
			}
		}
	}

	private void handleGroupModif(String key, ModificationItem mod)
			throws NamingException, IOException {
		switch (mod.getModificationOp()) {
		case DirContext.REMOVE_ATTRIBUTE:
			if (mod.getAttribute().size() > 0) {
				NamingEnumeration<?> groupdns = mod.getAttribute().getAll();
				while (groupdns.hasMore()) {
					Object groupdn = groupdns.next();
					String groupkey = groupdn instanceof byte[] ? new String(
							(byte[]) groupdn, "UTF-8") : groupdn.toString();
					groupkey = ScriptHelper.getRDNValue(groupkey);
					deleteMember(groupkey, key);
				}
			} else {
				for (Group g : getGroups(key)) {
					deleteMember(g.getEmail(), key);
				}
			}
			break;
		case DirContext.REPLACE_ATTRIBUTE: // remove all + add
			for (Group g : getGroups(key)) {
				deleteMember(g.getEmail(), key);
			}
		case DirContext.ADD_ATTRIBUTE:
			NamingEnumeration<?> groupdns = mod.getAttribute().getAll();
			while (groupdns.hasMore()) {
				Object groupdn = groupdns.next();
				String groupkey = groupdn instanceof byte[] ? new String(
						(byte[]) groupdn, "UTF-8") : groupdn.toString();
				groupkey = ScriptHelper.getRDNValue(groupkey);
				insertMember(groupkey, key);
			}
			break;
		}
	}

	/**
	 * The number of groups fetchable is limited by [pagesize]. Only the first
	 * page is fetched.
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	private List<Group> getGroups(String key) throws IOException {
		Groups g = dir.groups().list().setUserKey(key).execute();
		return g != null && g.getGroups() != null ? g.getGroups()
				: new ArrayList<Group>();
	}

	private UserName removeUserName(final Attributes attrs) throws IOException,
			JsonParseException, JsonMappingException,
			UnsupportedEncodingException {
		String blank = "(void)", fam = blank, giv = blank, namejson = ScriptHelper
				.getAttributeValue(attrs.remove(name + subdocument_token));
		Attribute attr_fam = attrs
				.remove(name + subdocument_token + familyName), attr_giv = attrs
				.remove(name + subdocument_token + givenName);
		if (namejson != null) {
			@SuppressWarnings("unchecked")
			Map<String, String> namemap = (Map<String, String>) normalizeValueObject(namejson);
			fam = namemap.get(familyName);
			giv = namemap.get(givenName);
		}
		if (attr_fam != null)
			fam = ScriptHelper.getAttributeValue(attr_fam);
		if (attr_giv != null)
			giv = ScriptHelper.getAttributeValue(attr_giv);
		return new UserName().setFamilyName(fam).setGivenName(giv);
	}

}
