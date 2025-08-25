package ste.toosla.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.http.HttpClient;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ste.toosla.api.KeyManager.KeyEntry;
import ste.toosla.api.dto.ErrorResponse;
import ste.toosla.api.dto.LoginRequest;
import ste.toosla.api.dto.ReadRequest;
import ste.toosla.zefiro.ZefiroClient;
import ste.toosla.zefiro.ZefiroException;
import ste.toosla.zefiro.ZefiroFileNotFoundException;
import ste.toosla.zefiro.ZefiroLoginException;
import ste.toosla.zefiro.ZefiroLoginResponse;
import ste.toosla.zefiro.ZefiroModificationException;

/**
 * A controller that handles the storage-related API endpoints.
 * This controller provides endpoints for logging in, reading, and writing files to the remote storage.
 */
@RestController
@Tag(name = "Storage", description = "API for storing and retrieving data from the remote storage.")
public class StorageController {

    private static final Logger LOG = Logger.getLogger(StorageController.class.getName());

    //
    // Regex pattern to match the different formats
    // Group 1: captures part1 (everything before colon, if present)
    // Group 2: captures part2 (everything after colon, if present)
    //
    private final Pattern pattern = Pattern.compile("^([^:]*?)(?::(.*))?$");

    @Autowired
    private KeyManager keyManager;
    @Autowired
    private HttpClient.Builder httpClientBuilder;
    private final ObjectMapper objectMapper;

    public StorageController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/storage/login")
    @Operation(summary = "Login to the storage service",
               description = "Authenticates the user with the given credentials and returns a session key.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Login successful",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ZefiroLoginResponse.class))),
                   @ApiResponse(responseCode = "401", description = "Invalid credentials",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "500", description = "Internal server error",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class)))
               })
    public ResponseEntity<?> login(
            @Parameter(description = "User credentials for login.", required = true,
                       schema = @Schema(implementation = LoginRequest.class))
            @Valid @RequestBody LoginRequest loginRequest) throws Exception {
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
            ZefiroClient zefiroClient =
                new ZefiroClient(account, secret).withHttpClientBuilder(httpClientBuilder);

            ZefiroLoginResponse zefiroResponse = zefiroClient.login();

            ObjectNode apiResponse = objectMapper.createObjectNode();

            final String validationKey = zefiroResponse.key();

            final String accessKey = keyManager.newKey(account, secret, validationKey);

            apiResponse.put("account", zefiroResponse.account());
            apiResponse.put("key", accessKey);

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
    @Operation(summary = "Read a file from the storage",
               description = "Reads the content of a file from the remote storage. The `If-Modified-Since` header can be used to check if the file has been modified.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "File content",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(type = "string"))),
                   @ApiResponse(responseCode = "304", description = "Not Modified"),
                   @ApiResponse(responseCode = "404", description = "File not found",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "500", description = "Internal server error",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class)))
               })
    public ResponseEntity<?> read(
            @Parameter(description = "Path of the file to read.", required = true,
                       schema = @Schema(implementation = ReadRequest.class))
            @Valid @RequestBody ReadRequest readRequest,
            @Parameter(description = "Date to check for modifications.", example = "2025-08-20T10:00:00Z")
            @RequestHeader(name = "If-Modified-Since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Date ifModifiedSince,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        LOG.info(() -> "Attempting to read file: " + readRequest.path() + " if modified since " + ifModifiedSince);

        try {
            final KeyEntry keyEntry = getValidKey(authorizationHeader);

            ZefiroClient zefiroClient =
                new ZefiroClient(keyEntry.account(), keyEntry.secret())
                    .withHttpClientBuilder(httpClientBuilder)
                    .withValidationKey(keyEntry.validationKey());

            Optional<String> content = zefiroClient.download(readRequest.path(), ifModifiedSince);
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
        } catch (ZefiroLoginException x) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ErrorResponse("Unauthorized", x.getMessage()));
        } catch (ZefiroException x) {
            LOG.log(Level.SEVERE, x, () -> "Error reading file: " + readRequest.path());
            return ResponseEntity.internalServerError().body(
                    new ErrorResponse("Error reading file", x.getMessage()));
        }
    }

    @PostMapping("/api/storage/write")
    @Operation(summary = "Write a file to the storage",
               description = "Writes the content of a file to the remote storage. The `If-Unmodified-Since` header can be used to prevent overwriting a more recent version of the file.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "File written successfully"),
                   @ApiResponse(responseCode = "404", description = "File not found",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "412", description = "Precondition Failed",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "500", description = "Internal server error",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class)))
               })
    public ResponseEntity<?> write(
            @Parameter(description = "Path and content of the file to write.", required = true,
                       schema = @Schema(implementation = ste.toosla.api.dto.WriteRequest.class))
            @Valid @RequestBody ste.toosla.api.dto.WriteRequest writeRequest,
            @Parameter(description = "Date to check for modifications.", example = "2025-08-20T10:00:00Z")
            @RequestHeader(name = "If-Unmodified-Since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Date ifUnmodifiedSince,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        LOG.info(() -> "Attempting to write file: " + writeRequest.path() + " with If-Unmodified-Since: " + ifUnmodifiedSince);
        try {
            final KeyEntry keyEntry = getValidKey(authorizationHeader);

            ZefiroClient zefiroClient =
                new ZefiroClient(keyEntry.account(), keyEntry.secret())
                .withHttpClientBuilder(httpClientBuilder)
                .withValidationKey(keyEntry.validationKey());


            zefiroClient.upload(writeRequest.path(), writeRequest.content(), ifUnmodifiedSince);
            LOG.info(() -> "File written successfully: " + writeRequest.path());
            if (ifUnmodifiedSince != null) {
                return ResponseEntity.ok().lastModified(ifUnmodifiedSince.toInstant().atZone(ZoneId.of("UTC"))).build();
            }
            return ResponseEntity.ok().build();
        } catch (ZefiroFileNotFoundException x) {
            LOG.warning(() -> "File not found: " + writeRequest.path());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponse("File not found", x.getMessage()));
        } catch (ZefiroModificationException x) {
            LOG.warning(() -> "Precondition Failed for file: " + writeRequest.path() + " - " + x.getMessage());
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(HttpStatus.PRECONDITION_FAILED);
            if (x.lastModified.isPresent()) {
                responseBuilder.lastModified(x.lastModified.get().getTime());
            }
            return responseBuilder.body(new ErrorResponse("Precondition Failed", x.getMessage()));
        } catch (ZefiroLoginException x) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ErrorResponse("Unauthorized", x.getMessage()));
        } catch (ZefiroException x) {
            LOG.log(Level.SEVERE, x, () -> "Error writing file: " + writeRequest.path());
            return ResponseEntity.internalServerError().body(
                    new ErrorResponse("Error writing file", x.getMessage()));
        }
    }

    // --------------------------------------------------------- private methods

    private KeyEntry getValidKey(final String authorizationHeader) throws ZefiroLoginException {
        KeyEntry keyEntry = null;
        if (
            authorizationHeader == null ||
            !authorizationHeader.startsWith("Bearer ") ||
            (keyEntry = keyManager.get(authorizationHeader.substring(7))) == null
            ) {
                throw new ZefiroLoginException("Missing or invalid Authorization header");
        }

        return keyEntry;
    }
}
