package com.rli.scripts.customobjects.workday30;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rli.scripts.customobjects.workdayhr30;
import com.rli.stubs.workday30_0.AddressDataType;
import com.rli.stubs.workday30_0.CommunicationMethodUsageDataType;
import com.rli.stubs.workday30_0.CommunicationUsageTypeReferenceType;
import com.rli.stubs.workday30_0.ContactDataType;
import com.rli.stubs.workday30_0.EmployeeDataType;
import com.rli.stubs.workday30_0.EmployeeGetType;
import com.rli.stubs.workday30_0.EmployeePersonalInfoUpdateType;
import com.rli.stubs.workday30_0.EmployeeReferenceType;
import com.rli.stubs.workday30_0.EmployeeType;
import com.rli.stubs.workday30_0.ExternalIntegrationIDReferenceDataType;
import com.rli.stubs.workday30_0.IDType;
import com.rli.stubs.workday30_0.InstantMessengerDataType;
import com.rli.stubs.workday30_0.InstantMessengerTypeReferenceType;
import com.rli.stubs.workday30_0.InternetEmailAddressDataType;
import com.rli.stubs.workday30_0.LastNameDataType;
import com.rli.stubs.workday30_0.NameDataType;
import com.rli.stubs.workday30_0.PersonDataType;
import com.rli.stubs.workday30_0.PersonalInfoDataType;
import com.rli.stubs.workday30_0.PhoneDeviceTypeReferenceType;
import com.rli.stubs.workday30_0.PhoneNumberDataType;
import com.rli.stubs.workday30_0.WorkerDataType;
import com.rli.stubs.workday30_0.WorkerPersonalInfoDataType;
import com.rli.stubs.workday30_0.WorkerType;
import com.rli.stubs.workday30_0.WorkersResponseDataType;
import com.rli.stubs.workday30_0.human_resources.HumanResourcesPort;
import com.rli.stubs.workday30_0.human_resources.ProcessingFaultMsg;
import com.rli.stubs.workday30_0.human_resources.ValidationFaultMsg;

public class Utils {
	private static Logger logger = LogManager.getLogger(Utils.class);

	// same as
	// https://support.onelogin.com/hc/en-us/articles/207294406-Workday-Bidirectional-Directory-Integration
	public static Attributes toAttributes(EmployeeDataType edt) {

		// = workerId
		String employeeId = edt.getEmployeeID();
		String userId = edt.getUserID();

		// fill attributes
		Attributes attributes = new BasicAttributes();

		Attribute attr = new BasicAttribute("workerid", employeeId);
		attributes.put(attr);

		if (userId != null) {
			attr = new BasicAttribute("userid", userId);
			attributes.put(attr);
		}

		List<PersonalInfoDataType> pidts = edt.getPersonalInfoData();
		if (pidts.size() == 0) {
			return attributes;
		}

		PersonDataType pdt = pidts.get(0).getPersonData();

		String legalFistName = null;
		String legalLastName = null;
		String preferredFirstName = null;
		String preferredLastName = null;
		List<NameDataType> names = pdt.getNameData();
		for (NameDataType name : names) {
			if (name.isIsLegal()) {
				legalFistName = name.getFirstName();

				List<LastNameDataType> lastNames = name.getLastName();
				if (lastNames.size() > 0) {
					legalLastName = lastNames.get(0).getValue();
				}
			}
			Boolean isPreferred = name.isIsPreferred();
			if (isPreferred != null && isPreferred) {
				preferredFirstName = name.getFirstName();

				List<LastNameDataType> lastNames = name.getLastName();
				if (lastNames.size() > 0) {
					preferredLastName = lastNames.get(0).getValue();
				}
			}
		}

		ContactDataType cdt = pdt.getContactData();

		String primaryWorkEmail = null;
		List<InternetEmailAddressDataType> emails = cdt.getInternetEmailAddressData();
		for (InternetEmailAddressDataType email : emails) {
			CommunicationUsageTypeReferenceType cutrt = email.getUsageData().getTypeReference().get(0);
			if (cutrt.getValue().equalsIgnoreCase("work") && cutrt.isPrimary()) {
				primaryWorkEmail = email.getEmailAddress();
				break;
			}
		}

		String primaryPhoneNumberWork = null;
		String primaryWorkPhoneDeviceType = null;
		String phoneNumberHome1 = null;
		String homePhone1DeviceType = null;
		List<PhoneNumberDataType> phones = cdt.getPhoneNumberData();
		for (PhoneNumberDataType phone : phones) {

			CommunicationMethodUsageDataType cmudt = phone.getUsageData();
			if (cmudt == null) {
				continue;
			}

			List<CommunicationUsageTypeReferenceType> cutrts = cmudt.getTypeReference();
			if (cutrts.size() == 0) {
				continue;
			}

			CommunicationUsageTypeReferenceType cutrt = cutrts.get(0);
			if ("work".equalsIgnoreCase(cutrt.getValue()) && cutrt.isPrimary()) {
				primaryPhoneNumberWork = phone.getPhoneNumber();

				PhoneDeviceTypeReferenceType pdtrt = phone.getPhoneDeviceTypeReference();
				if (pdtrt != null) {
					primaryWorkPhoneDeviceType = pdtrt.getPhoneDeviceTypeDescription();
				}

			} else if (cutrt.getValue().equalsIgnoreCase("home")) {
				phoneNumberHome1 = phone.getPhoneNumber();
				PhoneDeviceTypeReferenceType pdtrt = phone.getPhoneDeviceTypeReference();
				if (pdtrt != null) {
					homePhone1DeviceType = pdtrt.getPhoneDeviceTypeDescription();
				}
			}
		}

		String businessAddress = null;
		String businessAddressCity = null;
		String businessAddressState = null;
		String businessAddressZip = null;
		List<AddressDataType> addresses = cdt.getAddressData();
		for (AddressDataType address : addresses) {

			CommunicationMethodUsageDataType cmudt = address.getUsageData();
			if (cmudt == null) {
				continue;
			}

			List<CommunicationUsageTypeReferenceType> cutrts = cmudt.getTypeReference();
			if (cutrts.size() == 0) {
				continue;
			}

			CommunicationUsageTypeReferenceType cutrt = cutrts.get(0);
			if (cutrt.getValue().equalsIgnoreCase("business")) {
				businessAddress = address.getAddressLine().get(0).getValue();
				businessAddressCity = address.getMunicipality();
				businessAddressState = address.getRegion();
				businessAddressZip = address.getPostalCode();
				break;
			}
		}

		String primaryHomeMessengerProvider = null;
		String primaryHomeMessengerId = null;
		String homeMessenger1Provider = null;
		String homeMessenger1Id = null;

		List<InstantMessengerDataType> messengers = cdt.getInstantMessengerData();
		for (InstantMessengerDataType messenger : messengers) {

			CommunicationMethodUsageDataType cmudt = messenger.getUsageData();
			if (cmudt == null) {
				continue;
			}

			List<CommunicationUsageTypeReferenceType> cmudts = cmudt.getTypeReference();
			if (cmudts.size() == 0) {
				continue;
			}

			CommunicationUsageTypeReferenceType cutrt = cmudts.get(0);
			if (cutrt.getValue().equalsIgnoreCase("home") && cutrt.isPrimary()) {
				InstantMessengerTypeReferenceType imtrt = messenger.getInstantMessengerTypeReference();
				if (imtrt != null) {
					primaryHomeMessengerProvider = imtrt.getInstantMessengerProvider();
				}

				primaryHomeMessengerId = messenger.getInstantMessengerAddress();

			} else if (cutrt.getValue().equalsIgnoreCase("home")) {
				InstantMessengerTypeReferenceType imtrt = messenger.getInstantMessengerTypeReference();
				if (imtrt != null) {
					homeMessenger1Provider = imtrt.getInstantMessengerProvider();
				}

				homeMessenger1Id = messenger.getInstantMessengerAddress();
			}
		}

		if (legalFistName != null) {
			attr = new BasicAttribute("legalfirstname", legalFistName);
			attributes.put(attr);
		}

		if (legalLastName != null) {
			attr = new BasicAttribute("legallastname", legalLastName);
			attributes.put(attr);
		}

		if (preferredFirstName != null) {
			attr = new BasicAttribute("preferredfirstname", preferredFirstName);
			attributes.put(attr);
		}

		if (preferredLastName != null) {
			attr = new BasicAttribute("preferredlastname", preferredLastName);
			attributes.put(attr);
		}

		if (primaryWorkEmail != null) {
			attr = new BasicAttribute("primaryworkemail", primaryWorkEmail);
			attributes.put(attr);
		}

		if (primaryPhoneNumberWork != null) {
			attr = new BasicAttribute("primaryphonenumberwork", primaryPhoneNumberWork);
			attributes.put(attr);
		}

		if (primaryWorkPhoneDeviceType != null) {
			attr = new BasicAttribute("primaryworkphonedevicetype", primaryWorkPhoneDeviceType);
			attributes.put(attr);
		}

		if (phoneNumberHome1 != null) {
			attr = new BasicAttribute("phonenumberhome1", phoneNumberHome1);
			attributes.put(attr);
		}

		if (homePhone1DeviceType != null) {
			attr = new BasicAttribute("homephone1devicetype", homePhone1DeviceType);
			attributes.put(attr);
		}

		if (businessAddress != null) {
			attr = new BasicAttribute("businessaddress", businessAddress);
			attributes.put(attr);
		}

		if (businessAddressCity != null) {
			attr = new BasicAttribute("businessaddresscity", businessAddressCity);
			attributes.put(attr);
		}

		if (businessAddressState != null) {
			attr = new BasicAttribute("businessaddressstate", businessAddressState);
			attributes.put(attr);
		}

		if (businessAddressZip != null) {
			attr = new BasicAttribute("businessaddresszip", businessAddressZip);
			attributes.put(attr);
		}

		if (primaryHomeMessengerProvider != null) {
			attr = new BasicAttribute("primaryhomemessengerprovider", primaryHomeMessengerProvider);
			attributes.put(attr);
		}

		if (primaryHomeMessengerId != null) {
			attr = new BasicAttribute("primaryhomemessengerid", primaryHomeMessengerId);
			attributes.put(attr);
		}

		if (homeMessenger1Provider != null) {
			attr = new BasicAttribute("homemessenger1provider", homeMessenger1Provider);
			attributes.put(attr);
		}

		if (homeMessenger1Id != null) {
			attr = new BasicAttribute("homemessenger1id", homeMessenger1Id);
			attributes.put(attr);
		}

		return attributes;
	}

	public static EmployeePersonalInfoUpdateType toUpdate(Map<String, String> attributes, String employeeId,
			PersonDataType originalPdt) {
		EmployeePersonalInfoUpdateType epiut = new EmployeePersonalInfoUpdateType();

		// employee reference
		EmployeeReferenceType ert = new EmployeeReferenceType();
		epiut.setEmployeeReference(ert);
		ExternalIntegrationIDReferenceDataType eiirdt = new ExternalIntegrationIDReferenceDataType();
		ert.setIntegrationIDReference(eiirdt);
		IDType idType = new IDType();
		eiirdt.setID(idType);
		idType.setSystemID(workdayhr30.systemID);
		idType.setValue(employeeId);

		// info
		WorkerPersonalInfoDataType wpidt = new WorkerPersonalInfoDataType();
		epiut.setEmployeePersonalInfoData(wpidt);
		PersonalInfoDataType pidt = new PersonalInfoDataType();
		wpidt.setPersonalInfoData(pidt);
		pidt.setPersonData(originalPdt);

		if (attributes.containsKey("legalfirstname") || attributes.containsKey("legallastname")) {
			NameDataType ndt = null;
			boolean foundLegal = false;
			for (NameDataType x : originalPdt.getNameData()) {
				if (x.isIsLegal()) {
					foundLegal = true;
					break;
				}
			}

			if (!foundLegal) {
				ndt = new NameDataType();
				ndt.setIsLegal(true);
				originalPdt.getNameData().add(ndt);
			}

			for (NameDataType x : originalPdt.getNameData()) {
				if (x.isIsLegal()) {
					ndt = x;
					break;
				}
			}

			ndt.setFirstName(attributes.get("legalfirstname"));

			if (attributes.containsKey("legallastname")) {

				if (ndt.getLastName().size() == 0) {
					ndt.getLastName().add(new LastNameDataType());

				}
				ndt.getLastName().get(0).setValue(attributes.get("legallastname"));
			}
		}

		if (attributes.containsKey("primaryworkemail")) {
			ContactDataType cdt;
			if (originalPdt.getContactData() == null) {
				originalPdt.setContactData(new ContactDataType());
			}

			cdt = originalPdt.getContactData();

			boolean foundPrimaryWork = false;
			for (InternetEmailAddressDataType x : cdt.getInternetEmailAddressData()) {
				CommunicationUsageTypeReferenceType cutrt = x.getUsageData().getTypeReference().get(0);
				if (cutrt.getValue().equalsIgnoreCase("work") && cutrt.isPrimary()) {
					foundPrimaryWork = true;
					break;
				}
			}

			if (!foundPrimaryWork) {
				InternetEmailAddressDataType ieadt = new InternetEmailAddressDataType();
				cdt.getInternetEmailAddressData().add(ieadt);
				CommunicationMethodUsageDataType cmudat = new CommunicationMethodUsageDataType();
				ieadt.setUsageData(cmudat);
				CommunicationUsageTypeReferenceType cutrt = new CommunicationUsageTypeReferenceType();
				cmudat.getTypeReference().add(cutrt);
				cutrt.setPrimary(true);
				cutrt.setValue("work");
			}

			InternetEmailAddressDataType ieadt = null;
			for (InternetEmailAddressDataType x : cdt.getInternetEmailAddressData()) {
				CommunicationUsageTypeReferenceType cutrt = x.getUsageData().getTypeReference().get(0);
				if (cutrt.getValue().equalsIgnoreCase("work") && cutrt.isPrimary()) {
					ieadt = x;
					break;
				}
			}

			ieadt.setEmailAddress(attributes.get("primaryworkemail"));
		}
		return epiut;
	}

	public static Map<String, String> toMap(List<ModificationItem> modifs) throws NamingException {
		Map<String, String> result = new HashMap<String, String>();
		for (ModificationItem modif : modifs) {
			if (modif.getModificationOp() == DirContext.REMOVE_ATTRIBUTE) {
				continue;
			}

			String key = modif.getAttribute().getID();
			String value = (String) modif.getAttribute().get();
			result.put(key, value);
		}

		return result;
	}

	public static EmployeeDataType getEmployee(String employeeId, HumanResourcesPort hrp)
			throws ProcessingFaultMsg, ValidationFaultMsg {
		EmployeeGetType employeeGetType = new EmployeeGetType();
		EmployeeReferenceType employeeReferenceType = new EmployeeReferenceType();
		ExternalIntegrationIDReferenceDataType eid = new ExternalIntegrationIDReferenceDataType();
		IDType idt = new IDType();
		idt.setSystemID(workdayhr30.systemID);
		idt.setValue(employeeId);
		eid.setID(idt);
		employeeReferenceType.setIntegrationIDReference(eid);
		employeeGetType.setEmployeeReference(employeeReferenceType);

		EmployeeType employee = hrp.getEmployee(employeeGetType);

		List<EmployeeDataType> employeeDataType = employee.getEmployeeData();
		if (employeeDataType != null && employeeDataType.size() > 0) {
			return employeeDataType.get(0);
		}

		return null;
	}

	public static Attributes getEmployeeAttributes(String employeeId, HumanResourcesPort hrp)
			throws ProcessingFaultMsg, ValidationFaultMsg {
		EmployeeDataType edt = Utils.getEmployee(employeeId, hrp);

		String xml = null;
		try {
			xml = toXML(edt);
		} catch (JAXBException e) {
			// for log
		}

		logger.debug("EmployeeDataType: " + xml);
		if (edt != null) {

			logger.debug("Try to convert to LDAP attributes");
			Attributes attributes = toAttributes(edt);
			logger.debug("Converted to LDAP attributes");
			return attributes;
		}

		return null;
	}

	public static PersonDataType getOrigPdt(String employeeId, HumanResourcesPort hrp)
			throws ProcessingFaultMsg, ValidationFaultMsg {
		EmployeeDataType edt = Utils.getEmployee(employeeId, hrp);

		if (edt.getPersonalInfoData().size() == 0) {
			return new PersonDataType();
		}

		if (edt.getPersonalInfoData().get(0).getPersonData() == null) {
			return new PersonDataType();
		}

		return edt.getPersonalInfoData().get(0).getPersonData();
	}

	public static List<String> toIDList(WorkersResponseDataType workerResponseDataType) {

		List<String> result = new ArrayList<>();
		if (workerResponseDataType == null) {
			return result;
		}

		List<WorkerType> wts = workerResponseDataType.getWorker();
		boolean first = true;
		for (WorkerType wt : wts) {

			WorkerDataType wdt = wt.getWorkerData();
			if (wdt == null || wdt.getWorkerID() == null) {
				continue;
			}

			if (first) {
				String xml = null;
				try {
					xml = toXML(wt);
				} catch (JAXBException e) {
					// for log
				}
				logger.debug(xml);
				first = false;
			}

			result.add(wdt.getWorkerID());
		}

		return result;
	}

	public static <T> String toXML(T object) throws JAXBException {
		StringWriter stringWriter = new StringWriter();

		JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// format the XML output
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		QName qName = new QName("", "");
		JAXBElement<T> root = new JAXBElement<T>(qName, (Class<T>) object.getClass(), object);

		jaxbMarshaller.marshal(root, stringWriter);

		String result = stringWriter.toString();

		return result;
	}

	public static void main(String[] args) throws Exception {

		WorkerType wt = new WorkerType();
		WorkerDataType wdt = new WorkerDataType();
		wt.setWorkerData(wdt);
		wdt.setWorkerID("toto");

		System.out.println(toXML(wt));

		EmployeeDataType edt = new EmployeeDataType();
		edt.setEmployeeID("toto");

		System.out.println(toXML(edt));
	}
}
