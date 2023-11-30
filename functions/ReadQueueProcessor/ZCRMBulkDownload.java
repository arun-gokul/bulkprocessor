//$Id$

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.util.CommonUtil;
import com.zc.common.ZCProject;
import com.zc.component.cache.ZCCache;
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

public class ZCRMBulkDownload {
	public static String url = "https://zohoapis.com/crm/bulk/v5/read";
	
	private  static final Logger LOGGER = Logger.getLogger(ZCRMBulkDownload.class.getName());

	public static void download(JSONArray arr) throws Exception {
		ZCProject.initProject();
		OkHttpClient httpClient = new OkHttpClient();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject jsonObj = arr.getJSONObject(i);
			if(jsonObj.has(CommonUtil.BULK_READ)) {
				jsonObj = jsonObj.getJSONObject(CommonUtil.BULK_READ);
			}
			Integer pageNo = jsonObj.getInt("REQUESTED_PAGE_NO");
			String moduleName = jsonObj.getString("MODULE_NAME");
			Integer fetchedPageNo = jsonObj.getInt("FETCHED_PAGE_NO");
			String accessToken = CommonUtil.getCRMAccessToken();
			String downloadURL = (jsonObj.get("DOWNLOAD_URL") != null && jsonObj.get("DOWNLOAD_URL") != JSONObject.NULL) ? jsonObj.getString("DOWNLOAD_URL") : null;
			String crmJobId = (jsonObj.get("CRMJOBID") != null && jsonObj.get("CRMJOBID") != JSONObject.NULL) ? jsonObj.getString("CRMJOBID") : null;
			if (crmJobId != null && downloadURL != null) {
				Request downloadURLReq = new Request.Builder().url(downloadURL).addHeader("Authorization", accessToken).method("GET", null).build();
				try (Response response = httpClient.newCall(downloadURLReq).execute()) {
					if (!response.isSuccessful())
						throw new IOException("Unexpected code " + response);
					InputStream fileStream = response.body().byteStream();
					File tmpFile = new File("/tmp/" + crmJobId + ".zip");
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
								File csvFile = new File("/tmp/" + crmJobId + ".csv");
								try (FileOutputStream writer = new FileOutputStream(csvFile)) {
									byte[] buffer = new byte[4096];
									int bytesRead;
									while ((bytesRead = zipStream.read(buffer)) != -1) {
										writer.write(buffer, 0, bytesRead);
									}
								}
								ZCFileDetail fileDetails = ZCFile.getInstance().getFolderInstance(Long.parseLong(System.getenv(CommonUtil.FOLDER_ID))).uploadFile(csvFile);
								ZCRowObject rowObj = ZCRowObject.getInstance();
								rowObj.set("FILEID", fileDetails.getFileId());
								rowObj.set("CRM_JOB_ID", crmJobId);
								rowObj.set("MODULE", moduleName);
								ZCObject.getInstance().getTableInstance("ReadQueue").insertRow(rowObj);
							}
							zipStream.closeEntry();
						}
					}

				}
			}

			if (fetchedPageNo < pageNo) {
				
				Long id = jsonObj.getLong("ROWID");
				JSONObject callBackObj = new JSONObject();
				String callBackURL = CommonUtil.getCallBackURL();
				LOGGER.log(Level.SEVERE,"Callback URL"+callBackURL);
				callBackObj.put("url", CommonUtil.getCallBackURL());
				callBackObj.put("method", "post");
				JSONObject module = new JSONObject();
				module.put("api_name", moduleName);
				JSONObject query = new JSONObject();
				query.put("module", module);
				JSONObject input = new JSONObject();
				input.put("callback", callBackObj);
				input.put("query", query);
				input.put("file_type", "csv");

				Request request = new Request.Builder().url(url).addHeader("Authorization", accessToken).method("POST", RequestBody.create(MediaType.parse("application/json"), input.toString())).build();

				try (Response response = httpClient.newCall(request).execute()) {
					if (!response.isSuccessful())
						throw new IOException("Unexpected code " + response.body().string());
					JSONObject bulkResponse = new JSONObject(response.body().string());
					String jobId = bulkResponse.getJSONArray("data").getJSONObject(0).getJSONObject("details").get("id").toString();
					ZCQL.getInstance().executeQuery("Update BulkRead set CRMJOBID='" + jobId + "',FETCHED_PAGE_NO=" + pageNo + "  where ROWID='" + id + "'");
				}
			}

		}

		ZCCache.getInstance().putCacheValue("BulkReadDownload", "Working", 1l);

	}

}
