package com.rli.scripts.customobjects.googledirectory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

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
import org.codehaus.jackson.map.ObjectMapper;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.Member;
import com.rli.util.StringOperations;
import com.rli.util.djava.ScriptHelper;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.service.adap.core.exception.POSTParsingException;
import com.rli.web.http.service.adap.core.util.JSONParser;

public abstract class GoogleObject implements Handler {

	protected String customerId = "my_customer";
	protected final static String subdocument_token = "--";
	protected HashSet<String> attributes_type_list = new HashSet<String>();

	public GoogleObject(GoogleDirectoryHandler handler) {
		this.dir = handler.getDir();
		this.domain = handler.getDomain();
		this.pageSize = handler.getPageSize();
		this.metaDN = handler.getMetaDN();
	}

	protected Directory dir;
	protected String domain;
	protected int pageSize;
	protected String metaDN;

	protected String getDN(String key) {
		return StringOperations.replaceAll(metaDN, "@", ScriptHelper.escapeRDNValue(key));
	}

	protected Object getDN(String object_metaDN, String key) {
		return StringOperations.replaceAll(object_metaDN, "@", ScriptHelper.escapeRDNValue(key));
	}

	protected Attributes getAttributes(GenericJson googleModel)
			throws JSONException, POSTParsingException, IOException {
		return JSONParser.parseComplex(normalizeJSONentry("void", googleModel)).getAttributes();
	}

	protected void fillAttributes(Attributes attrs, GenericJson model) throws NamingException, JsonParseException,
			JsonMappingException, UnsupportedEncodingException, IOException {
		NamingEnumeration<? extends Attribute> allattrs = attrs.getAll();
		while (allattrs.hasMore()) {
			Attribute att = allattrs.next();
			addAttribute(model, att);
		}
	}

	protected void addAttribute(GenericJson model, Attribute att) throws NamingException, JsonParseException,
			JsonMappingException, UnsupportedEncodingException, IOException {
		String attname = att.getID();
		attname = attname.startsWith("[") && attname.endsWith("]") ? attname.substring(1, attname.length() - 1)
				: attname;
		if (attname.indexOf(subdocument_token) > 0)
			if (attname.endsWith(subdocument_token))
				attname = attname.substring(0, attname.length() - subdocument_token.length());
			else
				throw new UnsupportedOperationException(
						"The attribute " + att.getID() + " is not allowed to be modified. You may modify the attribute "
								+ attname.substring(0, attname.indexOf(subdocument_token)) + "-- instead.");
		if (attributes_type_list.contains(attname.toLowerCase())) {
			NamingEnumeration<?> allvalues = att.getAll();
			@SuppressWarnings("unchecked")
			List<Object> valuelist = model.get(attname) != null ? (List<Object>) model.get(attname)
					: new ArrayList<Object>();
			while (allvalues.hasMore()) {
				Object val = allvalues.next();
				if (val instanceof byte[])
					val = new String((byte[]) val, "UTF-8");
				valuelist.add(normalizeValueObject(val));
			}
			model.set(attname, valuelist);
		} else
			model.set(attname, att.get());
	}

	protected void removeAttribute(GenericJson model, Attribute att) throws NamingException, JsonParseException,
			JsonMappingException, UnsupportedEncodingException, IOException {
		String attrName = att.getID();
		attrName = attrName.startsWith("[") && attrName.endsWith("]") ? attrName.substring(1, attrName.length() - 1)
				: attrName;
		int index = attrName.indexOf(subdocument_token);
		if (index > 0)
			if (attrName.endsWith(subdocument_token))
				attrName = attrName.substring(0, attrName.length() - subdocument_token.length());
			else
				throw new UnsupportedOperationException(
						"The attribute " + att.getID() + " is not allowed to be modified. You may modify the attribute "
								+ attrName.substring(0, index) + "-- instead.");
		if (att.size() > 0 && attributes_type_list.contains(attrName.toLowerCase())) {
			NamingEnumeration<?> allAttrValues = att.getAll();
			@SuppressWarnings("unchecked")
			List<Object> valuelist = (List<Object>) model.get(attrName);
			while (allAttrValues.hasMore()) {
				Object nextValue = allAttrValues.next();
				if (nextValue instanceof byte[])
					nextValue = new String((byte[]) nextValue, "UTF-8");
				Object valueToRemove = normalizeValueObject(nextValue);
				valuelist.removeIf(value -> isValueEqual(value, valueToRemove));
			}
			model.set(attrName, valuelist);
		} else
			model.set(attrName, null);
	}

	protected void updateAttribute(GenericJson model, ModificationItem mod) throws NamingException, JsonParseException,
			JsonMappingException, UnsupportedEncodingException, IOException {
		switch (mod.getModificationOp()) {
		case DirContext.REMOVE_ATTRIBUTE:
			removeAttribute(model, mod.getAttribute());
			break;
		case DirContext.REPLACE_ATTRIBUTE: // remove+add
			removeAttribute(model, new BasicAttribute(mod.getAttribute().getID()));
		case DirContext.ADD_ATTRIBUTE:
			addAttribute(model, mod.getAttribute());
			break;
		}
	}

	protected void insertMember(String group_email, String user_primaryEmail) throws IOException {
		try {
			dir.members().insert(group_email, new Member().setRole("MEMBER").setEmail(user_primaryEmail)).execute();
		} catch (GoogleJsonResponseException e) {
			// we intercept code 409 Conflict exception, if member already exist
			// then it's fine.
			if (e.getStatusCode() != 409)
				throw e;
		}
	}

	protected void deleteMember(String group_email, String user_primaryEmail) throws IOException {
		try {
			dir.members().delete(group_email, user_primaryEmail).execute();
		} catch (GoogleJsonResponseException e) {
			// we intercept code 404 Unfound exception, if member does not exist
			// then it's fine.
			if (e.getStatusCode() != 404)
				throw e;
		}
	}

	private boolean isValueEqual(Object value, Object removevalue) {
		if (value instanceof ArrayMap && removevalue instanceof ArrayMap) {
			@SuppressWarnings("unchecked")
			ArrayMap<String, ?> v = (ArrayMap<String, ?>) value, rv = (ArrayMap<String, ?>) removevalue;
			boolean response = true;
			for (String key : rv.keySet()) {
				response = response && v.containsKey(key);
			}
			for (String key : v.keySet()) {
				response = response && rv.containsKey(key);
			}
			for (Entry<String, ?> field : v.entrySet()) {
				response = response && field.getValue().equals(rv.get(field.getKey()));
			}
			return response;
		}
		return value.equals(removevalue);
	}

	protected Object normalizeValueObject(Object value)
			throws JsonParseException, JsonMappingException, UnsupportedEncodingException, IOException {
		if (value != null && value.toString().startsWith("{"))
			return new ObjectMapper().readValue(value.toString().getBytes("UTF-8"), ArrayMap.class);
		else
			return value;
	}

	private String normalizeJSONentry(String dn, GenericJson attributes) throws IOException {
		return "{params={dn=\"" + dn + "\", attributes=" + attributes.toPrettyString() + "}}";
	}

	public abstract SearchResult generate(GenericJson model, String filter, String[] attributes)
			throws JSONException, POSTParsingException, IOException;
}
