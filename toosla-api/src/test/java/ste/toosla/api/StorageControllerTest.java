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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.net.http.HttpClient;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import ste.xtest.net.http.ANDMatcher;
import ste.xtest.net.http.BodyMatcher;
import ste.xtest.net.http.HeaderMatcher;
import ste.xtest.net.http.HttpClientStubber;
import ste.xtest.net.http.RequestMatcher;
import ste.xtest.net.http.StubHttpClient.NetworkError;
import ste.xtest.net.http.StubHttpClient.StubHttpResponse;
import ste.xtest.net.http.URIMatcher;
import org.junit.jupiter.api.AfterEach;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ste.toosla.zefiro.ZefiroClientTest.FIXED_EARLIER_DATE;
import static ste.toosla.zefiro.ZefiroClientTest.FIXED_LATER_DATE;
import static ste.toosla.zefiro.ZefiroClientTest.FIXED_MODIFICATION_DATE;
import static ste.toosla.zefiro.ZefiroClientTest.TEST_ACCOUNT;
import static ste.toosla.zefiro.ZefiroClientTest.TEST_BASIC_CREDENTIALS;
import static ste.toosla.zefiro.ZefiroClientTest.TEST_SECRET;
import static ste.toosla.zefiro.ZefiroClientTest.TEST_VALIDATION_KEY;
import static ste.toosla.zefiro.ZefiroClientTest.setUpFileStubs;
import ste.xtest.logging.ListLogHandler;
import ste.xtest.logging.LogAssertions;


//
// TODO: ideally, StorageControllerTest should not be aware of Zefiro urls;
// all stubbing stuff shall be provided by ZefiroClientTest or even better,
// we should have a fake implementation of the remote service.
//
@SpringBootTest
@AutoConfigureMockMvc
public class StorageControllerTest {


    public static final String SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE =
       "{\"data\":{\"roles\":[{\"name\":\"sync_user\",\"description\":\"\"},{\"name\":\"zefiropro\",\"description\":\"\"}],\"jsessionid\":\"60B4626852904AFCF9AA25D858730F8E.1i164\",\"validationkey\":\"test_key\"},\"responsetime\":1754811602118}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HttpClientStubber httpClientBuilder;

    @Autowired
    private KeyManager keyManager;

    private ListLogHandler logHandler;

    @TestConfiguration
    static class TestConfig {
        @Bean
        // This @Primary annotation is necessary to resolve a BeanDefinitionOverrideException.
        // The HttpClient.Builder is also defined in ApplicationConfig.java, and this test
        // needs to provide its own mock implementation. @Primary tells Spring to prefer
        // this bean when there are multiple beans of the same type.

        @Primary
        public HttpClient.Builder httpClientBuilder() {
            return new HttpClientStubber();
        }

        @Bean
        @Primary
        public KeyManager keyManager() {
            return new KeyManager(3600000); // 1 hour expiration
        }
    }

    @BeforeEach
    public void before() throws Exception {
        httpClientBuilder.stubs().clear();
        Logger logger = Logger.getLogger(StorageController.class.getName());
        logger.setLevel(Level.ALL);
        logHandler = new ListLogHandler();
        logger.addHandler(logHandler);
        logHandler.getRecords().clear(); // Clear any logs from previous tests or setup
    }

    @AfterEach
    public void after() {
        Logger storageControllerLogger = Logger.getLogger(StorageController.class.getName());
        storageControllerLogger.removeHandler(logHandler);
    }

    @Test
    public void login_successful() throws Exception {
        // Given
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me")
            ),
            new StubHttpResponse<String>().text(SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE)
        );

        // Test with user1:password1
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("user1"))
                .andExpect(jsonPath("$.key").exists());

        // Test with user1: (empty password)
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("user1"))
                .andExpect(jsonPath("$.key").exists());

        // Test with user1 (still empty password)
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("user1"))
                .andExpect(jsonPath("$.key").exists());
    }

    @Test
    public void login_failed() throws Exception {
        // Given
        httpClientBuilder.withStub(
            "https://zefiro.me/sapi/login?action=login",
            new StubHttpResponse<String>().statusCode(401).text("{\"success\": false}")
        );

        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"wrong_credentials\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Zefiro authentication failed"));
    }

    @Test
    public void login_malformed_json() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not a json string")) // Malformed JSON
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void login_with_various_credentials() throws Exception {
        // Given
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me"),
                new BodyMatcher("login=&password=user1password1")
            ), new StubHttpResponse<String>().statusCode(200).text(SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE)
        );

        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\":user1password1\"}")) // Credentials without ':'
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value(""))
                .andExpect(jsonPath("$.key").exists());

        httpClientBuilder.stubs().clear();
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me"),
                new BodyMatcher("login=++++&password=pwd")
            ), new StubHttpResponse<String>().statusCode(200).text(SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE)
        );
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"    :pwd\"}")) // blank account
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("    "))
                .andExpect(jsonPath("$.key").exists());
    }

    @Test
    public void login_zefiro_connection_error() throws Exception {
        // Given
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me")
            ),
            new NetworkError()
        );

        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error processing the Zefiro request"));
    }

    @Test
    public void login_zefiro_invalid_json_response() throws Exception {
        // Given
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me")
            ),
            new StubHttpResponse<String>().text("This is not JSON") // Invalid JSON response
        );

        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error processing the Zefiro request"));
    }

    @Test
    public void login_zefiro_missing_fields_in_response() throws Exception {
        // Given
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me")
            ),
            new StubHttpResponse<String>().text("{\"data\":{\"roles\":[{\"name\":\"sync_user\",\"description\":\"\"},{\"name\":\"zefiropro\",\"description\":\"\"}],\"jsessionid\":\"60B4626852904AFCF9AA25D858730F8E.1i164\"},\"responsetime\":1754811602118}")
        );

        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error processing the Zefiro request"));
    }

    @Test
    public void log_successful_login() throws Exception {
        // Given
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me")
            ),
            new StubHttpResponse<String>().text(SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE)
        );

        // When
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"));

        // Then
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting login")
                .containsINFO("Sending login request to Zefiro for account 'user1'")
                .containsINFO("Login successful for account 'user1'");
    }

    @Test
    public void log_failed_login() throws Exception {
        // Given
        httpClientBuilder.withStub(
            "https://zefiro.me/sapi/login?action=login",
            new StubHttpResponse<String>().statusCode(401).text("{\"success\": false}")
        );

        // When
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"wrong_credentials\"}"));

        // Then
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting login")
                .containsINFO("Sending login request to Zefiro for account 'wrong_credentials'")
                .containsINFO("Zefiro authentication failed - Not authorized");
    }

    @Test
    public void log_zefiro_missing_fields() throws Exception {
        // Given
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me")
            ),
            new StubHttpResponse<String>().text("{\"data\":{\"roles\":[{\"name\":\"sync_user\",\"description\":\"\"},{\"name\":\"zefiropro\",\"description\":\"\"}],\"jsessionid\":\"60B4626852904AFCF9AA25D858730F8E.1i164\"},\"responsetime\":1754811602118}")
        );

        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"));

        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting login")
                .containsINFO("Sending login request to Zefiro for account 'user1'")
                .containsSEVERE("Error processing the Zefiro request - Missing or empty 'validationkey' in Zefiro response");
    }

    @Test
    public void log_login_connection_error() throws Exception {
        // Given
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me")
            ),
            new NetworkError()
        );

        // When
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"));

        // Then
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting login")
                .containsINFO("Sending login request to Zefiro for account 'user1'");
    }

    @Test
    public void no_logs_when_level_off() throws Exception {
        // Given
        Logger storageControllerLogger = Logger.getLogger(StorageController.class.getName());
        storageControllerLogger.setLevel(Level.OFF); // Set log level to OFF

        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                new HeaderMatcher("Origin", "https://zefiro.me")
            ),
            new StubHttpResponse<String>().text(SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE)
        );

        // When
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"));

        // Then
        then(logHandler.getRecords()).isEmpty(); // Assert that no log records were captured

        // Reset log level to default for other tests
        storageControllerLogger.setLevel(Level.ALL);
    }

    @Test
    public void write_new_file_successful() throws Exception {
        // Given
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        setUpFileStubs(httpClientBuilder).withStub(
            new ANDMatcher(
                new URIMatcher("https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().text("{\"success\":\"Media uploaded successfully\",\"id\":\"12345\",\"status\":\"V\",\"etag\":\"J7XxRng02rtVeS3X9Wj58Q==\",\"responsetime\":1755272687861,\"type\":\"file\"}")
        );

        // When & Then
        mockMvc.perform(post("/api/storage/write")
            .header("If-Unmodified-Since", DateTimeFormatter.ISO_INSTANT.format(FIXED_MODIFICATION_DATE.toInstant()))
            .header("Authorization", "Bearer " + accessKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\": \"/OneMediaHub/Toosla/new_file.json\", \"content\": \"{\\\"key\\\":\\\"value\\\"}\"}"))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Last-Modified", "Tue, 19 Aug 2025 00:00:00 GMT")
            );
    }

    @Test
    public void write_file_precondition_failed_if_server_is_more_recent() throws Exception {
        // Given
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        setUpFileStubs(httpClientBuilder);

        // When & Then
        mockMvc.perform(post("/api/storage/write")
            .header("If-Unmodified-Since", DateTimeFormatter.ISO_INSTANT.format(FIXED_EARLIER_DATE.toInstant()))
            .header("Authorization", "Bearer " + accessKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\": \"/OneMediaHub/Toosla/toosla.json\", \"content\": \"{\\\"key\\\":\\\"value\\\"}\"}"))
            .andExpect(status().isPreconditionFailed())
            .andExpect(
                header().string("Last-Modified", "Tue, 19 Aug 2025 00:00:00 GMT")
            )
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Precondition Failed"))
            .andExpect(jsonPath("$.details").value("Modification conflict detected"));
    }

    @Test
    public void write_file_generic_zefiro_error() throws Exception {
        // Given
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        setUpFileStubs(httpClientBuilder);
        httpClientBuilder.withStub(
            new ANDMatcher(
                new URIMatcher("https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().statusCode(500).text("Generic Zefiro error during write")
        );

        // When & Then
        mockMvc.perform(post("/api/storage/write")
            .header("Authorization", "Bearer " + accessKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\": \"/OneMediaHub/Toosla/new_file.json\", \"content\": \"{\\\"key\\\":\\\"value\\\"}\"}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Error writing file"))
            .andExpect(jsonPath("$.details").value("Error connecting to Zefiro"));
    }

    @Test
    public void log_write_operations() throws Exception {
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        // Scenario 1: Successful write
        setUpFileStubs(httpClientBuilder).withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            }),
            new StubHttpResponse<String>().text("{\"success\":\"Media uploaded successfully\",\"id\":\"12345\",\"status\":\"V\",\"etag\":\"J7XxRng02rtVeS3X9Wj58Q==\",\"responsetime\":1755272687861,\"type\":\"file\"}")
        );
        mockMvc.perform(post("/api/storage/write")
            .header("If-Unmodified-Since", DateTimeFormatter.ISO_INSTANT.format(FIXED_MODIFICATION_DATE.toInstant()))
            .header("Authorization", "Bearer " + accessKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\": \"/OneMediaHub/Toosla/new_file.json\", \"content\": \"{\\\"key\\\":\\\"value\\\"}\"}"))
            .andExpect(status().isOk());
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting to write file: /OneMediaHub/Toosla/new_file.json with If-Unmodified-Since: " + FIXED_MODIFICATION_DATE)
                .containsINFO("File written successfully: /OneMediaHub/Toosla/new_file.json");

        //
        // Scenario 2: Precondition Failed
        //
        logHandler.getRecords().clear(); httpClientBuilder.stubs().clear();

        httpClientBuilder.stubs().clear(); setUpFileStubs(httpClientBuilder);
        mockMvc.perform(post("/api/storage/write")
            .header("If-Unmodified-Since", DateTimeFormatter.ISO_INSTANT.format(FIXED_EARLIER_DATE.toInstant()))
            .header("Authorization", "Bearer " + accessKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\": \"/OneMediaHub/Toosla/toosla.json\", \"content\": \"{\\\"key\\\":\\\"value\\\"}\"}"))
            .andExpect(status().isPreconditionFailed());
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting to write file: /OneMediaHub/Toosla/toosla.json with If-Unmodified-Since: " + FIXED_EARLIER_DATE)
                .containsWARNING("Precondition Failed for file: /OneMediaHub/Toosla/toosla.json - Modification conflict detected");

        //
        // Scenario 3: File not found
        //
        logHandler.getRecords().clear(); httpClientBuilder.stubs().clear();

        httpClientBuilder.withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            }),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[]}}")
        );
        mockMvc.perform(post("/api/storage/write")
                .header("Authorization", "Bearer " + accessKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"path\": \"/OneMediaHub/Toosla/new_file.json\", \"content\": \"{\\\"key\\\":\\\"value\\\"}\"}"))
                .andExpect(status().isNotFound());
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting to write file: /OneMediaHub/Toosla/new_file.json with If-Unmodified-Since: null")
                .containsWARNING("File not found: /OneMediaHub/Toosla/new_file.json");

        //
        // Scenario 4: Generic Zefiro error
        //
        logHandler.getRecords().clear(); httpClientBuilder.stubs().clear(); // Clear logs for next scenario
        httpClientBuilder.withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            }),
            new StubHttpResponse<String>().statusCode(500).text("Generic Zefiro error during write")
        );
        mockMvc.perform(post("/api/storage/write")
            .header("Authorization", "Bearer " + accessKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\": \"/OneMediaHub/Toosla/new_file.json\", \"content\": \"{\\\"key\\\":\\\"value\\\"}\"}"))
            .andExpect(status().isInternalServerError());
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting to write file: /OneMediaHub/Toosla/new_file.json with If-Unmodified-Since: null")
                .containsSEVERE("Error writing file: /OneMediaHub/Toosla/new_file.json");
    }

    @Test
    public void read_file_not_found() throws Exception {
        // Given
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        httpClientBuilder.withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            }),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487}]}}")
        ).withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            }),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"Toosla\",\"id\":12345}]}}")
        ).withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media/folder?action=list&parentid=12345&limit=200&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            }),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[]}}")
        ).withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media?action=get&folderid=12345&limit=200&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            }),
            new StubHttpResponse<String>().text("{\"data\":{\"media\":[]}}")
        );

        // When & Then
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessKey)
                .content("{\"path\":\"/OneMediaHub/Toosla/not_found.json\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File not found"))
                .andExpect(jsonPath("$.details").value("File not found: /OneMediaHub/Toosla/not_found.json"));
    }

    @Test
    public void read_from_wrong_path() throws Exception {
        // Given
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        setUpFileStubs(httpClientBuilder);

        // When & Then
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessKey)
                .content("{\"path\":\"/OneMediaHub/Toosla/not_found.json\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File not found"))
                .andExpect(jsonPath("$.details").value("File not found: /OneMediaHub/Toosla/not_found.json"));
    }

    @Test
    public void read_with_invalid_access_key_returns_unauthorized() throws Exception {
        // Given                                                                        │
        // No Authorization header provided                                             │

        // When & Then
        mockMvc.perform(post("/api/storage/read")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Unauthorized"))
            .andExpect(jsonPath("$.details").value("Missing or invalid Authorization header"));

        // When & Then
        mockMvc.perform(post("/api/storage/read")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer invalid access key")
            .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Unauthorized"))
            .andExpect(jsonPath("$.details").value("Missing or invalid Authorization header"));
    }

    @Test
    public void read_when_if_modified_since_is_in_the_past() throws Exception {
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        setUpFileStubs(httpClientBuilder);

        // When & Then
        mockMvc.perform(post("/api/storage/read")
                .header("Authorization", "Bearer " + accessKey)
                .contentType(MediaType.APPLICATION_JSON)
                .header("If-Modified-Since", DateTimeFormatter.ISO_INSTANT.format(FIXED_LATER_DATE.toInstant()))
                .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
                .andExpect(status().isNotModified());
    }

    @Test
    public void read_when_if_modified_since_is_in_the_future() throws Exception {
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        setUpFileStubs(httpClientBuilder);

        // When & Then
        mockMvc.perform(post("/api/storage/read")
                .header("Authorization", "Bearer " + accessKey)
                .contentType(MediaType.APPLICATION_JSON)
                .header("If-Modified-Since", DateTimeFormatter.ISO_INSTANT.format(FIXED_EARLIER_DATE.toInstant()))
                .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string("{\"content\":\"this is toosla\"}"));
    }

    @Test
    public void read_file_generic_zefiro_error() throws Exception {
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

         // When & Then
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessKey)
                .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error reading file"))
                .andExpect(jsonPath("$.details").value("Error connecting to Zefiro"));

        LogAssertions.then(logHandler.getRecords())
                .containsSEVERE("Error reading file: /OneMediaHub/Toosla/toosla.json");
    }

    @Test
    public void log_read_operations() throws Exception {
        final String accessKey = keyManager.newKey(TEST_ACCOUNT, TEST_SECRET, TEST_VALIDATION_KEY);

        // Scenario 1: Successful read
        setUpFileStubs(httpClientBuilder);
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("If-Modified-Since", DateTimeFormatter.ISO_INSTANT.format(FIXED_EARLIER_DATE.toInstant()))
                .header("Authorization", "Bearer " + accessKey)
                .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
                .andExpect(status().isOk());
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting to read file: /OneMediaHub/Toosla/toosla.json if modified since " + FIXED_EARLIER_DATE)
                .containsINFO("File read successfully: /OneMediaHub/Toosla/toosla.json");
        //
        // Scenario 2: File not modified
        //
        logHandler.getRecords().clear(); httpClientBuilder.stubs().clear();
        setUpFileStubs(httpClientBuilder);
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("If-Modified-Since", DateTimeFormatter.ISO_INSTANT.format(FIXED_LATER_DATE.toInstant()))
                .header("Authorization", "Bearer " + accessKey)
                .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
                .andExpect(status().isNotModified());
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting to read file: /OneMediaHub/Toosla/toosla.json if modified since " + FIXED_LATER_DATE)
                .containsINFO("File not modified: /OneMediaHub/Toosla/toosla.json");

        //
        // Scenario 3: File not found
        //
        logHandler.getRecords().clear(); httpClientBuilder.stubs().clear();
        httpClientBuilder.withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            }),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[]}}")
        );
        mockMvc.perform(post("/api/storage/read")
            .header("Authorization", "Bearer " + accessKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\":\"/OneMediaHub/Toosla/not_found.json\"}"))
            .andExpect(status().isNotFound());
        LogAssertions.then(logHandler.getRecords())
            .containsINFO("Attempting to read file: /OneMediaHub/Toosla/not_found.json if modified since null")
            .containsWARNING("File not found: /OneMediaHub/Toosla/not_found.json");
        logHandler.getRecords().clear(); // Clear logs for next scenario

        //
        // Scenario 4: Generic Zefiro error
        //
        logHandler.getRecords().clear(); httpClientBuilder.stubs().clear();
        httpClientBuilder.withStub(
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            }),
            new StubHttpResponse<String>().statusCode(500).text("Generic Zefiro error during read")
        );
        mockMvc.perform(post("/api/storage/read")
            .header("Authorization", "Bearer " + accessKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
            .andExpect(status().isInternalServerError());
        LogAssertions.then(logHandler.getRecords())
                .containsINFO("Attempting to read file: /OneMediaHub/Toosla/toosla.json if modified since null")
                .containsSEVERE("Error reading file: /OneMediaHub/Toosla/toosla.json");
    }

}