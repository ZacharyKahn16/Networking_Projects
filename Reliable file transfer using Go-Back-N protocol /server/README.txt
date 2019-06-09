
FastFtp Server

Running the server:
==============
Use the following script to start the server

    ./run.sh

To stop the server, type "quit".

Using the above script, the server starts with a set of default parameters.
You can modify the script to specify your own parameters.

If you like to run the server on a Windows machine, then copy the commands
inside the script and run them at the command line.
Be sure to modify the classpath as Windows uses ";" as the separator.

The following options can be used to pass command line arguments to the server:

	-v 	To specify the verbosity level of the server.
		Available values are "all", "info" and "off".
		Passing "-v all" generates the most amount of logging messages,
		while "-v off" turns off output messages completely.

	-p 	To specify the server port number.
		It can be any integer greater than 1024, less than 64K.

	-i 	Initial sequence number to be used by the server.
		The server communicates the initial sequence number to the client
		during the handshake process and expects the first segment arriving
		from the client to have this sequence number.

	-x 	The sequence number of the segment to be dropped by the server.
		This parameter is useful when observing the client response to
		packet losses specially when setting "-l 0".

	-d 	Average additional delay added to acknowledgements by the server.
		The server uses this parameter to wait for some random delay before
		sending each ACK. The actual delay is generated randomly so that the
		average delay is equal to this parameter.

	-l 	Average segment loss probability.
		It specifies the ratio of lost segments at the server. The server randomly
		drops arriving segments. The probability that the server drops a segment
		is given by this parameter.
