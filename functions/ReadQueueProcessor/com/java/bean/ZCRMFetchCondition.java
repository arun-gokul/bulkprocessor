//$Id$
package com.java.bean;

public class ZCRMFetchCondition {
	
	private String columnName;
	private String value;
	private String comparator;
	private enum COMPARATOR{
		EQUAL("equal"),
		NOT_EQUAL("not_equal"),
		IN("in"),
		NOT_IN("not_in"),
		CONTAINS("contains"),
		NOT_CONTAINS("not_contains"),
		STARTS_WITH("starts_with"),
		ENDS_WITH("ends_with");
		String value;
		COMPARATOR(String value){
			this.value=value;
			
		}
		public String getValue() {
			return this.value;
		}
		
	}	

}
