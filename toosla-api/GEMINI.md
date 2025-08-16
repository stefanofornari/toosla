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

# ZefiroClient.load Requirements

- The API provided by Zefiro to read a file is POST https://zefiro.me/sapi/media?action=get&origin=omh,dropbox&validationkey=543...d20
  - input payload: {"data":{"ids":[680005393],"fields":["name","creationdate","postingdate","size","thumbnails","viewurl","videometadata","exif","url","shared","exported","origin","folderid","playbackurl","transcodingstatus","etag"]}} - the important piece of information is ids, which specify the id of the items to download; in our case we will always download one file only
  - ouput payload: {"data":{"media":[{"id":"680005393","date":1751127510918,"mediatype":"file","status":"U","userid":"123","url":"https://zefiro.me/sapi/download/file?action=get&k=RlV...164","creationdate":1749970147000,"size":1722,"name":"identity-prod.tgz","etag":"58IEhYENeW04HhTImidzBw==","folder":867202,"shared":false,"origin":{"name":"omh"}}]},"responsetime":1751127803982} - here the important pice of information is "url" which provides the URL from which the file can be downloaded
- To get the item id we need to list the content of its parent folder; the API to list the content of a folder is GET https://zefiro.me/sapi/media/folder?action=list&parentid=47487&limit=200&validationkey=543...d20
  - output payload: {"data":{"folders":[{"name":"Mobile and Web Uploads","id":47488,"status":"N","magic":false,"offline":true,"parentid":47487,"date":1391491519690},...]},"responsetime":1751126649266} - here what we are looking for is in the data/folders field
- To list the content of the root folder we can use the API GET https://zefiro.me/sapi/media/folder/root?action=get&validationkey=543...d20
  - output payload: {"data":{"folders":[{"name":"OneMediaHub","id":47487,"status":"N","magic":true,"offline":false,"date":1391491519230},{"name":"Download","id":105269,"status":"N","magic":false,"offline":false,"devicename":"fol-9Tbabm+r+K5smt5KB5mtiA==","date":1404913723999},{"name":"Download","id":139809,"status":"N","magic":false,"offline":false,"devicename":"fol-9Tbabm+r+K5smt5KB5mtiA==","date":1406733538488}]},"responsetime":1751126649118}
- To retrieve the files inside a directory use the API POST https://zefiro.me/sapi/media?action=get&folderid=867202&limit=200&validationkey=543...d20
  - input payload {"data":{"fields":["name","modificationdate","size","thumbnails","videometadata","audiometadata","favorite","shared","etag"]}}
  - output payload: {"data":{"media":[{"id":"680005393","date":1751127510918,"mediatype":"file","status":"U","userid":"ste","modificationdate":1749970147000,"size":1722,"name":"identity-prod.tgz","etag":"58IEhYENeW04HhTImidzBw==","favorite":false,"shared":false}],"more":false},"responsetime":1755185053379}
- We want to implement a download method that given a path like /OneMediaHub/Toosla/toosla.jason navigates throught the folders to identify the id of the file, downloads the file and returns its content as it is
- We can assume the file is a JSON file
- An error is returned if the path does not exist
- Each call to zefiro API shall provide the validationKey as returned from the login method
- We follow the tdd workflow

# ZefiroClient.download with If-Modified-Since

- we shall have a download() overridden method that accepts a Date that represent the "modified since" timestamp
- we gather the creation date of the item on zefiro side from /data/creationdate when requesting the metadata of the file (ie. when we ask for the url); this timestamp is a unix timestap and we can take it as last modification timestamp on zefiro
- if the item's creation timestamp is more recent than the provided "modified since" timestamp, we download and return the content
- if the item's creation timestamp is not more recent, we return a 304 up to date status