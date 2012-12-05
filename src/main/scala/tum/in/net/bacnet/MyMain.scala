package tum.in.net.bacnet


import com.serotonin.bacnet4j._
import com.serotonin.bacnet4j.`type`.constructed._
import com.serotonin.bacnet4j.`type`.primitive
import com.serotonin.bacnet4j.`type`.enumerated._
import com.serotonin.bacnet4j.`type`.notificationParameters._
import com.serotonin.bacnet4j.`type`.Encodable
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest._
import com.serotonin.bacnet4j.obj._


class MyReader {
  
  def readConfigFromXML(file: java.io.File): List[String] = {
    println("reading configuration file "+file)
    val knownDevicesFromConfig = scala.io.Source.fromFile(file)
    val otherDevicesString: String = try {
      knownDevicesFromConfig.mkString
    } finally {
      knownDevicesFromConfig.close()
    }
    import scala.xml._
    require(!otherDevicesString.isEmpty())
    
    // config template
		//	  val xmlOtherDevices = <bacdevices>
		//  		<device>192.168.56.102</device>
		//  		<device>192.168.56.103</device>
		//  		<device>192.168.56.104</device>
		//  		</bacdevices>
    
    val xmlOtherDevices = XML.loadString(otherDevicesString)
	  assert(xmlOtherDevices.isInstanceOf[scala.xml.Elem])
	  val deviceIPs: NodeSeq = (xmlOtherDevices \\ "bacdevices")
	  assert(!deviceIPs.isEmpty)
	  val deviceIPsList: List[String] = (for (node <- (deviceIPs \\ "device"))yield node.text).toList
	  println("parsed devices from config: "+deviceIPsList)
    deviceIPsList
  }
  
  val otherDevices: List[String] = readConfigFromXML(new java.io.File("otherdevices.xml"))
  
  
	val localDevice = new MyLocalDevice(
	      broadcastAddress = "127.0.0.255", 
	      name = "corny's reader",
	      deviceId=1234)
	  
  val localReader = new SlaveDeviceReading(localDevice=localDevice,
      otherDevices=otherDevices)
	
  def start() = {
    localDevice.initialize()
    localReader.start()
  }
  
  
}

object MyMain {
  
  def slave(): Unit = {
    (new MySlaveDevice("127.0.0.255", "corny's personal slave")).start()
  }
  
  def randomSlave(): Unit = {
   (new MySlaveDevice("127.0.0.255", randomValues=true)).start()
  }
  
  def reader(): Unit = {
    (new MyReader()).start()
  }
  
  def help(): Unit = {
    println("available commands: "+cmds.keys)
    println("place a `otherdevices.xml' in your current working directory with the IP addresse "+
        "of all BACnet devices")
  }
  
  val cmds: Map[String, () => Unit] = Map("slave" -> slave, "randomSlave" -> randomSlave, "reader" -> reader, "help" -> help)

  def main(args: Array[String]) {
    System.setProperty("java.net.preferIPv4Stack", "true");
    
    
    if(args.length > 0){
      println("Oh cool, an argument")
      val actingThing = cmds.get(args(0))
      actingThing match {
        case None => println("available commands: "+cmds.keys)
        case Some(f) => f()
      }
    }else{
      println("starting default. pass command line argument help for help")
    	(new MyReader).start()
    }
    

  }
  
  
  
  
}