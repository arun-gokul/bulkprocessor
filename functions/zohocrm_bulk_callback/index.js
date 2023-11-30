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
  res.status(200).send("I am Live and Ready.");
});

module.exports = app;

//Create a JSON object for adding a new user
const signupConfig = {
  platform_type: "web",
  template_details: {
    senders_mail: "dogogetu@tutuapp.bid",
    subject: "Welcome to %APP_NAME% ",
    message: "<p>Hello ,</p> <p>Follow this link to join in %APP_NAME% .</p>",
  },
};
var userConfig = {
  first_name: "Dannie",
  last_name: "Boyle",
  email_id: "p.boyle@zylker.com",
  role_id: "3376000000159024",
};
