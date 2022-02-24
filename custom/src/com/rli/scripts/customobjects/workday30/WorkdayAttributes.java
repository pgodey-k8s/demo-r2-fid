package com.rli.scripts.customobjects.workday30;

import java.util.List;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.xml.datatype.XMLGregorianCalendar;

import com.rli.stubs.workday30_0.AddressDataType;
import com.rli.stubs.workday30_0.AddressInformationDataType;
import com.rli.stubs.workday30_0.AddressLineDataType;
import com.rli.stubs.workday30_0.BiographicDataType;
import com.rli.stubs.workday30_0.ContactDataType;
import com.rli.stubs.workday30_0.ContactInformationDataType;
import com.rli.stubs.workday30_0.CountryObjectType;
import com.rli.stubs.workday30_0.CountryOfBirthReferenceType;
import com.rli.stubs.workday30_0.CountryReferenceType;
import com.rli.stubs.workday30_0.DisabilityReferenceType;
import com.rli.stubs.workday30_0.EmailAddressInformationDataType;
import com.rli.stubs.workday30_0.EmployeeDataType;
import com.rli.stubs.workday30_0.EventTargetTransactionLogEntryDataType;
import com.rli.stubs.workday30_0.GenderReferenceType;
import com.rli.stubs.workday30_0.InstantMessengerDataType;
import com.rli.stubs.workday30_0.JobFamilyBaseObjectType;
import com.rli.stubs.workday30_0.JobProfileInPositionSummaryDataType;
import com.rli.stubs.workday30_0.LastNameDataType;
import com.rli.stubs.workday30_0.LegalNameDataType;
import com.rli.stubs.workday30_0.LocationObjectType;
import com.rli.stubs.workday30_0.LocationSummaryDataType;
import com.rli.stubs.workday30_0.NameDataType;
import com.rli.stubs.workday30_0.PersonNameDataType;
import com.rli.stubs.workday30_0.PersonNameDetailDataType;
import com.rli.stubs.workday30_0.PersonalInfoDataType;
import com.rli.stubs.workday30_0.PersonalInformationDataType;
import com.rli.stubs.workday30_0.PhoneInformationDataType;
import com.rli.stubs.workday30_0.PhoneNumberDataType;
import com.rli.stubs.workday30_0.PositionDetailDataType;
import com.rli.stubs.workday30_0.PositionTimeTypeObjectType;
import com.rli.stubs.workday30_0.PositionTimeTypeReferenceType;
import com.rli.stubs.workday30_0.PositionWorkerTypeObjectType;
import com.rli.stubs.workday30_0.SubregionDataType;
import com.rli.stubs.workday30_0.WebAddressDataType;
import com.rli.stubs.workday30_0.WorkerEmploymentInformationDataType;
import com.rli.stubs.workday30_0.WorkerJobDataType;
import com.rli.stubs.workday30_0.WorkerObjectType;
import com.rli.stubs.workday30_0.WorkerPositionDataType;
import com.rli.stubs.workday30_0.WorkerType;

public class WorkdayAttributes {

	public static Attributes setAttributes(WorkerType workerType) {
		Attributes attributes = new BasicAttributes();
		Attribute attr = null;

		// transactionEntryMoment
		EventTargetTransactionLogEntryDataType event = workerType.getWorkerData().getTransactionLogEntryData();
		if (event != null) {
			XMLGregorianCalendar xgc = event.getTransactionLogEntry().get(0).getTransactionLogData()
					.getTransactionEntryMoment();
			String stringXgc = WorkdayUtils.gregToString(xgc);
			attributes.put(new BasicAttribute("transactionentrymoment", stringXgc));
		}

		PersonalInformationDataType personalInformationDataType = workerType.getWorkerData().getPersonalData();

		if (personalInformationDataType != null) {
			PersonNameDataType personNameDataType = personalInformationDataType.getNameData();
			if (personNameDataType != null) {
				LegalNameDataType legalNameDataType = personNameDataType.getLegalNameData();
				if (legalNameDataType != null) {

					PersonNameDetailDataType personalDetailDataType = legalNameDataType.getNameDetailData();

					// ContactInformationDataType contactInformationDataType
					// =
					// workerType.getWorkerData().getPersonalData().getContactData();

					attr = new BasicAttribute("firstName", personalDetailDataType.getFirstName());
					attributes.put(attr);
					attr = new BasicAttribute("lastName", personalDetailDataType.getLastName());
					attributes.put(attr);

					attr = new BasicAttribute("middleName", personalDetailDataType.getMiddleName());
					attributes.put(attr);

					attr = new BasicAttribute("reportingName", personalDetailDataType.getReportingName());
					attributes.put(attr);
					String workerDescriptor = workerType.getWorkerReference().getDescriptor();
					if (workerDescriptor != null) {
						attr = new BasicAttribute("workerDescriptor", workerType.getWorkerReference().getDescriptor());
						attributes.put(attr);

						if (workerDescriptor.contains("(Terminated)")) {
							attr = new BasicAttribute("status", "T");
							attributes.put(attr);
						} else {
							attr = new BasicAttribute("status", "A");
							attributes.put(attr);
						}
					}

					String userID = workerType.getWorkerData().getUserID();
					attr = new BasicAttribute("userID", userID);
					attributes.put(attr);

					ContactInformationDataType contactDataInformation = workerType.getWorkerData().getPersonalData()
							.getContactData();
					if (contactDataInformation != null) {
						List<AddressInformationDataType> addressInformationDataTypeList = contactDataInformation
								.getAddressData();

						if (addressInformationDataTypeList != null) {
							for (AddressInformationDataType addressInformationDataType : addressInformationDataTypeList) {
								CountryObjectType countryObjectType = addressInformationDataType.getCountryReference();
								if (countryObjectType != null) {

									attr = new BasicAttribute("country", countryObjectType.getDescriptor());
									attributes.put(attr);

								}
								attr = new BasicAttribute("city", addressInformationDataType.getMunicipality());
								attributes.put(attr);
							}
						}
						List<PhoneInformationDataType> phoneInformationDataTypeList = contactDataInformation
								.getPhoneData();

						if (phoneInformationDataTypeList != null) {
							for (PhoneInformationDataType phoneInformationDataType : phoneInformationDataTypeList) {

								attr = new BasicAttribute("phoneNumber", phoneInformationDataType.getFormattedPhone());
								attributes.put(attr);
							}

						}

						List<EmailAddressInformationDataType> emailAddressInformationDataTypeList = contactDataInformation
								.getEmailAddressData();
						if (emailAddressInformationDataTypeList != null) {
							for (EmailAddressInformationDataType emailAddressInformationDataType : emailAddressInformationDataTypeList) {

								attr = new BasicAttribute("emailAddress",
										emailAddressInformationDataType.getEmailAddress());
								attributes.put(attr);
							}
						}

					}

					WorkerEmploymentInformationDataType workerEmploymentInformationDataType = workerType.getWorkerData()
							.getEmploymentData();

					if (workerEmploymentInformationDataType != null) {
						List<WorkerJobDataType> workerJobDataTypeList = workerEmploymentInformationDataType
								.getWorkerJobData();

						if (workerJobDataTypeList != null) {
							for (WorkerJobDataType workerJobDataType : workerJobDataTypeList) {

								PositionDetailDataType positionDetailDataType = workerJobDataType.getPositionData();

								if (positionDetailDataType != null) {

									JobProfileInPositionSummaryDataType jobProfileInPositionSummaryDataType = positionDetailDataType
											.getJobProfileSummaryData();

									if (jobProfileInPositionSummaryDataType != null) {
										String jobProfileName = jobProfileInPositionSummaryDataType.getJobProfileName();
										attr = new BasicAttribute("jobProfileName", jobProfileName);
										attributes.put(attr);

										List<JobFamilyBaseObjectType> jobFamilyObjectTypeList = jobProfileInPositionSummaryDataType
												.getJobFamilyReference();

										if (jobFamilyObjectTypeList != null) {
											for (JobFamilyBaseObjectType jobFamilyObjectType : jobFamilyObjectTypeList) {

												String jobFamily = jobFamilyObjectType.getDescriptor();
												attr = new BasicAttribute("jobFamily", jobFamily);
												attributes.put(attr);

											}
										}
										if (jobProfileInPositionSummaryDataType.isCriticalJob()) {
											attr = new BasicAttribute("criticalJob", "true");
											attributes.put(attr);
										} else {
											attr = new BasicAttribute("criticalJob", "false");
											attributes.put(attr);
										}

									}
									LocationSummaryDataType locationSummaryDataType = positionDetailDataType
											.getBusinessSiteSummaryData();

									if (locationSummaryDataType != null) {
										LocationObjectType locationObjectType = locationSummaryDataType
												.getLocationReference();

										attr = new BasicAttribute("location", locationObjectType.getDescriptor());
										attributes.put(attr);
									}
									PositionWorkerTypeObjectType positionWorkerTypeObjectType = positionDetailDataType
											.getWorkerTypeReference();

									if (positionWorkerTypeObjectType != null) {
										attr = new BasicAttribute("employeeType",
												positionWorkerTypeObjectType.getDescriptor());
										attributes.put(attr);

									}

									PositionTimeTypeObjectType positionTimeTypeObject = positionDetailDataType
											.getPositionTimeTypeReference();
									if (positionTimeTypeObject != null) {

										attr = new BasicAttribute("positionType",
												positionTimeTypeObject.getDescriptor());
										attributes.put(attr);
									}
									String positionTitle = positionDetailDataType.getPositionTitle();
									attr = new BasicAttribute("title", positionTitle);
									attributes.put(attr);

									XMLGregorianCalendar startDate = positionDetailDataType.getStartDate();
									attr = new BasicAttribute("hiredate", startDate);
									attributes.put(attr);

									List<WorkerObjectType> managerWorkerObjectTypeList = positionDetailDataType
											.getManagerAsOfLastDetectedManagerChangeReference();

									if (managerWorkerObjectTypeList != null) {
										for (WorkerObjectType managerObjectType : managerWorkerObjectTypeList) {
											String managerName = managerObjectType.getDescriptor();
											attr = new BasicAttribute("manager", managerName);
											attributes.put(attr);
										}
									}

									// Get the organization data

								}

							}
						}

					}
				}
			}
		}

		return attributes;
	}

	public static Attributes setSingleEmployeeAttributes(EmployeeDataType employeeData) {

		Attributes attributes = new BasicAttributes();

		Attribute attribute = null;

		attribute = new BasicAttribute("Employee_ID", employeeData.getEmployeeID());
		attributes.put(attribute);

		attribute = new BasicAttribute("User_ID", employeeData.getUserID());
		attributes.put(attribute);

		List<PersonalInfoDataType> pdata = employeeData.getPersonalInfoData();

		for (PersonalInfoDataType personalInfoDataType : pdata) {

			String businessTitle = personalInfoDataType.getBusinessTitle();

			attribute = new BasicAttribute("BusinessTitle", businessTitle);
			attributes.put(attribute);

			List<NameDataType> nameDataTypeList = personalInfoDataType.getPersonData().getNameData();
			for (NameDataType nameDataType : nameDataTypeList) {
				if (nameDataType.isIsLegal()) {
					attribute = new BasicAttribute("FirstName", nameDataType.getFirstName());
					attributes.put(attribute);

					List<LastNameDataType> lastNameList = nameDataType.getLastName();
					for (LastNameDataType lastName : lastNameList) {
						if (lastName.getType() != null && lastName.getType().equalsIgnoreCase("primary")) {
							attribute = new BasicAttribute("LastName", lastName.getValue());
							attributes.put(attribute);
						}
					}

				}
			}

			attributes = setContactData(attributes, personalInfoDataType.getPersonData().getContactData());
			attributes = setBGData(attributes, personalInfoDataType.getPersonData().getBiographicData());

		}

		List<WorkerPositionDataType> workerPositionDataTypeList = employeeData.getWorkerPositionData();

		for (WorkerPositionDataType workerPositionDataType : workerPositionDataTypeList) {

			// List<LocationContentDataType> locationContentDataTypeList =
			// workerPositionDataType.getBusinessSiteContentData();
			// for (LocationContentDataType locationContentDataType :
			// locationContentDataTypeList)
			// {
			// attribute = new BasicAttribute("location",
			// locationContentDataType.locationName);
			// attributes.put(attribute);
			//
			//
			// }

		}

		return attributes;

	}

	private static Attributes setContactData(Attributes attributes, ContactDataType contact) {

		Attribute attr = null;

		List<AddressDataType> addresses = contact.getAddressData();
		List<InstantMessengerDataType> ims = contact.getInstantMessengerData();
		List<PhoneNumberDataType> phonenos = contact.getPhoneNumberData();
		List<WebAddressDataType> webaddresses = contact.getWebAddressData();

		for (AddressDataType addr : addresses) {

			List<AddressLineDataType> adlines = addr.getAddressLine();
			int i = 0;
			for (AddressLineDataType adl : adlines) {
				if (i == 0)
					attr = new BasicAttribute("AddressLine", adl.getValue());
				else
					attr.add(adl.getValue());

				i++;
			}
			attributes.put(attr);
			i = 0;
			CountryReferenceType cr = addr.getCountryReference();
			attr = new BasicAttribute("CountryReference", cr.getCountryISOCode());
			attributes.put(attr);
			attr = new BasicAttribute("Municipality", addr.getMunicipality());
			attributes.put(attr);
			attr = new BasicAttribute("PostalCode", addr.getPostalCode());
			attributes.put(attr);
			attr = new BasicAttribute("Region", addr.getRegion());
			attributes.put(attr);

			List<String> subm = addr.getSubmunicipality();
			for (String submu : subm) {
				if (i == 0)
					attr = new BasicAttribute("Submunicipality", submu);
				else
					attr.add(submu);

				i++;
			}
			attributes.put(attr);
			List<SubregionDataType> subr = addr.getSubregion();
			i = 0;
			for (SubregionDataType sr : subr) {

				if (i == 0)
					attr = new BasicAttribute("Subregion", sr.getValue());
				else
					attr.add(sr.getValue());

				i++;
			}
			attributes.put(attr);

		}

		for (InstantMessengerDataType id : ims) {
			attr = new BasicAttribute("InstantMessengerAddress", id.getInstantMessengerAddress());
			attributes.put(attr);

			attr = new BasicAttribute("InstantMessengerComment", id.getInstantMessengerComment());
			attributes.put(attr);
			attr = new BasicAttribute("InstantMessengerProvider",
					id.getInstantMessengerTypeReference().getInstantMessengerProvider());
			attributes.put(attr);

		}
		for (PhoneNumberDataType id : phonenos) {
			attr = new BasicAttribute("CountryISOCode", id.getCountryISOCode());
			attributes.put(attr);

			attr = new BasicAttribute("InternationalPhoneCode", id.getInternationalPhoneCode());
			attributes.put(attr);
			attr = new BasicAttribute("PhoneExtension", id.getPhoneExtension());
			attributes.put(attr);
			attr = new BasicAttribute("PhoneNumber", id.getPhoneNumber());
			attributes.put(attr);
			attr = new BasicAttribute("PhoneDeviceTypeDescription",
					id.getPhoneDeviceTypeReference().getPhoneDeviceTypeDescription());
			attributes.put(attr);

		}

		for (WebAddressDataType id : webaddresses) {
			attr = new BasicAttribute("WebAddress", id.getWebAddress());
			attributes.put(attr);

			attr = new BasicAttribute("WebAddressComment", id.getWebAddressComment());
			attributes.put(attr);

		}
		return attributes;
	}

	private static Attributes setBGData(Attributes attributes, BiographicDataType bgd) {

		CountryOfBirthReferenceType cob = bgd.getCountryOfBirthReference();
		Attribute attr = null;
		if (cob != null) {
			CountryReferenceType cr = cob.getCountryReference();
			attr = new BasicAttribute("CountryReference", cr.getCountryISOCode());
			attributes.put(attr);
		}
		XMLGregorianCalendar dob = bgd.getDateOfBirth();
		if (dob != null) {
			attr = new BasicAttribute("DateOfBirth", dob.toGregorianCalendar().toString());
			attributes.put(attr);
		}
		List<DisabilityReferenceType> disref = bgd.getDisabilityReference();
		int i = 0;
		for (DisabilityReferenceType dr : disref) {
			if (i == 0)
				attr = new BasicAttribute("DisabilityName", dr.getDisabilityName());
			else
				attr.add(dr.getDisabilityName());

			i++;
		}
		GenderReferenceType gr = bgd.getGenderReference();
		attr = new BasicAttribute("GenderReference", gr.getGenderDescription());
		attributes.put(attr);
		attr = new BasicAttribute("PlaceOfBirth", bgd.getPlaceOfBirth());
		attributes.put(attr);

		return attributes;
	}

	private static Attributes setPhoneData(Attributes attributes, PhoneNumberDataType phoneNumberData) {

		if (phoneNumberData != null) {
			String phoneNumber = phoneNumberData.getPhoneNumber();
			Attribute attr = new BasicAttribute("phoneNumber", phoneNumber);
			attributes.put(attr);

		}

		return attributes;
	}

	private static Attributes setWorkerPositionData(Attributes attributes, WorkerPositionDataType workerPositionType) {
		Attribute attr = null;

		attr = new BasicAttribute("PositionTitle", workerPositionType.getPositionTitle());
		attributes.put(attr);

		List<PositionTimeTypeReferenceType> positionTimeReferenceList = workerPositionType
				.getPositionTimeTypeReference();

		for (PositionTimeTypeReferenceType positionTimeReference : positionTimeReferenceList) {
			attr = new BasicAttribute("positionTimeType", positionTimeReference.getTimeTypeDescription());
			attributes.put(attr);
		}

		return attributes;
	}

	// public static EmployeePersonalInfoType updateEmployeeData(
	// EmployeePersonalInfoType emp, InterceptParam prop)
	// throws NamingException {
	//
	// List<ModificationItem> modifications = prop.getModifications();
	// WorkerPersonalInfoDataType worker = emp.getEmployeePersonalInfoData();
	// PersonalInfoDataType pdata = worker.getPersonalInfoData();
	// for (ModificationItem mi : modifications) {
	//
	// Attribute attr = mi.getAttribute();
	// if (attr.getID().equalsIgnoreCase("CountryReference")) {
	// pdata.getPersonData().getBiographicData()
	// .getCountryOfBirthReference().getCountryReference()
	// .setCountryISOCode((String) attr.get(0));
	//
	// } else if (attr.getID().equalsIgnoreCase("DateOfBirth")) {
	// pdata.getPersonData().getBiographicData().setDateOfBirth(
	// (XMLGregorianCalendar) attr.get(0));
	//
	// } else if (attr.getID().equalsIgnoreCase("DisabilityName")) {
	//
	// pdata.getPersonData().getBiographicData()
	// .getDisabilityReference().get(0).setDisabilityName(
	// (String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("GenderReference")) {
	//
	// pdata.getPersonData().getBiographicData().getGenderReference()
	// .setGenderDescription((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("PlaceOfBirth")) {
	//
	// pdata.getPersonData().getBiographicData().setPlaceOfBirth(
	// (String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("AddressLine")) {
	//
	// pdata.getPersonData().getContactData().getAddressData().get(0)
	// .getAddressLine().get(0).setValue((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("Municipality")) {
	//
	// pdata.getPersonData().getContactData().getAddressData().get(0)
	// .setMunicipality((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("PostalCode")) {
	//
	// pdata.getPersonData().getContactData().getAddressData().get(0)
	// .setPostalCode((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("Region")) {
	//
	// pdata.getPersonData().getContactData().getAddressData().get(0)
	// .setRegion((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("Submunicipality")) {
	//
	// pdata.getPersonData().getContactData().getAddressData().get(0)
	// .getSubmunicipality().set(0, (String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("Subregion")) {
	//
	// pdata.getPersonData().getContactData().getAddressData().get(0)
	// .getSubregion().get(0).setValue((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("InstantMessengerAddress")) {
	//
	// pdata.getPersonData().getContactData()
	// .getInstantMessengerData().get(0)
	// .setInstantMessengerAddress((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("InstantMessengerComment")) {
	//
	// pdata.getPersonData().getContactData()
	// .getInstantMessengerData().get(0)
	// .setInstantMessengerComment((String) attr.get(0));
	// } else if (attr.getID()
	// .equalsIgnoreCase("InstantMessengerProvider")) {
	//
	// pdata.getPersonData().getContactData()
	// .getInstantMessengerData().get(0)
	// .getInstantMessengerTypeReference()
	// .setInstantMessengerProvider((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("PhoneNumber")) {
	//
	// pdata.getPersonData().getContactData().getPhoneNumberData()
	// .get(0).setPhoneNumber((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("PhoneExtension")) {
	//
	// pdata.getPersonData().getContactData().getPhoneNumberData()
	// .get(0).setPhoneExtension((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("InternationalPhoneCode")) {
	//
	// pdata.getPersonData().getContactData().getPhoneNumberData()
	// .get(0).setInternationalPhoneCode((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase(
	// "PhoneDeviceTypeDescription")) {
	//
	// pdata.getPersonData().getContactData().getPhoneNumberData()
	// .get(0).getPhoneDeviceTypeReference()
	// .setPhoneDeviceTypeDescription((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("WebAddress")) {
	//
	// pdata.getPersonData().getContactData().getWebAddressData().get(
	// 0).setWebAddress((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("WebAddressComment")) {
	//
	// pdata.getPersonData().getContactData().getWebAddressData().get(
	// 0).setWebAddressComment((String) attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("CustomDescription")) {
	//
	// pdata.getPersonData().getCustomIDData().get(0).setCustomDescription((String)
	// attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("CustomID")) {
	//
	// pdata.getPersonData().getCustomIDData().get(0).setCustomID((String)
	// attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("CustomIDTypeName")) {
	//
	// pdata.getPersonData().getCustomIDData().get(0).getCustomIDTypeReference().setCustomIDTypeName((String)
	// attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("IntegrationIDReference")) {
	//
	// pdata.getPersonData().getCustomIDData().get(0).getOrganizationReference().getIntegrationIDReference().getID().setValue((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("GovernmentID")) {
	//
	// pdata.getPersonData().getGovernmentIDData().get(0).setGovernmentID((String)
	// attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("GovernmentIDCountryReference"))
	// {
	//
	// pdata.getPersonData().getGovernmentIDData().get(0).getCountryReference().setCountryISOCode((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("GovernmentIDExpirationDate")) {
	//
	// pdata.getPersonData().getGovernmentIDData().get(0).setExpirationDate((XMLGregorianCalendar)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("GovernmentIDTypeReference")) {
	//
	// pdata.getPersonData().getGovernmentIDData().get(0).getGovernmentIDTypeReference().setGovernmentIDTypeName((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("GovernmentIDIssuedDate")) {
	//
	// pdata.getPersonData().getGovernmentIDData().get(0).setIssuedDate((XMLGregorianCalendar)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LicenseAuthorityName")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).getAuthorityReference().setAuthorityName((String)
	// attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LicenseCountryReference")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).getCountryReference().setCountryISOCode((String)
	// attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LicenseExpirationDate")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).setExpirationDate((XMLGregorianCalendar)attr.get(0));
	// } else if
	// (attr.getID().equalsIgnoreCase("LicenseCountryRegionReference")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).getCountryRegionReference().setCountryRegionName((String)
	// attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LicenseClass")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).setLicenseClass((String)
	// attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LicenseIDIssuedDate")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).setIssuedDate((XMLGregorianCalendar)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LicenseID")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).setLicenseID((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LicenseTypeName")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).getLicenseTypeReference().setLicenseTypeName((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LicenseVerificationDate")) {
	//
	// pdata.getPersonData().getLicenseIDData().get(0).setVerificationDate((XMLGregorianCalendar)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("AdditionalNameType")) {
	//
	// pdata.getPersonData().getNameData().get(0).setAdditionalNameType((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("FirstName")) {
	//
	// pdata.getPersonData().getNameData().get(0).setFirstName((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LastName")) {
	//
	// pdata.getPersonData().getNameData().get(0).getLastName().get(0).setValue((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("MiddleName")) {
	//
	// pdata.getPersonData().getNameData().get(0).setMiddleName((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("Prefix")) {
	//
	// pdata.getPersonData().getNameData().get(0).getPrefix().get(0).setValue((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("Suffix")) {
	//
	// pdata.getPersonData().getNameData().get(0).getSuffix().get(0).setValue((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("LocalName")) {
	//
	// pdata.getPersonData().getNameData().get(0).getLocalNameData().setLocalName((String)attr.get(0));
	// } else if (attr.getID().equalsIgnoreCase("PassportCountryReference")) {
	//
	// pdata.getPersonData().getPassportIDData().get(0).getCountryReference().setCountryISOCode((String)attr.get(0))
	// ;
	// } else if (attr.getID().equalsIgnoreCase("PassportIssuedDate")) {
	//
	// pdata.getPersonData().getPassportIDData().get(0).setIssuedDate((XMLGregorianCalendar)attr.get(0));
	// }else if (attr.getID().equalsIgnoreCase("PassportVerificationDate")) {
	//
	// pdata.getPersonData().getPassportIDData().get(0).setVerificationDate((XMLGregorianCalendar)attr.get(0));
	// }else if (attr.getID().equalsIgnoreCase("PassportNumber")) {
	//
	// pdata.getPersonData().getPassportIDData().get(0).setPassportNumber((String)attr.get(0));
	//
	// }else if (attr.getID().equalsIgnoreCase("PassportTypeName")) {
	//
	// pdata.getPersonData().getPassportIDData().get(0).getPassportTypeReference().setPassportTypeName((String)attr.get(0));
	//
	// }else if (attr.getID().equalsIgnoreCase("VisaID")) {
	//
	// pdata.getPersonData().getVisaIDData().get(0).setVisaID((String)attr.get(0));
	//
	// }else if (attr.getID().equalsIgnoreCase("VisaCountryReference")) {
	//
	// pdata.getPersonData().getVisaIDData().get(0).getCountryReference().setCountryISOCode((String)attr.get(0));
	//
	// }else if (attr.getID().equalsIgnoreCase("VisaExpirationDate")) {
	//
	// pdata.getPersonData().getVisaIDData().get(0).setExpirationDate((XMLGregorianCalendar)attr.get(0));
	//
	// }else if (attr.getID().equalsIgnoreCase("VisaIssuedDate")) {
	//
	// pdata.getPersonData().getVisaIDData().get(0).setIssuedDate((XMLGregorianCalendar)attr.get(0));
	//
	// }else if (attr.getID().equalsIgnoreCase("VisaVerificationDate")) {
	//
	// pdata.getPersonData().getVisaIDData().get(0).setVerificationDate((XMLGregorianCalendar)attr.get(0));
	//
	// }else if (attr.getID().equalsIgnoreCase("VisaTypeName")) {
	//
	// pdata.getPersonData().getVisaIDData().get(0).getVisaTypeReference().setVisaTypeName((String)attr.get(0));
	//
	// }
	//
	//
	//
	//
	// }
	// return emp;
	//
	// }

}
