//$Id$
package com.util;

public class Tables {
	
	public static  enum BULK_READ{
		TABLE("BulkRead"),
		ROWID("ROWID"),
		CRMJOBID("CRMJOBID"),
		REQUESTED_PAGE_NO("REQUESTED_PAGE_NO"),
		FETCHED_PAGE_NO("FETCHED_PAGE_NO"),
		MODULE_NAME("MODULE_NAME"),
		DOWNLOAD_URL("DOWNLOAD_URL"),
		FIELDS_TO_BE_PROCESSED("FIELDS_TO_BE_PROCESSED");
		String value;
		BULK_READ(String value){
			this.value=value;
		}
		
		public String value() {
			return this.value;
		}
		
	}
	
	public static  enum READ_QUEUE{
		TABLE("ReadQueue"),
		ROWID("ROWID"),
		CRM_JOB_ID("CRM_JOB_ID"),
		FILEID("FILEID"),
		LINE_PROCESSED("LINE_PROCESSED"),
		IS_PROCESS_STARTED("IS_PROCESS_STARTED"),
		IS_PROCESS_COMPLETED("IS_PROCESS_COMPLETED"),
		MODULE("MODULE");
		String value;
		READ_QUEUE(String value){
			this.value=value;
		}
		
		public String value() {
			return this.value;
		}
		
	}
	
	public static  enum WRITE_QUEUE{
		TABLE("WriteQueue"),
		ROWID("ROWID"),
		CRM_JOB_ID("CRM_JOB_ID"),
		FILE_ID("FILE_ID"),
		IS_UPLOADED("IS_UPLOADED"),
		MODULE("MODULE");
		String value;
		WRITE_QUEUE(String value){
			this.value=value;
		}
		
		public String value() {
			return this.value;
		}
		
	}

}
