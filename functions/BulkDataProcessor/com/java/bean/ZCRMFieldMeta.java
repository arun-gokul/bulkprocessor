//$Id$
package com.java.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ZCRMFieldMeta {

	public List<String> fields= new ArrayList<String>();

	public HashMap<String, String> fieldDataTypeMap = new HashMap<String, String>();
	
	public List<String> readOnlyFields = new ArrayList<String>();

	public List<String> getFields() {
		return fields;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	public HashMap<String, String> getFieldDataTypeMap() {
		return fieldDataTypeMap;
	}

	public void setFieldDataTypeMap(HashMap<String, String> fieldDataTypeMap) {
		this.fieldDataTypeMap = fieldDataTypeMap;
	}
	
	public void addField(String field,String dataType) {
		this.addField(field, dataType,false);
	}

	public void addField(String fieldName, String dataType,boolean isReadOnly) {
		this.getFields().add(fieldName);
		this.getFieldDataTypeMap().put(fieldName, dataType);
		if(isReadOnly) {
			this.readOnlyFields.add(fieldName);
		}
	}
}
