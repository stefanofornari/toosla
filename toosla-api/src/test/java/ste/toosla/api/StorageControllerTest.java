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
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import ste.toosla.zefiro.ZefiroClientTest;
import ste.xtest.logging.ListLogHandler;
import ste.xtest.logging.LogAssertions;

@SpringBootTest
@AutoConfigureMockMvc
public class StorageControllerTest {

    public static final String SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE =
       "{\"data\":{\"roles\":[{\"name\":\"sync_user\",\"description\":\"\"},{\"name\":\"zefiropro\",\"description\":\"\"}],\"jsessionid\":\"60B4626852904AFCF9AA25D858730F8E.1i164\",\"validationkey\":\"2e3f4b6c3e2566274023542970455031\"},\"responsetime\":1754811602118}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HttpClient.Builder httpClientBuilder;

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
    }

    @BeforeEach
    public void before() throws Exception {
        ((HttpClientStubber)httpClientBuilder).stubs().clear();
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
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                    new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me")
                    }
            ),
            new StubHttpResponse<String>().text(SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE)
        );

        // Test with user1:password1
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("user1"))
                .andExpect(jsonPath("$.key").value("2e3f4b6c3e2566274023542970455031"));

        // Test with user1: (empty password)
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1:\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("user1"))
                .andExpect(jsonPath("$.key").value("2e3f4b6c3e2566274023542970455031"));

        // Test with user1 (still empty password)
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"user1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("user1"))
                .andExpect(jsonPath("$.key").value("2e3f4b6c3e2566274023542970455031"));
    }

    @Test
    public void login_failed() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
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
        HttpClientStubber client = (HttpClientStubber)httpClientBuilder;

        // Given
        client.withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me"),
                    new BodyMatcher("login=&password=user1password1")
                }
            ), new StubHttpResponse<String>().statusCode(200).text(SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE)
        );

        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\":user1password1\"}")) // Credentials without ':'
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value(""))
                .andExpect(jsonPath("$.key").value("2e3f4b6c3e2566274023542970455031"));

        client.stubs().clear();
        client.withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me"),
                    new BodyMatcher("login=++++&password=pwd")
                }
            ), new StubHttpResponse<String>().statusCode(200).text(SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE)
        );
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"    :pwd\"}")) // blank account
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("    "))
                .andExpect(jsonPath("$.key").value("2e3f4b6c3e2566274023542970455031"));
    }

    @Test
    public void login_zefiro_connection_error() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me")
                }
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
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me")
                }
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
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me")
                }
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
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                    new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me")
                    }
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
        ((HttpClientStubber) httpClientBuilder).withStub(
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
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me")
                }
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
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me")
                }
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

        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                    new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login?action=login"),
                    new HeaderMatcher("Origin", "https://zefiro.me")
                    }
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
    public void read_file_not_found() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487}]}}")
        ).withStub(
            "https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"Toosla\",\"id\":12345}]}}")
        ).withStub(
            "https://zefiro.me/sapi/media/folder?action=list&parentid=12345&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[]}}")
        ).withStub(
            "https://zefiro.me/sapi/media?action=get&folderid=12345&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"media\":[]}}")
        );

        // When & Then
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Validation-Key", "test_key")
                .content("{\"path\":\"/OneMediaHub/Toosla/not_found.json\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File not found"))
                .andExpect(jsonPath("$.details").value("File not found: /OneMediaHub/Toosla/not_found.json"));
    }

    @Test
    public void read_without_if_modified_since() throws Exception {
        ZefiroClientTest.setUpDownloadStubs(httpClientBuilder, 0);

        // When & Then
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Validation-Key", "test_key")
                .content("{\"path\":\"/OneMediaHub/Toosla/not_found.json\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File not found"))
                .andExpect(jsonPath("$.details").value("File not found: /OneMediaHub/Toosla/not_found.json"));
    }

    @Test
    public void read_when_if_modified_since_is_in_the_past() throws Exception {
        final Date NOW = new Date();
        ZefiroClientTest.setUpDownloadStubs(httpClientBuilder, NOW.getTime()-(60*60*1000));

        // When & Then
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Validation-Key", "test_key")
                .header("If-Modified-Since", DateTimeFormatter.ISO_INSTANT.format(NOW.toInstant()))
                .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
                .andExpect(status().isNotModified());
    }

    @Test
    public void read_when_if_modified_since_is_in_the_future() throws Exception {
        final Date NOW = new Date();
        ZefiroClientTest.setUpDownloadStubs(httpClientBuilder, NOW.getTime()+(60*60*1000));

        // When & Then
        mockMvc.perform(post("/api/storage/read")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Validation-Key", "test_key")
                .header("If-Modified-Since", DateTimeFormatter.ISO_INSTANT.format(NOW.toInstant()))
                .content("{\"path\":\"/OneMediaHub/Toosla/toosla.json\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string("{\"content\":\"this is toosla\"}"));
    }
}