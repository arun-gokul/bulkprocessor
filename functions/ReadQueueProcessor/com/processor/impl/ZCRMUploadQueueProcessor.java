//$Id$
package com.processor.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.java.bean.ZCRMFieldMeta;
import com.opencsv.CSVReader;
import com.processor.ZCRMQueueProcessor;
import com.util.CommonUtil;
import com.util.Tables.WRITE_QUEUE;
import com.zc.component.files.ZCFile;
import com.zc.component.zcql.ZCQL;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ZCRMUploadQueueProcessor implements ZCRMQueueProcessor {

	private static final Logger LOGGER = Logger.getLogger(ZCRMUploadQueueProcessor.class.getName());

	@Override
	public void process(JSONArray arr) throws Exception {

		OkHttpClient httpClient = new OkHttpClient();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			if (obj.has(WRITE_QUEUE.TABLE.value())) {
				obj = obj.getJSONObject(WRITE_QUEUE.TABLE.value());
			}
			String moduleName = obj.getString(WRITE_QUEUE.MODULE.value());
			ZCRMFieldMeta meta = CommonUtil.getFields(moduleName);

			Long fileID = obj.getLong(WRITE_QUEUE.FILE_ID.value());
			Long rowID = obj.getLong("ROWID");
			if (obj.getBoolean("IS_UPLOADED")) {
				continue;
			}
			InputStream file = ZCFile.getInstance()
					.getFolderInstance(Long.parseLong(System.getenv(CommonUtil.FOLDER_ID))).downloadFile(fileID);
			File csvFile = new File("/tmp/data.csv");
			try (FileOutputStream csvOutputStream = new FileOutputStream(csvFile)) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = file.read(buffer)) != -1) {
					csvOutputStream.write(buffer, 0, bytesRead);
				}
			}
			CSVReader reader = new CSVReader(new FileReader(csvFile));
			String[] firstLine = reader.readNext();
			reader.close();
			File zipFile = new File("/tmp/out.zip");
			try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
					FileInputStream inputStream = new FileInputStream(csvFile)) {
				ZipEntry entry = new ZipEntry("data.csv");
				zipOutputStream.putNextEntry(entry);
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					zipOutputStream.write(buffer, 0, bytesRead);
				}
				zipOutputStream.closeEntry();
			}

			String accessToken = CommonUtil.getCRMAccessToken();
			String zgid = "";
			Request orgReq = new Request.Builder().url(CommonUtil.CRM_ORG_GET_URL)
					.addHeader("Authorization", accessToken).method("GET", null).build();
			try (Response response = httpClient.newCall(orgReq).execute()) {
				if (!response.isSuccessful())
					throw new IOException("Unexpected code " + response.body().string());
				JSONObject bulkResponse = new JSONObject(response.body().string());
				zgid = bulkResponse.getJSONArray("org").getJSONObject(0).getString("zgid");
			}
			RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file",
					zipFile.getName(), RequestBody.create(MediaType.parse("application/zip"), zipFile)).build();
			Request request = new Request.Builder().url(CommonUtil.CRM_UPLOAD_URL)
					.addHeader("Authorization", accessToken).addHeader("feature", "bulk-write")
					.addHeader("X-CRM-ORG", zgid).method("POST", formBody).build();
			LOGGER.log(Level.SEVERE, request.url().toString());
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful())
					throw new IOException("Unexpected code " + response.body().string());
				JSONObject bulkResponse = new JSONObject(response.body().string());
				String crmFileId = bulkResponse.getJSONObject("details").getString("file_id");
				LOGGER.log(Level.SEVERE, crmFileId);
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
				JSONArray fieldMappings = new JSONArray();
				for (int j = 0; j < firstLine.length; j++) {
					String dataType = meta.getFieldDataTypeMap().get(firstLine[j]);
					String apiName = firstLine[j];
					if (apiName.equalsIgnoreCase("Id")) {
						apiName = apiName.toLowerCase();
					}
					if (apiName.equals("id") || (dataType != null && !meta.readOnlyFields.contains(firstLine[i]))
							|| !dataType.equalsIgnoreCase("profileimage")
							|| !dataType.equalsIgnoreCase("formula") || !dataType.equalsIgnoreCase("autonumber")
							|| !dataType.equalsIgnoreCase("fileupload") || !dataType.equalsIgnoreCase("imageupload")) {
						JSONObject fieldMapJSON = new JSONObject();
						fieldMapJSON.put("api_name", apiName);
						fieldMapJSON.put("index", j);
						if (dataType != null && dataType.contains("lookup")) {
							fieldMapJSON.put("find_by", "id");
						}
						fieldMappings.put(fieldMapJSON);
					}
				}
				LOGGER.log(Level.SEVERE, "JSON " + fieldMappings);
				resourceObj.put("find_by", "id");
				if (fieldMappings.length() > 1) {
					resourceObj.put("field_mappings", fieldMappings);
				}
				resourceArr.put(resourceObj);
				bulkWriteInput.put("resource", resourceArr);
				LOGGER.log(Level.SEVERE, bulkWriteInput.toString());
				Request bulkWriteRequest = new Request.Builder().url(CommonUtil.CRM_BULK_READ_URL)
						.addHeader("Authorization", accessToken)
						.method("POST",
								RequestBody.create(MediaType.parse("application/json"), bulkWriteInput.toString()))
						.build();
				LOGGER.log(Level.SEVERE, request.url().toString());
				try (Response bulkWriteResponce = httpClient.newCall(bulkWriteRequest).execute()) {
					if (!bulkWriteResponce.isSuccessful()) {
						throw new IOException("Unexpected code " + bulkWriteResponce.body().string());
					}
					JSONObject bulkWriteResponseJSON = new JSONObject(bulkWriteResponce.body().string());
					String jobId = bulkWriteResponseJSON.getJSONObject("details").getString("id");
					ZCQL.getInstance().executeQuery("update WriteQueue set CRM_JOB_ID='" + jobId
							+ "',IS_UPLOADED=true where ROWID='" + rowID + "'");
				}
			}
		}
	}
}
