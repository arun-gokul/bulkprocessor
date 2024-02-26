import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.catalyst.Context;
import com.catalyst.event.CatalystEventHandler;
import com.catalyst.event.EVENT_STATUS;
import com.catalyst.event.EventRequest;
import com.processor.impl.ZCRMDownloadQueueProcessor;
import com.processor.impl.ZCRMReadQueueProcessor;
import com.processor.impl.ZCRMUploadQueueProcessor;
import com.util.Tables.BULK_READ;
import com.util.Tables.READ_QUEUE;
import com.zc.common.ZCProject;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCTable;

public class BulkDataProcessor implements CatalystEventHandler {

	private static final Logger LOGGER = Logger.getLogger(BulkDataProcessor.class.getName());

	@Override
	public EVENT_STATUS handleEvent(EventRequest paramEventRequest, Context paramContext) throws Exception {
		try {
			ZCProject.initProject();
			JSONArray eventData = new JSONArray(paramEventRequest.getData().toString());
			JSONObject projectData = paramEventRequest.getProjectDetails();
			ZCTable tableDetails = ZCObject.getInstance().getTable(paramEventRequest.getSourceEntityId());

			if (tableDetails.getName().equals(BULK_READ.TABLE.value())) {
				new ZCRMDownloadQueueProcessor().process(projectData, eventData);
			} else if (tableDetails.getName().equals(READ_QUEUE.TABLE.value())) {
				new ZCRMReadQueueProcessor(paramContext).process(projectData, eventData);
			} else {
				new ZCRMUploadQueueProcessor().process(projectData, eventData);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in Cron Function", e);
			return EVENT_STATUS.FAILURE;
		}
		return EVENT_STATUS.SUCCESS;
	}

}
