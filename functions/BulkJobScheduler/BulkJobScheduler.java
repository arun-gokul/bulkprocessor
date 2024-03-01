
import java.util.logging.Level;
import java.util.logging.Logger;

import com.catalyst.Context;
import com.catalyst.cron.CRON_STATUS;
import com.catalyst.cron.CronRequest;
import com.catalyst.cron.CatalystCronHandler;

import com.zc.common.ZCProject;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;

public class BulkJobScheduler implements CatalystCronHandler {

	private static final Logger LOGGER = Logger.getLogger(BulkJobScheduler.class.getName());

	@Override
	public CRON_STATUS handleCronExecute(CronRequest request, Context arg1) throws Exception {
		try {
			ZCProject.initProject();

			String MODULES = request.getCronParam("MODULES").toString();
			String FIELDS_TO_BE_PROCESSED = request.getCronParam("FIELDS_TO_BE_PROCESSED").toString();

			ZCRowObject row = ZCRowObject.getInstance();
			row.set("MODULE_NAME", MODULES);
			row.set("FIELDS_TO_BE_PROCESSED", FIELDS_TO_BE_PROCESSED);
			ZCObject.getInstance().getTableInstance("BulkRead").insertRow(row);

			LOGGER.log(Level.SEVERE, "Inserted SucessFully:)");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in Cron Function", e);
			return CRON_STATUS.FAILURE;
		}
		return CRON_STATUS.SUCCESS;
	}

}
