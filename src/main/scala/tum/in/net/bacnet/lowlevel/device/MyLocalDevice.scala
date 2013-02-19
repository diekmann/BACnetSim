package tum.in.net.bacnet.lowlevel.device


import com.serotonin.bacnet4j._
import com.serotonin.bacnet4j.`type`.constructed._
import com.serotonin.bacnet4j.`type`.primitive
import com.serotonin.bacnet4j.`type`.enumerated._
import com.serotonin.bacnet4j.`type`.notificationParameters._
import com.serotonin.bacnet4j.`type`.Encodable
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest._
import com.serotonin.bacnet4j.obj._
import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaBuffer


/**
 * My local BACnet device
 * Other classes in the lowlevel package extend its functionality
 * @author corny
 *
 */
class MyLocalDevice(val broadcastAddress: String,
    val name: String,
    val deviceId: Int, // make sure this value is unique!
    val port: Int = LocalDevice.DEFAULT_PORT) 
{
  
  // from: http://www.scadaengine.com/tutorial/server_api_faq.html#Q3
  //What is a Device ID?
	//The Device ID is used to uniquely identify each BACnet Device, it can be in the range of 0 to 4194304. 
  //There cannot be more than 1 device using the same Device ID. <-- bacnet4j seems to have a problem with this!!
  //Each of the sample applications operate as a device and requires it's own device id which defaults to zero.
	require(deviceId >= 0)
	require(deviceId < 4194304)
  
	protected val localDevice: LocalDevice = new LocalDevice(deviceId, broadcastAddress)
	
	private val deviceEventListener = new BACDummyListener() {
      val listenerName = name
      val localDevice = MyLocalDevice.this
	}
	
	def addEventListener(l: scala.actors.Actor) = deviceEventListener.addEventListener(l)
	
	localDevice.getConfiguration().setProperty(PropertyIdentifier.objectName, new primitive.CharacterString(name));
	localDevice.getEventHandler().addListener(deviceEventListener)
	localDevice.setPort(port);
	
	println("Created local device `"+name+"' on "+localDevice.getAddress().getInetAddress()+
	    ":"+localDevice.getAddress().getPort()+
	    " broadcast:"+broadcastAddress+" at port "+port+" with id "+deviceId)
	
	
	def sendUnconfirmedRequest(toAddr: String, 
	    request: service.unconfirmed.UnconfirmedRequestService,
	    toPort:Int=LocalDevice.DEFAULT_PORT): Unit =
	{
	  require(initialized)
	  val addrDotted = toAddr.split("\\.").map(s => s.toInt)
	  require(addrDotted.length == 4)
	  require(addrDotted.forall(n => n >= 0 && n <= 256))
	  
	  val addrByte = addrDotted.map(n => n.toByte).toList
	  
		val addrIP: Array[Byte] = addrByte.toArray[Byte]
	  val addr = new Address(addrIP, toPort)
	  
	  sendUnconfirmedRequest(addr, request)
	}
	
	def sendUnconfirmedRequest(toAddr: Address, 
	    request: service.unconfirmed.UnconfirmedRequestService): Unit = {
	  require(initialized)
	  
	  
	  println("connecting to device at "+toAddr.toIpPortString()+"  "+toAddr.getInetAddress())
	  val network: Network = new Network(toAddr.getNetworkNumber().intValue(), 
	      toAddr.getInetAddress().getAddress())
	  localDevice.sendUnconfirmed(toAddr, network, request)
	}
	
	def getIAm() = {
	  require(initialized)
	  localDevice.getIAm()
	}
	
	var initialized = false
	
	def getLocalDevice() = {
	  require(!initialized)
	  localDevice
	}
	
	def getLocalDevice(iKnowWatImdoing: Boolean) = {
	  require(iKnowWatImdoing)
	  localDevice
	}
	
	def getRemoteDevices(): List[RemoteDevice] = {
	  require(initialized)
    import scala.collection.JavaConversions._
	  localDevice.getRemoteDevices().toList
	}
	
	def getExtendedDeviceInformation(d: RemoteDevice): Unit = {
	  require(initialized)
	  localDevice.getExtendedDeviceInformation(d);
	}
	
	def sendReadPropertyAllowNull(d: RemoteDevice, 
	    oid: primitive.ObjectIdentifier, 
	    pid: PropertyIdentifier): Encodable = 
	{
	  require(initialized)
	  localDevice.sendReadPropertyAllowNull(d, oid, pid)
	}
	
	def initialize() = {
	  require(!initialized)
	  initialized = true
	  localDevice.initialize()
	  
	  
    // you need to send the IAm or COV subscription fails with
    //`ErrorAPDU(choice=5, errorClass=Services, errorCode=Cov subscription failed)'
	  println("sending IAm on "+broadcastAddress+":"+port)
    localDevice.sendBroadcast(port, localDevice.getIAm());
	}
	
	def terminate() = {
	  require(initialized)
	  localDevice.terminate()
	}
}