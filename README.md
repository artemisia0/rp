### Rocnikovy projekt: Monitoring zmien dat v ulozisku apache iceberg

**Developed and tested under linux but should (_hopefully_) work under windows as well.**


#### FIXME (this build & run guide is still under construction)

#### Build and run (tested on linux only, should work on windows too)
**Install java-11-openjdk-devel**

##### Install dependencies for frontend (vue.js and others)
```sh
cd frontend/
npm i  # it is assumed that npm is already installed
```

##### Building and running backend
```sh
./run.sh  # in project root dir (not in app/ cause there also is run.sh)
```

**Then go to** ```http://localhost:8080/```.

