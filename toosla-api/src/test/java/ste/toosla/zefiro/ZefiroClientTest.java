package ste.toosla.zefiro;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ste.xtest.net.http.HttpClientStubber;
import ste.xtest.net.http.StubHttpClient.StubHttpResponse;
import ste.xtest.net.http.ANDMatcher;
import ste.xtest.net.http.URIMatcher;
import ste.xtest.net.http.BodyMatcher;
import ste.xtest.net.http.HeaderMatcher;


/**
 *
 *
 */
public class ZefiroClientTest {

    public static final Date FIXED_MODIFICATION_DATE = new Date(1755561600000L); // Aug 19 2025 00:00:00 GMT
    public static final Date FIXED_EARLIER_DATE = new Date(1672531200000L); // Jan 1, 2023, 00:00:00 GMT
    public static final Date FIXED_LATER_DATE = new Date(1767225600000L); //  Jan 01 2026 00:00:00 GMT

    public static final String VALIDATION_KEY = "test_key";

    private HttpClientStubber httpClientBuilder;
    private ZefiroClient zefiro;

    @BeforeEach
    public void beforeEach() {
        httpClientBuilder = new HttpClientStubber();
        zefiro = new ZefiroClient(httpClientBuilder);
        httpClientBuilder.stubs().clear();
        httpClientBuilder.withStub(
            "https://zefiro.me/sapi/login?action=login",
            new StubHttpResponse<String>().text("{\"data\":{\"validationkey\":\"" + VALIDATION_KEY + "\"}}")
        );
    }

    //
    // ---------- login ----------
    //

    @Test
    public void login_successful() throws Exception {
        // Given
        httpClientBuilder.withStub(
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
        httpClientBuilder.stubs().clear(); // beforeEach adds a "valid login" stub
        // Given
        httpClientBuilder.withStub(
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
        httpClientBuilder.stubs().clear(); // beforeEach adds a "valid login" stub
        // Given
        httpClientBuilder.withStub(
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
        httpClientBuilder.stubs().clear(); // beforeEach adds a "valid login" stub
        // Given
        httpClientBuilder.withStub(
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
        httpClientBuilder.stubs().clear(); // beforeEach adds a "valid login" stub
        // Given
        httpClientBuilder.withStub(
            "https://zefiro.me/sapi/login?action=login",
            new StubHttpResponse<String>().statusCode(401)
        );

        // When & Then
        thenThrownBy(() -> zefiro.login("wrong_user", "wrong_password"))
                .isInstanceOf(ZefiroLoginException.class);
    }

    //
    // ---------- upload ----------
    //

    @Test
    public void upload__without_if_modified_since() throws Exception {
        // Given
        zefiro.login("test_user", "test_password");
        setUpTreeStubs(httpClientBuilder).withStub(
            "https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=true&validationkey=" + VALIDATION_KEY,
            new StubHttpResponse<String>().text("{\"success\":\"Media uploaded successfully\",\"id\":\"12345\",\"status\":\"V\",\"etag\":\"J7XxRng02rtVeS3X9Wj58Q==\",\"responsetime\":1755272687861,\"type\":\"file\"}")
        );

        String path = "/OneMediaHub/Toosla/new_file.json";
        String content = "{\"key\":\"value\"}";

        // When
        String uploadedFileId = zefiro.upload(path, content);

        // Then
        then(uploadedFileId).isEqualTo("12345");
    }

    @Test
    public void do_not_upload_file_is_more_recent_than_if_unmodified_since() throws Exception {
        // Given
        zefiro.login("test_user", "test_password");
        String path = "/OneMediaHub/Toosla/toosla.json";
        String content = "{\"key\":\"new_value\"}";

        // Stub Zefiro responses:
        // Use setUpFileStubs to set up the tree and file listing stubs
        // The creationDate passed to setUpFileStubs will be the "current" modification date on Zefiro
        setUpFileStubs(httpClientBuilder);

        // When & Then
        thenThrownBy(() -> zefiro.upload(path, content, FIXED_EARLIER_DATE))
            .isInstanceOf(ZefiroModificationException.class)
            .hasMessageContaining("Modification conflict detected")
            .extracting(e -> ((ZefiroModificationException) e).lastModified.get().getTime())
            .isEqualTo(FIXED_MODIFICATION_DATE.getTime());
    }

    @Test
    public void upload_if_file_is_not_more_recent_than_if_unmodified_since() throws Exception {
        // Given
        zefiro.login("test_user", "test_password");
        String path = "/OneMediaHub/Toosla/toosla.json";
        String content = "{\"key\":\"updated_value\"}";

        // Stub Zefiro responses:
        // Use setUpFileStubs to set up the tree and file listing stubs.
        // The creationDate passed to setUpFileStubs will be the "current" modification date on Zefiro.
        setUpFileStubs(httpClientBuilder).withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media/file?action=delete&softdelete=true&validationkey=" + VALIDATION_KEY),
                new BodyMatcher("{\"data\":{\"files\":[11111]}}"),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text("{\"success\":\"Media deleted successfully\"}")
        ).withStub(
            new ANDMatcher(
                new URIMatcher("https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=true&validationkey=" + VALIDATION_KEY),
                new BodyMatcher(Pattern.compile(
                    "\\A(------WebKitFormBoundary\\d+)\\r\\n" +
                    "Content-Disposition: form-data; name=\"data\"\\r\\n" +
                    "Content-Type: application/json\\r\\n" +
                    "\\r\\n" +
                    Pattern.quote("{\"data\":{\"name\":\"toosla.json\",\"size\":23,\"modificationdate\":1767225600000,\"contenttype\":\"application/json\",\"folderid\":12345}}") + "\\r\\n" +
                    "\\1\\r\\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"toosla\\.json\"\\r\\n" +
                    "Content-Type: application/json\\r\\n" +
                    "\\r\\n" +
                    Pattern.quote("{\"key\":\"updated_value\"}") + "\\r\\n" +
                    "\\1--\\r\\n\\z"
                )),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text("{\"success\":\"Media uploaded successfully\",\"id\":\"12345\",\"status\":\"V\",\"etag\":\"J7XxRng02rtVeS3X9Wj58Q==\",\"responsetime\":1755272687861,\"type\":\"file\"}")
        );

        // When
        String uploadedFileId = zefiro.upload(path, content, FIXED_LATER_DATE);

        // Then
        then(uploadedFileId).isEqualTo("12345");
    }

    //
    // ---------- download ----------
    //

    @Test
    public void download_file_from_subfolder() throws Exception {
        zefiro.login("test_user", "test_password");
        setUpFileStubs(httpClientBuilder);

        then(
            zefiro.download("/OneMediaHub/Toosla/toosla.json")
        ).isEqualTo("{\"content\":\"this is toosla\"}");
    }

    @Test
    public void download_connection_error() {
        // Given
        zefiro.login("test_user", "test_password");
        httpClientBuilder.withStub(
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + VALIDATION_KEY,
            new ste.xtest.net.http.StubHttpClient.NetworkError()
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/OneMediaHub/Toosla/toosla.json"))
                .isInstanceOf(ZefiroException.class)
                .hasMessage("Error connecting to Zefiro");
    }

    @Test
    public void download_invalid_json() {
        // Given
        zefiro.login("test_user", "test_password");
        httpClientBuilder.withStub(
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + VALIDATION_KEY,
            new ste.xtest.net.http.StubHttpClient.StubHttpResponse<String>().text("this is not json")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/OneMediaHub/Toosla/toosla.json"))
            .isInstanceOf(ZefiroException.class)
            .hasMessage("Invalid JSON response from Zefiro");
    }

    @Test
    public void download_file_not_found() throws Exception {
        // Given
        zefiro.login("test_user", "test_password");
        setUpTreeStubs(httpClientBuilder).withStub(
            //
            // list /OneMesiaHub/Toosla/ (files)
            //
            "https://zefiro.me/sapi/media?action=get&folderid=12345&limit=200&validationkey=" + VALIDATION_KEY,
            new StubHttpResponse<String>().text("{\"data\":{\"media\":[]}}")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/OneMediaHub/Toosla/not_found.json"))
            .isInstanceOf(ZefiroFileNotFoundException.class)
            .hasMessage("File not found: /OneMediaHub/Toosla/not_found.json");
    }

    @Test
    public void download_subdirectory_not_found() throws Exception {
        // Given
        zefiro.login("test_user", "test_password");
        httpClientBuilder.withStub(
            //
            // list root folder
            //
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + VALIDATION_KEY,
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487,\"status\":\"N\",\"magic\":true,\"offline\":false,\"date\":1391491519230}]}}")
        ).withStub(
            //
            // list /OneMediaHub (folders) - does not contain "NonExistentSubdir"
            //
            "https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=" + VALIDATION_KEY,
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"AnotherSubdir\",\"id\":67890,\"status\":\"N\",\"magic\":false,\"offline\":false,\"date\":456}]}}")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/OneMediaHub/NonExistentSubdir/file.json"))
            .isInstanceOf(ZefiroFileNotFoundException.class)
            .hasMessage("File not found: /OneMediaHub/NonExistentSubdir/file.json");
    }

    @Test
    public void download_if_file_is_more_recent_than_if_modified_since() throws Exception {
        // Given
        zefiro.login("test_user", "test_password");
        setUpFileStubs(httpClientBuilder);

        // When
        Optional<String> content = zefiro.download("/OneMediaHub/Toosla/toosla.json", FIXED_EARLIER_DATE);

        // Then
        then(content).isPresent().contains("{\"content\":\"this is toosla\"}");
    }

    @Test
    public void do_not_download_if_file_is_not_more_recent_than_if_modified_since() throws Exception {
        // Given
        zefiro.login("test_user", "test_password");
        setUpFileStubs(httpClientBuilder);

        // When
        Optional<String> content = zefiro.download("/OneMediaHub/Toosla/toosla.json", FIXED_LATER_DATE);

        // Then
        then(content).isNotPresent();
    }

    @Test
    public void download_uses_basic_authentication_after_login() throws Exception {
        // Given
        zefiro.login("test_user", "test_password");
        setUpFileStubs(httpClientBuilder).withStub(
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text(
                "{\"data\":{\"media\":[{\"id\":\"11111\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_11111\",\"creationdate\":1755561600000}]}}"
            )
        );

        // When
        zefiro.download("/OneMediaHub/Toosla/toosla.json");

        // Then
        // The test passes if the stub is matched, which means the Authorization header was sent.
    }

    // --------------------------------------------------------- static methods

    public static HttpClientStubber setUpTreeStubs(HttpClientStubber builder) {
        builder.withStub(
            //
            // list root (folders)
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487}]}}")
        ).withStub(
            //
            // list /OneMediaHub (folders)
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=" + VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"Toosla\",\"id\":12345}]}}")
        ).withStub(
            //
            // list /OneMediaHub/Toosla (folders)
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media/folder?action=list&parentid=12345&limit=200&validationkey=" + VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[]}}")
        );

        return builder;
    }

    public static HttpClientStubber setUpFileStubs(HttpClientStubber builder) {
        setUpTreeStubs(builder).withStub(
            //
            // list /OneMediaHub/Toosla (files)
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media?action=get&folderid=12345&limit=200&validationkey=" + VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text(
                "{\"data\":{\"media\":[{\"id\":\"11111\",\"name\":\"toosla.json\",\"creationdate\":1755561600000}, {\"id\":\"22222\",\"name\":\"another_toosla.json\",\"creationdate\":1755561600000}]}}"
            )
        ).withStub(
            //
            // medatada for toosla.json
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + VALIDATION_KEY),
                new BodyMatcher("{\"data\":{\"ids\":[11111],\"fields\":[\"url\",\"creationdate\"]}}"),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text(
                "{\"data\":{\"media\":[{\"id\":\"11111\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_11111\",\"creationdate\":1755561600000}]}}"
            )
        ).withStub(
            //
            // medatada for another_toosla.json
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + VALIDATION_KEY),
                new BodyMatcher("{\"data\":{\"ids\":[22222],\"fields\":[\"url\",\"creationdate\"]}}"),
                new HeaderMatcher("Authorization", "Basic dGVzdF91c2VyOnRlc3RfcGFzc3dvcmQ=")
            ),
            new StubHttpResponse<String>().text(
                "{\"data\":{\"media\":[{\"id\":\"22222\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_22222\",\"creationdate\":1755561600000}]}}"
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

        return builder;
    }
}