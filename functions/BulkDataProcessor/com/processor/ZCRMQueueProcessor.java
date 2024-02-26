//$Id$
package com.processor;

import org.json.JSONArray;
import org.json.JSONObject;

public interface ZCRMQueueProcessor {

	public void process(JSONObject obj, JSONArray arr) throws Exception;

}
