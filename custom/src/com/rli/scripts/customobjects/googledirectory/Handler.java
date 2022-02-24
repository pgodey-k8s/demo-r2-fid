package com.rli.scripts.customobjects.googledirectory;

import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

public interface Handler {

	public SearchResult lookupEntry(String dn, String filter,
			String[] attributesRequested) throws Exception;

	public GoogleDirectoryEnumeration search(String baseDN, String filter, String[] attributes) throws Exception;

	public void delete(String dn) throws Exception;

	public void insert(String dn, Attributes attrs) throws Exception;

	public void update(String dn, List<ModificationItem> modificationAttrs)
			throws Exception;

}
