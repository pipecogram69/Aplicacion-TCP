1 — Conexión Directa con Políticas 
El cliente se conecta directamente al servidor sin intermediarios. El servidor cuenta con un panel de control que permite iniciarlo en dos modos: sin reinicio automático o con reinicio automático. El cliente aplica una política de reintentos con un número máximo de intentos y un tiempo de espera entre cada uno. Si se agotan los reintentos sin éxito, el cliente entra en estado de timeout. Si el servidor vuelve a estar disponible antes de que se agoten los reintentos, el cliente se reconecta solo.

2 — Balanceo de Carga
Un balanceador de carga recibe las conexiones de los clientes y las distribuye entre un pool de tres servidores usando el algoritmo Round Robin. Si un servidor cae, el balanceador lo detecta, lo excluye del pool y sigue atendiendo a los clientes con los servidores disponibles. Cada cierto tiempo verifica si los servidores caídos se recuperaron para reincorporarlos al pool. El cliente se reconecta automáticamente si pierde la conexión.
Ejercicio 2 — Conexión Directa con Políticas
