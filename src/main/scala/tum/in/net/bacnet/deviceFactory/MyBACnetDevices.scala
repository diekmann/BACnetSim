package tum.in.net.bacnet.deviceFactory


import tum.in.net.bacnet.lowlevel.device.MyLocalDevice
import tum.in.net.bacnet.lowlevel.SlaveDeviceReading


/**
 * reads an xml file with other known BACnet devices
 * queries and sets their values
 * supervisory device
 */
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
    // TODO
    // we could pass abortOnNoRemotes=true and catch the assertion error, if no remote devices are found
    // retry every X seconds
    // although, this is better done in the SlaveDeviceReading's act method act
  }
}


/**
 * reads an xml file with other known BACnet devices
 * queries and sets their values
 * Also is an active BACnet device with changing Input/Output values
 */
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