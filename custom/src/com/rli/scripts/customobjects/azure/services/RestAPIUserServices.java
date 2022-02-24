/**
 * 
 */
package com.rli.scripts.customobjects.azure.services;

import java.util.HashMap;

import com.rli.scripts.customobjects.azure.dto.User;
import com.rli.scripts.customobjects.azure.dto.UserPageInfo;
import com.rli.scripts.customobjects.azure.exception.CustomAzureException;
import com.rli.scripts.customobjects.azure.utils.AzureUtilities;
import com.rli.web.http.json.JSONArray;
import com.rli.web.http.json.JSONException;
import com.rli.web.http.json.JSONObject;



/**
 * This class provides all the read functionalities.
 *
 */
public class RestAPIUserServices {
	
	/**
	 * A map that would hold the skip Tokens for different pages.
	 */
	public static HashMap<Integer, String> pageMap = null;
	
	/**
	 * This method returns a particular page of users.
	 * @param pageNumber The page number to be returned.
	 * @return A page of users
	 * @throws CustomAzureException if the operation is unsuccessful.
	 */
	public static UserPageInfo getUsersPage(int pageNumber, String sessionKey) throws CustomAzureException{
		
		String response = "";
		
		/**
		 * If the pageMap is not instantiated,
		 * instantiate it.
		 */
		if(pageMap == null){
			pageMap = new HashMap<Integer, String>();
		}
		
		/**
		 * If the request is for the first page, we do not need an skip token,
		 * we just need to use the $top query option.
		 */
		if(pageNumber == 1){
			response = HttpRequestHandler.handleRequest("/Users", "$top=" + CustomObjectParameter.getUserPerPage(), sessionKey);
		}
		
		/**
		 * Again, if the required skip token is found in the page map, retrieve the skiptoken,
		 * and send the appropriate http get request using this token.
		 */
		else if(pageMap.containsKey(new Integer(pageNumber))){			
			String queryOption = String.format("$top=%d&$skiptoken=%s", CustomObjectParameter.getUserPerPage(), pageMap.get(new Integer(pageNumber)));
			response = HttpRequestHandler.handleRequest("/Users", queryOption, sessionKey);
		}
		/**
		 * Finally, if the token can't be found in the page map, this request can not be 
		 * satisfied. However, this case can only arise if there is a bug in the application.
		 */
		else{
			throw new CustomAzureException(CustomObjectParameter.internalError, CustomObjectParameter.internalErrorMessage, null);
		}
				
		/**
		 * Get an array of JSON Objects by parsing the response.
		 */
		JSONArray userArray = JSONDataParser.parseJSonDataCollection(response);
		
		/**
		 * Retrive the skiptoken for the next page returned with the http response.
		 */
		String skipTokenForNextPage = JSONDataParser.parseSkipTokenForNextPage(response);
		
		/**
		 * Create a new UserPageInfo that would hold all the users and set the pageNumber
		 * with the current pageNumber.
		 */
		UserPageInfo thisPage = new UserPageInfo();	
		thisPage.setPageNumber(pageNumber);
		
		/**
		 * If the skiptoken for the next page is not empty,
		 * that is there is indeed a next page, set the hasNextPage attribute to 
		 * true and put this skiptoken to the pagemap..
		 */
		if(!skipTokenForNextPage.equalsIgnoreCase("")){
			pageMap.put(new Integer(pageNumber + 1), skipTokenForNextPage);
			thisPage.setHasNextPage(true);
		}
		
		/**
		 * Else, just set the hasNextPage to false.
		 */
		else{
			thisPage.setHasNextPage(false);
		}
		
		/**
		 * If it is the first page, then we don't have any previous page.
		 */
		if(pageNumber == 1){
			thisPage.setHasPrevPage(false);
		}
		
		/**
		 * Otherwise we do have a previous page.
		 */
		else{
			thisPage.setHasPrevPage(true);
		}
				
		/**
		 * For all the users in the JSON Array, retrieve the DisplayName, ObjectId and UserPrincipalName and add
		 * them in the UserPageInfo.
		 */
		for(int i = 0; i < userArray.length(); i++){
			try {
				thisPage.addNewUserInfo(userArray.getJSONObject(i).optString("DisplayName"), 
										userArray.getJSONObject(i).optString("ObjectId"), 
										userArray.getJSONObject(i).optString("UserPrincipalName") );
							
			} catch (JSONException e) {
				throw new CustomAzureException(CustomObjectParameter.ErrorParsingJSONException, e.getMessage(), e);
			}
		}
				
		return thisPage;
		
	}
	
	

	/**
	 * This method gets an particular user identified by its user ID.
	 * @param objectId ObjectId of the user to be retrieved.
	 * @return An user object populated with the relevant attributes.
	 * @throws CustomAzureException If an exception occurs during the process.
	 */
	public static User getUser(String objectId, String sessionKey) throws CustomAzureException{
				
		/**
		 * Create the additional path and also we don't have any query option
		 * for this operation. Invoke the handleRequest method and get the 
		 * response in a String object.		
		 */
		String response = HttpRequestHandler.handleRequest(
				String.format("/Users('User_%s')", objectId), 
				null,
				sessionKey);
		
		/**
		 * Get a JSONObject that would hold the attributes of the user.
		 */
		JSONObject userObject = JSONDataParser.parseJSonDataSingleObject(response);				
		
		/**
		 * Creates a new user object and get the data copied over from the JSONObject
		 * to this object.
		 */
		User user = new User();
		AzureUtilities.copyAttrFromJSONObject(userObject, user);
		
		/**
		 * Get the manager of this user and set them in the user object.
		 */
		JSONObject manager = getManager(userObject.optString("ObjectId"), sessionKey);
		if(manager != null){
			user.setManager(manager.optString("DisplayName"), manager.optString("ObjectId"));
		}
		
		/**
		 * Get the directReport objects of this user and populate them
		 * in the user object.
		 */
		JSONArray directReports = getDirectReports(userObject.optString("ObjectId"), sessionKey);
		for(int i = 0; i < directReports.length(); i++ ){
			user.addNewDirectReport(directReports.optJSONObject(i).optString("DisplayName"), directReports.optJSONObject(i).optString("ObjectId"));
		}
		
		/**
		 * Get the list of all objects of which this user is a member.
		 */
		JSONArray groups = getMembersOf(userObject.optString("ObjectId"), sessionKey);
		for(int i = 0; i < groups.length(); i++){
			
			/**
			 * If this particular object is of type group,
			 * add it to the user object as a group.
			 */
			if(groups.optJSONObject(i).optString("ObjectType").equalsIgnoreCase("group")){
				user.addNewGroup(groups.optJSONObject(i).optString("DisplayName"), groups.optJSONObject(i).optString("ObjectId"));
			}
			
			/**
			 * If this particular object is of type role,
			 * add it to the user object as a role.
			 */
			else if(groups.optJSONObject(i).optString("ObjectType").equalsIgnoreCase("role")){
				user.addNewRole(groups.optJSONObject(i).optString("DisplayName"), groups.optJSONObject(i).optString("ObjectId"));
			}
		}
		
		return user;
	}

	
	/**
	 * This method returns the list of all objects of which a particular user is a member.
	 * @param objectID ObjectId of the member user.
	 * @return An array of JSON Object containing the list of MemberOf objects.
	 * @throws CustomAzureException If the operation can not be carried out successfully.
	 */
	private static JSONArray getMembersOf(String objectID, String sessionKey) throws CustomAzureException {
		
		/**
		 * Send a particular http get request and receive the request.
		 */
		String response = HttpRequestHandler.handleRequest(
				String.format("/Users('User_%s')/MemberOf", objectID), 
				null,
				sessionKey);

		JSONArray groups = null;
		
		try {
			/**
			 * Retrieve the json array and set it to groups.
			 */
			groups = (new JSONObject(response)).
					getJSONObject("d").
					getJSONArray("results");
		} catch (JSONException e) {
			new CustomAzureException(
					CustomObjectParameter.ErrorParsingJSONException, 
					e.getMessage(), 
					e);
		}			
		
		return groups;
	}



	/**
	 * This method would return the list of Direct Reports an particular user identified by
	 * ObjectId has.
	 * @param objectID The ObjectId of the User.
	 * @return The list of Direct Reports.
	 * @throws CustomAzureException Throws an exception if the operation can't be performed successfully.
	 */
	private static JSONArray getDirectReports(String objectID, String sessionKey) throws CustomAzureException {
		
		/**
		 * Send the http request, and get the response.
		 */
		String response = HttpRequestHandler.handleRequest(
				String.format("/Users('User_%s')/DirectReports", objectID), 
				null, sessionKey);
		
		
		JSONArray dReports = null;
		
		try {
			
			/**
			 * Retrieve the json array and set it to dReports.
			 */
			dReports = (new JSONObject(response)).
					getJSONObject("d").
					getJSONArray("results");
		} catch (JSONException e) {
			throw new CustomAzureException(
					CustomObjectParameter.ErrorParsingJSONException, 
					e.getMessage(), 
					e);
		}
		
		return dReports;
	}
	
	
	
	/**
	 * This method returns an JSON Object containing the information of a manager of a
	 * particular object identified by its ObjectId.
	 * @param objectID The objectId of the user whose manager is to be returned.
	 * @return The information of the manager in a JSON Object.
	 * @throws CustomAzureException If the operation can not be performed successfully.
	 */
	private static JSONObject getManager(String objectID, String sessionKey) throws CustomAzureException{
		
		JSONObject object = null;
		try {
			/**
			 * Send the http request and get the response.
			 */
			String response = HttpRequestHandler.handleRequest(
					String.format("/Users('User_%s')/Manager", objectID), 
					null,
					sessionKey);
			
		    object = (new JSONObject(response)).optJSONObject("d"); 
		} catch (CustomAzureException e) {
			/**
			 * If this is a MessageIdResourceNofFound exception, then it simply means
			 * the manager does not exist. So, return null.
			 */
			if(e.getCode().equalsIgnoreCase(CustomObjectParameter.MessageIdResourceNotFound)){
				return null;
			}
			
			/**
			 * Else, this is some other exception, therefore can't be ignored.
			 */
			else{
				throw e;
			}
		} catch (JSONException e) {
			/**
			 * If there is an exception parsing JSON data, throw a new sample application.
			 */
			throw new CustomAzureException(
					CustomObjectParameter.ErrorParsingJSONException, 
					e.getMessage(), 
					e);
		}
		return object;

	}


	/**
	 * This method returns the results of a query for users.
	 * @param attributeName The attribute on which the queries are made of.
	 * @param opName The operator name that would be applied to the attribute.
	 * @param searchString The string that would be searched for this attribute.
	 * @return A page of users satisfying this query criteria.
	 * @throws CustomAzureException If the operation can not be carried out successfully.
	 */

	public static UserPageInfo queryUsers(
			String attributeName, 
			String opName, 
			String searchString,
			String sessionKey) throws CustomAzureException {
		
		/**
		 * This object would hold all the user information.
		 */
		UserPageInfo thisPage = new UserPageInfo();
		
		if(attributeName.trim().isEmpty() || opName.trim().isEmpty() || 
				searchString.trim().isEmpty() || (attributeName == null) || 
				(opName == null) || (searchString == null)){
				/**
				 * If any of the agruments are empty or null, throw an exception. In the ideal case,
				 * this case should never happen since this case should be taken care of in the client
				 * side. 	
				 */
				throw new CustomAzureException(CustomObjectParameter.internalError, CustomObjectParameter.internalErrorMessage, null);
				}
		
		/**
		 * Build the queryOption.
		 */
		String queryOption = null;
		/**
		 * If this is an account Enabled query.
		 */
		if(attributeName.trim().equalsIgnoreCase("AccountEnabled")){
			queryOption = String.format("$filter=%s %s %s", attributeName, opName, searchString);
		}else{
			/**
			 * If this is an general query.
			 */
			queryOption = String.format("$filter=%s %s '%s'", attributeName, opName, searchString);
		}

		/**
		 * Send the query with the built queryOption.
		 */
		String response = HttpRequestHandler.handleRequest("/Users", queryOption, sessionKey);
		JSONArray users = JSONDataParser.parseJSonDataCollection(response);
		
		/**
		 * Get the DisplayName, ObjectId, UserPrincipalName from the JSON Array and populate them
		 * in the page object.
		 */
		for(int i = 0; i < users.length(); i++){
			try {
				thisPage.addNewUserInfo(users.getJSONObject(i).optString("DisplayName"), 
										users.getJSONObject(i).optString("ObjectId"), 
										users.getJSONObject(i).optString("UserPrincipalName") );
							
			} catch (JSONException e) {
				throw new CustomAzureException(
						CustomObjectParameter.ErrorParsingJSONException,
						e.getMessage(),
						e);
			}
		}
		return thisPage;
	}



	/**
	 * This method returns the list of company administrators.
	 * @return A list of company administrators.
	 * @throws CustomAzureException If the operation can not be done successfully.
	 */
	public static UserPageInfo queryCompanyAdmins(String sessionKey) throws CustomAzureException {
		String companyAdminRoleId = getCompanyAdminRoleId(sessionKey);
		/**
		 * If no such Administrator Role exists, handle the error.
		 */
		if(companyAdminRoleId == null){
			throw new CustomAzureException(CustomObjectParameter.NoCompanyAdminRole, CustomObjectParameter.NoCompanyAdminRoleMessage, null);
		}
		
		/**
		 * Get the list of users that belong to the role identified by the 
		 * companyAdminRoleId.
		 */
		UserPageInfo thisPage = RestAPIGroupServices.getRoleMembers(companyAdminRoleId, sessionKey);
		
		return thisPage;
	}



	/**
	 * This method returns  the Object Id of the "Company Administrator" Role. 
	 * @return The Object Id of the company Administrator Role.
	 * @throws CustomAzureException
	 */
	public static String getCompanyAdminRoleId(String sessionKey) throws CustomAzureException {
		String response = HttpRequestHandler.handleRequest("/Roles", null, sessionKey);
		JSONArray allRoles = JSONDataParser.parseJSonDataCollection(response);
		for(int i = 0; i < allRoles.length(); i++){
			if(allRoles.optJSONObject(i).optString("DisplayName").equalsIgnoreCase("Company Administrator")){
				return allRoles.optJSONObject(i).optString("ObjectId");
			}
		}
		return null;
	}
	
}
