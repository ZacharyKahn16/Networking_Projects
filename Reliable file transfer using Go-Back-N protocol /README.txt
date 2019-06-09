To run program:
---------------

Refer to README.txt's in the client and server directories.

Program Functionality:
----------------------

A simplified FTP client based on UDP is implemented that provides reliable data
transfer using the Go-Back-N protocol. This class establishes a connection using
a TCP handshake with a UDP socket. A file is then sent to the server through the
UDP socket. The main thread handles sending the file contents, a ReceiverThread
is created to receive acknowledgements from the server, and a Timer thread is
used if a timeout occurs (due to a lost packet). All threads are synchronized
and make use of the Sement and TxQueue classes which are provided by the
libftp.jar file.
