//$Id$
package com.util;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.java.bean.ZCRMFieldMeta;
import com.zc.auth.connectors.ZCConnection;
import com.zc.auth.connectors.ZCConnector;
import com.zc.common.ZCProject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommonUtil {

	public static final String CRM_UPLOAD_URL = "https://content.zohoapis.com/crm/v5/upload";
	public static final String CRM_BULK_READ_URL = "https://zohoapis.com/crm/bulk/v5/write";
	public static final String CRM_ORG_GET_URL = "https://zohoapis.com/crm/v5/org";
	public static final String CRM_BULK_WRITE_URL = "https://zohoapis.com/crm/bulk/v5/read";
	public static final String CRM_FIELD_API = "https://zohoapis.com/crm/v5/settings/fields";
	public static final String BULK_READ = "BulkRead";
	public static final String CSVFILES = "CSVFILES";

	@SuppressWarnings("unchecked")
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
		authJson.put("refresh_token", refreshToken);
		org.json.simple.JSONObject connectorJson = new org.json.simple.JSONObject();
		connectorJson.put("CRMConnector", authJson);

		ZCConnection conn = ZCConnection.getInstance(connectorJson);
		ZCConnector crmConnector = conn.getConnector("CRMConnector");
		return "Zoho-oauthtoken " + crmConnector.getAccessToken();
	}

	public static String getCallBackURL(JSONObject projectData) throws Exception {

		return ZCProject.getProjectConfig(Long.parseLong(projectData.getString("id"))).getProjectDomain()
				+ "/server/zohocrm_bulk_callback/job?catalyst-codelib-secret-key="
				+ System.getenv("CODELIB_SECRET_KEY");

	}

	public static ZCRMFieldMeta getFields(String module) throws Exception {

		ZCRMFieldMeta meta = new ZCRMFieldMeta();
		OkHttpClient httpClient = new OkHttpClient();
		Request downloadRequest = new Request.Builder().url(CRM_FIELD_API + "?module=" + module + "&type=all")
				.addHeader("Authorization", getCRMAccessToken()).method("GET", null).build();
		try (Response response = httpClient.newCall(downloadRequest).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}
			JSONObject fieldData = new JSONObject(response.body().string());
			JSONArray fields = fieldData.getJSONArray("fields");
			for (int i = 0; i < fields.length(); i++) {
				JSONObject fieldDetails = fields.getJSONObject(i);
				String apiName = fieldDetails.getString("api_name");
				String dataType = fieldDetails.getString("data_type");
				boolean isReadOnly = fieldDetails.getJSONObject("operation_type").getBoolean("api_update");
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
