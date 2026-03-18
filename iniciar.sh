#!/bin/bash
BASE="/Volumes/SSD_FELIPE/Universidad/Sistemas_Distribuidos/TCP_2corte"

# Matar procesos previos
lsof -ti:4000,5001,5002,5003 | xargs kill -9 2>/dev/null
sleep 1

# Iniciar servidores
osascript -e "tell app \"Terminal\" to do script \"java -jar $BASE/ServidorSimple.jar 5001\""
osascript -e "tell app \"Terminal\" to do script \"java -jar $BASE/ServidorSimple.jar 5002\""
osascript -e "tell app \"Terminal\" to do script \"java -jar $BASE/ServidorSimple.jar 5003\""
sleep 2

# Iniciar balanceador
osascript -e "tell app \"Terminal\" to do script \"java -jar $BASE/Balanceador.jar\""
sleep 2

# Iniciar cliente
java -jar "$BASE/ClienteGUI.jar"
