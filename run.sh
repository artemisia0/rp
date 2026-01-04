./build_frontend.sh
if [ "$?" -eq 0 ]; then
  cd app
  ./run.sh
fi

