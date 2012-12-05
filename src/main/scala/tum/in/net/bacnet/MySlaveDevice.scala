package tum.in.net.bacnet

import com.serotonin.bacnet4j.`type`.enumerated._
import com.serotonin.bacnet4j.LocalDevice

class MySlaveDevice(broadcastAddress: String, name: String = "corny's personal slave", randomValues: Boolean = false)
{
  val random = if (randomValues) new scala.util.Random() else null
  
  // create random values
  val deviceId = if (randomValues) random.nextInt(4000000) else MySlaveDevice.deviceId
  val deviceName = if (randomValues){
    val x1 = MySlaveDevice.deviceNames1(random.nextInt(MySlaveDevice.deviceNames1.length))
    val x2 = MySlaveDevice.deviceNames2(random.nextInt(MySlaveDevice.deviceNames2.length))
    val x3 = MySlaveDevice.deviceNames3(random.nextInt(MySlaveDevice.deviceNames3.length))
    x1+"'s "+x2+" "+x3
  	} else name
  val analogueIO = if (randomValues){
    val numIN = 1 max random.nextInt(4)
    val numOUT = 1 max random.nextInt(3)
    val in = for (i <- 0 until numIN) yield {
      val dev = MySlaveDevice.analogeIOnames(random.nextInt(MySlaveDevice.analogeIOnames.length))
      (IOTypeInput, dev._1+"IN", dev._2)
    }
    val out = for (i <- 0 until numOUT) yield {
      val dev = MySlaveDevice.analogeIOnames(random.nextInt(MySlaveDevice.analogeIOnames.length))
      (IOTypeOutput, dev._1+"OUT", dev._2)
    }
    in.toList ::: out.toList
  	} else MySlaveDevice.ConfiguredAnalogueIO
  val binaryIO = if (randomValues){
    val numIN = 1 max random.nextInt(3)
    val numOUT = 1 max random.nextInt(3)
    val in = for (i <- 0 until numIN) yield {
      val dev = MySlaveDevice.binaryIOnames(random.nextInt(MySlaveDevice.binaryIOnames.length))
      (IOTypeInput, dev._1+"IN", dev._2)
    }
    val out = for (i <- 0 until numOUT) yield {
      val dev = MySlaveDevice.binaryIOnames(random.nextInt(MySlaveDevice.binaryIOnames.length))
      (IOTypeOutput, dev._1+"OUT", dev._2)
    }
    in.toList ::: out.toList
  	} else MySlaveDevice.ConfiguredBinaryIO
  
  // initialize
  val localDevice = new MyLocalDevice(
      broadcastAddress = broadcastAddress, 
      name = deviceName, deviceId = deviceId)
  
  val localIO = new SlaveDeviceIO(localDevice,
      analogueIO,
      binaryIO)
  
  println("initializing local device `"+deviceName+" with "+analogueIO+" and "+binaryIO)
  
  def start() = {
    localDevice.initialize()
    localIO.start()
  }
}

object MySlaveDevice {
  protected val deviceId = 1968
  
  protected val ConfiguredAnalogueIO: List[(IOType, String, EngineeringUnits)] = List(
      (IOTypeInput, "TemperatureIN", EngineeringUnits.degreesCelsius),
      (IOTypeInput, "HeatingEnergyIN", EngineeringUnits.amperes),
      (IOTypeInput, "WindowOpenIN", EngineeringUnits.centimeters)
      )

 protected val ConfiguredBinaryIO: List[(IOType, String, (String, String))] = List(
  		(IOTypeInput, "BinaryIN", ("ON", "OFF")), (IOTypeInput, "OtherBinaryIN", ("one", "two"))
  		)
 
	// values for random initialization
	protected val deviceNames1 = List("corny", "orc", "facility", "holger", "heiko")
	protected val deviceNames2 = List("personal", "devoted", "loyal", "evil", "sinister")
	protected val deviceNames3 = List("slave", "device", "servant", "attendant")
	
	protected val analogeIOnames = List(("Temperature", EngineeringUnits.degreesCelsius),
	    ("Temperature", EngineeringUnits.degreesCelsius),
	    ("HeatingEnergy", EngineeringUnits.amperes),
	    ("WindowOpen", EngineeringUnits.centimeters),
	    ("Uptime", EngineeringUnits.days)
	    )
	
	protected val binaryIOnames: List[(String, (String, String))] = List(
	    ("Power", ("ON", "OFF")),
	    ("Magic", ("ON", "OFF")),
	    ("WindowOpen", ("Open", "Closed")),
	    ("Light", ("ON", "OFF"))
	    )


}