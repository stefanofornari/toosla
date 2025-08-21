# toosla-api

## Project Description

The `toosla-api` project aims to provide a backend API for a web application that functions as a cockpit for a Tesla car. This cockpit application is designed to work primarily offline, synchronizing data with the remote storage via this API when network conditions allow. This enables users to back up their local data and access it from various devices (e.g., a todo list created in the car can be viewed on a mobile phone).

## Implemented Endpoints

The following API endpoints are currently implemented:

*   **`POST /api/storage/login`**: Authenticates and authorizes a user to access the remote storage. This endpoint handles user credentials and returns a validation key for subsequent storage operations.
*   **`POST /api/storage/read`**: Reads the latest snapshot of data from the remote storage. This endpoint supports conditional retrieval using the `If-Modified-Since` header. If the data has not been modified since the provided timestamp, a `304 Not Modified` status is returned. Otherwise, the latest data is returned.
*   **`POST /api/storage/write`**: Writes a snapshot of local storage data to the remote storage.
*   **`GET /health`**: Provides a health check for the API, returning the service status and other relevant information.

## Zefiro API

### Get root folder

`
  GET https://zefiro.me/sapi/media/folder/root?action=get&validationkey=543...d20
  > {"data":{"folders":[{"name":"OneMediaHub","id":47487,"status":"N","magic":true,"offline":false,"date":1391491519230},{"name":"Download","id":105269,"status":"N","magic":false,"offline":false,"devicename":"fol-9Tbabm+r+K5smt5KB5mtiA==","date":1404913723999},{"name":"Download","id":139809,"status":"N","magic":false,"offline":false,"devicename":"fol-9Tbabm+r+K5smt5KB5mtiA==","date":1406733538488}]},"responsetime":1751126649118}
`

### Get OneMediaOne subfolders

`
  GET https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=543...d20
  > {"data":{"folders":[{"name":"Mobile and Web Uploads","id":47488,"status":"N","magic":false,"offline":true,"parentid":47487,"date":1391491519690},...]},"responsetime":1751126649266}
`
### Get the files in a folder

`
  POST https://zefiro.me/sapi/media?action=get&folderid=867202&limit=200&validationkey=543...d20
  > {"data":{"fields":["name","modificationdate","size","thumbnails","videometadata","audiometadata","favorite","shared","etag"]}}
  < {"data":{"media":[{"id":"680005393","date":1751127510918,"mediatype":"file","status":"U","userid":"ste","modificationdate":1749970147000,"size":1722,"name":"identity-prod.tgz","etag":"58IEhYENeW04HhTImidzBw==","favorite":false,"shared":false}],"more":false},"responsetime":1755185053379}
`
### Create Toosla folder

`
  POST https://zefiro.me/sapi/media/folder?action=save&validationkey=543...d20
  > {"data":{"magic":false,"offline":false,"name":"Toosla","parentid":47487}}
  < {"data":{"folder":{"name":"Toosla","id":867202,"lastupdate":1751127150132},"success":"Folder saved successfully"},"success":"Folder saved successfully","id":867202,"lastupdate":1751127150132,"responsetime":1751127150176}
`

### Upload a file

`
  POST https://upload.zefiro.me/sapi/upload?action=save&acceptasynchronous=false&validationkey=543...d20
  > multipart
  ------geckoformboundary8267430e1b10bd40b9bf906074cf7b95
  Content-Disposition: form-data; name="data"

  {"data":{"name":"test.json","size":21,"modificationdate":"20250815T174426Z","contenttype":"application/json","folderid":867202,"id":112233}}
  ------geckoformboundary8267430e1b10bd40b9bf906074cf7b95
  Content-Disposition: form-data; name="file"; filename="test.json"
  Content-Type: application/json

  {
    "text":"hello"
  }
  ------geckoformboundary8267430e1b10bd40b9bf906074cf7b95--
  > {"success":"Media uploaded successfully","id":"680245514","status":"V","etag":"J7XxRng02rtVeS3X9Wj58Q==","responsetime":1755272687861,"type":"file"}
`
Note that id in the metadata part shall be provided to override the item with the provided id

### Delete a file
  POST https://zefiro.me/sapi/media/file?action=delete&softdelete=true&validationkey=543...d20
  > {"data":{"files":[680245515]}}
  < {"success":"Files soft deleted successfully","responsetime":1755273215646}

### Download a file

`
 POST https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=543...d20
 > {"data":{"ids":[680005393],"fields":["name","creationdate","postingdate","size","thumbnails","viewurl","videometadata","exif","url","shared","exported","origin","folderid","playbackurl","transcodingstatus","etag"]}}
 < {"data":{"media":[{"id":"680005393","date":1751127510918,"mediatype":"file","status":"U","userid":"123","url":"https://zefiro.me/sapi/download/file?action=get&k=RlV...164","creationdate":1749970147000,"size":1722,"name":"identity-prod.tgz","etag":"58IEhYENeW04HhTImidzBw==","folder":867202,"shared":false,"origin":{"name":"omh"}}]},"responsetime":1751127803982}

 GET https://zefiro.me/sapi/download/file?action=get&k=RlV...eWU&node=1i164&filename=identity-prod.tgz
 <   ... binary data ...
`
