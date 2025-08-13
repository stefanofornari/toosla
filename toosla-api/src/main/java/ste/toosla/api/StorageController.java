/*
 * Toosla.me
 * ---------
 *
 * Copyright (C) 2025 Stefano Fornari. Licensed under the
 * EUPL-1.2 or later (see LICENSE).
 *
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Stefano Fornari.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * STEFANO FORNARI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. STEFANO FORNARI SHALL NOT BE LIABLE FOR ANY
 * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */

package ste.toosla.api;

import jakarta.validation.Valid;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ste.toosla.api.dto.ErrorResponse;
import ste.toosla.api.dto.LoginRequest;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class StorageController {

    //
    // Regex pattern to match the different formats
    // Group 1: captures part1 (everything before colon, if present)
    // Group 2: captures part2 (everything after colon, if present)
    //
    private final Pattern pattern = Pattern.compile("^([^:]*?)(?::(.*))?$");

    private final HttpClient.Builder httpClientBuilder;
    private final ObjectMapper objectMapper;

    public StorageController(HttpClient.Builder httpClientBuilder, ObjectMapper objectMapper) {
        this.httpClientBuilder = httpClientBuilder;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/storage/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) throws Exception {
        // Extract credentials from the LoginRequest record
        final String credentials = loginRequest.credentials();

        final Matcher matcher = pattern.matcher(credentials);

        matcher.matches();

        // Group 1 contains part1 (or empty string if starts with :)
        final String account = matcher.group(1) != null ? matcher.group(1) : "";
        // Group 2 contains part2 (or null if no colon or ends with :)
        final String secret = matcher.group(2) != null ? matcher.group(2) : "";

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

        if (response.statusCode() >= 400) {
            ErrorResponse error = new ErrorResponse("Login failed", "Received status " + response.statusCode() + " from Zefiro");
            return ResponseEntity.status(response.statusCode()).body(error);
        }

        // Parse Zefiro's response
        JsonNode zefiroResponse = objectMapper.readTree(response.body());
        JsonNode validationKeyNode = zefiroResponse.at("/data/validationkey");
        if (validationKeyNode.isMissingNode() || validationKeyNode.asText().isEmpty()) {
            throw new IllegalStateException("Missing or empty 'validationkey' in Zefiro response");
        }

        // Construct our API's response
        ObjectNode apiResponse = objectMapper.createObjectNode();
        apiResponse.put("account", account);
        apiResponse.put("key", validationKeyNode.asText());

        return ResponseEntity.ok().body(apiResponse);
    }
}