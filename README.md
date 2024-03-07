
# Zoho CRM Bulk Data Processing CodeLib Solution

The Zoho CRM Bulk Data Processing CodeLib solution allows you to process data in bulk based on your own custom logic from a specific module in your Zoho CRM.

**Note:** You can get more detailed information on the steps to install and configure the Zoho CRM Bulk Data Processing CodeLib solution from your Catalyst console. Navigate to the bottom of your Catalyst console where you will find the Catalyst CodeLib section. Click on the Zoho CRM Bulk Data Processing CodeLib tile to access the steps.

## How does the CodeLib solution work?

Upon installing this CodeLib solution, pre-defined Catalyst components specific to the solution will be automatically configured in your project. These include three [Catalyst Serverless functions](https://catalyst.zoho.com/help/functions.html) (Cron, Event, and Advanced I/O), a rule in the [Catalyst Component Event Listener](https://docs.catalyst.zoho.com/en/cloud-scale/help/event-listeners/component-event-listeners/), and a folder in the [Catalyst Cloud Scale File Store](https://catalyst.zoho.com/help/file-store.html) component. The Cron and Event functions are implemented in Java, whereas the Advanced I/O function has been implemented in Node.js.

To authenticate and access the resources of your Zoho CRM account securely, you will need to register a self-client application from Zoho's [API console](https://api-console.zoho.com/). Note down the generated Client ID and Client secret credentials for accessing your Zoho CRM account. Refer to [this page](https://catalyst.zoho.com/help/api/introduction/access-and-refresh.html) for the steps to generate access and refresh tokens. Configure these credentials as constant values in the functions component after the CodeLib solution is installed.

You will need to create a cron job in the [Catalyst Cloud Scale Cron](https://docs.catalyst.zoho.com/en/cloud-scale/help/cron/introduction/) component and schedule it to execute the pre-configured cron function(BulkJobSchedule) automatically. You will need to configure the CRM module name for which you need the data to be processed by the CodeLib solution and the specific fields to be processed by them as params to this cron job.

The cron job starts running at the scheduled time and triggers the BulkJobSchedule function. This function includes the logic to update the module name in the BulkRead table of the Catalyst Data Store. 

The auto-created [Catalyst Component Event Listener](https://docs.catalyst.zoho.com/en/cloud-scale/help/event-listeners/component-event-listeners/) is associated with the three tables in the Catalyst Data Store (BulkRead, ReadQueue, WriteQueue) and upon occurrences of Insert and Update events in these tables, it executes the event function (BulkDataProcessor).

## Bulk Read

- When the module name is updated in BulkRead table, the BulkDataProcessor event function gets called. This function triggers a [Bulk Read API](https://www.zoho.com/crm/developer/docs/api/v5/bulk-read/create-job.html) call to the specified CRM module. We have also configured a callback function (zohocrm_bulk_callback) that handles the read operations in Catalyst.

**Note:** To enable access to your Zoho CRM account, please make sure you configure the client credentials in the catalyst-config.json file of the BulkDataProcessor event function.

- The Bulk Read API is configured to fetch a maximum of 200,000 records per API call. Therefore, the number of API calls that will be made solely depends on the number of records present in the specified CRM module. We have used the [Get Fields MetaData](https://www.zoho.com/crm/developer/docs/api/v5/field-meta.html) API to fetch the field metadata of the specified module, which helps filter out the data fields that need to be processed.

- A folder will be auto-created in the Catalyst File Store of your project upon installation of the CodeLib solution. The data fetched from CRM will be stored temporarily in this folder as .csv files, and the metadata of the file will be parallelly updated in the ReadQueue table of the Catalyst Data Store.

## Bulk Processing

- Upon the metadata updation in the ReadQueue table, the associated event function BulkDataProcessor gets called again.

- The BulkDataProcessor function handles the processing of the bulk read data. You can code your custom data processing logic in the ZCRMRecordsProcessorImpl.java file of the BulkDataProcessor function and [deploy](https://catalyst.zoho.com/help/cli-deploy.html) it to the Catalyst console.

- The function then writes the processed records as output files (.csv format) to the Catalyst File Store. We have accomplished this using the [uploadFile()](https://docs.catalyst.zoho.com/en/sdk/java/v1/cloud-scale/file-store/upload-file/) method of Catalyst Java SDK.

- We will be writing these processed records back to CRM in the next steps using the [Bulk Write](https://www.zoho.com/crm/developer/docs/api/v5/bulk-write/create-job.html) API. This API is configured to write 25,000 records per call in a single file. Hence, we will be splitting these processed records in the output files with 25,000 records in a single file.

- Similar to the bulk read operation, the Advanced I/O function (zohocrm_bulk_callback) handles the callback operation from Catalyst end when performing bulk write tasks.

- The metadata of the processed files will then be updated as entries in the WriteQueue table of the Catalyst Data Store.

## Bulk Upload & Write

- When data is updated in the WriteQueue table, the event function gets triggered again.

- Now the output files are compressed into a single zip file, and the CRM [Bulk File Upload API](https://www.zoho.com/crm/developer/docs/api/v3/bulk-write/upload-file.html) uploads the zip to CRM. We have used the [Get Organization Data](https://www.zoho.com/crm/developer/docs/api/v5/get-org-data.html) API to fetch the organization details of the Zoho CRM account in order to write the records to the specified module in CRM.

- Once the file is uploaded to CRM, the [Bulk Write API](https://www.zoho.com/crm/developer/docs/api/v5/bulk-write/create-job.html) will be called. On API execution, the zip file will be unzipped, and the output files will be written back to the CRM module.

**Note:**

- The support for HIPAA compliance is not automatically enabled for the Catalyst resources pre-configured as a part of this CodeLib solution. If you are working with ePHI and other sensitive user data, we strongly recommend you to make use of [Catalyst's HIPAA compliance](https://catalyst.zoho.com/help/hipaa-compliance.html) support and configure your resources accordingly.

- You can get more detailed information on the steps to install and configure the Zoho CRM Bulk Data Processing CodeLib solution from the Catalyst CodeLib section in your Catalyst console.

## Resources Involved:

The following Catalyst resources are auto-configured and used as a part of the Zoho CRM Bulk Data Processing CodeLib solution:

1. [Catalyst Serverless Functions](https://catalyst.zoho.com/help/functions.html):
   - The BulkJobSchedule(Cron) function handles the logic to be executed each time the auto-created cron job is triggered. This function updates the module name to the BulkRead table.
   - The BulkDataProcessor(Event) function contains the logic to be executed on the occurrence of insertion or updation events in the BulkRead, ReadQueue, WriteQueue tables in the Catalyst DataStore.
   - The zohocrm_bulk_callback(Advanced I/O) function contains the definitions of the callback operations /read and /write, that are executed when performing bulk read and write operations with Zoho CRM data.

2. [Catalyst Cloud Scale Cron](https://docs.catalyst.zoho.com/en/cloud-scale/help/cron/introduction/): A cron job is auto-scheduled in the Catalyst Cron component on installation of the CodeLib solution. This job runs on a daily basis and executes the associated cron function(BulkJobSchedule). This cron job also ensures that the latest updated CRM records are processed every day.

3. [Catalyst Cloud Scale Data Store](https://docs.catalyst.zoho.com/en/cloud-scale/help/data-store/introduction/): Three tables named BulkRead, ReadQueue, WriteQueue will be auto-created and configured on installation of the CodeLib solution. An event rule is associated with the tables and on occurrences of INSERT or UPDATE, the event function(BulkDataProcessor) gets triggered.

4. [Catalyst Cloud Scale Event Listener](https://catalyst.zoho.com/help/event-listeners.html): A rule in the Catalyst Component Event Listener of your project will be auto-created and configured.

5. [Catalyst Cloud Scale File Store](https://catalyst.zoho.com/help/file-store.html): The records from the CRM module will be stored temporarily under a folder named csvfiles in the Catalyst FileStore during processing of the data.
