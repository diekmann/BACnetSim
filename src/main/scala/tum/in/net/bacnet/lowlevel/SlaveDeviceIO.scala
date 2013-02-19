package tum.in.net.bacnet.lowlevel

import com.serotonin.bacnet4j._
import com.serotonin.bacnet4j.`type`.constructed._
import com.serotonin.bacnet4j.`type`.primitive
import com.serotonin.bacnet4j.`type`.enumerated._
import com.serotonin.bacnet4j.`type`.notificationParameters._
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest._
import com.serotonin.bacnet4j.obj._
import scala.actors.Actor
import scala.actors.Actor._
import tum.in.net.bacnet.lowlevel.device.MyLocalDevice

abstract sealed class IOType
case object IOTypeInput extends IOType
case object IOTypeOutput extends IOType


/**
 * Add to BACnet device `localDevice` Input/Ouput values
 * @author corny
 *
 */
class SlaveDeviceIO(
    val localDevice: MyLocalDevice,
    ConfiguredAnalogueIO: List[(IOType, String, EngineeringUnits)],
    ConfiguredBinaryIO: List[(IOType, String, (String, String))]
    ) extends Actor
{
	import scala.collection.JavaConversions._
	
	/**
	 * create a new Analogue device and register it at the current device.
	 * Input or Output
	 * Specify the name
	 * Specify the unit in what the input measures. E. g. EngineeringUnits.centimeters
	 */
	private def newAnalogueDevice(inout: IOType, 
	    description: String, 
	    unitType: EngineeringUnits): BACnetObject = 
	{
		val nextOID = inout match {
		  case IOTypeInput => localDevice.getLocalDevice().getNextInstanceObjectIdentifier(ObjectType.analogInput)
		  case IOTypeOutput => localDevice.getLocalDevice().getNextInstanceObjectIdentifier(ObjectType.analogOutput)
		}
				val ad = new BACnetObject(localDevice.getLocalDevice(), nextOID)
		ad.setProperty(PropertyIdentifier.units, unitType);
		ad.setProperty(PropertyIdentifier.objectName, new primitive.CharacterString(description));
		localDevice.getLocalDevice().addObject(ad);
		ad
	}
	
	/**
	 * specify what analogue devices exist. (I/O, name, unit)
	 */
	private val anlogueInputs: List[BACnetObject] = {
	  val ConfiguredAnalogueInputs = ConfiguredAnalogueIO.filter(d => d._1 == IOTypeInput)
	  for (obj <- ConfiguredAnalogueInputs) yield newAnalogueDevice(obj._1, obj._2, obj._3)
	}
	
	private val anlogueOutputs: List[BACnetObject] = {
	  val ConfiguredAnalogueInputs = ConfiguredAnalogueIO.filter(d => d._1 == IOTypeOutput)
	  for (obj <- ConfiguredAnalogueInputs) yield newAnalogueDevice(obj._1, obj._2, obj._3)
	}
	
	
  def newBinaryDevice(inout: IOType, 
      description: String, 
      values: (String, String)): BACnetObject = 
  {
		val nextOID = inout match {
		  case IOTypeInput => localDevice.getLocalDevice().getNextInstanceObjectIdentifier(ObjectType.binaryInput)
		  case IOTypeOutput => localDevice.getLocalDevice().getNextInstanceObjectIdentifier(ObjectType.binaryOutput)
		}
    val bi = new BACnetObject(localDevice.getLocalDevice(), nextOID)
    bi.setProperty(PropertyIdentifier.objectName, new primitive.CharacterString(description));
		bi.setProperty(PropertyIdentifier.inactiveText, new primitive.CharacterString(values._1));
		bi.setProperty(PropertyIdentifier.activeText, new primitive.CharacterString(values._2));
		localDevice.getLocalDevice().addObject(bi);
		bi
  }
  
  private val binaryInputs: List[BACnetObject] = {
	  val ConfiguredBinaryInputs = ConfiguredBinaryIO.filter(d => d._1 == IOTypeInput)
	  for (obj <- ConfiguredBinaryInputs) yield newBinaryDevice(obj._1, obj._2, obj._3)
	}
	
	private val binaryOutputs: List[BACnetObject] = {
	  val ConfiguredBinaryInputs = ConfiguredBinaryIO.filter(d => d._1 == IOTypeOutput)
	  for (obj <- ConfiguredBinaryInputs) yield newBinaryDevice(obj._1, obj._2, obj._3)
	}


	
	
// TODO multi state in/out
//	private var mso0: BACnetObject = null
//		mso0 = new BACnetObject(
//				localDevice, localDevice.getNextInstanceObjectIdentifier(ObjectType.multiStateOutput));
//		mso0.setProperty(PropertyIdentifier.objectName, new CharacterString("Vegetable"));
//		mso0.setProperty(PropertyIdentifier.numberOfStates, new UnsignedInteger(4));
//		mso0.setProperty(PropertyIdentifier.stateText, 1, new CharacterString("Tomato"));
//		mso0.setProperty(PropertyIdentifier.stateText, 2, new CharacterString("Potato"));
//		mso0.setProperty(PropertyIdentifier.stateText, 3, new CharacterString("Onion"));
//		mso0.setProperty(PropertyIdentifier.stateText, 4, new CharacterString("Broccoli"));
//		mso0.setProperty(PropertyIdentifier.presentValue, new UnsignedInteger(1));
//		localDevice.addObject(getMso0());


	
	private var terminate: Boolean = false;
	
	def act(): Unit = {
		try {
			println(localDevice.name+" start changing values");
			
			val random = new scala.util.Random()

			while (!terminate) {
				println("Change values");
				
				def updateAI(ai: BACnetObject) = {
				  val old = ai.getProperty(PropertyIdentifier.presentValue)
				  assert(old.isInstanceOf[primitive.Primitive])
				  assert(old.isInstanceOf[primitive.Real])
				  val oldVal = old.asInstanceOf[primitive.Real].floatValue()
				  val newVal = oldVal + random.nextFloat()
				  println("AI "+ai.getObjectName()+" change: "+oldVal+" -> "+newVal)
				  ai.setProperty(PropertyIdentifier.presentValue, new primitive.Real(newVal));
				}
				anlogueInputs.foreach(updateAI(_))
				
				
				def updateBI(bi: BACnetObject) = {
				  val old = bi.getProperty(PropertyIdentifier.presentValue)
				  assert(old.isInstanceOf[primitive.Primitive])
				  assert(old.isInstanceOf[`type`.enumerated.BinaryPV])
				  val oldVal = old.asInstanceOf[`type`.enumerated.BinaryPV].intValue()
				  val newVal = if(oldVal == 0) 1 else 0
				  println("BI "+bi.getObjectName()+" change: "+oldVal+" -> "+newVal)
				  bi.setProperty(PropertyIdentifier.presentValue, new `type`.enumerated.BinaryPV(newVal));
				}
				binaryInputs.foreach(updateBI(_))
				
//				bi0value = !bi0value;
//				bi1value = !bi1value;
//
//				bi0.setProperty(PropertyIdentifier.presentValue, bi0value ? BinaryPV.active : BinaryPV.inactive);
//				bi1.setProperty(PropertyIdentifier.presentValue, bi1value ? BinaryPV.active : BinaryPV.inactive);

				Thread.sleep(10000)
			}
			println("Close " + localDevice.name);
		} finally {
			localDevice.terminate();
		}
	}

}