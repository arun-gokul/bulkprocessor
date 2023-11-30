
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.catalyst.Context;
import com.catalyst.event.CatalystEventHandler;
import com.catalyst.event.EVENT_STATUS;
import com.catalyst.event.EventRequest;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.processor.ZCRMRecordsProcessor;
import com.processor.impl.ZCRMDownloadQueueProcessor;
import com.processor.impl.ZCRMReadQueueProcessor;
import com.processor.impl.ZCRMUploadQueueProcessor;
import com.processor.record.ZCRMRecordsProcessorImpl;
import com.util.CommonUtil;
import com.util.Tables.BULK_READ;
import com.util.Tables.READ_QUEUE;
import com.zc.common.ZCProject;
import com.zc.component.files.ZCFile;
import com.zc.component.files.ZCFileDetail;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.object.ZCTable;
import com.zc.component.zcql.ZCQL;

public class ReadQueueProcessor implements CatalystEventHandler {

	private static final Logger LOGGER = Logger.getLogger(ReadQueueProcessor.class.getName());


	@Override
	public EVENT_STATUS handleEvent(EventRequest paramEventRequest, Context paramContext) throws Exception {
		try {
			ZCProject.initProject();
			JSONArray eventData = new JSONArray(paramEventRequest.getData().toString());
			LOGGER.log(Level.WARNING,"Event data"+eventData.toString());
			Long sourceEntityId = paramEventRequest.getSourceEntityId();
			ZCTable tableDetails = ZCObject.getInstance().getTable(sourceEntityId);
			if (tableDetails.getName().equals(BULK_READ.TABLE.value())) {
				new ZCRMDownloadQueueProcessor().process(eventData);
			} else if (tableDetails.getName().equals(READ_QUEUE.TABLE.value())) {
				new ZCRMReadQueueProcessor(paramContext).process(eventData);
			} else {
				new ZCRMUploadQueueProcessor().process(eventData);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in Cron Function", e);
			return EVENT_STATUS.FAILURE;
		}
		return EVENT_STATUS.SUCCESS;
	}

}
