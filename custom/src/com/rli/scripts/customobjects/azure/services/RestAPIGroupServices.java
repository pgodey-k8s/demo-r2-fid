package com.rli.scripts.customobjects.azure.services;

import com.rli.scripts.customobjects.azure.dto.Group;
import com.rli.scripts.customobjects.azure.dto.Role;
import com.rli.scripts.customobjects.azure.dto.UserPageInfo;
import com.rli.scripts.customobjects.azure.exception.CustomAzureException;
import com.rli.scripts.customobjects.azure.utils.AzureUtilities;
import com.rli.web.http.json.JSONArray;
import com.rli.web.http.json.JSONObject;

/**
 * This class provides all the methods corresponding to the
 * functionalities regarding groups & roles.
 *
 */
public class RestAPIGroupServices {
	
	/**
	 * This method returns a particular group identified by its ObjectId
	 * @param objectId The group to be retrieved.
	 * @return A group object containing all the relevant information.
	 * @throws CustomAzureException If the operation can not be performed successfully.
	 */
	public static Group getGroup(String objectId, String sessionKey) throws CustomAzureException{
		
		/**
		 * Send the get request using appropriate path, also we don't
		 * have any query option for this request.
		 */
		String response = HttpRequestHandler.handleRequest(
				String.format("/Groups('%s')", objectId), 
				null,
				sessionKey);
		
		
		/**
		 * Get the JSON Object which would contain all the group information.
		 */
		JSONObject groupObject = JSONDataParser.parseJSonDataSingleObject(response);				
		Group group = new Group();
		
		/**
		 * Copy the relevant attribute values from the jsonObject to the
		 * group object.
		 */
		AzureUtilities.copyAttrFromJSONObject(groupObject, group);
		return group;
	}

	
	/**
	 * This method returns all the members of a particular group identified by its 
	 * ObjectId.
	 * @param grpId The Object Id of the group whose members would be returned.
	 * @return The list of all the member user of this group.
	 * @throws CustomAzureException Throw exception if the operation can not be successful.
	 */
	public static UserPageInfo getGroupMembers(String grpId, String sessionKey) throws CustomAzureException {
		
		/**
		 * Send the appropriate http request and get the response.
		 */
		String response = HttpRequestHandler.handleRequest(
						String.format("/Groups('Group_%s')/Members", grpId), 
						null,
						sessionKey);

		/**
		 * Get the list of members in a jsonObject.
		 */
		JSONArray groupMembers = JSONDataParser.parseJSonDataCollection(response);
		
		/**
		 * For all the users in the JSON List, retrieve their DisplayName and ObjectId and populate them
		 * in the UserPageInfo object.
		 */
		UserPageInfo thisPage = new UserPageInfo();
		for(int i = 0; i < groupMembers.length(); i++){				
			thisPage.addNewUserInfo( groupMembers.optJSONObject(i).optString("DisplayName"), groupMembers.optJSONObject(i).optString("ObjectId") );
		}
		
		return thisPage;


	}


	/**
	 * This method returns all the members of a particular role identified by its 
	 * ObjectId.
	 * @param roleId The Object Id of the role whose members would be returned.
	 * @return The list of all the member user of this role.
	 * @throws CustomAzureException Throw exception if the operation can not be successful.
	 */	
	public static UserPageInfo getRoleMembers(String roleId, String sessionKey) throws CustomAzureException {

		/**
		 * Send the appropriate http request and get the response.
		 */
		String response = HttpRequestHandler.handleRequest(
				String.format("/Roles('Role_%s')/Members", roleId), 
				null,
				sessionKey);

		/**
		 * Get the roleMembers into a JSON Array.
		 */
		JSONArray roleMembers = JSONDataParser.parseJSonDataCollection(response);
		
		/**
		 * Populate each of the member into the UserPageInfo Object.
		 */
		UserPageInfo thisPage = new UserPageInfo();
		for(int i = 0; i < roleMembers.length(); i++){				
			thisPage.addNewUserInfo( roleMembers.optJSONObject(i).optString("DisplayName"), roleMembers.optJSONObject(i).optString("ObjectId") );
		}
		
		return thisPage;
	}

	
	
	public static Role getRole(String objectId, String sessionKey) throws CustomAzureException {				
		
		/**
		 * Send the appropriate http request and get the response.
		 */		
		String response = HttpRequestHandler.handleRequest(
				String.format("/Roles('%s')", objectId), 
				null,
				sessionKey);
				
		/**
		 * Get the JSON Object representing the role.
		 */
		JSONObject roleObject = JSONDataParser.parseJSonDataSingleObject(response);				
		
		/**
		 * Copy the attributes from the roleObject to the Role object.
		 */
		Role role = new Role();
		AzureUtilities.copyAttrFromJSONObject(roleObject, role);
		
		return role;
	}


}
