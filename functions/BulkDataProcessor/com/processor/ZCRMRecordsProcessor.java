//$Id$
package com.processor;

import java.util.List;

import com.processor.record.ZCRMRecord;


public interface ZCRMRecordsProcessor {	
	public List<ZCRMRecord> processRecords(List<ZCRMRecord> zcrmRecords) throws Exception;

}
