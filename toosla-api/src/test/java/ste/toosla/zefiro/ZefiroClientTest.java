package ste.toosla.zefiro;

import java.net.http.HttpClient;
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
    public void download_file_from_subfolder() throws Exception {
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
            new StubHttpResponse<String>().text("""
                {
                   "data":{
                        "media":[
                            {
                                "id":"12345",
                                "date":1751127510918,
                                "mediatype":"file",
                                "status":"U",
                                "userid":"ste",
                                "modificationdate":1749970147000,
                                "size":1722,
                                "name":"toosla.json",
                                "etag":"58IEhYENeW04HhTImidzBw==",
                                "favorite":false,
                                "shared":false
                            },
                            {
                                "id":"67890",
                                "date":1751127510918,
                                "mediatype":"file",
                                "status":"U",
                                "userid":"ste",
                                "modificationdate":1749970147000,
                                "size":1000,
                                "name":"another_toosla.json",
                                "etag":"58IEhYENeW04HhTImidzBw==",
                                "favorite":false,
                                "shared":false
                            }
                        ],
                        "more":false
                    },
                    "responsetime":1755185053379
                }
            """)
        ).withStub(
            //
            // get /OneMesiaHub/Toosla/toosla.json file metadata
            //
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=test_key"),
                new BodyMatcher("{\"data\":{\"ids\":[12345],\"fields\":[\"url\"]}}")
            }),
            new StubHttpResponse<String>().text("{\"data\":{\"media\":[{\"id\":\"12345\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_12345\"}]}}")
        ).withStub(
            //
            // get /OneMesiaHub/Toosla/another_toosla.json file metadata
            //
            new ANDMatcher(new RequestMatcher[] {
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=test_key"),
                new BodyMatcher("{\"data\":{\"ids\":[67890],\"fields\":[\"url\"]}}")
            }),
            new StubHttpResponse<String>().text("{\"data\":{\"media\":[{\"id\":\"67890\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_67890\"}]}}")
        ).withStub(
            //
            // download content of toosla.json
            //
            "https://zefiro.me/sapi/download/file?action=get&k=key_for_12345",
            new StubHttpResponse<String>().text("{\"content\":\"this is toosla\"}")
        ).withStub(
            //
            // download content of another_toosla.json
            //
            "https://zefiro.me/sapi/download/file?action=get&k=key_for_67890",
            new StubHttpResponse<String>().text("{\"content\":\"this is another toosla\"}")
        );

        then(
            zefiro.download("/OneMediaHub/Toosla/toosla.json", "test_key")
        ).isEqualTo("{\"content\":\"this is toosla\"}");

        then(
            zefiro.download("/OneMediaHub/Toosla/another_toosla.json", "test_key")
        ).isEqualTo("{\"content\":\"this is another toosla\"}");


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