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

/**
 * A client for the Zefiro service.
 */
public class ZefiroClient {

    private final HttpClient.Builder httpClientBuilder;
    private final ObjectMapper jsonMapper;

    /**
     * Creates a new instance of the ZefiroClient.
     */
    public ZefiroClient() {
        this(HttpClient.newBuilder());
    }

    /**
     * Creates a new instance of the ZefiroClient.
     *
     * @param httpClientBuilder the http client builder
     */
    public ZefiroClient(HttpClient.Builder httpClientBuilder) {
        this.httpClientBuilder = httpClientBuilder;
        this.jsonMapper = new ObjectMapper();
    }

    /**
     * Logs in to the Zefiro service.
     *
     * @param account the account name
     * @param secret the account secret
     * @return the login response
     * @throws ZefiroException if an error occurs
     * @throws ZefiroLoginException if the login fails
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
     * Downloads a file from Zefiro.
     *
     * @param path the path to the file
     * @param validationKey the validation key
     * @return the content of the file
     * @throws ZefiroException if an error occurs
     */
    public String download(String path, String validationKey) throws ZefiroException {
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
                for (JsonNode folder: subfolders) {
                    if (pathParts[i].equals(folder.at("/name").asText())) {
                        folderId = folder.at("/id").asLong();
                        parent = jsonMapper.readTree(listFolders(folderId, validationKey));
                        break;
                    }
                }
                //
                // Folder not found - To do: throw an exception
                //
            }

            long fileId = 0;
            JsonNode files = jsonMapper.readTree(listFiles(folderId, validationKey));
            for (JsonNode file: files.at("/data/media")) {
                if (pathParts[pathParts.length - 1].equals(file.at("/name").asText())) {
                    fileId = file.at("/id").asLong();
                    break;
                }
            }

            //
            // todo: if fileId is 0 here, the file was not found
            //

            // Get download URL
            HttpRequest downloadUrlRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + validationKey))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"data\":{\"ids\":[" + fileId + "],\"fields\":[\"url\"]}}"))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> downloadUrlResponse = client.send(downloadUrlRequest, HttpResponse.BodyHandlers.ofString());

            if (downloadUrlResponse.statusCode() >= 400) {
                throw new ZefiroException("Failed to get download URL: " + downloadUrlResponse.statusCode());
            }

            JsonNode downloadUrlJson = jsonMapper.readTree(downloadUrlResponse.body());
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

            return fileContentResponse.body();

        } catch (IOException | InterruptedException e) {
            throw new ZefiroException("Error downloading file", e);
        }
    }

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
