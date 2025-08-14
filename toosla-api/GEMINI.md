# Technologies

- maven
- junit
- assertj
- ste.xtest packages
- java HTTP client
- Spring Boot

# Preferences

# Project Scope and API Description

The scope of the toosla-api module is to create an API to serve a web application which implements a cockpit for a tesla car. The cockpit shows some useful tools like a calculator, a clock, a todo list, etc. The frontend of the application is designed to work primarily offline and only when needed, and the network allows, use the services delivered by the API.

The first service the API must provide is a remote storage interface so that the client can backup and share the local data. For example, the user can create a new todo in the todo widget on the car's browser. The new item will be stored locally and then synchronized to the remote storage. In this way the same user can leave the car and see the todo on the mobile phone.

This API is fairly easy:
1. /api/storage/write is the command to write a snapshot of the local storage in the remote storage
2. /api/storage/read is the command to read the latest snapshot in the remote storage
3. /api/storgae/login is the command to authenticate and authorize to the storage

Some additional important context:
- as real backend we will use an object storage called Zefiro
- the API uses If-Modified-Since, If-Unmodified-Since, Last-Modified headers to retrieve or store the most recent version of the data
