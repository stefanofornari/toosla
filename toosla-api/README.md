# toosla-api

## Project Description

The `toosla-api` project aims to provide a backend API for a web application that functions as a cockpit for a Tesla car. This cockpit application is designed to work primarily offline, synchronizing data with the remote storage via this API when network conditions allow. This enables users to back up their local data and access it from various devices (e.g., a todo list created in the car can be viewed on a mobile phone).

## Implemented Endpoints

The following API endpoints are currently implemented:

*   **`POST /api/storage/login`**: Authenticates and authorizes a user to access the remote storage. This endpoint handles user credentials and returns a validation key for subsequent storage operations.

*   **`GET /health`**: Provides a health check for the API, returning the service status and other relevant information.

## Planned Endpoints

The following endpoints are planned for future implementation:

*   **`POST /api/storage/write`**: Used to write a snapshot of local storage data to the remote storage.
*   **`GET /api/storage/read`**: Used to read the latest snapshot of data from the remote storage.