package com.rli.scripts.customobjects.workday30;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import com.rli.stubs.workday30_0.human_resources.HumanResourcesPort;
import com.rli.stubs.workday30_0.human_resources.HumanResourcesService;

public class HrpCache {
	private static HumanResourcesPort hrp;

	public static synchronized HumanResourcesPort getHrp(String url, String username, String password)
			throws MalformedURLException, NoSuchAlgorithmException, KeyManagementException {
		if (hrp == null) {

			HumanResourcesService hrservice = new HumanResourcesService(new URL(url + "?wsdl"));
			hrp = hrservice.getHumanResources();
		}
		// AUTHENTICATE
		// Add the WorkdayCredentials handler to the client stub
		WorkdayCredentials.addWorkdayCredentials((BindingProvider) hrp, username, password);

		// Assign the Endpoint URL
		Map<String, Object> requestContext = ((BindingProvider) hrp).getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

		return hrp;
	}

	public static void forgetHrp() {
		hrp = null;
	}

	public static void main(String[] args) throws MalformedURLException {
		// HumanResourcesService hrservice = new HumanResourcesService();
		// hrp = hrservice.getHumanResources();

		URL url = new URL("http://localhost:8080");
		HumanResourcesService hrservice = new HumanResourcesService(url,
				new QName("urn:com.workday/bsvc/Human_Resources", "Human_ResourcesService"));
		hrp = hrservice.getPort(HumanResourcesPort.class);
	}
}
