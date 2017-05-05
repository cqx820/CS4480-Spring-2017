#!usr/bin/python

from mininet.cli import CLI
from mininet.link import Link
from mininet.link import TCLink
from mininet.net import Mininet
from mininet.term import makeTerm
from mininet.node import RemoteController


# class MyTopo():

#	def __init__(self):	


def MyTopo():
	net = Mininet(controller=RemoteController, autoSetMacs=True, autoStaticArp=True)
	c0 = net.addController('c0')
	s1 = net.addSwitch('s1')
	h1 = net.addHost('h1')
    	h2 = net.addHost('h2')
    	h3 = net.addHost('h3')
    	h4 = net.addHost('h4')
    	h5 = net.addHost('h5', ip='10.0.0.5/8')  # h5 has ip 10.0.0.5
    	h6 = net.addHost('h6', ip='10.0.0.6/8')

    	net.addLink(h1, s1, port2=1)  # add link and set dest port to 1
    	net.addLink(h2, s1, port2=2)
   	net.addLink(h3, s1, port2=3)
    	net.addLink(h4, s1, port2=4)
#	net.addLink(h5, s1, port1=1, port2=5, bw=1)
#	net.addLink(h6, s1, port1=2, port2=6, bw=1)

	TCLink(h5, s1, 1, 5, bw=1, loss=0)  # default bandwidth is 1 MB/s, this can be any number
    	TCLink(h6, s1, 2, 6, bw=1, loss=0)

	net.build();
    	net.startTerms()  # open terminal
   	c0.start()
    	s1.start([c0])
    	s1.sendCmd('ovs-vsctl set bridge s1 protocols=OpenFlow13')  # do I need to add sudo here?
   	h1.sendCmd('arp -s 10.0.0.5/8 00:00:00:00:00:05')
#	h1.sendCmd('arp -s 10.0.0.10 00:00:00:00:00:05')

    	h3.sendCmd('arp -s 10.0.0.5/8 00:00:00:00:00:05')
#	h3.sendCmd('arp -s 10.*.*.* 00:00:00:00:00:05')

    	h2.sendCmd('arp -s 10.0.0.6/8 00:00:00:00:00:06')
#	h2.sendCmd('arp -s 10.*.*.* 00:00:00:00:00:06')

    	h4.sendCmd('arp -s 10.0.0.6/8 00:00:00:00:00:06')
#	h4.sendCmd('arp -s 10.*.*.* 00:00:00:00:00:06')
    	h5.setMAC("00:00:00:00:00:05")
    	h6.setMAC("00:00:00:00:00:06")

    	CLI(net)

    	net.stop()


if __name__ == '__main__':
    MyTopo()
