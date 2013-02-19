# BACnetSim
=========

A scala BACnet simulator using bacnet4J


## Short introduction:



### Building:

* Deprecated: Download compiled binary at https://github.com/diekmann/BACnetSim/downloads
* New: Download code and build with sbt (http://www.scala-sbt.org/0.12.1/docs/home.html#install)
    1. `sbt compile`
    2. `sbt one-jar`

  
### Running:

* Get otherdevices.xml from repository, copy to same directory as compiled jar.
* Run: `java -jar bacnetsimulator_2.9.2-0.1.1-one-jar.jar`
* Configuration:
    * Place IPs of other instances of BACnet devices (or this simulator) in otherdevices.xml
    * Pass the command line argument `randomSlave` to get a random BACnet slave device (sensor/actor)
    * Pass the command line argument `randomRWSlace` to get a random BACnet device with simulated sensors/actors which connects to other BACnet devices and queries/changes their values


Only one simulator instance per host. You need multiple (virtual) machines to set up a BACnet test network.

Happy Hacking
