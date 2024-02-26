//$Id$
package com.java.bean;

import java.util.HashMap;

public class ZCRMRecord {
	public String moduleName;
	
	public String id;
	
	public HashMap<String,Object> data = new HashMap<String, Object>();

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
