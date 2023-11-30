//$Id$
package com.processor;

import org.json.JSONArray;

public interface ZCRMQueueProcessor {
	
	public void process(JSONArray arr) throws Exception;

}
