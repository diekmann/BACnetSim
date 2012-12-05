BACnetSim
=========

A scala BACnet simulator using bacnet4J


Short introduction:
Download comiled binary at https://github.com/diekmann/BACnetSim/downloads

Get otherdevices.xml from repository, copy to same directory as comiped jar.

Run:
    java -jar bacnetsimulator_2.9.2-0.1.0\ -one-jar.ja


Configuration:
Place IPs of other instances of BACnet devices or this simulator in otherdevices.xml

Pass the command line argument `randomSlave' to get a random BACnet slave device (sensor/actor)

Only one simulator instance per host. You need multiple (virtual) machines to set up a BACnet test network.

Happy Hacking
