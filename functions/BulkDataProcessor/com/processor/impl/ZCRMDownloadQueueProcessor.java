//$Id$
package com.processor.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.processor.ZCRMQueueProcessor;
import com.util.CommonUtil;
import com.util.Tables.BULK_READ;
import com.util.Tables.READ_QUEUE;
import com.zc.common.ZCProject;
import com.zc.component.files.ZCFile;
import com.zc.component.files.ZCFileDetail;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.zcql.ZCQL;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ZCRMDownloadQueueProcessor implements ZCRMQueueProcessor {

	@SuppressWarnings("resource")
	@Override
	public void process(JSONObject projectData, JSONArray tableData) throws Exception {
		ZCProject.initProject();
		OkHttpClient httpClient = new OkHttpClient();
		for (int i = 0; i < tableData.length(); i++) {
			JSONObject rowData = tableData.getJSONObject(i);
			if (rowData.has(CommonUtil.BULK_READ)) {
				rowData = rowData.getJSONObject(BULK_READ.TABLE.value());
			}
			Integer pageNo = rowData.getInt(BULK_READ.REQUESTED_PAGE_NO.value());
			String moduleName = rowData.getString(BULK_READ.MODULE_NAME.value());
			Integer fetchedPageNo = rowData.getInt(BULK_READ.FETCHED_PAGE_NO.value());
			String downloadURL = (rowData.get(BULK_READ.DOWNLOAD_URL.value()) != null
					&& rowData.get(BULK_READ.DOWNLOAD_URL.value()) != JSONObject.NULL)
							? rowData.getString(BULK_READ.DOWNLOAD_URL.value())
							: null;
			String crmJobId = (rowData.get(BULK_READ.CRMJOBID.value()) != null
					&& rowData.get(BULK_READ.CRMJOBID.value()) != JSONObject.NULL)
							? rowData.getString(BULK_READ.CRMJOBID.value())
							: null;
			String[] fieldsToBeProcessed = rowData.get(BULK_READ.FIELDS_TO_BE_PROCESSED.value()).toString().split(",");
			String accessToken = CommonUtil.getCRMAccessToken();
			if (crmJobId != null && downloadURL != null) {
				Request downloadURLReq = new Request.Builder().url(downloadURL)
						.addHeader("Authorization", accessToken)
						.method("GET", null).build();
				try (Response response = httpClient.newCall(downloadURLReq).execute()) {
					if (!response.isSuccessful())
						throw new IOException("Unexpected code " + response);
					InputStream fileStream = response.body().byteStream();
					File tmpFile = new File(System.getProperty("java.io.tmpdir") + "/" + crmJobId + ".zip");
					try (FileOutputStream writer = new FileOutputStream(tmpFile)) {
						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = fileStream.read(buffer)) != -1) {
							writer.write(buffer, 0, bytesRead);
						}
					}
					try (ZipInputStream zipStream = new ZipInputStream(new FileInputStream(tmpFile))) {
						ZipEntry entry;
						while ((entry = zipStream.getNextEntry()) != null) {
							String fileName = entry.getName();
							if (fileName.contains("csv")) {
								File csvFile = new File(System.getProperty("java.io.tmpdir") + "/" + crmJobId + ".csv");
								try (FileOutputStream writer = new FileOutputStream(csvFile)) {
									byte[] buffer = new byte[4096];
									int bytesRead;
									while ((bytesRead = zipStream.read(buffer)) != -1) {
										writer.write(buffer, 0, bytesRead);
									}
								}
								ZCFileDetail fileDetails = ZCFile.getInstance().getFolderInstance(CommonUtil.CSVFILES)
										.uploadFile(csvFile);
								ZCRowObject rowObj = ZCRowObject.getInstance();
								rowObj.set(READ_QUEUE.FILEID.value(), fileDetails.getFileId());
								rowObj.set(READ_QUEUE.CRM_JOB_ID.value(), crmJobId);
								rowObj.set(READ_QUEUE.MODULE.value(), moduleName);
								ZCObject.getInstance().getTableInstance(READ_QUEUE.TABLE.value()).insertRow(rowObj);
							}
							zipStream.closeEntry();
						}
					}

				}
			}

			if (fetchedPageNo < pageNo) {

				Long id = rowData.getLong("ROWID");
				JSONObject callBackObj = new JSONObject();
				callBackObj.put("url", CommonUtil.getCallBackURL(projectData));
				callBackObj.put("method", "post");

				JSONObject module = new JSONObject();
				module.put("api_name", moduleName);

				JSONObject query = new JSONObject();
				query.put("module", module);
				query.put("fields", fieldsToBeProcessed);

				JSONObject input = new JSONObject();
				input.put("callback", callBackObj);
				input.put("query", query);
				input.put("file_type", "csv");

				Request request = new Request.Builder().url(CommonUtil.CRM_BULK_READ_URL)
						.addHeader("Authorization", accessToken)
						.method("POST", RequestBody.create(MediaType.parse("application/json"), input.toString()))
						.build();

				try (Response response = httpClient.newCall(request).execute()) {
					if (!response.isSuccessful())
						throw new IOException("Unexpected code " + response.body().string());
					JSONObject bulkResponse = new JSONObject(response.body().string());
					String jobId = bulkResponse.getJSONArray("data").getJSONObject(0).getJSONObject("details").get("id")
							.toString();
					ZCQL.getInstance().executeQuery("update BulkRead set CRMJOBID='" + jobId + "',FETCHED_PAGE_NO="
							+ pageNo + "  where ROWID='" + id + "'");
				}
			}

		}

	}

}
