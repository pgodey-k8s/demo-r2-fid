package com.rli.scripts.customobjects.sharepointco;

/*
 * PropertyData bean used for capturing the values we get from webservice
 */
public class PropertyData {
	private boolean isPrivacyChanged;

	private boolean isValueChanged;

	private java.lang.String name;

	private String privacy;

	private String[] values;

	public PropertyData() {
	}

	public PropertyData(boolean isPrivacyChanged, boolean isValueChanged,
			java.lang.String name, String privacy, String[] values) {
		this.isPrivacyChanged = isPrivacyChanged;
		this.isValueChanged = isValueChanged;
		this.name = name;
		this.privacy = privacy;
		this.values = values;
	}

	/**
	 * Gets the isPrivacyChanged value for this PropertyData.
	 * 
	 * @return isPrivacyChanged
	 */
	public boolean isIsPrivacyChanged() {
		return isPrivacyChanged;
	}

	/**
	 * Sets the isPrivacyChanged value for this PropertyData.
	 * 
	 * @param isPrivacyChanged
	 */
	public void setIsPrivacyChanged(boolean isPrivacyChanged) {
		this.isPrivacyChanged = isPrivacyChanged;
	}

	/**
	 * Gets the isValueChanged value for this PropertyData.
	 * 
	 * @return isValueChanged
	 */
	public boolean isIsValueChanged() {
		return isValueChanged;
	}

	/**
	 * Sets the isValueChanged value for this PropertyData.
	 * 
	 * @param isValueChanged
	 */
	public void setIsValueChanged(boolean isValueChanged) {
		this.isValueChanged = isValueChanged;
	}

	/**
	 * Gets the name value for this PropertyData.
	 * 
	 * @return name
	 */
	public java.lang.String getName() {
		return name;
	}

	/**
	 * Sets the name value for this PropertyData.
	 * 
	 * @param name
	 */
	public void setName(java.lang.String name) {
		this.name = name;
	}

	/**
	 * Gets the privacy value for this PropertyData.
	 * 
	 * @return privacy
	 */
	public String getPrivacy() {
		return privacy;
	}

	/**
	 * Sets the privacy value for this PropertyData.
	 * 
	 * @param privacy
	 */
	public void setPrivacy(String privacy) {
		this.privacy = privacy;
	}

	public String[] getValues() {
		return values;
	}

	public void setValues(String[] values) {
		this.values = values;
	}

	
}
