localStorage.setItem("/OneMediaHub/folder/file.txt", "hello world");
localStorage.setItem("/AnotherRootFolder/", null);

const ROOT_PREFIX = "/OneMediaHub";

export const mocks = [
    MockServiceWorker.http.post('https://toosla.me/api/storage/:command', async ({request, params}) => {
        console.debug("Handling request ", request.url, JSON.stringify(params));

        const url = new URL(request.url);

        if (params.command === "login") {
            return await login(request);
        } else if (params.command === "read") {
            return await read(request);
        } else if (params.command === "write") {
            return await write(request);
        } else {
            return new MockServiceWorker.HttpResponse(command + " not handled", {
                status: 404,
                statusText: "unhandled"
            });
        }
    })
];

async function login(request) {
    console.debug("handling login API");
    try {
        const auth = await request.formData();
        const headers = request.headers;
        console.log("Logging with ", auth.get("credentials"));

        console.debug("request headers", ...headers);
        console.debug("auth data", auth, auth.get("credentials"));

        //
        // 1. if auth data contains "fail-XXX" fail with status code XXX
        //
        const credentials = auth.get("credentials");
        const failurePos = credentials.indexOf("fail-");
        if (failurePos < 0) {
            return MockServiceWorker.HttpResponse.json({
                "account": credentials.substring(0, credentials.indexOf(":")),
                "validationkey": "key-" + credentials
            });
        } else {
            //
            // unauthorized
            //
            return new MockServiceWorker.HttpResponse("unauthorized", {
                status: 401,
                statusText: "unauthorized"
            });
        }
    } catch (e) {
        console.error(e);
        console.error(e.stack);
        throw e;
    }
}

async function read(request) {
    console.debug("handling read API");

    try {
        const body = await request.json();
        const key = authKey(request);

        if (!key) {
            return new MockServiceWorker.HttpResponse("invalid authorization header", {
                status: 401,
                statusText: "invalid authorization header"
            });
        }

        // Get the stored item from our mock database (localStorage)
        const storedItem = localStorage.getItem(ROOT_PREFIX + body.path);
        if (!storedItem) {
            return new MockServiceWorker.HttpResponse("not found", {
                status: 404
            });
        }

        console.debug("storedItem", storedItem);

        // The stored item is a JSON string containing the content and the server's timestamp
        const storedData = JSON.parse(storedItem);
        const serverLastModified = storedData.lastModified ? Date.parse(storedData.lastModified) : null;

        // Get the client's timestamp from the header
        const clientIfModifiedSince = Date.parse(request.headers.get("If-Modified-Since"));

        // If the client's version is up-to-date, send back 304 Not Modified
        if (clientIfModifiedSince && serverLastModified && serverLastModified <= clientIfModifiedSince) {
            return new MockServiceWorker.HttpResponse("not modified", { status: 304 });
        }

        // Otherwise, send back the content and the server's timestamp in the Last-Modified header
        return MockServiceWorker.HttpResponse.json(storedData, {
            headers: {
                "Last-Modified": serverLastModified ? new Date(serverLastModified).toUTCString() : new Date().toUTCString()
            }
        });
    } catch (e) {
        console.error(e);
        console.error(e.stack);
        throw e;
    }
}

async function write(request) {
    console.debug("handling save API");
    try {
        const key = authKey(request);
        console.debug("authorization key", key);

        if (!key) {
            return new MockServiceWorker.HttpResponse("invalid authorization header", {
                status: 401,
                statusText: "invalid authorization header"
            });
        }

        const item = await request.json();
        const fullPath = ROOT_PREFIX + item.path;

        const storedItem = localStorage.getItem(fullPath);
        const storedData = storedItem ? JSON.parse(storedItem) : {};
        const serverLastModified = storedData.lastModified ? Date.parse(storedData.lastModified) : null;

        const clientIfUnmodifiedSince = Date.parse(request.headers.get("If-Unmodified-Since"));

        // Conflict: The client's version is stale.
        if (clientIfUnmodifiedSince && serverLastModified && serverLastModified > clientIfUnmodifiedSince) {
            return new MockServiceWorker.HttpResponse("the server has more recent data", { status: 412 });
        }

        // Success: Update the data and the timestamp.
        const newLastModified = new Date();
        item.content.lastModified = newLastModified.toISOString();

        localStorage.setItem(fullPath, JSON.stringify(item.content));

        dumpLocalStorage();
        return new MockServiceWorker.HttpResponse("OK", {
            status: 200,
            headers: {
                "Last-Modified": newLastModified.toISOString()
            }
        });
    } catch (e) {
        console.error(e);
        console.error(e.stack);
        throw e;
    }
}

function authKey(request) {
    const key = request.headers.get("authorization");

    if (key && key.startsWith("token ")) {
        return key.substring(6);
    }

    return null;
}

function dumpLocalStorage() {
    console.log("local storage");
    console.log("-------------");
    for (let i = 0; i < localStorage.length; i++)   {
        const key = localStorage.key(i);
        console.log(key + "=[" + localStorage.getItem(key) + "]");
    }
    console.log("-------------");
}