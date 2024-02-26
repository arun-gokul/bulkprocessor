//$Id$
package com.processor.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.catalyst.Context;
import com.java.bean.ZCRMFieldMeta;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.processor.ZCRMQueueProcessor;
import com.processor.ZCRMRecordsProcessor;
import com.processor.record.ZCRMRecord;
import com.processor.record.ZCRMRecordsProcessorImpl;
import com.util.CommonUtil;
import com.util.Tables.READ_QUEUE;
import com.util.Tables.WRITE_QUEUE;
import com.zc.component.files.ZCFile;
import com.zc.component.files.ZCFileDetail;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.zcql.ZCQL;

public class ZCRMReadQueueProcessor implements ZCRMQueueProcessor {

	private static final Integer BATCH_SIZE = 500;
	private static final String INPUT_FILE = System.getProperty("java.io.tmpdir") + "/input.csv";
	private static final String OUTPUT_FILE = System.getProperty("java.io.tmpdir") + "/output.csv";

	private Context context;

	public ZCRMReadQueueProcessor(Context context) {
		this.context = context;
	}

	@Override
	public void process(JSONObject projectData, JSONArray tableData) throws Exception {

		JSONObject rowData = tableData.getJSONObject(0);
		if (rowData.has(READ_QUEUE.TABLE.value())) {
			rowData = rowData.getJSONObject(READ_QUEUE.TABLE.value());
		}
		Long rowId = rowData.getLong(READ_QUEUE.ROWID.value());
		ArrayList<ZCRowObject> rows = ZCQL.getInstance()
				.executeQuery("Select * from ReadQueue where IS_PROCESS_COMPLETED=false and ROWID='" + rowId + "'");
		ZCRMRecordsProcessor processor = new ZCRMRecordsProcessorImpl();

		for (ZCRowObject rowObj : rows) {
			String fileId = rowObj.get(READ_QUEUE.FILEID.value()).toString();
			String module = rowObj.get(READ_QUEUE.MODULE.value()).toString();
			ZCRMFieldMeta meta = CommonUtil.getFields(module);
			List<String> moduleFields = meta.getFields();
			Long processedLine = (rowObj.get(READ_QUEUE.LINE_PROCESSED.value()) != null)
					? Long.parseLong(rowObj.get(READ_QUEUE.LINE_PROCESSED.value()).toString())
					: 0;
			InputStream file = ZCFile.getInstance().getFolderInstance(CommonUtil.CSVFILES)
					.downloadFile(Long.parseLong(fileId));
			File targetFile = new File(INPUT_FILE);
			File outputFile = new File(OUTPUT_FILE);
			byte[] buffer = new byte[4096];
			int bytesRead;
			try (OutputStream outStream = new FileOutputStream(targetFile)) {
				while ((bytesRead = file.read(buffer)) > 0) {
					outStream.write(buffer, 0, bytesRead);
				}
			}
			int lineNumber = 0;
			int outputLineNumber = 0;
			List<ZCRMRecord> recordList = new ArrayList<ZCRMRecord>();
			CSVReader reader = new CSVReader(new FileReader(INPUT_FILE));
			CSVWriter writer = new CSVWriter(new FileWriter(OUTPUT_FILE));
			String[] nextLine;
			String[] firstLine = null;
			List<Integer> indexes = new ArrayList<Integer>();
			while ((nextLine = reader.readNext()) != null && this.context.getRemainingExecutionTimeMs() > 60000) {
				lineNumber++;
				ZCRMRecord rec = new ZCRMRecord();
				if (lineNumber == 1) {
					firstLine = nextLine;

					for (int j = 0; j < firstLine.length; j++) {
						if (!moduleFields.stream().anyMatch(firstLine[j]::equalsIgnoreCase)) {
							indexes.add(j);
						}
					}
					for (int j = indexes.size() - 1; j >= 0; j--) {
						firstLine = ArrayUtils.remove(firstLine, indexes.get(j));
					}
					ArrayList<String> temp = new ArrayList<String>();
					for (String headerValue : firstLine) {
						if (!meta.readOnlyFields.contains(headerValue)) {
							temp.add(headerValue);
						}
					}
					String[] tempArr = new String[temp.size()];
					tempArr = temp.toArray(tempArr);
					writer.writeNext(tempArr);
					continue;
				}
				for (int j = indexes.size() - 1; j >= 0; j--) {
					nextLine = ArrayUtils.remove(nextLine, indexes.get(j));
				}
				for (int i = 0; i < nextLine.length && lineNumber > 1; i++) {
					rec.data.put(firstLine[i], nextLine[i]);
				}
				if (lineNumber <= processedLine) {
					continue;
				}
				recordList.add(rec);
				outputLineNumber++;
				if (recordList.size() == BATCH_SIZE) {
					processedLine = process(processor, writer, recordList, processedLine, firstLine, meta);
				}
				if (outputLineNumber >= 25000) {
					uploadCSVFile(outputFile, writer, rowObj, processedLine, module);
					outputLineNumber = 0;
				}
			}
			reader.close();
			if (recordList.size() > 0) {
				processedLine = process(processor, writer, recordList, processedLine, firstLine, meta);
			}
			if (outputLineNumber >= 0) {
				uploadCSVFile(outputFile, writer, rowObj, processedLine, module);
			}
			ZCQL.getInstance()
					.executeQuery("update ReadQueue set IS_PROCESS_COMPLETED=true where ROWID='" + rowId + "'");
		}
	}

	public static void uploadFile(File file, String module) throws Exception {
		ZCFileDetail fileDetails = ZCFile.getInstance().getFolderInstance(CommonUtil.CSVFILES).uploadFile(file);
		ZCRowObject fileData = ZCRowObject.getInstance();
		fileData.set(WRITE_QUEUE.FILE_ID.value(), fileDetails.getFileId());
		fileData.set(WRITE_QUEUE.MODULE.value(), module);
		ZCObject.getInstance().getTableInstance(WRITE_QUEUE.TABLE.value()).insertRow(fileData);
	}

	public static void uploadCSVFile(File outputFile, CSVWriter writer, ZCRowObject tableRow, Long lineProcessed,
			String module) throws Exception {
		writer.flush();
		writer.close();
		uploadFile(outputFile, module);
		outputFile.delete();
		outputFile = new File(OUTPUT_FILE);
		Long rowId = Long.parseLong(tableRow.get(READ_QUEUE.ROWID.value()).toString());
		ZCQL.getInstance().executeQuery(
				"update ReadQueue set LINE_PROCESSED='" + lineProcessed + "' where ROWID='" + rowId + "'");
		writer = new CSVWriter(new FileWriter(OUTPUT_FILE));
		writer.close();
	}

	public static Long process(ZCRMRecordsProcessor processor, CSVWriter writer, List<ZCRMRecord> recordList,
			Long processedLine, String[] headers, ZCRMFieldMeta meta) throws Exception {
		List<ZCRMRecord> outputRecords = processor.processRecords(recordList);
		processedLine += recordList.size();
		for (ZCRMRecord outputRecord : outputRecords) {
			ArrayList<String> outputArr = new ArrayList<String>();
			for (int i = 0; i < headers.length; i++) {
				if (!meta.readOnlyFields.contains(headers[i])) {
					outputArr.add((String) outputRecord.data.get(headers[i]));
				}
			}
			String[] temp = new String[outputArr.size()];
			temp = outputArr.toArray(temp);
			writer.writeNext(temp);
			recordList = new ArrayList<ZCRMRecord>();
		}
		return processedLine;
	}

}
