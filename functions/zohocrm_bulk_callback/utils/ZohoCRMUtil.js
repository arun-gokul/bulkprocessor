const validator = require("validator");

const AppError = require("./AppError");

class ZohoCRMUtil {
  static #NUMERIC_REGEX = /^\d+$/;

  static buildDownloadURL = (path = "") => {
    console.log(path)
    console.log(path.slice(1))
    console.log("https://zohoapis.com/" + path.slice(1))
    return "https://zohoapis.com/" + path.slice(1);
  };
  static validatePayload = ({ job_id, result } = {}) => {
    if (!job_id) {
      throw new AppError(400, "job_id cannot be empty.");
    }

    if (!this.#NUMERIC_REGEX.test(job_id)) {
      throw new AppError(
        400,
        "Invalid value for job_id. job_id should be a positive number."
      );
    }
    if (!result.download_url) {
      throw new AppError(400, "download_url cannot be empty.");
    }
    if (!validator.isURL(this.buildDownloadURL(result.download_url))) {
      throw new AppError(400, "Invalid value for download_url.");
    }

    if (!result?.page) {
      throw new AppError(400, "page cannot be empty.");
    }

    if (!this.#NUMERIC_REGEX.test(result.page)) {
      throw new AppError(
        400,
        "Invalid value for page. page should be a positive number."
      );
    }
  };
}
module.exports = ZohoCRMUtil;
