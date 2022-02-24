package com.rli.scripts.customobjects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.SearchResult;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rli.scripts.customobjects.workday30.HrpCache;
import com.rli.scripts.customobjects.workday30.SCIMNamingEnumeration;
import com.rli.scripts.customobjects.workday30.Utils;
import com.rli.scripts.customobjects.workday30.WorkdayAttributes;
import com.rli.scripts.customobjects.workday30.WorkdayUtils;
import com.rli.slapd.server.LDAPException;
import com.rli.stubs.workday30_0.BusinessProcessParametersType;
import com.rli.stubs.workday30_0.ChangeOtherIDsBusinessProcessDataType;
import com.rli.stubs.workday30_0.ChangeOtherIDsRequestType;
import com.rli.stubs.workday30_0.ChangeOtherIDsResponseType;
import com.rli.stubs.workday30_0.CustomIDDataType;
import com.rli.stubs.workday30_0.CustomIDType;
import com.rli.stubs.workday30_0.CustomIDTypeObjectIDType;
import com.rli.stubs.workday30_0.CustomIDTypeObjectType;
import com.rli.stubs.workday30_0.CustomIdentificationDataType;
import com.rli.stubs.workday30_0.CustomIdentifierReferenceObjectIDType;
import com.rli.stubs.workday30_0.CustomIdentifierReferenceObjectType;
import com.rli.stubs.workday30_0.EffectiveAndUpdatedDateTimeDataType;
import com.rli.stubs.workday30_0.GetWorkersRequestType;
import com.rli.stubs.workday30_0.GetWorkersResponseRootType;
import com.rli.stubs.workday30_0.GetWorkersResponseType;
import com.rli.stubs.workday30_0.PersonDataType;
import com.rli.stubs.workday30_0.ResponseFilterType;
import com.rli.stubs.workday30_0.TransactionLogCriteriaType;
import com.rli.stubs.workday30_0.WorkerObjectIDType;
import com.rli.stubs.workday30_0.WorkerObjectType;
import com.rli.stubs.workday30_0.WorkerRequestCriteriaType;
import com.rli.stubs.workday30_0.WorkerType;
import com.rli.stubs.workday30_0.WorkersResponseDataType;
import com.rli.stubs.workday30_0.human_resources.HumanResourcesPort;
import com.rli.stubs.workday30_0.human_resources.ProcessingFaultMsg;
import com.rli.stubs.workday30_0.human_resources.ValidationFaultMsg;
import com.rli.synsvcs.common.ConnString2;
import com.rli.util.djava.ScriptHelper;
import com.rli.vds.util.InterceptParam;
import com.rli.vds.util.UserDefinedInterception2;

public class workdayhr30 implements UserDefinedInterception2 {

	private static Logger logger = LogManager.getLogger(workdayhr30.class);

	// https://wd5-impl-services1.workday.com/ccx/service/optiv1/Human_resources/v26.0

	public static final String systemID = "wd-emplid";

	@Override
	public void authenticate(InterceptParam prop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compare(InterceptParam prop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(InterceptParam prop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void insert(InterceptParam prop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void invoke(InterceptParam prop) {
		// TODO Auto-generated method stub

	}

	public static Attributes workerToAttributes(WorkerType wt, HumanResourcesPort hrp) {

		boolean customIDRequired = true;

		Attributes attributes = WorkdayAttributes.setAttributes(wt);

		String employeeID = wt.getWorkerData().getWorkerID();

		Attribute singleAttribute = new BasicAttribute("workerid", employeeID);
		attributes.put(singleAttribute);

		// Moving this to separate method to
		// check if custom
		// field is required or not.

		customIDRequired = isCustomFieldRequired(wt, attributes);

		if (customIDRequired) {
			// Move this to separate method
			// - which updates
			// custom field on workday
			// record.

			updateCustomRecordAndAttribute(wt, employeeID, hrp, attributes);
		}

		return attributes;
	}

	@Override
	public void select(InterceptParam prop) {

		try {
			logger.debug("In select");
			// datasource info
			String url = prop.getConnectionstringUrl();
			String username = prop.getConnectionstringUsername();
			String password = prop.getConnectionstringPassword();

			logger.debug("Url: " + url);
			logger.debug("Username: " + username);
			logger.debug("Password length: " + password.length());

			// to build the ldap response
			String baseDn = prop.getDn();
			String scope = prop.getScope();

			logger.debug("Try to get HumanResourcesPort");
			HumanResourcesPort hrp = HrpCache.getHrp(url, username, password);

			logger.debug("Got HumanResourcesPort");

			// internal timestamp logic for capture connector
			String ldapFilter = prop.getFilter();

			if (ldapFilter != null && ldapFilter.toLowerCase().startsWith("updatedfrom=")) {

				String updatedFrom = ldapFilter.substring("updatedfrom=".length());
				SCIMNamingEnumeration sne = new SCIMNamingEnumeration();

				// bean for search params
				GetWorkersRequestType gwrt = new GetWorkersRequestType();

				WorkerRequestCriteriaType wrc = new WorkerRequestCriteriaType();
				gwrt.setRequestCriteria(wrc);

				TransactionLogCriteriaType tlct = new TransactionLogCriteriaType();
				wrc.getTransactionLogCriteriaData().add(tlct);

				EffectiveAndUpdatedDateTimeDataType eaudtdt = new EffectiveAndUpdatedDateTimeDataType();
				tlct.setTransactionDateRangeData(eaudtdt);
				eaudtdt.setUpdatedFrom(WorkdayUtils.stringToGreg(updatedFrom));

				// do the soap search
				GetWorkersResponseRootType gwrrt;
				try {
					gwrrt = hrp.getWorkers(gwrt);
				} catch (ProcessingFaultMsg | ValidationFaultMsg e) {
					handleException(prop, e);
					return;
				}

				WorkersResponseDataType wrdt = gwrrt.getResponseData();
				List<WorkerType> wts = wrdt.getWorker();
				for (WorkerType wt : wts) {
					Attributes attrs = workerToAttributes(wt, hrp);
					sne.addResult(new SearchResult(baseDn, null, attrs));
				}

				prop.setResultSet_Object(sne);
				return;
			}
			SCIMNamingEnumeration sne = new SCIMNamingEnumeration();
			prop.setResultSet_Object(sne);
			if (scope.equals("base")) {

				// get the id
				String id = ScriptHelper.getRDNValue(baseDn);

				logger.debug("Base. Id: " + id);

				Attributes attributes;
				try {
					attributes = Utils.getEmployeeAttributes(id, hrp);
				} catch (ProcessingFaultMsg | ValidationFaultMsg e) {
					handleException(prop, e);
					return;
				}

				if (attributes != null) {
					sne.addResult(new SearchResult(baseDn, null, attributes));
				}

			} else {
				logger.debug("Sub.");
				int sizeLimit = prop.getSizelimit();

				// Set the current date/time
				GregorianCalendar cal = new GregorianCalendar();
				XMLGregorianCalendar xmlCal;
				try {
					xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
				} catch (DatatypeConfigurationException e) {
					handleException(prop, e);
					return;
				}

				// Create a "request" object
				GetWorkersRequestType request = new GetWorkersRequestType();

				// Set the date/time & page parameters in the request
				ResponseFilterType responseFilter = new ResponseFilterType();
				responseFilter.setAsOfEntryDateTime(xmlCal);
				responseFilter.setAsOfEffectiveDate(xmlCal);

				responseFilter.setPage(BigDecimal.valueOf(1));
				responseFilter.setCount(BigDecimal.valueOf(sizeLimit));
				request.setResponseFilter(responseFilter);

				// Submit the request creating the "response" object
				GetWorkersResponseType response = null;
				try {
					logger.debug("Trying to get Workers");
					response = hrp.getWorkers(request);
					logger.debug("Got Workers: " + response);
				} catch (Exception e) {
					handleException(prop, e);
					return;
				}

				List<String> ids = Utils.toIDList(response.getResponseData());
				logger.debug("Sub. Id size: " + ids.size());

				logger.debug("Trying to convert Workday attributes to LDAP");
				for (String id : ids) {
					Attributes attributes;
					try {
						attributes = Utils.getEmployeeAttributes(id, hrp);
					} catch (ProcessingFaultMsg | ValidationFaultMsg e) {
						handleException(prop, e);
						return;
					}

					if (attributes != null) {
						sne.addResult(new SearchResult("Employee=" + id + "," + prop.getDn(), null, attributes));
					}
				}
				logger.debug("Converted Workday attributes to LDAP");
			}
		} catch (Exception e) {
			logger.error(e);
			handleException(prop, e);
		}
	}

	private static List<WorkerType> iterateWorkers(WorkersResponseDataType workerResponseDataType) {
		List<WorkerType> workersList = new ArrayList<WorkerType>();
		List<WorkerType> workerResponseList = null;
		if (workerResponseDataType != null) {
			workerResponseList = workerResponseDataType.getWorker();
		}

		if (workerResponseList != null && workerResponseList.size() > 0) {
			for (WorkerType workerType : workerResponseList) {
				if (workerType != null && workerType.getWorkerData() != null
						&& workerType.getWorkerReference() != null) {
					workersList.add(workerType);
				}
			}
		}
		return workersList;
	}

	public static boolean isCustomFieldRequired(WorkerType worker, Attributes attributes) {

		return false;
		// boolean customIDRequired = true;
		//
		// PersonalInformationDataType personalInformationDataType = worker
		// .getWorkerData().getPersonalData();
		// if (personalInformationDataType != null) {
		// PersonIdentificationDataType personIdentificationDataType =
		// personalInformationDataType
		// .getIdentificationData();
		// if (personIdentificationDataType != null) {
		// List<CustomIDType> customIDTypeArray = worker.getWorkerData()
		// .getPersonalData().getIdentificationData()
		// .getCustomID();
		//
		// for (CustomIDType customIDTypeLocal : customIDTypeArray) {
		// if (customIDTypeLocal != null) {
		// CustomIDDataType customIDDataType = customIDTypeLocal
		// .getCustomIDData();
		//
		// CustomIDTypeObjectType customIDTypeObjectType = customIDDataType
		// .getIDTypeReference();
		// String descriptor = customIDTypeObjectType
		// .getDescriptor();
		// if (descriptor != null
		// && descriptor.equalsIgnoreCase("SAILPOINT ID")) {
		// List<UniqueIdentifierObjectIDType> uniqueIdentifierObjectIDTypeArray
		// = customIDTypeLocal
		// .getCustomIDReference().getID();
		// if (uniqueIdentifierObjectIDTypeArray != null) {
		// for (UniqueIdentifierObjectIDType uniqueIdentifierObjectIDType :
		// uniqueIdentifierObjectIDTypeArray) {
		// String type = uniqueIdentifierObjectIDType
		// .getType();
		// if (type != null
		// && type.equalsIgnoreCase("WID")) {
		// String customUniqueWID = uniqueIdentifierObjectIDType
		// .getValue();
		// System.out
		// .println("customUniqueWID--->"
		// + customUniqueWID);
		// // Add the custom Unique WID to
		// // attribute
		//
		// Attribute customAttr = new BasicAttribute(
		// "SID", customIDDataType.getID());
		// attributes.put(customAttr);
		// customIDRequired = false;
		// }
		// }
		// }
		// } else if (descriptor != null
		// && descriptor.equalsIgnoreCase("NetSuite ID")) {
		// List<UniqueIdentifierObjectIDType> uniqueIdentifierObjectIDTypeArray
		// = customIDTypeLocal
		// .getCustomIDReference().getID();
		// if (uniqueIdentifierObjectIDTypeArray != null) {
		// for (UniqueIdentifierObjectIDType uniqueIdentifierObjectIDType :
		// uniqueIdentifierObjectIDTypeArray) {
		// String type = uniqueIdentifierObjectIDType
		// .getType();
		// if (type != null
		// && type.equalsIgnoreCase("WID")) {
		// String customUniqueWID = uniqueIdentifierObjectIDType
		// .getValue();
		// System.out
		// .println("customUniqueWID--->"
		// + customUniqueWID);
		// // Add the custom Unique WID to
		// // attribute
		//
		// Attribute customAttr = new BasicAttribute(
		// "NetSuiteID",
		// customIDDataType.getID());
		// attributes.put(customAttr);
		//
		// }
		// }
		// }
		// }
		//
		// }
		// }
		// }
		// } else {
		// customIDRequired = false;
		// }
		//
		// return customIDRequired;
	}

	public static void updateCustomRecordAndAttribute(WorkerType worker, String employeeID, HumanResourcesPort hrport,
			Attributes attributes) {

		ChangeOtherIDsRequestType changeOtherIDsRequest = new ChangeOtherIDsRequestType();

		BusinessProcessParametersType businessProcessParametersType = new BusinessProcessParametersType();

		businessProcessParametersType.setAutoComplete(true);
		businessProcessParametersType.setRunNow(true);

		ChangeOtherIDsBusinessProcessDataType changeOtherIDsBusinessProcessDataType = new ChangeOtherIDsBusinessProcessDataType();

		// Prepare Worker Reference
		WorkerObjectType updateWorkerObjType = new WorkerObjectType();
		WorkerObjectIDType updateWorkerObjectIdType = new WorkerObjectIDType();

		updateWorkerObjectIdType.setType("Employee_ID");
		// updateWorkerObjectIdType.setValue("761871");
		updateWorkerObjectIdType.setValue(employeeID);

		updateWorkerObjType.getID().add(updateWorkerObjectIdType);

		// Set the Worker Reference
		changeOtherIDsBusinessProcessDataType.setWorkerReference(updateWorkerObjType);

		// Custom Identification Data
		CustomIdentificationDataType customIdentificationDataType = new CustomIdentificationDataType();
		customIdentificationDataType.setReplaceAll(false);

		CustomIDType customIDType = new CustomIDType();
		customIDType.setDelete(false);

		CustomIDDataType customIDDataType = new CustomIDDataType();

		// Generate unique id - 8 digit.
		String uniqueID = generateRandomString();// "A5671N1"; // Call generate
													// method for random string
		customIDDataType.setID(uniqueID);

		// ID_Type_Reference
		CustomIDTypeObjectType customIDTypeObjectType = new CustomIDTypeObjectType();

		// For ID
		CustomIDTypeObjectIDType customIDTypeObjectIDType = new CustomIDTypeObjectIDType();
		customIDTypeObjectIDType.setValue("SAILPOINT_ID");
		customIDTypeObjectIDType.setType("Custom_ID_Type_ID");

		customIDTypeObjectType.getID().add(customIDTypeObjectIDType);
		customIDDataType.setIDTypeReference(customIDTypeObjectType);

		customIDType.setCustomIDData(customIDDataType);

		changeOtherIDsBusinessProcessDataType.setCustomIdentificationData(customIdentificationDataType);

		CustomIdentifierReferenceObjectType customIdentifierReferenceObjectType = new CustomIdentifierReferenceObjectType();
		CustomIdentifierReferenceObjectIDType customIdentifierReferenceObjectIDType = new CustomIdentifierReferenceObjectIDType();

		// customIdentifierReferenceObjectIDType.setType("WID");
		// //
		// customIdentifierReferenceObjectIDType.setValue("4bec987f329f10797d413a3a1cd910b8");
		// customIdentifierReferenceObjectIDType.setValue("4bec987f329f107a21c70bdd05aa17b3");
		//
		//
		// customIdentifierReferenceObjectType.getID().add(customIdentifierReferenceObjectIDType);
		//
		// customIDType.setCustomIDSharedReference(customIdentifierReferenceObjectType);

		// add to custom identification data
		customIdentificationDataType.getCustomID().add(customIDType);

		ChangeOtherIDsRequestType changeOtherIDsRequestType = new ChangeOtherIDsRequestType();
		changeOtherIDsRequestType.setBusinessProcessParameters(businessProcessParametersType);

		changeOtherIDsRequestType.setChangeOtherIDsData(changeOtherIDsBusinessProcessDataType);

		changeOtherIDsRequest.setBusinessProcessParameters(businessProcessParametersType);

		try {

			ChangeOtherIDsResponseType changeOtherIDsResponse = hrport.changeOtherIDs(changeOtherIDsRequestType);

			if (changeOtherIDsResponse != null) {
				// Add the generated unique numner to result set- attributes
				Attribute customAttr = new BasicAttribute("SID", uniqueID);
				attributes.put(customAttr);
			}
		} catch (Exception customRecordException) {
			customRecordException.printStackTrace();
		}

	}

	public static String generateRandomString() {

		String alphaCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String numberCharacters = "0123456789";
		Random ran = new Random();
		String firstPart = generateString(ran, alphaCharacters, 1);
		String secondPart = generateString(ran, numberCharacters, 6);

		return firstPart + secondPart;
	}

	public static String generateString(Random rng, String characters, int length) {
		char[] text = new char[length];
		for (int i = 0; i < length; i++) {
			text[i] = characters.charAt(rng.nextInt(characters.length()));
		}
		return new String(text);
	}

	@Override
	public void update(InterceptParam prop) {

		String url = prop.getConnectionstringUrl();
		String username = prop.getConnectionstringUsername();
		String password = prop.getConnectionstringPassword();

		String id = ScriptHelper.getRDNValue(prop.getDn());
		logger.debug("Update. Id: " + id);

		try {
			HumanResourcesPort hrp = HrpCache.getHrp(url, username, password);
			PersonDataType pdt = Utils.getOrigPdt(id, hrp);
			Map<String, String> attributes = Utils.toMap(prop.getModifications());
			Utils.toUpdate(attributes, id, pdt);

		} catch (Exception e) {
			prop.setErrorcode(LDAPException.OPERATION_ERROR);
			prop.setStatusFailed();
			prop.setErrormessage(e.getMessage());
		}
	}

	public static void main(String[] args) throws Exception {
		InterceptParam ip = new InterceptParam();
		ConnString2 conn = new ConnString2();
		conn.setUrl("https://<workdayUrl>/Human_Resources/v26.0");
		conn.setUsername("username");
		conn.setPassword("password");
		ip.setConnectionstring(conn);

		workdayhr30 wd = new workdayhr30();
		wd.select(ip);

	}

	private static void handleException(InterceptParam param, Exception e) {
		param.setErrorcode(LDAPException.OPERATION_ERROR);
		param.setStatusFailed();
		param.setErrormessage(e.getMessage());
	}
}
