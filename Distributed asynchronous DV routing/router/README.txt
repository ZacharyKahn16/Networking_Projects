
Router Driver
 
Running the driver:
==============
The following scripts can be used to compile and run the driver

	./build.sh --> to build the driver

	./run.sh "name"--> to run the driver, where "name" is the name of the router instance.

The following options can be used to pass command line arguments to the Router program by modifying the "run" script:

	-n	Router name, must be a unique string.

	-p 	Router port number.

	-t 	Duration of the keepalive timer.

	-i 	Duration of the inactivity timer.

Only "-n" is required, the rest are optional (if you do not specify them, their default values will be used).
