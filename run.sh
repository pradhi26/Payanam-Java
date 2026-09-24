#!/usr/bin/env bash
echo "==================================================================="
echo "              PAYANAM - Tourist Booking Web Platform              "
echo "==================================================================="
echo ""
mkdir -p bin
echo "[1/2] Compiling Payanam Java Source Code..."
javac -encoding UTF-8 -d bin -sourcepath src/main/java \
    src/main/java/com/payanam/model/*.java \
    src/main/java/com/payanam/util/*.java \
    src/main/java/com/payanam/repository/*.java \
    src/main/java/com/payanam/server/*.java \
    src/main/java/com/payanam/Main.java

if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed."
    exit 1
fi

echo "[OK] Compilation successful!"
echo ""
echo "[2/2] Starting Payanam Web Server at http://localhost:8085 ..."
echo "-------------------------------------------------------------------"
echo " * Open Web Browser: http://localhost:8085"
echo " * Default Admin:    admin@gmail.com / admin123"
echo " * Default Tourist:  rahul@gmail.com / rahul123"
echo "-------------------------------------------------------------------"
java -cp bin com.payanam.Main 8085
