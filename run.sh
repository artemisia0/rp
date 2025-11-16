./build_frontend.sh
if [ "$?" -eq 0 ]; then
  cd app
  ./mvnw spring-boot:run
fi

