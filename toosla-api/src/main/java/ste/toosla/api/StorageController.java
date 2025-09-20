package ste.toosla.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.http.HttpClient;
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
import ste.toosla.api.dto.LoginResponse;
import ste.toosla.api.dto.ReadRequest;
import ste.toosla.api.dto.WriteRequest;
import ste.toosla.zefiro.ZefiroClient;
import ste.toosla.zefiro.ZefiroException;
import ste.toosla.zefiro.ZefiroFileNotFoundException;
import ste.toosla.zefiro.ZefiroLoginException;
import ste.toosla.zefiro.ZefiroLoginResponse;
import ste.toosla.zefiro.ZefiroModificationException;
import ste.toosla.zefiro.ZefiroUploadResponse;

/**
 * Controller for the remote storage API.
 *
 * <p>This controller exposes endpoints for client applications to authenticate, read,
 * and write file data. It acts as a facade, translating RESTful API calls into
 * operations on the Zefiro object storage backend.
 *
 * <p>Client authentication is managed via a Bearer token provided in the
 * {@code Authorization} header for read and write operations.
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
               description = "Authenticates the user against the backend storage service and returns a temporary access token for subsequent API calls.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Login successful. The response body contains the access token.",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = LoginResponse.class))),
                   @ApiResponse(responseCode = "401", description = "Invalid credentials",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "500", description = "A server-side error occurred while trying to authenticate.",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class)))
               })
    public ResponseEntity<?> login(
            @Parameter(description = "User credentials for the backend storage service.", required = true,
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

            final String validationKey = zefiroResponse.key();

            final String accessKey = keyManager.newKey(account, secret, validationKey);

            LOG.info(() -> "Login successful for account '" + account + "'");

            return ResponseEntity.ok().body(new LoginResponse(zefiroResponse.account(), accessKey, validationKey));
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
    @Operation(
        summary = "Read a file from storage",
        description = "Reads the content of a file from the remote storage. This endpoint supports conditional requests using the `If-Modified-Since` header to conserve bandwidth.",
        responses = {
            @ApiResponse(responseCode = "200", description = "File content returned in the response body.",
                         content = @Content(mediaType = "application/json",
                                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "304", description = "The file has not been modified since the date specified in the `If-Modified-Since` header."),
            @ApiResponse(responseCode = "401", description = "Unauthorized. The `Authorization` header is missing, invalid, or expired.",
                         content = @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The specified file or path does not exist.",
                         content = @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "A server-side error occurred while reading the file.",
                         content = @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public ResponseEntity<?> read(
            @Parameter(description = "The path of the file to read.", required = true,
                       schema = @Schema(implementation = ReadRequest.class))
            @Valid @RequestBody ReadRequest readRequest,
            @Parameter(description = "Standard HTTP `If-Modified-Since` header. If provided, the server returns a `304 Not Modified` status if the file has not changed since this date.", example = "Wed, 21 Oct 2015 07:28:00 GMT")
            @RequestHeader(name = "If-Modified-Since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Date ifModifiedSince,
            @Parameter(description = "The Bearer token obtained from the `/login` endpoint.", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        LOG.info(() -> "Attempting to read file: " + readRequest.path() + " if modified since " + ifModifiedSince);

        try {
            final KeyEntry keyEntry = getValidKey(authorizationHeader);

            ZefiroClient zefiroClient =
                new ZefiroClient(keyEntry.account(), keyEntry.secret())
                    .withHttpClientBuilder(httpClientBuilder)
                    .withValidationKey(keyEntry.validationKey());

            Optional<ZefiroClient.DownloadResult> result = zefiroClient.download(readRequest.path(), ifModifiedSince);
            if (result.isPresent()) {
                LOG.info(() -> "File read successfully: " + readRequest.path());
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .lastModified(result.get().lastModified().toInstant())
                        .body(result.get().content());
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
    @Operation(summary = "Write a file to storage",
               description = "Writes or overwrites a file in the remote storage. This endpoint supports optimistic locking via the `If-Unmodified-Since` header to prevent lost updates.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "File written successfully. The `Last-Modified` header in the response contains the new timestamp of the file."),
                   @ApiResponse(responseCode = "401", description = "Unauthorized. The `Authorization` header is missing, invalid, or expired.",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "404", description = "The specified path's parent folder does not exist.",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "412", description = "Precondition Failed. The file has been modified on the server since the date specified in the `If-Unmodified-Since` header.",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "500", description = "A server-side error occurred while writing the file.",
                                content = @Content(mediaType = "application/json",
                                                   schema = @Schema(implementation = ErrorResponse.class)))
               })
    public ResponseEntity<?> write(
            @Parameter(description = "The path and content of the file to write.", required = true,
                       schema = @Schema(implementation = WriteRequest.class))
            @Valid @RequestBody WriteRequest writeRequest,
            @Parameter(description = "Standard HTTP `If-Unmodified-Since` header. If provided, the server only processes the request if the file has not been modified since this date.", example = "Wed, 21 Oct 2015 07:28:00 GMT")
            @RequestHeader(name = "If-Unmodified-Since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Date ifUnmodifiedSince,
            @Parameter(description = "The Bearer token obtained from the `/login` endpoint.", required = true)
            @RequestHeader(name = "Authorization") String authorizationHeader) {
        LOG.info(() -> "Attempting to write file: " + writeRequest.path() + " with If-Unmodified-Since: " + ifUnmodifiedSince);
        try {
            final KeyEntry keyEntry = getValidKey(authorizationHeader);

            ZefiroClient zefiroClient =
                new ZefiroClient(keyEntry.account(), keyEntry.secret())
                .withHttpClientBuilder(httpClientBuilder)
                .withValidationKey(keyEntry.validationKey());

            ZefiroUploadResponse zefiroResponse = zefiroClient.upload(writeRequest.path(), writeRequest.content(), ifUnmodifiedSince);
            LOG.info(() -> "File written successfully: " + writeRequest.path());

            ResponseEntity.BodyBuilder response = ResponseEntity.ok();
            response.lastModified(zefiroResponse.lastModified().toInstant());
            return response.build();
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
