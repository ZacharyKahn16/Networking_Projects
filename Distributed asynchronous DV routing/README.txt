This program uses mininet to simulate the network.
To run this program you will need to use a VM.
Ubuntu's VM was what I used.


The network topology is defined in:
/mininet/network.py

To run mininet:
 Open up Vm
 Open up terminal and go into the router director and run:
  $ sudo python ../mininet/network.py     // Initializes mininet topology
  $ xterm r0 r1 r2 // ... all the way up to r6, defined by network.py

Upon running the above command, xterm will make a terminal for each r_ you
inputted.

In each xterm terminal run the following:
  $ ./build.sh    // Only need to run this in the xterm termianl once.
  $ ./run.sh r_    // where _ is the number for the terminal you want to run.

To kill a router, while it is running type in:
quit

This will cause the router to shutdown and it will output the cost to each node
in its distance vector (DV) as well as what neighboring node it takes to get
to that particular destination.

Router structure:
-----------------

A router with a unique name listens on a specified port number of other
neighbors in the network. The router is configured to broadcast to all links
that connect to it, and will periodically broadcast its local DV to let
its neighbors know it is alive. To learn about other nodes in the network
the Bellman-Ford algorithm is implemented. If neighboring nodes become
inactive, they are removed from the local DV table. The maximum allowable
cost to a node in the network is defined by BasicRouter.COST_INFINITY. A node
at or above this cost is considered to be an inactive node within the network
and so it is removed from the local DV and forwarding table.

Refer to the Router.java class for more details.
