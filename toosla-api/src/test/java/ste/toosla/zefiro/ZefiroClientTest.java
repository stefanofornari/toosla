package ste.toosla.zefiro;

import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

    public static final String TEST_ACCOUNT = "test_user";
    public static final String TEST_SECRET = "test_password";
    public static final String TEST_BASIC_CREDENTIALS = Base64.getEncoder().encodeToString(
        (TEST_ACCOUNT + ":" + TEST_SECRET).getBytes()
    );

    public static final String TEST_VALIDATION_KEY = "test_validation_key";
    public static final String TEST_ACCESS_KEY = "test_access_key";

    private HttpClientStubber httpClientBuilder;

    @BeforeEach
    public void beforeEach() {
        httpClientBuilder = new HttpClientStubber();
        httpClientBuilder.stubs().clear();
        httpClientBuilder.withStub("https://zefiro.me/sapi/login?action=login",
            new StubHttpResponse<String>().text("{\"data\":{\"validationkey\":\"" + TEST_VALIDATION_KEY + "\"}}")
        );
    }

    //
    // ---------- login ----------
    //

    @Test
    public void login_successful() throws Exception {
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password").withHttpClientBuilder(httpClientBuilder);

        // When
        ZefiroLoginResponse response = zefiro.login();

        // Then
        then(response.account()).isEqualTo("test_user");
        then(response.key()).isEqualTo("test_validation_key");
    }

    @Test
    public void can_override_api_url_for_login() throws Exception {
        // Given
        final String testUrl = "https://my-test-host.com";
        httpClientBuilder.stubs().clear(); // Remove default stubs
        httpClientBuilder.withStub(
            testUrl + "/sapi/login?action=login",
            new StubHttpResponse<String>().text("{\"data\":{\"validationkey\":\"a_different_key\"}}")
        );

        ZefiroClient zefiro = new ZefiroClient("test_user", "test_password")
            .withHttpClientBuilder(httpClientBuilder)
            .withApiUrl(testUrl);

        // When
        ZefiroLoginResponse response = zefiro.login();

        // Then
        then(response.key()).isEqualTo("a_different_key");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t", "\n"})
    public void withApiUrl_should_throw_exception_for_invalid_url(String invalidUrl) {
        // Given
        ZefiroClient zefiro = new ZefiroClient("user", "pass");

        // When & Then
        thenThrownBy(() -> zefiro.withApiUrl(invalidUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("apiUrl can not be null or empty");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t" })
    public void withUploadUrl_should_throw_exception_for_invalid_url(String invalidUrl) {
        // Given
        ZefiroClient zefiro = new ZefiroClient("user", "pass");

        // When & Then
        thenThrownBy(() -> zefiro.withUploadUrl(invalidUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("uploadUrl can not be null or empty");
    }

    @Test
    public void login_connection_error() {
        httpClientBuilder.stubs().clear(); // beforeEach adds a "valid login" stub
        // Given
        httpClientBuilder.withStub(
            "https://zefiro.me/sapi/login?action=login",
            new ste.xtest.net.http.StubHttpClient.NetworkError()
        );

        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password").withHttpClientBuilder(httpClientBuilder);

        // When & Then
        thenThrownBy(() -> zefiro.login())
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
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password").withHttpClientBuilder(httpClientBuilder);
        thenThrownBy(() -> zefiro.login())
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
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password").withHttpClientBuilder(httpClientBuilder);
        thenThrownBy(() -> zefiro.login())
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
        ZefiroClient zefiro =
            new ZefiroClient("wrong_user", "wrong_password").withHttpClientBuilder(httpClientBuilder);
        thenThrownBy(() -> zefiro.login())
                .isInstanceOf(ZefiroLoginException.class);
    }

    //
    // ---------- upload ----------
    //

    @Test
    public void upload_without_if_modified_since() throws Exception {
        // Given
        ZefiroClient zefiro = newZefiroClient();
        setUpFileStubs(httpClientBuilder).withStub(
            "https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY,
            new StubHttpResponse<String>().text("{\"success\":\"Media uploaded successfully\",\"id\":\"12345\",\"status\":\"V\",\"etag\":\"J7XxRng02rtVeS3X9Wj58Q==\",\"responsetime\":1755272687861,\"type\":\"file\"}")
        );

        String path = "/Toosla/new_file.json";
        String content = "{\"key\":\"value\"}";

        // When
        ZefiroUploadResponse response = zefiro.upload(path, content);

        // Then
        then(response.id()).isEqualTo("12345");
    }

    @Test
    public void do_not_upload_file_is_more_recent_than_if_unmodified_since() throws Exception {
        // Given
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password")
            .withHttpClientBuilder(httpClientBuilder)
            .withValidationKey(TEST_VALIDATION_KEY);
        String path = "/Toosla/toosla.json";
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
        ZefiroClient zefiro = newZefiroClient();
        String path = "/Toosla/toosla.json";
        String content = "{\"key\":\"updated_value\"}";

        // Stub Zefiro responses:
        // Use setUpFileStubs to set up the tree and file listing stubs.
        // The creationDate passed to setUpFileStubs will be the "current" modification date on Zefiro.
        setUpFileStubs(httpClientBuilder).withStub(new ANDMatcher(
                new URIMatcher("https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY),
                new BodyMatcher(
                    Pattern.compile("--------zfrclient\\d+.*" + Pattern.quote(content) + ".*", Pattern.DOTALL)
                ),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().text("{\"success\":\"Media uploaded successfully\",\"id\":\"11111\",\"status\":\"V\",\"etag\":\"J7XxRng02rtVeS3X9Wj58Q==\",\"responsetime\":1755272687861,\"type\":\"file\"}")
        );

        // When
        final Date NOW = new Date();
        ZefiroUploadResponse response = zefiro.upload(path, content, FIXED_LATER_DATE);

        // Then
        then(response.id()).isEqualTo("11111");
        then(response.lastModified()).isAfter(NOW);
    }

    @Test
    public void upload_new_file_does_not_send_id() throws Exception {
        // Given
        ZefiroClient zefiro = zefiro = newZefiroClient();

        final String path = "/Toosla/new_file.json";
        final String content = "{\"key\":\"new_value\"}";

        setUpFileStubs(httpClientBuilder).withStub(// this does not contain /Toosla/new_file.json
            new ANDMatcher(
                new URIMatcher("https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY),
                new BodyMatcher(Pattern.compile("--------zfrclient\\d+.*" + Pattern.quote(content) + ".*", Pattern.DOTALL))
            ),
            new StubHttpResponse<String>().text("{\"success\":\"Media uploaded successfully\",\"id\":\"67890\"}")
        );

        // When
        ZefiroUploadResponse response = zefiro.upload(path, content, FIXED_LATER_DATE);

        // Then
        then(response.id()).isEqualTo("67890");
    }

    @Test
    public void upload_existing_file_sends_id() throws Exception {
        ZefiroClient zefiro = zefiro = newZefiroClient();

        final String path = "/Toosla/toosla.json";
        final String contentPattern = "{\"key\":\"value\"}",
                     idPattern = "\"id\":11111";

        setUpFileStubs(httpClientBuilder).withStub(// this does not contain /Toosla/toosla.json
            new ANDMatcher(
                new URIMatcher("https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY),
                new BodyMatcher(Pattern.compile("--------zfrclient\\d+.*" + Pattern.quote(idPattern) + ".*--------zfrclient\\d+.*", Pattern.DOTALL)),
                new BodyMatcher(Pattern.compile("--------zfrclient\\d+.*" + Pattern.quote(contentPattern) + ".*", Pattern.DOTALL))
            ),
            new StubHttpResponse<String>().text("{\"success\":\"Media uploaded successfully\",\"id\":\"67890\"}")
        );

        //
        // Upload without if-unmodified-since
        //
        ZefiroUploadResponse response = zefiro.upload(path, contentPattern);
        then(response.id()).isEqualTo("67890");

        //
        // Upload with if-unmodified-since
        //
        response = zefiro.upload(path, contentPattern, FIXED_LATER_DATE);
        then(response.id()).isEqualTo("67890");
    }

    @Test
    public void can_override_upload_url() throws Exception {
        // Given
        final String testUploadUrl = "https://my-test-upload.com";

        // Client uses default apiUrl but a custom uploadUrl
        ZefiroClient zefiro = newZefiroClient()
            .withUploadUrl(testUploadUrl);

        // Use existing stubs for helpers, which point to the default API URL
        setUpFileStubs(httpClientBuilder);
        // Add a new stub for our custom upload URL
        httpClientBuilder.withStub(
            new URIMatcher(testUploadUrl + "/sapi/upload?action=save&acceptasynchronous=false&validationkey=" + TEST_VALIDATION_KEY),
            new StubHttpResponse<String>().text("{\"id\":\"a_different_id\"}")
        );

        // When
        ZefiroUploadResponse response = zefiro.upload("/Toosla/new_file.json", "{\"key\":\"value\"}");

        // Then
        then(response.id()).isEqualTo("a_different_id");
    }

    //
    // ---------- download ----------
    //

    @Test
    public void download_file_from_subfolder() throws Exception {
        ZefiroClient zefiro = zefiro = newZefiroClient();
        setUpFileStubs(httpClientBuilder);

        then(
            zefiro.download("/Toosla/toosla.json")
        ).isEqualTo("{\"content\":\"this is toosla\"}");
    }

    @Test
    public void download_connection_error() {
        // Given
        ZefiroClient zefiro = newZefiroClient();
        httpClientBuilder.withStub("https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + TEST_VALIDATION_KEY,
            new ste.xtest.net.http.StubHttpClient.NetworkError()
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/Toosla/toosla.json"))
                .isInstanceOf(ZefiroException.class)
                .hasMessage("Error connecting to Zefiro");
    }

    @Test
    public void download_invalid_json() {
        // Given
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password")
            .withHttpClientBuilder(httpClientBuilder)
            .withValidationKey(TEST_VALIDATION_KEY);
        httpClientBuilder.withStub("https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + TEST_VALIDATION_KEY,
            new ste.xtest.net.http.StubHttpClient.StubHttpResponse<String>().text("this is not json")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/Toosla/toosla.json"))
            .isInstanceOf(ZefiroException.class)
            .hasMessage("Invalid JSON response from Zefiro");
    }

    @Test
    public void download_file_not_found() throws Exception {
        // Given
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password")
            .withHttpClientBuilder(httpClientBuilder)
            .withValidationKey(TEST_VALIDATION_KEY);
        setUpTreeStubs(httpClientBuilder).withStub(//
            // list /OneMesiaHub/Toosla/ (files)
            //
            "https://zefiro.me/sapi/media?action=get&folderid=12345&limit=200&validationkey=" + TEST_VALIDATION_KEY,
            new StubHttpResponse<String>().text("{\"data\":{\"media\":[]}}")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/Toosla/not_found.json"))
            .isInstanceOf(ZefiroFileNotFoundException.class)
            .hasMessage("File not found: /Toosla/not_found.json");
    }

    @Test
    public void download_subdirectory_not_found() throws Exception {
        // Given
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password")
            .withHttpClientBuilder(httpClientBuilder)
            .withValidationKey(TEST_VALIDATION_KEY);
        httpClientBuilder.withStub(//
            // list root folder
            //
            "https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + TEST_VALIDATION_KEY,
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487,\"status\":\"N\",\"magic\":true,\"offline\":false,\"date\":1391491519230}]}}")
        ).withStub(//
            // list /OneMediaHub (folders) - does not contain "NonExistentSubdir"
            //
            "https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=" + TEST_VALIDATION_KEY,
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"AnotherSubdir\",\"id\":67890,\"status\":\"N\",\"magic\":false,\"offline\":false,\"date\":456}]}}")
        );

        // When & Then
        thenThrownBy(() -> zefiro.download("/NonExistentSubdir/file.json"))
            .isInstanceOf(ZefiroFileNotFoundException.class)
            .hasMessage("File not found: /NonExistentSubdir/file.json");
    }

    @Test
    public void download_if_file_is_more_recent_than_if_modified_since() throws Exception {
        // Given
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password")
        .withHttpClientBuilder(httpClientBuilder)
        .withValidationKey(TEST_VALIDATION_KEY);
        setUpFileStubs(httpClientBuilder);

        // When
        Optional<ZefiroClient.DownloadResult> result = zefiro.download("/Toosla/toosla.json", FIXED_EARLIER_DATE);

        // Then
        then(result).isPresent();
        then(result.get().content()).isEqualTo("{\"content\":\"this is toosla\"}");
        then(result.get().lastModified()).isEqualTo(FIXED_MODIFICATION_DATE);
    }

    @Test
    public void do_not_download_if_file_is_not_more_recent_than_if_modified_since() throws Exception {
        // Given
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password")
            .withHttpClientBuilder(httpClientBuilder)
            .withValidationKey(TEST_VALIDATION_KEY);
        setUpFileStubs(httpClientBuilder);

        // When
        Optional<ZefiroClient.DownloadResult> result = zefiro.download("/Toosla/toosla.json", FIXED_LATER_DATE);

        // Then
        then(result).isNotPresent();
    }

    @Test
    public void download_uses_basic_authentication_after_login() throws Exception {
        // Given
        ZefiroClient zefiro =
            new ZefiroClient("test_user", "test_password")
            .withHttpClientBuilder(httpClientBuilder)
            .withValidationKey(TEST_VALIDATION_KEY);
        setUpFileStubs(httpClientBuilder).withStub(new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().text(
                "{\"data\":{\"media\":[{\"id\":\"11111\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_11111\",\"modificationdate\":1755561600000}]}}"
            )
        );

        // When
        zefiro.download("/Toosla/toosla.json");

        // Then
        // The test passes if the stub is matched, which means the Authorization header was sent.
    }

    // ---------------------------------------------------------- static methods

    public static HttpClientStubber setUpTreeStubs(HttpClientStubber builder) {
        builder.withStub(//
            // list root (folders)
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media/folder/root?action=get&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"OneMediaHub\",\"id\":47487}]}}")
        ).withStub(//
            // list /OneMediaHub (folders)
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().text("{\"data\":{\"folders\":[{\"name\":\"Toosla\",\"id\":12345}]}}")
        ).withStub(//
            // list /OneMediaHub/Toosla (folders)
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media/folder?action=list&parentid=12345&limit=200&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
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
                new URIMatcher("https://zefiro.me/sapi/media?action=get&folderid=12345&limit=200&validationkey=" + TEST_VALIDATION_KEY),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().text(
                "{\"data\":{\"media\":[{\"id\":\"11111\",\"name\":\"toosla.json\",\"modificationdate\":1755561600000}, {\"id\":\"22222\",\"name\":\"another_toosla.json\",\"modificationdate\":1755561600000}]}}"
            )
        ).withStub(
            //
            // medatada for toosla.json
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + TEST_VALIDATION_KEY),
                new BodyMatcher("{\"data\":{\"ids\":[11111],\"fields\":[\"url\",\"modificationdate\"]}}"),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().text(
                "{\"data\":{\"media\":[{\"id\":\"11111\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_11111\",\"modificationdate\":1755561600000}]}}"
            )
        ).withStub(
            //
            // medatada for another_toosla.json
            //
            new ANDMatcher(
                new URIMatcher("https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=" + TEST_VALIDATION_KEY),
                new BodyMatcher("{\"data\":{\"ids\":[22222],\"fields\":[\"url\",\"modificationdate\"]}}"),
                new HeaderMatcher("Authorization", "Basic " + TEST_BASIC_CREDENTIALS)
            ),
            new StubHttpResponse<String>().text(
                "{\"data\":{\"media\":[{\"id\":\"22222\",\"url\":\"https://zefiro.me/sapi/download/file?action=get&k=key_for_22222\",\"modificationdate\":1755561600000}]}}"
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

    // --------------------------------------------------------- private methods

    private ZefiroClient newZefiroClient() {
        return new ZefiroClient("test_user", "test_password")
            .withHttpClientBuilder(httpClientBuilder)
            .withValidationKey(TEST_VALIDATION_KEY);
    }
}