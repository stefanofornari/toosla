localStorage.setItem("/OneMediaHub/folder/file.txt", "hello world");
localStorage.setItem("/AnotherRootFolder/", null);

const ROOT_PREFIX = "/OneMediaHub";

export const mocks = [
    MockServiceWorker.http.post('https://toosla.me/api/storage', async ({request}) => {
        console.debug("Handling request ", request.url);

        const url = new URL(request.url);

        if (url.hash === "#login") {
            return await login(request);
        } else if (url.hash === "#read") {
            return await read(request);
        } else if (url.hash === "#write") {
            return await write(request);
        } else {
            return new MockServiceWorker.HttpResponse(url.hash + " not handled", {
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
        console.debug("body", JSON.stringify(body));

        const key = authKey(request);
        console.debug("authorization key", key);

        if (!key) {
            return new MockServiceWorker.HttpResponse("invalid authorization header", {
                status: 401,
                statusText: "invalid authorization header"
            });
        }

        const content = localStorage.getItem(ROOT_PREFIX + body.path);
        console.debug("content", content, "-", JSON.parse(content));
        if (content) {
            return MockServiceWorker.HttpResponse.json(JSON.parse(content));
        }

        return new MockServiceWorker.HttpResponse("not found", {
            status: 404,
            statusText: "not found"
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
        const url = new URL(request.url);
        const queryParams = url.searchParams;

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
        console.debug("fullPath", fullPath);
        localStorage.setItem(fullPath, JSON.stringify(item.content));

        dumpLocalStorage();
        return new MockServiceWorker.HttpResponse("error", {
            status: 200,
            statusText: item.path
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