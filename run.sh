./build_frontend.sh
if [ $? -eq 0 ]; then
  cd app
  export JAVA_HOME=/usr/lib/jvm/jdk-11.0.29+7/
  export PATH=$JAVA_HOME/bin:$PATH
  mvn spring-boot:run
fi

