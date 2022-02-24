package com.rli.scripts.customobjects.workday30;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.xerces.jaxp.datatype.DatatypeFactoryImpl;

import com.rli.stubs.workday30_0.LegalNameDataType;
import com.rli.stubs.workday30_0.PersonNameDataType;
import com.rli.stubs.workday30_0.PersonNameDetailDataType;
import com.rli.stubs.workday30_0.PersonalInformationDataType;
import com.rli.stubs.workday30_0.WorkerDataType;
import com.rli.stubs.workday30_0.WorkerType;

public class WorkdayUtils {

	public static XMLGregorianCalendar stringToGreg(String ts) {

		DatatypeFactory df = new DatatypeFactoryImpl();
		return df.newXMLGregorianCalendar(ts);
	}

	public static String gregToString(XMLGregorianCalendar xgc) {
		return xgc.toXMLFormat();
	}

	public static Attributes toAttributes(WorkerType workerType) {
		Attributes attributes = new BasicAttributes();

		String workerId = workerType.getWorkerData().getWorkerID();

		// get worker data
		WorkerDataType wdt = workerType.getWorkerData();
		PersonalInformationDataType pidt = wdt.getPersonalData();
		PersonNameDataType pndt = pidt.getNameData();
		LegalNameDataType lndt = pndt.getLegalNameData();
		PersonNameDetailDataType pnddt = lndt.getNameDetailData();
		String formattedName = pnddt.getFormattedName();
		String lastName = pnddt.getLastName();

		Attribute attr = new BasicAttribute("id", workerId);
		Attribute attr2 = new BasicAttribute("formattedname", formattedName);
		Attribute attr2b = new BasicAttribute("lastname", lastName);

		// transactionEntryMoment
		XMLGregorianCalendar xgc = wdt.getTransactionLogEntryData().getTransactionLogEntry().get(0)
				.getTransactionLogData().getTransactionEntryMoment();
		String stringXgc = WorkdayUtils.gregToString(xgc);

		Attribute attr3 = new BasicAttribute("transactionentrymoment", stringXgc);

		attributes.put(attr);
		attributes.put(attr2);
		attributes.put(attr2b);
		attributes.put(attr3);

		return attributes;
	}
}
