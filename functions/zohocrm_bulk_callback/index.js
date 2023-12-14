"use strict";

const express = require("express");
const catalyst = require("zcatalyst-sdk-node");

const app = express();
app.use(express.json());

app.post("/read", async (req, res) => {
  console.log(req.body);
  res.status(200).send("I am Live and Ready.");
});

app.post("/write", async (req, res) => {
  console.log(req.body);
  let data = req.body;
  if (req.query.CODELIB_SECRET_KEY === process.env[CODELIB_SECRET_KEY]) {
    if (data.state == "COMPLETED") {
      let jobId = data.job_id;
      let operation = data.operation;
      let downloadURL = data.result.download_url;
      let page = data.result.page;
      let newPage = page;
      downloadURL = "https://zohoapis.com" + downloadURL;
      let more_records = data.result.more_records;
      console.log(more_records);
      if (more_records) {
        newPage = newPage + 1;
      }
      const catalystApp = catalyst.initialize(req);
      if (operation == "read") {
        let res = await catalystApp
          .zcql()
          .executeZCQLQuery(
            "update BulkRead set DOWNLOAD_URL='" +
              downloadURL +
              "',REQUESTED_PAGE_NO='" +
              newPage +
              "' where CRMJOBID='" +
              jobId +
              "'"
          );
        console.log(res);
      }
    }
    res.status(200);
  } else {
    res.status(401).send("You are unauthorised to perform this action");
  }
});

module.exports = app;
