package ste.toosla.zefiro;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * A client for the Zefiro service, providing methods to interact with the Zefiro API
 * for login, file download, and folder listing operations.
 */
public class ZefiroClient {

    private HttpClient.Builder httpClientBuilder;
    private String apiUrl = "https://zefiro.me";
    private String uploadUrl = "https://upload.zefiro.me";
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final String username;
    private final String password;
    private String validationKey;

    /**
     * Creates a new instance of the ZefiroClient with a default HttpClient builder.
     */
    public ZefiroClient(final String username, final String password) {
        this.username = username;
        this.password = password;
        this.httpClientBuilder = HttpClient.newBuilder();
        this.validationKey = null;
    }

    public ZefiroClient withHttpClientBuilder(final HttpClient.Builder builder) {
        this.httpClientBuilder = builder; return this;
    }

    public ZefiroClient withValidationKey(final String validationKey) {
        this.validationKey = validationKey; return this;
    }

    public ZefiroClient withApiUrl(final String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalArgumentException("apiUrl can not be null or empty");
        }
        this.apiUrl = apiUrl; return this;
    }

    public ZefiroClient withUploadUrl(final String uploadUrl) {
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new IllegalArgumentException("uploadUrl can not be null or empty");
        }
        this.uploadUrl = uploadUrl; return this;
    }

    public String validationKey() {
        return validationKey;
    }

    /**
     * Logs in to the Zefiro service to obtain a validation key.
     *
     * @return a {@link ZefiroLoginResponse} containing the account and validation key
     * @throws ZefiroException if a general error occurs during the login process (e.g., network issues, invalid JSON response)
     * @throws ZefiroLoginException if the login fails due to invalid credentials (HTTP 401)
     */
    public ZefiroLoginResponse login() throws ZefiroException, ZefiroLoginException {
        try {
            // Construct URL-form-encoded body for Zefiro
            String zefiroRequestBody = "login=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                                       "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/sapi/login?action=login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Origin", "https://zefiro.me")
                    .POST(HttpRequest.BodyPublishers.ofString(zefiroRequestBody))
                    .build();

            HttpResponse<String> response = httpClientBuilder.build().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new ZefiroLoginException();
            } else if (response.statusCode() >= 400) {
                throw new ZefiroException("Login failed with status code: " + response.statusCode());
            }

            // Parse Zefiro's response
            JsonNode zefiroResponse = jsonMapper.readTree(response.body());
            JsonNode validationKeyNode = zefiroResponse.at("/data/validationkey");
            if (validationKeyNode.isMissingNode() || validationKeyNode.asText().isEmpty()) {
                throw new ZefiroException("Missing or empty 'validationkey' in Zefiro response");
            }
            this.validationKey = validationKeyNode.asText();
            return new ZefiroLoginResponse(username, validationKeyNode.asText());
        } catch (JsonParseException x) {
            throw new ZefiroException("Invalid JSON response from Zefiro", x);
        } catch (IOException | InterruptedException x) {
            throw new ZefiroException("Error connecting to Zefiro", x);
        }
    }

    /**
     * Uploads a file to the Zefiro service.
     * This simplified implementation assumes the folder structure exists and uses a hardcoded folder ID.
     *
     * @param path the full path to the file (e.g., "/OneMediaHub/Toosla/new_file.json")
     * @param content the content of the file as a String
     * @return the ID of the uploaded file
     * @throws ZefiroException if an error occurs during the upload process
     */
    public String upload(String path, String content) throws ZefiroException {
        return upload(path, content, null);
    }

    /**
     * Uploads a file to the Zefiro service.
     * This simplified implementation assumes the folder structure exists and uses a hardcoded folder ID.
     *
     * Note that files in Zefiro must be under /OneMediaHub, which is specific
     * to this backend. Therefore, /OneMediaHub will be transparently prepended
     * to the provided path to build the real path. For example, if the client
     * writes the file /Toosla/toosla.json, this method writes into Zefiro's
     * file /OneMediaHub/Toosla/toosla.json
     *
     * @param path the absolute file path (e.g. "/Toosla/new_file.json")
     * @param content the content of the file as a String
     * @param ifUnmodifiedSince a {@link Date} object representing the timestamp to check against.
     *                          If the file on Zefiro has been modified more recently than this timestamp, a ZefiroException
     *                          will be thrown. If empty, no such check is performed.
     * @return the ID of the uploaded file
     * @throws ZefiroException if an error occurs during the upload process or if the precondition fails
     */
    public String upload(String path, String content, Date ifUnmodifiedSince)
    throws ZefiroException {
        path = "/OneMediaHub" + path;
        try {
            final HttpClient httpClient = httpClientBuilder.build();
            // Extract fileName from path
            final String[] pathParts = path.substring(1).split("/");
            final String fileName = pathParts[pathParts.length - 1];


            long folderId = findFolderId(httpClient, pathParts);
            long fileId = 0;

            // Check If-Unmodified-Since precondition
            if (ifUnmodifiedSince != null) {
                Optional<JsonNode> existingFileMetadata = getFileMetadata(httpClient, folderId, fileName);
                if (existingFileMetadata.isPresent()) {
                    long creationDate = existingFileMetadata.get().at("/creationdate").asLong();
                    if (creationDate > ifUnmodifiedSince.getTime()) {
                        throw new ZefiroModificationException(new Date(creationDate));
                    } else {
                        // Precondition met, get existing file id to update it
                        fileId = existingFileMetadata.get().at("/id").asLong();
                    }
                }
            } else {
                ifUnmodifiedSince = new Date();
            }

            final Multipart body = buildMultipartBody2(
                fileName, content, folderId, fileId
            );

            HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl + "/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + this.validationKey))
                    .header("Content-Type", body.contentType())
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.password).getBytes()))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.content()))
                    .build();

            HttpResponse<String> uploadResponse = httpClient.send(
                uploadRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (uploadResponse.statusCode() >= 400) {
                throw new ZefiroException("Failed to upload file: " + uploadResponse.statusCode());
            }

            JsonNode uploadJson = jsonMapper.readTree(uploadResponse.body());
            String uploadedFileId = uploadJson.at("/id").asText();

            return uploadedFileId;

        } catch (JsonParseException x) {
            throw new ZefiroException("Invalid JSON response from Zefiro", x);
        } catch (IOException | InterruptedException x) {
            throw new ZefiroException("Error connecting to Zefiro", x);
        }
    }

    /**
     * Downloads a file from the Zefiro service. This method will always attempt to download the file.
     *
     * @param path the full path to the file (e.g., "/OneMediaHub/Toosla/toosla.json")
     * @return the content of the file as a String
     * @throws ZefiroException if a general error occurs during the download process
     * @throws ZefiroFileNotFoundException if the specified file or any subdirectory in the path is not found
     */
    public String download(String path) throws ZefiroException {
        return download(path, null).get();
    }

    /**
     * Downloads a file from the Zefiro service, with an option to check if the file has been modified since a given date.
     *
     * @param path the full path to the file (e.g., "/OneMediaHub/Toosla/toosla.json")
     * @param ifModifiedSince a {@link Date} object representing the timestamp to check against. If the file's creation date
     *                        on Zefiro is not more recent than this date, an empty Optional is returned.
     * @return an {@link Optional} containing the file content as a String if modified, or empty if not modified.
     * @throws ZefiroException if a general error occurs during the download process
     * @throws ZefiroFileNotFoundException if the specified file or any subdirectory in the path is not found
     */
    public Optional<String> download(String path, Date ifModifiedSince) throws ZefiroException {
        path = "/OneMediaHub" + path;
        try {
            final HttpClient httpClient = httpClientBuilder.build();
            final String[] pathParts = path.substring(1).split("/"); // Remove leading / and split
            long folderId = findFolderId(httpClient, pathParts);

            long fileId = 0;
            JsonNode files = jsonMapper.readTree(listFiles(httpClient, folderId));
            for (JsonNode file: files.at("/data/media")) {
                if (pathParts[pathParts.length - 1].equals(file.at("/name").asText())) {
                    fileId = file.at("/id").asLong();
                    break;
                }
            }

            if (fileId == 0) {
                throw new ZefiroFileNotFoundException("File not found: " + path.substring(12)); // stripping out /OneMediaHub
            }

            // Get download URL
            HttpRequest downloadUrlRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/sapi/media?action=get&origin=omh,dropbox&validationkey=" + this.validationKey))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.password).getBytes()))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"data\":{\"ids\":[" + fileId + "],\"fields\":[\"url\",\"creationdate\"]}}"))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> downloadUrlResponse = httpClient.send(
                downloadUrlRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (downloadUrlResponse.statusCode() >= 400) {
                throw new ZefiroException("Failed to get download URL: " + downloadUrlResponse.statusCode());
            }

            JsonNode downloadUrlJson = jsonMapper.readTree(downloadUrlResponse.body());

            if (ifModifiedSince != null) {
                long creationDate = downloadUrlJson.at("/data/media/0/creationdate").asLong();
                if (creationDate <= ifModifiedSince.getTime()) {
                    return Optional.empty();
                }
            }

            String downloadUrl = downloadUrlJson.at("/data/media/0/url").asText();

            // Download file content
            HttpRequest downloadFileRequest = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .GET()
                    .build();
            HttpResponse<String> fileContentResponse = httpClient.send(
                downloadFileRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (fileContentResponse.statusCode() >= 400) {
                throw new ZefiroException("Failed to download file content: " + fileContentResponse.statusCode());
            }

            return Optional.of(fileContentResponse.body());
        } catch (JsonParseException x) {
            throw new ZefiroException("Invalid JSON response from Zefiro", x);
        } catch (IOException | InterruptedException x) {
            throw new ZefiroException("Error connecting to Zefiro", x);
        }
    }

    // --------------------------------------------------------- private methods

    private Optional<JsonNode> getFileMetadata(HttpClient httpClient, long folderId, String fileName) throws ZefiroException, IOException, InterruptedException {
        JsonNode files = jsonMapper.readTree(listFiles(httpClient, folderId));
        for (JsonNode file: files.at("/data/media")) {
            if (fileName.equals(file.at("/name").asText())) {
                // Found the file, now get its metadata
                HttpRequest metadataRequest = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "/sapi/media?action=get&origin=omh,dropbox&validationkey=" + this.validationKey))
                        .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.password).getBytes()))
                        .POST(HttpRequest.BodyPublishers.ofString("{\"data\":{\"ids\":[" + file.at("/id").asLong() + "],\"fields\":[\"url\",\"creationdate\"]}}"))
                        .header("Content-Type", "application/json")
                        .build();
                HttpResponse<String> metadataResponse = httpClient.send(
                    metadataRequest, HttpResponse.BodyHandlers.ofString()
                );

                if (metadataResponse.statusCode() >= 400) {
                    throw new ZefiroException("Failed to get file metadata: " + metadataResponse.statusCode());
                }
                return Optional.of(jsonMapper.readTree(metadataResponse.body()).at("/data/media/0"));
            }
        }
        return Optional.empty(); // File not found
    }

    private long findFolderId(final HttpClient httpClient, final String[] pathParts)
    throws IOException, InterruptedException {
        long folderId = 0;

        // Get root folder ID
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/sapi/media/folder/root?action=get&validationkey=" + this.validationKey))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.password).getBytes()))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() >= 400) {
            throw new ZefiroException("Failed to get root folder: " + response.statusCode());
        }

        JsonNode parent = jsonMapper.readTree(response.body());

        for (int i = 0; i < pathParts.length - 1; ++i) {
            JsonNode subfolders = parent.at("/data/folders");

            //
            // look for the current part in all subfolders
            //
            folderId = 0; // Reset folderId for each part of the path
            for (JsonNode folder: subfolders) {
                if (pathParts[i].equals(folder.at("/name").asText())) {
                    folderId = folder.at("/id").asLong();
                    parent = jsonMapper.readTree(listFolders(httpClient, folderId));
                    break;
                }
            }
            if (folderId == 0) {
                throw new ZefiroFileNotFoundException("File not found: /" + String.join("/", pathParts).substring(12)); // stripping out /OneMediaHub
            }
        }

        return folderId;
    }

    /**
     * Lists the folders within a specified parent folder on Zefiro.
     *
     * @param parentId the ID of the parent folder. Use 0 for the root folder.
     * @return a JSON string containing the list of folders
     * @throws ZefiroException if an error occurs during the API call
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    private String listFolders(final HttpClient httpClient, final long parentId)
    throws ZefiroException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/sapi/media/folder?action=list&parentid=" + parentId + "&limit=200&validationkey=" + this.validationKey))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.password).getBytes()))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ZefiroException("Failed to list folder content: " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Lists the files within a specified folder on Zefiro.
     *
     * @param parentId the ID of the folder to list files from
     * @return a JSON string containing the list of files
     * @throws ZefiroException if an error occurs during the API call
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    private String listFiles(final HttpClient httpClient, final long parentId)
    throws ZefiroException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/sapi/media?action=get&folderid=" + parentId + "&limit=200&validationkey=" + this.validationKey))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.password).getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString("{\"data\":{\"fields\":[\"name\",\"modificationdate\",\"size\",\"thumbnails\",\"videometadata\",\"audiometadata\",\"favorite\",\"shared\",\"etag\"]}}"))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ZefiroException("Failed to list folder content: " + response.statusCode());
        }

        return response.body();
    }

    private String buildFileMetadata(
        final String fileName,
        final long folderId, final long fileId,
        final long size
    ) {
        StringBuilder jsonData = new StringBuilder();
        jsonData.append("{\"data\":{\"name\":\"").append(fileName).append("\"")
            //.append(",\"creationdate\":\"20110101T012030Z\"")
            //.append(",\"modificationdate\":\"2013-02-01 11:11:11\"")
            .append(",\"size\":").append(size)
            .append(",\"contenttype\":\"application/octet-stream\"")
            .append(",\"folderid\":").append(folderId);
        if (fileId > 0) {
            jsonData.append(",\"id\":").append(fileId);
        }
        jsonData.append("}}");

        return jsonData.toString();
    }

    private Multipart buildMultipartBody2(
        final String fileName, final String content,
        final long folderId, final long fileId
    ) {
        // Construct multipart/form-data body as per RFC 7578, CRLF after each line
        final String boundary = "------zfrclient" + System.currentTimeMillis();
        final String contentType = "multipart/form-data; boundary=" + boundary;
        final String CRLF = "\r\n";

        StringBuilder bodyBuilder = new StringBuilder();

        // Part 1: data
        bodyBuilder.append("--").append(boundary).append(CRLF);
        bodyBuilder.append("Content-Disposition: form-data; name=\"data\"").append(CRLF);
        bodyBuilder.append("Content-Type: application/json").append(CRLF).append(CRLF);
        bodyBuilder.append(buildFileMetadata(
            fileName, folderId, fileId, content.length()
        )).append(CRLF);

        // Part 2: file
        bodyBuilder.append("--").append(boundary).append(CRLF);
        bodyBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"").append(CRLF);
        bodyBuilder.append("Content-Type: application/octet-stream").append(CRLF).append(CRLF);
        bodyBuilder.append(content).append(CRLF);

        // Final boundary
        bodyBuilder.append("--").append(boundary).append("--").append(CRLF);

        return new Multipart(contentType, bodyBuilder.toString().getBytes());
    }

    private record Multipart(String contentType, byte[] content) {
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();

            sb.append("ContentType: ").append(contentType).append('\n');
            sb.append("Content:\n").append(new String(content));

            return sb.toString();
        }
    }
}