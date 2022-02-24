package com.rli.scripts.changeMessageConvertors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.rli.synsvcs.common.RliCon.CHANGE_TYPE;
import com.rli.synsvcs.etl.changeMessages.ChangeMessage;
import com.rli.synsvcs.etl.changeMessages.ChangeMessageConvertor;
import com.rli.synsvcs.etl.changeMessages.ChangeMessageException;
import com.rli.synsvcs.etl.changeMessages.ChangeMessageList;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.json.JSONObject;
import com.rli.web.http.json.JSONTokener;

/**
 * This class is included as a sample to show how the
 * <code>ChangeMessageConvertor</code> interface can be implemented to convert
 * custom change message to <code>ChangeMessage</code> or vice-versa. Note: In
 * Capture/Apply Connector properties in Control Panel, selecting "GoldenGate
 * JSON" doesn't call methods in this class for conversion. To run methods in
 * this class, enter the name of this class as value for "Message Format"
 * property.
 * 
 * 
 * See oracle documentation for GoldenGateJSON format.
 * https://docs.oracle.com/goldengate/bd1221/gg-bd/GADBD/GUID-F0FA2781-0802-4530-B1F0-5E102B982EC0.htm#GADBD501
 */
public class GoldenGateJSONConvertor implements ChangeMessageConvertor {

	// goldengate's default metadata keys
	private static final String GG_TABLE_KEY = "table";
	private static final String GG_OP_TYPE_KEY = "op_type";
	private static final String GG_OP_TS_KEY = "op_ts";
	private static final String GG_CURRENT_TS_KEY = "current_ts";
	private static final String GG_POSITION_KEY = "pos";

	// keys whose value contains goldengate table change info
	private static final String GG_AFTER_KEY = "after";
	private static final String GG_BEFORE_KEY = "before";

	@Override
	public ChangeMessageList convertToRLIChangeMessages(String goldenGateMsg) throws Exception {
		ChangeMessageList listOfRLIMessages = new ChangeMessageList();

		for (JSONObject goldenGateJSON : getListOfJSONObjects(goldenGateMsg, "")) {

			ChangeMessage rliMessage;

			String op_type = (String) goldenGateJSON.get(GG_OP_TYPE_KEY);

			if (op_type.equals("I")) {
				rliMessage = new ChangeMessage(CHANGE_TYPE.insert);
				addJSONKeystoRLIMessage(goldenGateJSON.getJSONObject(GG_AFTER_KEY), rliMessage);
				applyGoldenGateMetaDataAttributes(goldenGateJSON, rliMessage);
			}

			else if (op_type.equals("U")) {
				rliMessage = new ChangeMessage(CHANGE_TYPE.update);
				performUpdateConversion(goldenGateJSON, rliMessage);
				applyGoldenGateMetaDataAttributes(goldenGateJSON, rliMessage);
			}

			else if (op_type.equals("D")) {
				rliMessage = new ChangeMessage(CHANGE_TYPE.delete);
				addJSONKeystoRLIMessage(goldenGateJSON.getJSONObject(GG_BEFORE_KEY), rliMessage);
				applyGoldenGateMetaDataAttributes(goldenGateJSON, rliMessage);
			}

			else if (op_type.equals("T"))
				continue;

			else {
				throw new Exception("Invalid Goldengate Message");
			}

			listOfRLIMessages.add(rliMessage);
		}
		return listOfRLIMessages;
	}

	/**
	 * The implementation in this method does not convert to GoldenGate JSON format
	 * and its only purpose is to demonstrate how the <code>ChangeMessageList</code>
	 * and <code>ChangeMessage</code> API can be used to get all the change
	 * information
	 */
	@Override
	public List<String> convertFromRLIChangeMessages(ChangeMessageList rliChangeMessages) {

		int messageCount = 0;
		for (ChangeMessage changeMessage : rliChangeMessages) {
			messageCount++;
			System.out.println("Message " + messageCount);
			CHANGE_TYPE messageChangeType = changeMessage.getMessageType();
			System.out.println("Type of message: " + messageChangeType.toString());
			if (messageChangeType == CHANGE_TYPE.update) {
				try {
					System.out.println("Added Keys: " + Arrays.toString(changeMessage.getAddedKeys().toArray()));
					System.out.println("Deleted Keys: " + Arrays.toString(changeMessage.getDeletedKeys().toArray()));
					System.out.println("Updated keys: " + Arrays.toString(changeMessage.getUpdatedKeys().toArray()));
				} catch (ChangeMessageException e) {
					e.printStackTrace();
				}
			}
			System.out.println("All the keys and their values:" + System.lineSeparator() + changeMessage.getAsJSON());

		}
		return null;
	}

	private void performUpdateConversion(JSONObject goldenGateJSON, ChangeMessage rliMessage)
			throws ChangeMessageException, JSONException {

		JSONObject beforeJson = goldenGateJSON.getJSONObject(GG_BEFORE_KEY);
		JSONObject afterJson = goldenGateJSON.getJSONObject(GG_AFTER_KEY);

		Iterator beforeJsonIterator = beforeJson.keys();
		while (beforeJsonIterator.hasNext()) {
			String currentKey = (String) beforeJsonIterator.next();
			String beforeValue = beforeJson.getString(currentKey);

			if (afterJson.has(currentKey) && !(afterJson.getString(currentKey)).equals(beforeValue)) {

				if (beforeJson.isNull(currentKey))
					rliMessage.putAsAdded(currentKey, afterJson.get(currentKey));
				else if (afterJson.isNull(currentKey))
					rliMessage.putAsDeleted(currentKey);
				else
					rliMessage.putAsUpdated(currentKey, afterJson.get(currentKey));

			} else if (!beforeJson.isNull(currentKey)) {
				rliMessage.put(currentKey, beforeJson.get(currentKey));
			}
		}
	}

	/*
	 * Helper functions which takes JSON input as argument and add key value pair to
	 * RLIMessage
	 * 
	 * @ JSONObject
	 * 
	 * @RLIMessage
	 */
	private void addJSONKeystoRLIMessage(JSONObject json, ChangeMessage rliMessage)
			throws JSONException, ChangeMessageException {
		Iterator it = json.keys();

		while (it.hasNext()) {
			String key = (String) it.next();
			// skip keys that have null values
			if (!json.isNull(key))
				rliMessage.put(key, json.get(key));
		}
	}

	/*
	 * Helper function takes puts goldenGate metadata attributes from the input json
	 * and puts into RLI ChangeMessage
	 * 
	 * @ JSONObject
	 * 
	 * @RLIMessage
	 */
	private void applyGoldenGateMetaDataAttributes(JSONObject goldenGateJson, ChangeMessage rliMessage)
			throws Exception {

		try {
			rliMessage.put(GG_TABLE_KEY, goldenGateJson.get(GG_TABLE_KEY));
			rliMessage.put(GG_OP_TS_KEY, goldenGateJson.get(GG_OP_TS_KEY));
			rliMessage.put(GG_CURRENT_TS_KEY, goldenGateJson.get(GG_CURRENT_TS_KEY));
			rliMessage.put(GG_POSITION_KEY, goldenGateJson.get(GG_POSITION_KEY));
		} catch (JSONException e) {
			throw new Exception("Missing required metadata attributes from the GoldenGate JSON message");
		}
	}

	/**
	 * Helper function which iterates over a string containing JSON messages and
	 * separates them based on input the delimiter. returns
	 * 
	 * @param jsonMessages The String containing several JSON messages
	 * @param delimiter    The String that separates two JSON messages. It could be
	 *                     empty string or NULL
	 * @return List<JSONObject> The list of JSONObjects extracted from the input
	 *         param <code>jsonMessages</code>
	 * @throws JSONException
	 */
	private static List<JSONObject> getListOfJSONObjects(String jsonMessages, String delimiter) throws JSONException {

		List<JSONObject> listOfJSON = new ArrayList<JSONObject>();

		JSONTokener x = new JSONTokener(jsonMessages);
		boolean exitWhile = false;

		while (!exitWhile && x.nextClean() == '{') {
			x.back();
			JSONObject currentJSON = new JSONObject(x);
			listOfJSON.add(currentJSON);

			if (delimiter == null || delimiter.isEmpty())
				continue;

			// skip delimiter
			for (char c : delimiter.toCharArray()) {
				try {
					x.next(c);
				} catch (JSONException e) {
					exitWhile = true;
					break;
				}
			}
		}
		return listOfJSON;
	}
}
