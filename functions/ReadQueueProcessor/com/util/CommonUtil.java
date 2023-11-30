//$Id$
package com.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.catalyst.config.ZCThreadLocal;
import com.java.bean.ZCRMFieldMeta;
import com.zc.auth.connectors.ZCConnection;
import com.zc.auth.connectors.ZCConnector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommonUtil {

	public static final String FOLDER_ID = "FOLDER_ID";

	public static final String CRM_UPLOAD_URL = "https://content.zohoapis.com/crm/v5/upload";
	public static final String CRM_BULK_READ_URL = "https://zohoapis.com/crm/bulk/v5/write";
	public static final String CRM_ORG_GET_URL = "https://zohoapis.com/crm/v5/org";
	public static final String CRM_BULK_WRITE_URL = "https://zohoapis.com/crm/bulk/v5/read";
	public static final String CRM_FIELD_API = "https://zohoapis.com/crm/v5/settings/fields";
	public static final String BULK_READ = "BulkRead";

	public static String getCRMAccessToken() throws Exception {
		String clientId = System.getenv("CRM_CLIENT_ID");
		String clientSecret = System.getenv("CRM_SECRET");
		String refreshToken = System.getenv("CRM_REFRESH_TOKEN");

		org.json.simple.JSONObject authJson = new org.json.simple.JSONObject();
		// The json object holds the client id, client secret, refresh token and refresh
		// url
		authJson.put("client_id", clientId);
		authJson.put("client_secret", clientSecret);
		authJson.put("auth_url", "https://accounts.zoho.com/oauth/v2/token");
		authJson.put("refresh_url", "https://accounts.zoho.com/oauth/v2/token");
		// If referesh token is not provided, then code should be provided to generate
		// the refresh token.
		authJson.put("refresh_token", refreshToken);
		org.json.simple.JSONObject connectorJson = new org.json.simple.JSONObject();
		connectorJson.put("CRMConnector", authJson);
		// It can have multiple service connector information
		ZCConnection conn = ZCConnection.getInstance(connectorJson);
		ZCConnector crmConnector = conn.getConnector("CRMConnector");
		return "Zoho-oauthtoken " + crmConnector.getAccessToken();

	}

	public static String getCallBackURL() {
		String domain = System.getenv("DOMAIN");
		return domain + "/server/zohocrm_bulk_callback/write";
	}

	public static ZCRMFieldMeta getFields(String module) throws Exception {
		String accessToken = getCRMAccessToken();
		ZCRMFieldMeta meta = new ZCRMFieldMeta();
		OkHttpClient httpClient = new OkHttpClient();
		Request downloadURLReq = new Request.Builder().url(CRM_FIELD_API + "?module=" + module + "&type=all")
				.addHeader("Authorization", accessToken).method("GET", null).build();
		try (Response response = httpClient.newCall(downloadURLReq).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}
			String fieldResp = response.body().string();
			JSONObject fieldJSON = new JSONObject(fieldResp);
			JSONArray arr = fieldJSON.getJSONArray("fields");
			for (int i = 0; i < arr.length(); i++) {
				JSONObject fieldDetails = arr.getJSONObject(i);
				String apiName = fieldDetails.getString("api_name");
				String dataType = fieldDetails.getString("data_type");
				JSONObject operation_type = fieldDetails.getJSONObject("operation_type");
				boolean isReadOnly = operation_type.getBoolean("api_update");
				if (apiName.contains("Tag") || dataType.equalsIgnoreCase("profileimage")
						|| dataType.equalsIgnoreCase("formula") || dataType.equalsIgnoreCase("autonumber")
						|| dataType.equalsIgnoreCase("fileupload") || dataType.equalsIgnoreCase("imageupload")) {
					meta.addField(apiName, dataType, true);
				} else {
					meta.addField(apiName, dataType, !isReadOnly);
				}
			}
			return meta;
		}
	}

}
