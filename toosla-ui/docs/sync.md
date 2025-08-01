# Synchronization Strategy: Optimistic Locking

This document outlines the synchronization strategy used in `TooslaStorage.js` to ensure data consistency when multiple clients are interacting with the same remote storage.

## The Challenge

When multiple clients can read and write to the same data source, we face several challenges:

1.  **Clock Skew:** Client clocks cannot be trusted. A client might have an incorrect time, leading to updates being recorded in the wrong order if we rely on client-generated timestamps.
2.  **Race Conditions:** If two clients fetch the same data and then both try to update it, one client's update could be overwritten and lost without a proper concurrency control mechanism.

## The Solution: Optimistic Locking with HTTP Headers

To solve these issues, we use a standard, robust technique called **Optimistic Locking**. This approach assumes that conflicts are rare, so we don't lock the data on the server. Instead, we let the server check for conflicts when a client tries to write.

This is implemented using standard HTTP headers, making the process efficient and compliant with web standards. The server is the single source of truth for the data's modification time.

### The Workflow

#### 1. Fetching Data (`sync` method)

-   **Goal:** Get the latest data from the server, but only if it has changed since our last sync.
-   **Mechanism:** The client sends a request to the read endpoint. If the client has a `lastModified` timestamp from a previous sync, it includes the `If-Modified-Since` header.
    ```http
    GET /api/storage#read
    If-Modified-Since: <HTTP-date representation of lastModified>
    ```
-   **Server Response:**
    -   If the data on the server has **not** changed, the server returns a `304 Not Modified` status with an empty body. The client knows it's up-to-date.
    -   If the data **has** changed, the server returns a `200 OK` status with the new data in the body and a `Last-Modified` header containing the new timestamp. The client then updates its local data and its `lastModified` value.

#### 2. Writing Data (`saveLocalStorage` method)

-   **Goal:** Save changes to the server, but only if no other client has modified the data since we last fetched it.
-   **Mechanism:** The client sends a `POST` request to the write endpoint. It includes the `If-Unmodified-Since` header with the `lastModified` timestamp it has stored.
    ```http
    POST /api/storage#write
    If-Unmodified-Since: <HTTP-date representation of lastModified>

    { "content": "..." }
    ```
-   **Server Response:**
    -   **Success:** If the `If-Unmodified-Since` timestamp matches the server's current timestamp, the server accepts the update. It saves the new data, generates a **new `Last-Modified` timestamp**, and returns `200 OK` with the new timestamp in the `Last-Modified` header. The client then updates its `lastModified` value.
    -   **Conflict:** If the timestamps do **not** match, it means another client has updated the data in the meantime. The server rejects the update and returns a `412 Precondition Failed` error.

#### 3. Handling Conflicts

-   When the client receives a `412 Precondition Failed` error, it knows its local data is out of date.
-   The client must then trigger the `sync()` process again to fetch the latest version of the data from the server.
-   After syncing, the client can then re-apply its local changes (if necessary) and attempt to save them again. This ensures that no updates are accidentally lost.

This strategy ensures data integrity and provides a reliable mechanism for handling concurrent updates from multiple clients.
