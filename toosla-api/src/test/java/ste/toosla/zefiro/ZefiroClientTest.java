package ste.toosla.zefiro;

import java.net.http.HttpClient;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ste.xtest.net.http.HttpClientStubber;
import ste.xtest.net.http.StubHttpClient.StubHttpResponse;

public class ZefiroClientTest {

    private HttpClient.Builder httpClientBuilder;
    private ZefiroClient zefiro;

    @BeforeEach
    public void setUp() {
        httpClientBuilder = new HttpClientStubber();
        zefiro = new ZefiroClient(httpClientBuilder);
    }

    @Test
    public void login_connection_error() {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            "https://zefiro.me/sapi/login?action=login",
            new ste.xtest.net.http.StubHttpClient.NetworkError()
        );

        // When & Then
        thenThrownBy(() -> zefiro.login("test_user", "test_password"))
                .isInstanceOf(ZefiroException.class)
                .hasMessage("Error connecting to Zefiro");
    }

    @Test
    public void login_invalid_json() {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            "https://zefiro.me/sapi/login?action=login",
            new ste.xtest.net.http.StubHttpClient.StubHttpResponse<String>().text("this is not json")
        );

        // When & Then
        thenThrownBy(() -> zefiro.login("test_user", "test_password"))
                .isInstanceOf(ZefiroException.class)
                .hasMessage("Invalid JSON response from Zefiro");
    }

    @Test
    public void login_missing_validation_key() {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            "https://zefiro.me/sapi/login?action=login",
            new ste.xtest.net.http.StubHttpClient.StubHttpResponse<String>().text("{\"data\":{}}")
        );

        // When & Then
        thenThrownBy(() -> zefiro.login("test_user", "test_password"))
                .isInstanceOf(ZefiroException.class)
                .hasMessage("Missing or empty 'validationkey' in Zefiro response");
    }

    @Test
    public void login_wrong_credentials() {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            "https://zefiro.me/sapi/login?action=login",
            new StubHttpResponse<String>().statusCode(401)
        );

        // When & Then
        thenThrownBy(() -> zefiro.login("test_user", "test_password"))
                .isInstanceOf(ZefiroLoginException.class);
    }

    @Test
    public void login_successful() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            "https://zefiro.me/sapi/login?action=login",
            new StubHttpResponse<String>().text("{\"data\":{\"validationkey\":\"test_key\"}}")
        );

        // When
        ZefiroLoginResponse response = zefiro.login("test_user", "test_password");

        // Then
        then(response.account()).isEqualTo("test_user");
        then(response.key()).isEqualTo("test_key");
    }
}