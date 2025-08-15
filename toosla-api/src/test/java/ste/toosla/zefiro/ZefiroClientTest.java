package ste.toosla.zefiro;

import java.net.http.HttpClient;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ste.xtest.net.http.HttpClientStubber;
import ste.xtest.net.http.StubHttpClient.StubHttpResponse;
import ste.xtest.net.http.ANDMatcher;
import ste.xtest.net.http.URIMatcher;
import ste.xtest.net.http.BodyMatcher;
import ste.xtest.net.http.RequestMatcher;


/**
 * TODO:
 *  - FileNotFoundException if a subdir is missing
 *
 */
public class ZefiroClientTest {

    private static final long ONE_DAY = 24 * 60 * 60 * 1000;

    private HttpClient.Builder httpClientBuilder;
    private ZefiroClient zefiro;

    @BeforeEach
    public void beforeEach() {
        httpClientBuilder = new HttpClientStubber();
        zefiro = new ZefiroClient(httpClientBuilder);
    }

    //
    // ---------- login ----------
    //

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

    //
    // ---------- download ----------
    //

    @Test
    public void download_file_from_subfolder() throws Exception {
        setUpDownloadStubs(httpClientBuilder, new Date().getTime());

        then(
            zefiro.download("/OneMediaHub/Toosla/toosla.json", "test_key")
        ).isEqualTo("{\"content\":\"this is toosla\"}");
    }

    @Test
    public void download_connection_error() {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=test_key",
            new ste.xtest.net.http.StubHttpClient.NetworkError()
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/OneMediaHub/Toosla/toosla.json", "test_key"))
                .isInstanceOf(ZefiroException.class)
                .hasMessage("Error connecting to Zefiro");
    }

    @Test
    public void download_invalid_json() {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=test_key",
            new ste.xtest.net.http.StubHttpClient.StubHttpResponse<String>().text("this is not json")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/OneMediaHub/Toosla/toosla.json", "test_key"))
            .isInstanceOf(ZefiroException.class)
            .hasMessage("Invalid JSON response from Zefiro");
    }

    @Test
    public void download_file_not_found() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            //
            // list root folder
            //
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487,\"status\":\"N\",\"magic\":true,\"offline\":false,\"date\":1391491519230}]}}")
        ).withStub(
            //
            // list /OneMediaHub (folders)
            //
            "https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"Toosla\",\"id\":12345,\"status\":\"N\",\"magic\":false,\"offline\":false,\"date\":123}]}}")
        ).withStub(
            //
            // list /OneMediaHub/Toolsa/ (folders)
            //
            "https://zefiro.me/sapi/media/folder?action=list&parentid=12345&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[]}}")
        ).withStub(
            //
            // list /OneMesiaHub/Toosla/ (files)
            //
            "https://zefiro.me/sapi/media?action=get&folderid=12345&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"media\":[]}}")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/OneMediaHub/Toosla/not_found.json", "test_key"))
            .isInstanceOf(ZefiroFileNotFoundException.class)
            .hasMessage("File not found: /OneMediaHub/Toosla/not_found.json");
    }

    @Test
    public void download_subdirectory_not_found() throws Exception {
        // Given
        ((HttpClientStubber) httpClientBuilder).withStub(
            //
            // list root folder
            //
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487,\"status\":\"N\",\"magic\":true,\"offline\":false,\"date\":1391491519230}]}}")
        ).withStub(
            //
            // list /OneMediaHub (folders) - does not contain "NonExistentSubdir"
            //
            "https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"AnotherSubdir\",\"id\":67890,\"status\":\"N\",\"magic\":false,\"offline\":false,\"date\":456}]}}")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/OneMediaHub/NonExistentSubdir/file.json", "test_key"))
            .isInstanceOf(ZefiroFileNotFoundException.class)
            .hasMessage("File not found: /OneMediaHub/NonExistentSubdir/file.json");
    }

    @Test
    public void download_if_modified_since_and_file_is_more_recent() throws Exception {
        // Given
        setUpDownloadStubs(httpClientBuilder, new Date().getTime());

        // When
        Optional<String> content = zefiro.download("/OneMediaHub/Toosla/toosla.json", "test_key", new Date(new Date().getTime() - ONE_DAY));

        // Then
        then(content).isPresent().contains("{\"content\":\"this is toosla\"}");
    }

    @Test
    public void download_if_modified_since_and_file_is_not_more_recent() throws Exception {
        // Given
        setUpDownloadStubs(httpClientBuilder, new Date().getTime() - ONE_DAY);

        // When
        Optional<String> content = zefiro.download("/OneMediaHub/Toosla/toosla.json", "test_key", new Date());

        // Then
        then(content).isNotPresent();
    }

    // --------------------------------------------------------- static methods

    public static void setUpDownloadStubs(HttpClient.Builder builder, long creationDate) {
        ((HttpClientStubber)builder).withStub(
            //
            // list root (folders)
            //
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487}]}}")
        ).withStub(
            //
            // list /OneMediaHub (folders)
            //
            "https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"Toosla\",\"id\":12345}]}}")
        ).withStub(
            //
            // list /OneMediaHub/Toosla (folders)
            //
            "https://zefiro.me/sapi/media/folder?action=list&parentid=12345&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[]}}")
        ).withStub(
            //
            // list /OneMediaHub/Toosla (files)
            //
            "https://zefiro.me/sapi/media?action=get&folderid=12345&limit=200&validationkey=test_key",
            new StubHttpResponse<String>().text(
                String.format(
                    "{\"data\":{\"media\":[{\"id\":\"11111\",\"name\":\"toosla.json\",\"creationdate\":%d}, {\"id\":\"22222\",\"name\":\"another_toosla.json\",\"creationdate\":%d}]}}",
                    creationDate, creationDate
                )
            )
        ).withStub(
            //
            // medatada for toosla.json
            //
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=test_key"),
                new BodyMatcher("{\"data\":{\"ids\":[11111],\"fields\":[\"url\",\"creationdate\"]}}")
            }),
            new StubHttpResponse<String>().text(
                String.format(
                    "{\"data\":{\"media\":[{\"id\":\"11111\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_11111\",\"creationdate\":%d}]}}",
                    creationDate
                )
            )
        ).withStub(
            //
            // medatada for another_toosla.json
            //
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=test_key"),
                new BodyMatcher("{\"data\":{\"ids\":[22222],\"fields\":[\"url\",\"creationdate\"]}}")
            }),
            new StubHttpResponse<String>().text(
                String.format(
                    "{\"data\":{\"media\":[{\"id\":\"22222\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_22222\",\"creationdate\":%d}]}}",
                    creationDate
                )
            )
        ).withStub(
            //
            // content for toosla.json
            //
            "https://zefiro.me/sapi/download/file?action=get&k=key_for_11111",
            new StubHttpResponse<String>().text("{\"content\":\"this is toosla\"}")
        ).withStub(
            //
            // content for another_toosla.json
            //
            "https://zefiro.me/sapi/download/file?action=get&k=key_for_22222",
            new StubHttpResponse<String>().text("{\"content\":\"this is another toosla\"}")
        );
    }
}