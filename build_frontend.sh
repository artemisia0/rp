#!/usr/bin/env bash

# Colors
RED='\033[31m'
GREEN='\033[32m'
NC='\033[0m'   # No Color

# Helper to run a command and check status
run() {
    desc="$1"
    shift

    echo -e "▶ ${desc}..."
    "$@"
    status=$?

    if [ $status -ne 0 ]; then
        echo -e "❌ ${RED}${desc} FAILED (exit $status)${NC}"
        exit $status
    else
        echo -e "✔ ${GREEN}${desc} SUCCESS${NC}"
    fi
}

# --- Operations ---

run "Entering frontend directory" cd frontend
run "Building frontend" npm run build
run "Returning to root directory" cd ..
rm -rf app/src/main/resources/static/*
run "Copying build to static resources" cp -r frontend/dist/* app/src/main/resources/static/

echo -e "${GREEN}FRONTEND BUILT${NC}"
