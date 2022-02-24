package com.rli.scripts.customobjects.workday30;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

public class SCIMNamingEnumeration implements NamingEnumeration<SearchResult> {

	List<SearchResult> srs = new ArrayList<SearchResult>();

	public void setResults(List<SearchResult> srs) {
		this.srs = srs;
	}

	public void addResult(SearchResult sr) {
		srs.add(sr);
	}

	@Override
	public boolean hasMoreElements() {
		return srs.size() > 0;
	}

	@Override
	public SearchResult nextElement() {
		if (srs.size() > 0) {
			SearchResult sr = srs.remove(0);

			return sr;
		} else {
			return null;
		}
	}

	@Override
	public SearchResult next() throws NamingException {
		if (srs.size() > 0) {
			SearchResult sr = srs.remove(0);

			return sr;
		} else {
			return null;
		}
	}

	@Override
	public boolean hasMore() throws NamingException {
		return srs.size() > 0;
	}

	@Override
	public void close() throws NamingException {
		// TODO Auto-generated method stub

	}

}
