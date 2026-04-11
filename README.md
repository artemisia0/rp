#### Install java-11-openjdk-devel
- Download it: [java](https://adoptium.net/download?link=https%3A%2F%2Fgithub.com%2Fadoptium%2Ftemurin11-binaries%2Freleases%2Fdownload%2Fjdk-11.0.29%252B7%2FOpenJDK11U-jdk_x64_linux_hotspot_11.0.29_7.tar.gz&vendor=Adoptium)
- then ```tar -xf <downloaded_archive>``` and add all files in bin/ to PATH env variable
- check if ```java -version``` and ```javac -version``` print correct version (11.x.x)
#### Install dependencies for frontend (npm should already be installed)
```sh
cd frontend/
npm i
```
#### Build and run the whole project
```sh
./run.sh  # in root directory of the project
```

#### Then go to ```http://localhost:8080/``` in a browser

Use the **Connection** tab first to choose your Iceberg catalog/warehouse/database/table.
Use **Save and initialize** there to create missing warehouse/database/table (empty table schema: `id INT, data STRING`).
Then use the **SQL Workbench** tab to create snapshots with SQL queries.

--------------------------------

### (OPTIONAL) Testing API manually
**Server endpoints are**
- ```/``` is a root page (frontend static files server by a static spring boot server)
- ```/api/snapshots``` returns a list of all available snapshots (TODO: add filtering)
- ```/api/diffs``` returns a list of all diffs (data changes) between snapshots (TODO: add filtering)
If we have ```n > 0``` snapshots that there will be ```n-1``` diffs (data/snapshot changes)

#### Testing API with ```curl```
```sh
curl http://localhost:8080/api/snapshots
```
```sh
curl http://localhost:8080/api/diffs
```
