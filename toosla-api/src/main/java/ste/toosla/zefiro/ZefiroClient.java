package ste.toosla.zefiro;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        } catch (JsonProcessingException e) {
            throw new ZefiroException("Invalid JSON response from Zefiro", e);
        } catch (IOException | InterruptedException e) {
            throw new ZefiroException("Error connecting to Zefiro", e);
        }
    }
}
