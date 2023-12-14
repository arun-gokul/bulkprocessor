//$Id$

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.util.CommonUtil;
import com.zc.component.files.ZCFile;
import com.zc.component.zcql.ZCQL;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ZCRMBulkUpload {

	private static String uploadURL = "https://content.zohoapis.com/crm/v5/upload";
	private static String bulkWriteURL = "https://zohoapis.com/crm/bulk/v5/write";
	private static String orgURL="https://zohoapis.com/crm/v5/org";

	public static void uploadFile(JSONArray arr) throws Exception {

		OkHttpClient httpClient = new OkHttpClient();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			if(obj.has("WriteQueue")) {
				obj = obj.getJSONObject("BulkWrite");
			}
			String moduleName = obj.getString("MODULE");
			Long fileID = obj.getLong("FILE_ID");
			Long rowID = obj.getLong("ROWID");
			if (obj.getBoolean("IS_UPLOADED")) {
				continue;
			}
			InputStream file = ZCFile.getInstance().getFolderInstance("CSVFILES").downloadFile(fileID);
			File csvFile = new File("/tmp/data.csv");
			try (FileOutputStream csvOutputStream = new FileOutputStream(csvFile)) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = file.read(buffer)) != -1) {
					csvOutputStream.write(buffer,0,bytesRead);
				}
			}
			File zipFile = new File("/tmp/out.zip");
			try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile)); FileInputStream inputStream = new FileInputStream(csvFile)) {
				ZipEntry entry = new ZipEntry("data.csv");
				zipOutputStream.putNextEntry(entry);
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					zipOutputStream.write(buffer,0,bytesRead);
				}
				zipOutputStream.closeEntry();
			}

			String accessToken = CommonUtil.getCRMAccessToken();
			String zgid="";
			Request orgReq = new Request.Builder().url(orgURL).addHeader("Authorization", accessToken).method("GET", null).build();
			try(Response response = httpClient.newCall(orgReq).execute()){
				if (!response.isSuccessful())
					throw new IOException("Unexpected code " + response.body().string());
				JSONObject bulkResponse = new JSONObject(response.body().string());
				zgid=bulkResponse.getJSONArray("org").getJSONObject(0).getString("zgid");
			}

			RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", zipFile.getName(), RequestBody.create(MediaType.parse("application/zip"), zipFile)).build();
			Request request = new Request.Builder().url(uploadURL).addHeader("Authorization", accessToken).addHeader("feature", "bulk-write").addHeader("X-CRM-ORG", zgid).method("POST", formBody).build();

			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful())
					throw new IOException("Unexpected code " + response.body().string());
				JSONObject bulkResponse = new JSONObject(response.body().string());
				String crmFileId = bulkResponse.getJSONObject("details").getString("file_id");
				JSONObject bulkWriteInput = new JSONObject();
				bulkWriteInput.put("operation", "upsert");
				bulkWriteInput.put("ignore_empty", true);
				JSONArray resourceArr = new JSONArray();
				JSONObject resourceObj = new JSONObject();
				resourceObj.put("type", "data");
				JSONObject moduleObj = new JSONObject();
				moduleObj.put("api_name", moduleName);
				resourceObj.put("module", moduleObj);
				resourceObj.put("file_id", crmFileId);
				resourceObj.put("find_by", "id");
				resourceArr.put(resourceObj);
				bulkWriteInput.put("resource", resourceArr);
				Request bulkWriteRequest = new Request.Builder().url(bulkWriteURL).addHeader("Authorization", accessToken).method("POST", RequestBody.create(MediaType.parse("application/json"), bulkWriteInput.toString())).build();
				try (Response bulkWriteResponce = httpClient.newCall(bulkWriteRequest).execute()) {
					if (!bulkWriteResponce.isSuccessful())
					{
						throw new IOException("Unexpected code " + bulkWriteResponce.body().string());
					}			
					JSONObject bulkWriteResponseJSON = new JSONObject(bulkWriteResponce.body().string());
					String jobId = bulkWriteResponseJSON.getJSONObject("details").getString("id");
					ZCQL.getInstance().executeQuery("update WriteQueue set CRM_JOB_ID='" + jobId + "',IS_UPLOADED=true where ROWID='" + rowID + "'");
				}

			}
		}
	}

}
