
import java.util.logging.Level;
import java.util.logging.Logger;

import com.catalyst.Context;
import com.catalyst.cron.CRON_STATUS;
import com.catalyst.cron.CronRequest;
import com.catalyst.cron.CatalystCronHandler;

import com.zc.common.ZCProject;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;

public class BulkJobSchedule implements CatalystCronHandler {

	private static final Logger LOGGER = Logger.getLogger(BulkJobSchedule.class.getName());
 
	@Override
	public CRON_STATUS handleCronExecute(CronRequest request, Context arg1) throws Exception {
		try {
			ZCProject.initProject();

			String[] MODULES = System.getenv("MODULES").split(",");

			for (String module : MODULES) {
				ZCRowObject row = ZCRowObject.getInstance();
				row.set("MODULE_NAME", module);
				ZCObject.getInstance().getTableInstance("BulkRead").insertRow(row);
			}
			LOGGER.log(Level.SEVERE, "Inserted SucessFully:)");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in Cron Function", e);
			return CRON_STATUS.FAILURE;
		}
		return CRON_STATUS.SUCCESS;
	}

}
