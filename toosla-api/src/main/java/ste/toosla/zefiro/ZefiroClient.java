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
import java.util.Date;
import java.util.Optional;

/**
 * A client for the Zefiro service, providing methods to interact with the Zefiro API
 * for login, file download, and folder listing operations.
 */
public class ZefiroClient {

    private final HttpClient.Builder httpClientBuilder;
    private final ObjectMapper jsonMapper;

    /**
     * Creates a new instance of the ZefiroClient with a default HttpClient builder.
     */
    public ZefiroClient() {
        this(HttpClient.newBuilder());
    }

    /**
     * Creates a new instance of the ZefiroClient with a custom HttpClient builder.
     *
     * @param httpClientBuilder the http client builder to use for making requests
     */
    public ZefiroClient(HttpClient.Builder httpClientBuilder) {
        this.httpClientBuilder = httpClientBuilder;
        this.jsonMapper = new ObjectMapper();
    }

    /**
     * Logs in to the Zefiro service to obtain a validation key.
     *
     * @param account the user account name
     * @param secret the user account secret (password)
     * @return a {@link ZefiroLoginResponse} containing the account and validation key
     * @throws ZefiroException if a general error occurs during the login process (e.g., network issues, invalid JSON response)
     * @throws ZefiroLoginException if the login fails due to invalid credentials (HTTP 401)
     */
    public ZefiroLoginResponse login(String account, String secret) throws ZefiroException, ZefiroLoginException {
        try {
            HttpClient client = httpClientBuilder.build();

            // Construct URL-form-encoded body for Zefiro
            String zefiroRequestBody = "login=" + URLEncoder.encode(account, StandardCharsets.UTF_8) +
                                       "&password=" + URLEncoder.encode(secret, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://zefiro.me/sapi/login?action=login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Origin", "https://zefiro.me")
                    .POST(HttpRequest.BodyPublishers.ofString(zefiroRequestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

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
            return new ZefiroLoginResponse(account, validationKeyNode.asText());
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
     * @param validationKey the validation key obtained from a successful login
     * @return the content of the file as a String
     * @throws ZefiroException if a general error occurs during the download process
     * @throws ZefiroFileNotFoundException if the specified file or any subdirectory in the path is not found
     */
    public String download(String path, String validationKey) throws ZefiroException {
        return download(path, validationKey, null).get();
    }

    /**
     * Downloads a file from the Zefiro service, with an option to check if the file has been modified since a given date.
     *
     * @param path the full path to the file (e.g., "/OneMediaHub/Toosla/toosla.json")
     * @param validationKey the validation key obtained from a successful login
     * @param ifModifiedSince a {@link Date} object representing the timestamp to check against. If the file's creation date
     *                        on Zefiro is not more recent than this date, an empty Optional is returned.
     * @return an {@link Optional} containing the file content as a String if modified, or empty if not modified.
     * @throws ZefiroException if a general error occurs during the download process
     * @throws ZefiroFileNotFoundException if the specified file or any subdirectory in the path is not found
     */
    public Optional<String> download(String path, String validationKey, Date ifModifiedSince) throws ZefiroException {
        try {
            HttpClient client = httpClientBuilder.build();

            // Get root folder ID
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + validationKey))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new ZefiroException("Failed to get root folder: " + response.statusCode());
            }

            JsonNode parent = jsonMapper.readTree(response.body());

            long folderId = 0;

            String[] pathParts = path.substring(1).split("/"); // Remove leading / and split
            for (int i = 0; i < pathParts.length - 1; ++i) {
                JsonNode subfolders = parent.at("/data/folders");

                //
                // look for the current part in all subfolders
                //
                folderId = 0; // Reset folderId for each part of the path
                for (JsonNode folder: subfolders) {
                    if (pathParts[i].equals(folder.at("/name").asText())) {
                        folderId = folder.at("/id").asLong();
                        parent = jsonMapper.readTree(listFolders(folderId, validationKey));
                        break;
                    }
                }
                if (folderId == 0) {
                    throw new ZefiroFileNotFoundException("File not found: " + path);
                }
            }

            long fileId = 0;
            JsonNode files = jsonMapper.readTree(listFiles(folderId, validationKey));
            for (JsonNode file: files.at("/data/media")) {
                if (pathParts[pathParts.length - 1].equals(file.at("/name").asText())) {
                    fileId = file.at("/id").asLong();
                    break;
                }
            }

            if (fileId == 0) {
                throw new ZefiroFileNotFoundException("File not found: " + path);
            }

            // Get download URL
            HttpRequest downloadUrlRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + validationKey))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"data\":{\"ids\":[" + fileId + "],\"fields\":[\"url\",\"creationdate\"]}}"))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> downloadUrlResponse = client.send(downloadUrlRequest, HttpResponse.BodyHandlers.ofString());

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
            HttpResponse<String> fileContentResponse = client.send(downloadFileRequest, HttpResponse.BodyHandlers.ofString());

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

    /**
     * Lists the folders within a specified parent folder on Zefiro.
     *
     * @param parentId the ID of the parent folder. Use 0 for the root folder.
     * @param validationKey the validation key obtained from a successful login
     * @return a JSON string containing the list of folders
     * @throws ZefiroException if an error occurs during the API call
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    private String listFolders(long parentId, String validationKey) throws ZefiroException, IOException, InterruptedException {
        HttpClient client = httpClientBuilder.build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://zefiro.me/sapi/media/folder?action=list&parentid=" + parentId + "&limit=200&validationkey=" + validationKey))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ZefiroException("Failed to list folder content: " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Lists the files within a specified folder on Zefiro.
     *
     * @param parentId the ID of the folder to list files from
     * @param validationKey the validation key obtained from a successful login
     * @return a JSON string containing the list of files
     * @throws ZefiroException if an error occurs during the API call
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    private String listFiles(long parentId, String validationKey) throws ZefiroException, IOException, InterruptedException {
        HttpClient client = httpClientBuilder.build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://zefiro.me/sapi/media?action=get&folderid=" + parentId + "&limit=200&validationkey=" + validationKey))
                .POST(HttpRequest.BodyPublishers.ofString("{\"data\":{\"fields\":[\"name\",\"modificationdate\",\"size\",\"thumbnails\",\"videometadata\",\"audiometadata\",\"favorite\",\"shared\",\"etag\"]}}"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ZefiroException("Failed to list folder content: " + response.statusCode());
        }

        return response.body();
    }
}