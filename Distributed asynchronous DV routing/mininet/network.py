#!/usr/bin/python

"""Create a network and configure IP addresses

This script is intentionally verbose to show you
very simple steps to create and configure a network.
You can modify the scripts to create different topologies.

The network topology created by this script is shown below:

    r1----------r2-----------r3----------r4
    |          /             |           |
    |        /               |           |
    |      /                 |           |
    |    /                   |           |
    |  /                     |           |
    r0 --------------------- r6----------r5

See the accompanying topology picture for more details.
"""

from mininet.net import Mininet
from mininet.node import Node
from mininet.log import setLogLevel
from mininet.cli import CLI


def configLink(link, subnet):
    """configure network interfaces at both ends of the link

        the subnet length is hardcoded: subnets are 24 bits long
    """

    link.intf1.node.cmd('ifconfig ' + link.intf1.name + ' ' + subnet + '.1' + ' netmask 255.255.255.0 ' + 'broadcast ' + subnet + '.255')
    link.intf2.node.cmd('ifconfig ' + link.intf2.name + ' ' + subnet + '.2' + ' netmask 255.255.255.0 ' + 'broadcast ' + subnet + '.255')


def run():
    """create the network

        1. create nodes
        2. create links between nodes
        3. start the network
        4. assign IP addresses
    """

    net = Mininet()

    # add nodes to the network
    r0 = net.addHost('r0')
    r1 = net.addHost('r1')
    r2 = net.addHost('r2')
    r3 = net.addHost('r3')
    r4 = net.addHost('r4')
    r5 = net.addHost('r5')
    r6 = net.addHost('r6')

    # add links, specify network interface names for each link
    net.addLink(r0, r1, intfName1='eth0', intfName2='eth1')
    net.addLink(r0, r2, intfName1='eth1', intfName2='eth1')
    net.addLink(r1, r2, intfName1='eth0', intfName2='eth2')
    net.addLink(r2, r3, intfName1='eth0', intfName2='eth0')
    net.addLink(r3, r4, intfName1='eth1', intfName2='eth0')
    net.addLink(r4, r5, intfName1='eth1', intfName2='eth0')
    net.addLink(r5, r6, intfName1='eth1', intfName2='eth0')
    net.addLink(r6, r3, intfName1='eth1', intfName2='eth2')
    net.addLink(r0, r6, intfName1='eth2', intfName2='eth2')

    # start the network
    net.start()

    # configure IP addresses
    links = net.links
    for i in range(len(links)):
        configLink(links[i], "10.0." + str(i))


    # start command line
    CLI( net )
    net.stop()


if __name__ == '__main__':
    """main method"""

    setLogLevel( 'info' )
    run()
