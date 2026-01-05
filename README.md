## Rocnikovy projekt: Monitoring zmien dat v ulozisku apache iceberg

### Build and run (linux only for now but may work on other OSs as well)
#### Install java-11-openjdk-devel
- Download it: (https://adoptium.net/download?link=https%3A%2F%2Fgithub.com%2Fadoptium%2Ftemurin11-binaries%2Freleases%2Fdownload%2Fjdk-11.0.29%252B7%2FOpenJDK11U-jdk_x64_linux_hotspot_11.0.29_7.tar.gz&vendor=Adoptium)
- then ```tar -xf <downloaded_archive>``` and add all files in bin/ to PATH env variable
- check if ```java -version``` and ```javac -version``` print correct version (11.x.x)
#### Install dependencies for frontend (npm should be installed already)
```sh
cd frontend/
npm i
```
#### Build and run the whole project
```sh
./run.sh  # in root directory of the project (there's run.sh in app/ also)
```
#### Then go to ```http://localhost:8080/``` in a browser
### Populate iceberg with sample data
- download spark: (https://www.apache.org/dyn/closer.lua/spark/spark-3.5.7/spark-3.5.7-bin-hadoop3-scala2.13.tgz) (first link at the top)
- then ```tar -xf <downloaded_archive>``` (make sure that the result is in ~/Downloads/)
- then see ```spark.sh``` script in ```app/``` to see how to run spark (or you may edit that script as you like and then run it). this script performs some operations that is creates new snapshots so that we will see new diffs

