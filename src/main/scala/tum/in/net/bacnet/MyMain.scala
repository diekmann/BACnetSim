package tum.in.net.bacnet


import com.serotonin.bacnet4j._
import com.serotonin.bacnet4j.`type`.constructed._
import com.serotonin.bacnet4j.`type`.primitive
import com.serotonin.bacnet4j.`type`.enumerated._
import com.serotonin.bacnet4j.`type`.notificationParameters._
import com.serotonin.bacnet4j.`type`.Encodable
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest._
import com.serotonin.bacnet4j.obj._


class MyReader(val localDevice: MyLocalDevice = new MyLocalDevice(
	      broadcastAddress = "127.0.0.255", 
	      name = "corny's reader",
	      deviceId=1234), abortOnNoRemotes: Boolean = true){
  
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
	  try{
		  assert(!deviceIPs.isEmpty)
	  } catch {
	    case e: java.lang.AssertionError => 
	      println("Error: "+e)
	      if (abortOnNoRemotes)
	        throw e
	      else 
	        println("continue")
	  }
	  val deviceIPsList: List[String] = (for (node <- (deviceIPs \\ "device"))yield node.text).toList
	  println("parsed devices from config: "+deviceIPsList)
    deviceIPsList
  }
  
  val otherDevices: List[String] = readConfigFromXML(new java.io.File("otherdevices.xml"))
  
	  
  val localReader = new SlaveDeviceReading(localDevice=localDevice,
      otherDevices=otherDevices, 
      abortOnNoRemotes=abortOnNoRemotes)
	
  def start(initLocalDevice: Boolean = true) = {
    if (initLocalDevice){
    	localDevice.initialize()
    }
    localReader.start()
  }
}

class MyReaderWriterActiveMasterSlaveDevice{
  
  val hostname: String = try {
    java.net.InetAddress.getLocalHost.getHostName
  }catch{
    case e: java.net.UnknownHostException => 
      println("Error: "+e)
      val hostnameguess_array = e.toString().split(' ')
      if (hostnameguess_array.length < 2){
        null
      }
      
      val hostnameguess = hostnameguess_array(hostnameguess_array.length - 1)
      if (hostnameguess.length() >= 5){
        println("guessing hostname `"+hostnameguess+"'")
        hostnameguess
      } else {
    	null
      }
  }
  
  /* acts, changes values */
  val localSlaveDevice = new MySlaveDevice(
	      broadcastAddress = "127.0.0.255", 
	      name=hostname,
	      randomValues=true)
  
  val localDevice = localSlaveDevice.localDevice
  
  val localReader = new MyReader(localDevice, abortOnNoRemotes=false)
  
  
  def start() = {
    localSlaveDevice.start()
    localReader.start(initLocalDevice=false)
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
  
  def randomRWSlave(): Unit = {
    println("Will generate a slave device with IO which also connects to other slaves")
    (new MyReaderWriterActiveMasterSlaveDevice()).start()
  }
  
  
  def help(): Unit = {
    println("available commands: "+cmds.keys)
    println("place a `otherdevices.xml' in your current working directory with the IP addresse "+
        "of all BACnet devices")
  }
  
  val cmds: Map[String, () => Unit] = Map("slave" -> slave, 
      "randomSlave" -> randomSlave, 
      "reader" -> reader, 
      "randomRWSlave" -> randomRWSlave, 
      "help" -> help)

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