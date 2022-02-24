package com.rli.scripts.customobjects.gcontacts;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import com.google.gdata.client.Query;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.BaseEntry;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.contacts.ContactGroupEntry;
import com.google.gdata.data.contacts.ContactGroupFeed;
import com.google.gdata.data.contacts.GroupMembershipInfo;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.PhoneNumber;
import com.google.gdata.data.extensions.PostalAddress;
import com.rli.scripts.customobjects.api.WSEnumeration;
import com.rli.scripts.customobjects.api.WSHandler;
import com.rli.util.djava.ScriptHelper;
import com.rli.util.jndi.vdap.compound.CompoundObject;

public class GdataHandler extends WSHandler {

	// BACKEND ATTRIBUTES
	private final static String ATTR_ID = "id";
	private final static String ATTR_NAME = "name";
	private final static String ATTR_EMAIL_HOME = "homeemail";
	private final static String ATTR_EMAIL_WORK = "workemail";
	private final static String ATTR_EMAIL_OTHER = "otheremail";
	private final static String ATTR_EMAIL_PRIMARY = "primaryemail";
	private static final String ATTR_ADDRESS_WORK = "workpostaladdress";
	private static final String ATTR_ADDRESS_HOME = "homepostaladdress";
	private static final String ATTR_ADDRESS_OTHER = "otherpostaladdress";
	private static final String ATTR_PHONE_WORK = "workphonenumber";
	private static final String ATTR_PHONE_HOME = "homephonenumber";
	private static final String ATTR_PHONE_MOBILE = "mobilephonenumber";
	private static final String ATTR_PHONE_OTHER = "otherphonenumber";
	private final static String ATTR_GROUP = "group";
	
	private final static String ATTR_MEMBERS = "members";
	
//	private static final String ATTR_PICTURE_URL = "pictureUrl";
	
	private static final String DEFAULT_FEED = "https://www.google.com/m8/feeds/";
	private static final String DEFAULT_PROJECTION = "thin";
	private static final String SERVICE_NAME = "radiantlogic-vds";
	private static final String DEFAULT_GROUP_NAME = "Contacts";
	
	private ContactsService service;
	private URL feedUrl;
	
	
	public GdataHandler(CompoundObject co) throws Exception{
		
		super(co);
		
	    if (userName == null || password == null)
	    	throw new Exception("Both username and password must be specified");
	    
	    // GET SERVICE
	    service = new ContactsService(SERVICE_NAME);
	    service.setUserCredentials(userName, password);
	    
	    // CREATE URL	    
	    String url = DEFAULT_FEED + (isGroup()?"groups/":"contacts/") + userName + "/" + DEFAULT_PROJECTION;
	    feedUrl = new URL(url);
	    	    
	}
	
	@Override
	public SearchResult lookupEntry(String dn,String filter, String[] attributesRequested) throws Exception{
		return lookup(getGoogleEntryFromDN(dn), attributesRequested);
	}
	
	@Override
	public WSEnumeration search(String filter, String[] attributes){
		return new GdataEnumeration(this,filter, attributes,50);
	}
	
	private SearchResult lookup(BaseEntry entry,String[] attributesRequested) throws Exception{
		
		if(entry!=null){
			if(entry instanceof ContactGroupEntry)
				return generate((ContactGroupEntry)entry, attributesRequested);
			else
				return generate((ContactEntry)entry, attributesRequested);
		}
				
		return null;
	}
	
	private BaseEntry lookup(String googleId) throws Exception{
		
		if(isGroup()){
			ContactGroupEntry entry=service.getEntry(
					new URL(feedUrl.toString()+"/"+googleId),ContactGroupEntry.class);
			if(entry!=null)
				return entry;
		}
		else{
			ContactEntry entry=service.getEntry(
					new URL(feedUrl.toString()+"/"+googleId),ContactEntry.class);
			if(entry!=null)
				return entry;
		}
		
		return null;
	}
	
	URL getFeedUrl(){
		return feedUrl;
	}

	ContactsService getService(){
		return service;
	}
	
	/**
	 * extract the google id from the googleID URL
	 * @param googleIdURL
	 * @return
	 */
	private static String extractId(String googleIdURL){
		int posSep=googleIdURL.lastIndexOf('/');
		if(posSep!=-1)
			return googleIdURL.substring(googleIdURL.lastIndexOf('/')+1);
		else
			return googleIdURL;
	}
	
	/**
	 * generate a group searchresult
	 * @param group
	 * @param attributesRequested
	 * @return
	 * @throws Exception
	 */
	public SearchResult generate(ContactGroupEntry group,String[] attributesRequested) throws Exception{
		Attributes attributes=new BasicAttributes();
		
		// GET THE KEY
		String internalId=null;
		
		String keyName=getKeyName();
			
		// DEFAULT KEY FROM THE GOOGLE ID
		if(keyName.equalsIgnoreCase(ATTR_ID)){
			internalId=extractId(group.getId());
			attributes.put(new BasicAttribute(getVirtualName(ATTR_ID), internalId));
		}

		boolean isKey=false;
		// NAME
		String nameAttrName = getVirtualName(ATTR_NAME);
		if (nameAttrName != null 
				&& 
				((internalId==null && (isKey=keyName.equalsIgnoreCase(nameAttrName))) || 
					(attributesRequested == null || ScriptHelper.contains(
							attributesRequested, nameAttrName, true)))) {
			String title = null;
			
			if (group.hasSystemGroup())
				title=group.getSystemGroup().getId();
			else
				title=group.getTitle().getPlainText();
			
			if (!title.equals(""))
				attributes.put(new BasicAttribute(nameAttrName, title));
			
			if(isKey){
				internalId=title;
				isKey=false;
			}
		}
				
		// FETCH MEMBERS ?
		String memberAttrName=getVirtualName(ATTR_MEMBERS);
		if (memberAttrName!=null && (attributesRequested == null || ScriptHelper.contains(attributesRequested, memberAttrName,true))){
			// GET CONNECTION FOR USERS COMPOUND
			CompoundObject userCo=findUserCompoundObject();
			if(userCo==null)
				throw new Exception("Can't find the user node the custom view");
			GdataHandler usersConnection=new GdataHandler(userCo);
			List<String> members=usersConnection.fetchMembersDN(group.getId());
			if(members!=null & members.size()>0){
				Attribute membersAttr=new BasicAttribute(memberAttrName);
				for (String memberDn : members) {
					membersAttr.add(memberDn);
				}
				
				attributes.put(membersAttr);
			}
		}
		
		if(internalId==null)
			return null;
		
		return prepareSearchResult(internalId, attributes);
	}
	
	/**
	 * generate the user search result
	 * @param contact
	 * @param attributesRequested
	 * @return
	 */
	public SearchResult generate(ContactEntry contact, String[] attributesRequested) throws Exception{
		Attributes attributes = new BasicAttributes();

		// GET THE KEY
		String internalId=null;
		
		String keyName=getKeyName();
			
		// DEFAULT KEY FROM THE GOOGLE ID
		if(keyName.equalsIgnoreCase(ATTR_ID)){
			internalId=extractId(contact.getId());
			attributes.put(new BasicAttribute(getVirtualName(ATTR_ID), internalId));
		}

		boolean isKey=false;
		// NAME
		String nameAttrName = getVirtualName(ATTR_NAME);
		if (nameAttrName != null 
				&& 
				((internalId==null && (isKey=keyName.equalsIgnoreCase(nameAttrName))) || 
					(attributesRequested == null || ScriptHelper.contains(
							attributesRequested, nameAttrName, true)))) {
			String title = contact.getTitle().getPlainText();
			if (!title.equals(""))
				attributes.put(new BasicAttribute(nameAttrName, title));
			
			if(isKey){
				internalId=title;
				isKey=false;
			}
		}

		// GROUP
		String groupAttrName = getVirtualName(ATTR_GROUP);
		if (groupAttrName != null &&
				(attributesRequested == null || ScriptHelper.contains(
						attributesRequested, groupAttrName, true))) {
			List<GroupMembershipInfo> groupMembershipInfos = contact.getGroupMembershipInfos();
			if (groupMembershipInfos!=null){
				Attribute groupAttr=new BasicAttribute(groupAttrName);
				for (GroupMembershipInfo groupMembershipInfo : groupMembershipInfos) {
					String groupIdURL=groupMembershipInfo.getHref();
					
					// RESOLVE THE DN
					CompoundObject groupCo=findGroupCompoundObject();
					String groupDN=getDNFromGoogleId(groupCo, extractId(groupIdURL));
					if(groupDN!=null)
						groupAttr.add(groupDN);
				}
				attributes.put(groupAttr);
			}
			
		}
		
		// EMAIL ADDRESSES
		if (contact.hasEmailAddresses()) {
			Attribute attributeEmailHome = null;
			Attribute attributeEmailWork = null;
			Attribute attributeEmailOther = null;
			for (Email email : contact.getEmailAddresses()) {
				
				// PRIMARY EMAIL
				if (email.getPrimary()) {
					String emailAttrName = getVirtualName(ATTR_EMAIL_PRIMARY);
					if(emailAttrName!=null
							&& ((internalId==null && (isKey=keyName.equalsIgnoreCase(emailAttrName))) ||
									 (attributesRequested == null || ScriptHelper
											.contains(attributesRequested,
													emailAttrName, true)))) {
						attributes.put(new BasicAttribute(
								emailAttrName, email.getAddress()));
						if(isKey){
							internalId=email.getAddress();
							isKey=false;
						}
					}
				}
				
				String rel;
				if ((rel = email.getRel()) != null) {
					if (rel.equals(Email.Rel.HOME)) {
						String emailAttrName = getVirtualName(ATTR_EMAIL_HOME);

						if (emailAttrName != null
								&& ((internalId==null && (isKey=keyName.equalsIgnoreCase(emailAttrName))) ||
								 (attributesRequested == null || ScriptHelper
										.contains(attributesRequested,
												emailAttrName, true)))) {
							if (attributeEmailHome == null) {
								attributeEmailHome = new BasicAttribute(
										emailAttrName, email.getAddress());
								attributes.put(attributeEmailHome);
							} else
								attributeEmailHome.add(email.getAddress());
							
							if(isKey){
								internalId=email.getAddress();
								isKey=false;
							}
						}
					} else if (rel.equals(Email.Rel.WORK)) {

						String emailAttrName = getVirtualName(ATTR_EMAIL_WORK);

						if (emailAttrName != null
								&& ((internalId==null && (isKey=keyName.equalsIgnoreCase(emailAttrName))) ||
										 (attributesRequested == null || ScriptHelper
												.contains(attributesRequested,
														emailAttrName, true)))) {
							if (attributeEmailWork == null) {
								attributeEmailWork = new BasicAttribute(
										emailAttrName, email.getAddress());
								attributes.put(attributeEmailWork);
							} else
								attributeEmailWork.add(email.getAddress());
							
							if(isKey){
								internalId=email.getAddress();
								isKey=false;
							}
						}
					} else if (rel.equals(Email.Rel.OTHER)) {
						String emailAttrName = getVirtualName(ATTR_EMAIL_OTHER);

						if (emailAttrName != null
								&& ((internalId==null && (isKey=keyName.equalsIgnoreCase(emailAttrName))) ||
										 (attributesRequested == null || ScriptHelper
												.contains(attributesRequested,
														emailAttrName, true)))) {
							if (attributeEmailOther == null) {
								attributeEmailOther = new BasicAttribute(
										emailAttrName, email.getAddress());
								attributes.put(attributeEmailOther);
							} else
								attributeEmailOther.add(email.getAddress());
							
							if(isKey){
								internalId=email.getAddress();
								isKey=false;
							}
						}
					}
				}
			}
		}
		
		if(internalId==null)
			return null;

		return prepareSearchResult(internalId, attributes);
	}
	
	/**
	 * fetch the members of a group and convert them to DN
	 * @param groupID
	 * @return
	 * @throws Exception
	 */
	private List<String> fetchMembersDN(String groupID) throws Exception{
		List<String> membersDN=new ArrayList<String>();
		
		int cursor=1;
		while(true){
			int membersFetched=fetchMembersDN(groupID, cursor, membersDN);
			if(membersFetched==0)
				break;
			else
				cursor+=membersFetched;
		}
		return membersDN;
	}
		
	private int fetchMembersDN(String groupID,int cursor,List<String> membersDN) throws Exception{
		Query query = new Query(getFeedUrl());
		query.setStartIndex(cursor);
		query.setMaxResults(100);
		query.setStringCustomParameter("group", groupID);
		
		ContactFeed contactFeed = getService().query(query, ContactFeed.class);
		for (ContactEntry entry : contactFeed.getEntries()) {
			membersDN.add(getDNFromGoogleId(findUserCompoundObject(), extractId(entry.getId())));
		}
		
		return contactFeed.getEntries().size();
	}
	
	private String getDNFromGoogleId(CompoundObject co,String googleId) throws Exception{
		GdataHandler connection=new GdataHandler(co);
		
		BaseEntry entry=connection.lookup(googleId);
		if(entry==null)
			return null;
		
		String keyName=connection.getKeyName();
		
		if(keyName.equalsIgnoreCase(ATTR_ID)){
			return connection.getDNFromInternalId(extractId(entry.getId()));
		}
		else if(keyName.equalsIgnoreCase(ATTR_NAME)){
			if(connection.isGroup() && ((ContactGroupEntry)entry).hasSystemGroup())
				return connection.getDNFromInternalId(((ContactGroupEntry)entry).getSystemGroup().getId());				
			else
				return connection.getDNFromInternalId(entry.getTitle().getPlainText());
		}
		else if(keyName.equalsIgnoreCase(ATTR_EMAIL_PRIMARY) && entry instanceof ContactEntry ){
			ContactEntry contactEntry=(ContactEntry)entry;
			if (contactEntry.hasEmailAddresses()) {
				List<Email> emails = contactEntry.getEmailAddresses();

				for (Email email : emails) {
					if (email.getPrimary()) {
						return connection.getDNFromInternalId(email.getAddress());
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * get the google ID (not URL) from a given DN
	 * @param dn
	 * @return
	 * @throws Exception
	 */
	private static BaseEntry getGoogleEntryFromDN(String dn) throws Exception{
		
		GdataHandler connection=new GdataHandler(findCompoundObject(dn));
		
		String keyName=connection.getKeyName();
		
		String rdnValue=ScriptHelper.getRDNValue(dn);
		
		if(keyName.equalsIgnoreCase(ATTR_ID)){
			String googleId=ScriptHelper.getRDNValue(dn);
			if(connection.isGroup()){
				ContactGroupEntry entry=connection.getService().getEntry(
						new URL(connection.getFeedUrl().toString()+"/"+googleId),ContactGroupEntry.class);
				if(entry!=null)
					return entry;
			}
			else{
				ContactEntry entry=connection.getService().getEntry(
						new URL(connection.getFeedUrl().toString()+"/"+googleId),ContactEntry.class);
				if(entry!=null)
					return entry;
			}
			
			return null; // NOT FOUND
		}
		else if(!connection.isGroup() && keyName.equalsIgnoreCase(ATTR_EMAIL_PRIMARY)){
			Query query = new Query(connection.getFeedUrl());
//			query.setMaxResults(1);
//			query.setStringCustomParameter("email", rdnValue);
			
			ContactFeed contactFeed = connection.getService().query(query, ContactFeed.class);
			for (ContactEntry entry : contactFeed.getEntries()) {
				if (entry.hasEmailAddresses()) {
					List<Email> emails = entry.getEmailAddresses();

					for (Email email : emails) {
						if (email.getPrimary()) {
							if(rdnValue.equalsIgnoreCase(email.getAddress()))
								return entry;
						}
					}
				}
			}
			
			return null; // NOT FOUND
		}
		else if(keyName.equalsIgnoreCase(ATTR_NAME)){
			Query query = new Query(connection.getFeedUrl());
//			query.setMaxResults(1);
//			query.setStringCustomParameter("title", rdnValue);
			
			if(connection.isGroup()){
				ContactGroupFeed contactGroupFeed = connection.getService().query(query, ContactGroupFeed.class);
				for (ContactGroupEntry entry : contactGroupFeed.getEntries()) {
					String name=null;
					if(entry.hasSystemGroup())
						name=entry.getSystemGroup().getId();
					else
						name=entry.getTitle().getPlainText();
					if(rdnValue.equalsIgnoreCase(name)) // NO WAY TO POST FILTER :(
						return entry;// entry.getId();
				}
			}
			else{
				ContactFeed contactFeed = connection.getService().query(query, ContactFeed.class);
				for (ContactEntry entry : contactFeed.getEntries()) {
					if(rdnValue.equalsIgnoreCase(entry.getTitle().getPlainText())) // NO WAY TO POST FILTER :(
						return entry;// entry.getId();
				}
			}
			return null; // NOT FOUND
		}
		
		throw new Exception(keyName+" not supported as a key");
		
	}

	@Override
	public void delete(String dn) throws Exception{
		BaseEntry entry=getGoogleEntryFromDN(dn);
		if(entry!=null)
			entry.delete();
		else
			throw new Exception("Can't find any entry in the underlying webservice for "+dn);
	}

	@Override
	public void insert(String dn, Attributes attrs)  throws Exception{
		if(isGroup()){
			// NEW GROUP
			ContactGroupEntry group = new ContactGroupEntry();

			Attribute groupNameAttr=attrs.get(getVirtualName(ATTR_NAME));
			if(groupNameAttr!=null)
				group.setTitle(new PlainTextConstruct((String) groupNameAttr.get(0)));
			
			// COMMIT
			group=service.insert(feedUrl, group);
			
			// UPDATE MEMBERS
			Attribute groupMemberAttr=attrs.get(getVirtualName(ATTR_MEMBERS));
			if(groupMemberAttr!=null){
				addGroupAttribute(group, groupMemberAttr);
			}
		}
		else{
			// NEW CONTACT
			ContactEntry contact = new ContactEntry();

			addContactAttributes(contact,attrs);

			service.insert(feedUrl, contact);
		}
	}
	
	private void addGroupAttribute(ContactGroupEntry group,Attribute attr)
			throws Exception {
		
		String name=getWSAttributeName(attr.getID());
		
		if(name.equalsIgnoreCase(ATTR_NAME)){
			if(group.hasSystemGroup()){
				throw new Exception("Name of system groups are read only");
			}
			else{
				group.setTitle(new PlainTextConstruct((String) attr.get(0)));
				group.update();
			}
		}
		else if(name.equalsIgnoreCase(ATTR_MEMBERS)){
			NamingEnumeration enu=attr.getAll();
			if(enu!=null){
				GdataHandler handler=new GdataHandler(userCompound);
				while(enu.hasMore()){
					Object obj=enu.next();
					String contactDN;
					if(obj instanceof byte[])
						contactDN=new String((byte[])obj);
					else
						contactDN=(String)obj;
					BaseEntry entry=getGoogleEntryFromDN(contactDN);
					if(entry!=null){
						ContactEntry contact=(ContactEntry) entry;
						GroupMembershipInfo groupMembershipInfo = new GroupMembershipInfo();
						groupMembershipInfo.setDeleted(false);
						groupMembershipInfo.setHref(group.getId());
						contact.addGroupMembershipInfo(groupMembershipInfo);
						contact.update();
					}
				}
			}
		}
		
	}
	
	private void removeGroupAttribute(ContactGroupEntry group,Attribute attr)
			throws Exception {
		
		String name=getWSAttributeName(attr.getID());
		
		if(name.equalsIgnoreCase(ATTR_NAME)){
			throw new Exception("Can't change the name attribute");
		}
		else if(name.equalsIgnoreCase(ATTR_MEMBERS)){
			NamingEnumeration enu=attr.getAll();
			if(enu!=null){
				GdataHandler handler=new GdataHandler(userCompound);
				while(enu.hasMore()){
					Object obj=enu.next();
					String contactDN;
					if(obj instanceof byte[])
						contactDN=new String((byte[])obj);
					else
						contactDN=(String)obj;
					BaseEntry entry=getGoogleEntryFromDN(contactDN);
					if(entry!=null){
						ContactEntry contact=(ContactEntry) entry;
						GroupMembershipInfo groupMembershipInfo = new GroupMembershipInfo();
						groupMembershipInfo.setDeleted(false);
						groupMembershipInfo.setHref(group.getId());
						List<GroupMembershipInfo> members=contact.getGroupMembershipInfos();
						members.remove(groupMembershipInfo);
						contact.update();
						
					}
				}
			}
		}
		
	}
	
	private void addContactAttributes(ContactEntry contact,Attributes attrs)
			throws Exception {
		

		NamingEnumeration attributesEnu = attrs.getAll();
		while (attributesEnu.hasMore()) {
			Attribute attr = (Attribute) attributesEnu.next();
			addContactAttribute(contact, attr);
		}
		
		// PRIMARY EMAIL
		if(attrs.get(getVirtualName(ATTR_EMAIL_PRIMARY))!=null){
			String mainEmail=(String)attrs.get(getVirtualName(ATTR_EMAIL_PRIMARY)).get(0);
			List<Email> emails = contact.getEmailAddresses();
			for (Email email : emails) {
				if(email.getAddress().equals(mainEmail))
					email.setPrimary(true);
				else
					email.setPrimary(false);
			}
		}
		
		// GROUP
//		Attribute groupAttr=attrs.get(getVirtualName(ATTR_GROUP));
//		if(groupAttr==null){
//			// DEFAULT TO MY CONTACTS
//			String myContactsId=getDefaultGroupId();
//			if(myContactsId!=null){
//				GroupMembershipInfo groupMembershipInfo = new GroupMembershipInfo();
//				groupMembershipInfo.setDeleted(false);
//				groupMembershipInfo.setHref(myContactsId);
//				contact.addGroupMembershipInfo(groupMembershipInfo);
//			}			
//		}
	}
	
	private void addContactAttribute(ContactEntry contact,Attribute attr) throws Exception{
		String name = attr.getID();
		name=getWSAttributeName(name);
		
		if(name==null)
			return;

		if (name.equalsIgnoreCase(ATTR_NAME)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				contact.setTitle(new PlainTextConstruct((String) value));
			}
		} else if (name.equalsIgnoreCase(ATTR_EMAIL_HOME)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<Email> emails = contact.getEmailAddresses();
				Email eMail = new Email();
				eMail.setAddress((String) value);
				eMail.setRel(Email.Rel.HOME);				
				emails.add(eMail);
			}
		} else if (name.equalsIgnoreCase(ATTR_EMAIL_WORK)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<Email> emails = contact.getEmailAddresses();
				Email eMail = new Email();
				eMail.setAddress((String) value);
				eMail.setRel(Email.Rel.WORK);				
				emails.add(eMail);
			}
		} else if (name.equalsIgnoreCase(ATTR_EMAIL_OTHER)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<Email> emails = contact.getEmailAddresses();
				Email eMail = new Email();
				eMail.setAddress((String) value);
				eMail.setRel(Email.Rel.OTHER);				
				emails.add(eMail);
			}
		} else if (name.equalsIgnoreCase(ATTR_PHONE_HOME)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<PhoneNumber> phones = contact.getPhoneNumbers();
				PhoneNumber phone = new PhoneNumber();
				phone.setPhoneNumber((String) value);
				phone.setRel(PhoneNumber.Rel.HOME);
				phones.add(phone);
			}
		} else if (name.equalsIgnoreCase(ATTR_PHONE_MOBILE)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<PhoneNumber> phones = contact.getPhoneNumbers();
				PhoneNumber phone = new PhoneNumber();
				phone.setPhoneNumber((String) value);
				phone.setRel(PhoneNumber.Rel.MOBILE);
				phones.add(phone);
			}
		} else if (name.equalsIgnoreCase(ATTR_PHONE_WORK)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<PhoneNumber> phones = contact.getPhoneNumbers();
				PhoneNumber phone = new PhoneNumber();
				phone.setPhoneNumber((String) value);
				phone.setRel(PhoneNumber.Rel.WORK);
				phones.add(phone);
			}
		} else if (name.equalsIgnoreCase(ATTR_PHONE_OTHER)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<PhoneNumber> phones = contact.getPhoneNumbers();
				PhoneNumber phone = new PhoneNumber();
				phone.setPhoneNumber((String) value);
				phone.setRel(PhoneNumber.Rel.OTHER);
				phones.add(phone);
			}
		} else if (name.equalsIgnoreCase(ATTR_ADDRESS_HOME)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<PostalAddress> addresses = contact.getPostalAddresses();
				PostalAddress address = new PostalAddress();
				address.setValue((String) value);
				address.setRel(PostalAddress.Rel.HOME);
				addresses.add(address);
			}
		} else if (name.equalsIgnoreCase(ATTR_ADDRESS_WORK)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<PostalAddress> addresses = contact.getPostalAddresses();
				PostalAddress address = new PostalAddress();
				address.setValue((String) value);
				address.setRel(PostalAddress.Rel.WORK);
				addresses.add(address);
			}
		} else if (name.equalsIgnoreCase(ATTR_ADDRESS_OTHER)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				List<PostalAddress> addresses = contact.getPostalAddresses();
				PostalAddress address = new PostalAddress();
				address.setValue((String) value);
				address.setRel(PostalAddress.Rel.OTHER);
				addresses.add(address);
			}
		} else if (name.equalsIgnoreCase(ATTR_GROUP)) {
			for (int i = 0; i < attr.size(); i++) {
				Object value = attr.get(i);
				String groupDN= (String)value;
				BaseEntry entry=getGoogleEntryFromDN(groupDN);
				if(entry!=null){
					GroupMembershipInfo groupMembershipInfo = new GroupMembershipInfo();
					groupMembershipInfo.setDeleted(false);
					groupMembershipInfo.setHref(entry.getId());
					contact.addGroupMembershipInfo(groupMembershipInfo);
				}
			}
		}
	}
	
	private void removeContactAttribute(ContactEntry contact, Attribute attr)
			throws Exception {

		String name = attr.getID();
		name=getWSAttributeName(name);
		
		boolean removeAll=attr.size()==0;
		
		boolean group=name.equalsIgnoreCase(ATTR_GROUP);
		
		List<Object> obj2Delete=new ArrayList<Object>();
		List<String> valuesToDelete=new ArrayList<String>();
		for (int i = 0; i < attr.size(); i++) {
			String value = (String)attr.get(i);
			if(group){
				BaseEntry entry=getGoogleEntryFromDN(value);
				if(entry!=null)
					valuesToDelete.add(entry.getId().toLowerCase());
			}
			else
				valuesToDelete.add(value.toLowerCase());
		}
		
		if (name.equalsIgnoreCase(ATTR_EMAIL_HOME)) {
			List<Email> emails = contact.getEmailAddresses();
			for (Email email : emails) {
				if(email.getRel().equals(Email.Rel.HOME) &&
						(removeAll || valuesToDelete.contains(email.getAddress().toLowerCase()))){
					obj2Delete.add(email);
				}
			}
			
			emails.removeAll(obj2Delete);
			obj2Delete.clear();
		} else if (name.equalsIgnoreCase(ATTR_EMAIL_WORK)) {
			List<Email> emails = contact.getEmailAddresses();
			for (Email email : emails) {
				if(email.getRel().equals(Email.Rel.WORK) &&
						(removeAll || valuesToDelete.contains(email.getAddress().toLowerCase()))){
					obj2Delete.add(email);
				}
			}
			
			emails.removeAll(obj2Delete);
			obj2Delete.clear();
		} else if (name.equalsIgnoreCase(ATTR_EMAIL_OTHER)) {
			List<Email> emails = contact.getEmailAddresses();
			for (Email email : emails) {
				if(email.getRel().equals(Email.Rel.OTHER) &&
						(removeAll || valuesToDelete.contains(email.getAddress().toLowerCase()))){
					obj2Delete.add(email);
				}
			}
			
			emails.removeAll(obj2Delete);
			obj2Delete.clear();
		} else if (name.equalsIgnoreCase(ATTR_PHONE_HOME)) {
			List<PhoneNumber> phones = contact.getPhoneNumbers();
			for (PhoneNumber phone: phones) {
				if(phone.getRel().equals(PhoneNumber.Rel.HOME) &&
						(removeAll || valuesToDelete.contains(phone.getPhoneNumber().toLowerCase()))){
					obj2Delete.add(phone);
				}
			}
			
			phones.removeAll(obj2Delete);
			obj2Delete.clear();
		} else if (name.equalsIgnoreCase(ATTR_PHONE_MOBILE)) {
			List<PhoneNumber> phones = contact.getPhoneNumbers();
			for (PhoneNumber phone: phones) {
				if(phone.getRel().equals(PhoneNumber.Rel.MOBILE) &&
						(removeAll || valuesToDelete.contains(phone.getPhoneNumber().toLowerCase()))){
					obj2Delete.add(phone);
				}
			}
			
			phones.removeAll(obj2Delete);
			obj2Delete.clear();
		} else if (name.equalsIgnoreCase(ATTR_PHONE_WORK)) {
			List<PhoneNumber> phones = contact.getPhoneNumbers();
			for (PhoneNumber phone: phones) {
				if(phone.getRel().equals(PhoneNumber.Rel.WORK) &&
						(removeAll || valuesToDelete.contains(phone.getPhoneNumber().toLowerCase()))){
					obj2Delete.add(phone);
				}
			}
			
			phones.removeAll(obj2Delete);
			obj2Delete.clear();
		} else if (name.equalsIgnoreCase(ATTR_PHONE_OTHER)) {
			List<PhoneNumber> phones = contact.getPhoneNumbers();
			for (PhoneNumber phone: phones) {
				if(phone.getRel().equals(PhoneNumber.Rel.OTHER) &&
						(removeAll || valuesToDelete.contains(phone.getPhoneNumber().toLowerCase()))){
					obj2Delete.add(phone);
				}
			}
			
			phones.removeAll(obj2Delete);
			obj2Delete.clear();
		} else if (name.equalsIgnoreCase(ATTR_ADDRESS_HOME)) {
			List<PostalAddress> addresses = contact.getPostalAddresses();
			for (PostalAddress address: addresses) {
				if(address.getRel().equals(PostalAddress.Rel.HOME) &&
						(removeAll || valuesToDelete.contains(address.getValue().toLowerCase()))){
					obj2Delete.add(address);
				}
			}
			
			addresses.removeAll(obj2Delete);
			obj2Delete.clear();
			
		} else if (name.equalsIgnoreCase(ATTR_ADDRESS_WORK)) {
			List<PostalAddress> addresses = contact.getPostalAddresses();
			for (PostalAddress address: addresses) {
				if(address.getRel().equals(PostalAddress.Rel.WORK) &&
						(removeAll || valuesToDelete.contains(address.getValue().toLowerCase()))){
					obj2Delete.add(address);
				}
			}
			
			addresses.removeAll(obj2Delete);
			obj2Delete.clear();
		} else if (name.equalsIgnoreCase(ATTR_ADDRESS_OTHER)) {
			List<PostalAddress> addresses = contact.getPostalAddresses();
			for (PostalAddress address: addresses) {
				if(address.getRel().equals(PostalAddress.Rel.OTHER) &&
						(removeAll || valuesToDelete.contains(address.getValue().toLowerCase()))){
					obj2Delete.add(address);
				}
			}
			
			addresses.removeAll(obj2Delete);
			obj2Delete.clear();
		}
		else if (name.equalsIgnoreCase(ATTR_GROUP)) {
			List<GroupMembershipInfo> groups=contact.getGroupMembershipInfos();
			for (GroupMembershipInfo groupMembershipInfo : groups) {
				if((removeAll || valuesToDelete.contains(groupMembershipInfo.getHref().toLowerCase()))){
					obj2Delete.add(groupMembershipInfo);
				}
			}
			
			groups.removeAll(obj2Delete);
			obj2Delete.clear();
		}
	}
	
	private static String getDefaultGroupId() throws Exception {
		
		GdataHandler connection=new GdataHandler(groupCompound);
		
		Query query = new Query(connection.getFeedUrl());
			
		ContactGroupFeed contactGroupFeed = connection.getService().query(query, ContactGroupFeed.class);
		for (ContactGroupEntry entry : contactGroupFeed.getEntries()) {
				String name=null;
				if(entry.hasSystemGroup())
					name=entry.getSystemGroup().getId();
				else
					name=entry.getTitle().getPlainText();
				
				if(name.equalsIgnoreCase(DEFAULT_GROUP_NAME))
					return entry.getId();
		}
		
		return null;
	}
	
	private void replaceGroupAttribute(BaseEntry entry,Attribute attr,String groupDN) throws Exception{
		String wsAttributeName=getWSAttributeName(attr.getID());
		if(ATTR_NAME.equalsIgnoreCase(wsAttributeName)){
			ContactGroupEntry groupEntry=(ContactGroupEntry)entry;
			if(groupEntry.hasSystemGroup())
				throw new Exception("Name of system groups are read only");
			else{
				if(attr.size()==1)
					groupEntry.setTitle(new PlainTextConstruct((String) attr.get(0)));
				else
					throw new Exception("Group name attribute is mono valued");
			}
			
			groupEntry.update();
		}
		else if(ATTR_MEMBERS.equalsIgnoreCase(wsAttributeName)){
			if(attr.size()==0){
				removeGroupAttribute((ContactGroupEntry)entry, attr);
			}
			else{
				
				// PREPARE NEW MEMBER LIST
				List<String> newMembers=new ArrayList<String>();
				NamingEnumeration attrEnu=attr.getAll();
				while(attrEnu!=null && attrEnu.hasMore()){
					Object obj=attrEnu.next();
					String contactDN;
					if(obj instanceof byte[])
						contactDN=new String((byte[])obj);
					else
						contactDN=(String)obj;
					newMembers.add(contactDN);
				}
				
				// PREPARE OLD MEMBER LIST
				List<String> oldMembers=new ArrayList<String>();
				SearchResult groupSr=lookupEntry(groupDN, null, null);
				if(groupSr!=null){
					Attribute oldMembersAttr=groupSr.getAttributes().get(attr.getID());
					if(oldMembersAttr!=null){
						NamingEnumeration membersEnu=oldMembersAttr.getAll();
						while(membersEnu!=null && membersEnu.hasMore()){
							Object obj=membersEnu.next();
							String contactDN;
							if(obj instanceof byte[])
								contactDN=new String((byte[])obj);
							else
								contactDN=(String)obj;
							oldMembers.add(contactDN);
						}
					}
				}
				
				// NOW COMPARE AND MODIFY THE ATTRIBUTE ONE BY ONE
				for (String newMember : newMembers) {
					if(!oldMembers.remove(newMember))
						addGroupAttribute((ContactGroupEntry)entry, new BasicAttribute(attr.getID(),newMember));
				}
				
				for (String oldMember : oldMembers) {
					removeGroupAttribute((ContactGroupEntry)entry, new BasicAttribute(attr.getID(),oldMember));							
				}
			}
		}
		else{
			removeGroupAttribute((ContactGroupEntry)entry, new BasicAttribute(attr.getID()));
			addGroupAttribute((ContactGroupEntry)entry, attr);				
		}
	}
	
	private void update(BaseEntry entry,ModificationItem modif,String groupDN) throws Exception{
		int modificationOp = modif.getModificationOp();
		Attribute attr = modif.getAttribute();
		
		if(entry instanceof ContactGroupEntry){
			switch (modificationOp) {
			case DirContext.ADD_ATTRIBUTE:
				addGroupAttribute((ContactGroupEntry)entry, attr);
				break;
			case DirContext.REMOVE_ATTRIBUTE:
				removeGroupAttribute((ContactGroupEntry)entry, attr);
				break;
			case DirContext.REPLACE_ATTRIBUTE:
				replaceGroupAttribute((ContactGroupEntry)entry, attr, groupDN);				
				break;

			}
		}
		else{
			switch (modificationOp) {
			case DirContext.ADD_ATTRIBUTE:
				addContactAttribute((ContactEntry)entry, attr);
				break;
			case DirContext.REMOVE_ATTRIBUTE:
				removeContactAttribute((ContactEntry)entry, attr);
				break;
			case DirContext.REPLACE_ATTRIBUTE:
				removeContactAttribute((ContactEntry)entry, new BasicAttribute(attr.getID()));
				addContactAttribute((ContactEntry)entry, attr);				
				break;

			}
			entry.update();
		}
	}

	@Override
	public void update(String dn, List<ModificationItem> modificationAttrs)  throws Exception{
		BaseEntry entry=getGoogleEntryFromDN(dn);
		for (ModificationItem modificationItem : modificationAttrs) {
			update(entry, modificationItem, dn);
		}
	}
	
	
}
