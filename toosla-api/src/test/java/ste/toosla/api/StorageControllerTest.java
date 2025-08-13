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

@SpringBootTest
@AutoConfigureMockMvc
public class StorageControllerTest {

    public static final String SUCCESSFUL_ZEFIRO_LOGIN_RESPONSE =
       "{\"data\":{\"roles\":[{\"name\":\"sync_user\",\"description\":\"\"},{\"name\":\"zefiropro\",\"description\":\"\"}],\"jsessionid\":\"60B4626852904AFCF9AA25D858730F8E.1i164\",\"validationkey\":\"2e3f4b6c3e2566274023542970455031\"},\"responsetime\":1754811602118}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HttpClient.Builder httpClientBuilder;

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
    }

    @Test
    public void login_successful() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                    new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login"),
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
            "https://zefiro.me/sapi/login",
            new StubHttpResponse<String>().statusCode(401).text("{\"success\": false}")
        );

        // When & Then
        mockMvc.perform(post("/api/storage/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentials\":\"wrong_credentials\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Login failed"));
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
                    new URIMatcher("https://zefiro.me/sapi/login"),
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
                    new URIMatcher("https://zefiro.me/sapi/login"),
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
                    new URIMatcher("https://zefiro.me/sapi/login"),
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
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    public void login_zefiro_invalid_json_response() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login"),
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
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    public void login_zefiro_missing_fields_in_response() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            new ANDMatcher(
                new RequestMatcher[] {
                    new URIMatcher("https://zefiro.me/sapi/login"),
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
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}