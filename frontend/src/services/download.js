import api from "./api";

/**
 * Downloads a file from a protected URL (the JWT is added by api.js) and
 * saves it in the browser. A plain <a href> would not send the token.
 */
export async function downloadFile(url, fileName, params) {
    const response = await api.get(url, { params, responseType: "blob" });
    const objectUrl = window.URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(objectUrl);
}
