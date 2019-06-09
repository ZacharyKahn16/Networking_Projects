(1) to compile:
javac -d . *.java


(2) to run:
java ServerDriver 3535

use any port you like instead of 3535.

(3) to run client (optional, you can a different client as well):
java TCPClient


Functionality provided
----------------------
The WebServer class is the main thread that listens for connection requests.
As new requests are accepted from a client, a new worker thread is made
to handle the connection. The WebServer spins up threads to handle the
requests using a Worker thread.
The Worker has two modes, Web and Proxy Server mode. In Proxy Server mode, the
Worker forwards the client's request to the specified host and port and relays
the response back to the client.
In Web Server mode, the Worker searches for the requested file and sends it back
to the client. The Worker thread also handles range requests. The Worker also
checks if the client request is correctly formatted and if the file requested
exists.
