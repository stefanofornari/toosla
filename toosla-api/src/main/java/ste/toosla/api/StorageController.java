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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpClient;
import java.util.logging.Level;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ste.toosla.api.dto.LoginRequest;
import ste.toosla.api.dto.ReadRequest;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ste.toosla.api.dto.ErrorResponse;
import ste.toosla.zefiro.ZefiroClient;
import ste.toosla.zefiro.ZefiroException;
import ste.toosla.zefiro.ZefiroLoginException;
import ste.toosla.zefiro.ZefiroLoginResponse;
import ste.toosla.zefiro.ZefiroFileNotFoundException;
import java.util.Optional;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
public class StorageController {

    private static final Logger LOG = Logger.getLogger(StorageController.class.getName());

    //
    // Regex pattern to match the different formats
    // Group 1: captures part1 (everything before colon, if present)
    // Group 2: captures part2 (everything after colon, if present)
    //
    private final Pattern pattern = Pattern.compile("^([^:]*?)(?::(.*))?$");

    private final ZefiroClient zefiroClient;
    private final ObjectMapper objectMapper;

    public StorageController(HttpClient.Builder httpClientBuilder, ObjectMapper objectMapper) {
        this.zefiroClient = new ZefiroClient(httpClientBuilder);
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/storage/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) throws Exception {
        LOG.info(() -> "Attempting login");
        //
        // Extract credentials from the LoginRequest record and process them
        // to get account and password to use with to Zefiro
        //
        final Matcher matcher = pattern.matcher(loginRequest.credentials());

        matcher.matches();

        // Group 1 contains part1 (or empty string if starts with :)
        final String account = matcher.group(1) != null ? matcher.group(1) : "";
        // Group 2 contains part2 (or null if no colon or ends with :)
        final String secret = matcher.group(2) != null ? matcher.group(2) : "";

        LOG.info(() -> "Sending login request to Zefiro for account '" + account + "'");
        ResponseEntity[] error = new ResponseEntity[1];
        Level errorLevel = Level.OFF;
        try {
            ZefiroLoginResponse zefiroResponse = zefiroClient.login(account, secret);
            ObjectNode apiResponse = objectMapper.createObjectNode();
            apiResponse.put("account", zefiroResponse.account());
            apiResponse.put("key", zefiroResponse.key());
            LOG.info(() -> "Login successful for account '" + account + "'");

            return ResponseEntity.ok().body(apiResponse);
        } catch (ZefiroLoginException x) {
            error[0] = ResponseEntity.status(401).body(
                new ErrorResponse("Zefiro authentication failed", x.getMessage())
            );
            errorLevel = Level.INFO;
        } catch (ZefiroException x) {
            error[0] = ResponseEntity.internalServerError().body(
                new ErrorResponse("Error processing the Zefiro request", x.getMessage())
            );
            errorLevel = Level.SEVERE;
        }

        LOG.log(errorLevel, () -> {
            final ErrorResponse R = (ErrorResponse)error[0].getBody();
            return R.getMessage() + " - " + R.getDetails();
        });

        return error[0];
    }

    @PostMapping("/api/storage/read")
    public ResponseEntity<?> read(
            @Valid @RequestBody ReadRequest readRequest,
            @RequestHeader("X-Validation-Key") String validationKey,
            @RequestHeader(name = "If-Modified-Since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Date ifModifiedSince) {
        LOG.info(() -> "Attempting to read file: " + readRequest.path() + " if modified since " + ifModifiedSince);
        try {
            Optional<String> content = zefiroClient.download(readRequest.path(), validationKey, ifModifiedSince);
            if (content.isPresent()) {
                LOG.info(() -> "File read successfully: " + readRequest.path());
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(content.get());
            } else {
                LOG.info(() -> "File not modified: " + readRequest.path());
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
        } catch (ZefiroFileNotFoundException x) {
            LOG.warning(() -> "File not found: " + readRequest.path());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponse("File not found", x.getMessage()));
        } catch (ZefiroException x) {
            LOG.log(Level.SEVERE, x, () -> "Error reading file: " + readRequest.path());
            return ResponseEntity.internalServerError().body(
                    new ErrorResponse("Error reading file", x.getMessage()));
        }
    }
}