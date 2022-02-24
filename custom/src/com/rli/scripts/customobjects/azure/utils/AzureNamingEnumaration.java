package com.rli.scripts.customobjects.azure.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;

import com.rli.scripts.customobjects.azure.services.HttpRequestHandler;
import com.rli.scripts.customobjects.azure.services.JSONDataParser;
import com.rli.slapd.filter.JDAPFilter;
import com.rli.util.RLIConstants;
import com.rli.util.jndi.vdap.LDAPFilter;
import com.rli.vds.util.InterceptParam;
import com.rli.web.http.json.JSONArray;

public class AzureNamingEnumaration implements NamingEnumeration<SearchResult> {

	private List<SearchResult> searchResults = new ArrayList<SearchResult>();

	private int sizelimit;

	private int currentIndex = 1;

	private InterceptParam param;

	private static final int MAX_PAGE_SIZE = 100;

	private int resultsSize = 0;

	private int pageNumber = 1;

	private ArrayList<String> attlist = null;

	private boolean isClosed = false;

	private static final String ATTR_ID = "ObjectId";

	private String scimfilter = null;

	public static HashMap<Integer, String> pageMap = null;

	public AzureNamingEnumaration(int sizelimit, InterceptParam param)
			throws Exception {

		this.sizelimit = sizelimit;
		this.param = param;

		buildFilter();
		getNextPage();
	}

	private void buildFilter() throws Exception {

		if (!param.getFilter().equalsIgnoreCase(LDAPFilter.DEFAULT_FILTER)) {
			String filter = param.getFilter();

			JDAPFilter jdapFilter = JDAPFilter.getFilter(filter);
			attlist = (ArrayList<String>) jdapFilter.getAttributeNames();
			// build list of required attributes from filtered attributes
			// (except objectclass)
			scimfilter = jdapFilter.toSCIM();
		}
	}

	@Override
	public void close() throws NamingException {
		isClosed = true;
		searchResults.clear();

	}

	@Override
	public boolean hasMore() throws NamingException {
		return searchResults.size() > 0;
	}

	@Override
	public SearchResult next() throws NamingException {

		if (searchResults.size() > 0) {

			SearchResult sr = searchResults.remove(0);
			if (searchResults.size() == 0)
				try {
					if (!isClosed)
						getNextPage();
				} catch (Exception e) {
					new NamingException();
				}
			return sr;
		} else
			return null;
	}

	@Override
	public boolean hasMoreElements() {

		try {
			return hasMore();
		} catch (NamingException e) {
			return false;
		}
	}

	@Override
	public SearchResult nextElement() {
		try {
			return next();
		} catch (NamingException e) {
			return null;
		}
	}

	private void getNextPage() throws Exception {

		if (isClosed)
			return;

		try {
			/**
			 * If the pageMap is not instantiated, instantiate it.
			 */
			if (pageMap == null) {
				pageMap = new HashMap<Integer, String>();
			}
			String scimResponse = "";
			// create the entries vector
			int count = MAX_PAGE_SIZE;
			if (sizelimit > 0) {
				count = sizelimit - resultsSize;
				if (count > MAX_PAGE_SIZE)
					count = MAX_PAGE_SIZE;
				if (count == 0) {
					isClosed = true;
					return;
				}
			}
			StringBuilder queryString = null;
			if (pageNumber == 1) {
				queryString = new StringBuilder("$top=" + count);
			} else if (pageMap.containsKey(new Integer(pageNumber))) {
				queryString = new StringBuilder("$top=" + count
						+ "&$skiptoken=" + pageMap.get(new Integer(pageNumber)));
			}
			if (scimfilter != null)
				queryString.append("&$filter=" + scimfilter);

			if (!param.getName().contains("group"))
				scimResponse = HttpRequestHandler.handleRequest("/Users",
						queryString.toString(), null);
			else
				scimResponse = HttpRequestHandler.handleRequest("/Groups",
						queryString.toString(), null);

			/**
			 * Get an array of JSON Objects by parsing the response.
			 */
			JSONArray userArray = null;

			userArray = JSONDataParser.parseJSonDataCollection(scimResponse);
			String skipTokenForNextPage = JSONDataParser
					.parseSkipTokenForNextPage(scimResponse);

			for (int i = 0; i < userArray.length(); i++) {

				String id = userArray.getJSONObject(i).optString("ObjectId");

				// prepare attributes...
				Attributes attributes = new BasicAttributes();
				String value = null;
				// get requested attribute list
				if (attlist != null) {
					for (int j = 0; j < attlist.size(); j++) {
						value = userArray.getJSONObject(i).optString(
								attlist.get(j));
						System.out.println("Attribute Name :" + attlist.get(j)
								+ " and Value : " + value);
						Attribute attribute = new BasicAttribute(
								attlist.get(j), value);
						attributes.put(attribute);
					}
				}

				// ID
				Attribute attribute = new BasicAttribute(ATTR_ID, id);
				attributes.put(attribute);

				attributes.put(attribute);
				String dn = param.getTypename() + RLIConstants.EQUAL + id;

				searchResults.add(new SearchResult(dn, null, attributes));

				resultsSize++;
			}
			/**
			 * If the skiptoken for the next page is not empty, that is there is
			 * indeed a next page, set the hasNextPage attribute to true and put
			 * this skiptoken to the pagemap..
			 */
			if (!skipTokenForNextPage.equalsIgnoreCase("")) {
				pageMap.put(new Integer(pageNumber + 1), skipTokenForNextPage);

			}
			pageNumber++;
			System.out.println("resultset Size : " + searchResults.size());
			if (searchResults.size() < count)
				isClosed = true;
			currentIndex = currentIndex + searchResults.size();

		} catch (Exception e) {
			isClosed = true;
			throw e;
		}
	}

}
