package tum.in.net.bacnet

import com.serotonin.bacnet4j._
import com.serotonin.bacnet4j.`type`.constructed._
import com.serotonin.bacnet4j.`type`.primitive
import com.serotonin.bacnet4j.`type`.enumerated._
import com.serotonin.bacnet4j.`type`.notificationParameters._
import com.serotonin.bacnet4j.`type`.Encodable
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest._
import com.serotonin.bacnet4j.obj._



abstract sealed trait BACListenerMessage
case class BACListeneriAmReceived(d: RemoteDevice) extends BACListenerMessage

trait BACDummyListener extends event.DeviceEventListener{
  val listenerName: String
  val localDevice: MyLocalDevice
  
  /*
   * A scala actor can be notified if something happens in this DeviceListener
   */
  protected var bacEventListeners: List[scala.actors.Actor] = List()
  def addEventListener(l: scala.actors.Actor): Unit = {
    require(l ne null)
    require(!bacEventListeners.contains(l))
    
    bacEventListeners ::= l
  }
  
  protected def notifyListeners(msg: BACListenerMessage): Unit = {
    import scala.actors.!
    println("notifying: "+bacEventListeners)
    bacEventListeners.foreach(l => l ! msg)
  }
  
	override def synchronizeTime(dateTime: DateTime, utc: Boolean){
		throw new org.apache.commons.lang.NotImplementedException();
	}

	override def reinitializeDevice(reinitializedStateOfDevice: ReinitializedStateOfDevice){
		throw new org.apache.commons.lang.NotImplementedException();
	}

	override def listenerException(e: Throwable) {
		println(listenerName+" listenerException"+e);
	}

	/*
	 * I an iAm is receivd, we send an iAm back
	 * Thus, unicast discovery via iAm (not whoIs) is possible
	 */
	var ignoreIamOnce: Set[RemoteDevice] = Set()
	override def iAmReceived(d: RemoteDevice) {
		println(listenerName+" iAmReceived from: "+d.getName()+" at "+
		    d.getAddress().getInetAddress()+":"+d.getAddress().getPort())
		println(d.getAddress())
		
		if (! ignoreIamOnce.contains(d)){
			println("\t answering with IAm")
			localDevice.sendUnconfirmedRequest(d.getAddress(), localDevice.getIAm())
			ignoreIamOnce += d
		} else {
		  ignoreIamOnce -= d
		}
		
		notifyListeners(BACListeneriAmReceived(d))
	}

	override def allowPropertyWrite(obj: BACnetObject, pv: PropertyValue) =  {
		println(listenerName+" allowPropertyWrite")
		true
	}

	override def propertyWritten(obj: BACnetObject, pv: PropertyValue) {
		println(listenerName+" propertyWritten");
		if (pv.getPropertyIdentifier() != (PropertyIdentifier.presentValue)){
			println("\t something is strange: not writing to present value!!"+pv);
		}
		println("\twrote "+obj.getObjectName()+" new value: "+pv.getValue());
	}

	override def iHaveReceived(d: RemoteDevice, o: RemoteObject) {
		println(listenerName+" iHaveReceived");
	}

	override def covNotificationReceived(subscriberProcessIdentifier: primitive.UnsignedInteger, 
			initiatingDevice: RemoteDevice, monitoredObjectIdentifier: primitive.ObjectIdentifier,
			timeRemaining: primitive.UnsignedInteger, listOfValues: SequenceOf[PropertyValue]) {
		val str_head = "["+listenerName+"] covNotificationReceived from "+
				initiatingDevice.getAddress().toIpPortString() +" "
		val str_tail = "  subscriberPID: "+subscriberProcessIdentifier + " " +
				"timeRemaining: "+timeRemaining
		//println("listOfValues: "+listOfValues)
		val lovscala = scala.collection.JavaConversions.iterableAsScalaIterable(listOfValues)
		val currentVals = 
		lovscala.filter(v => v.getPropertyIdentifier() == PropertyIdentifier.presentValue)
		println(str_head + monitoredObjectIdentifier + " Current value(s): "+currentVals.map(v => v.getValue()) + str_tail)
	}

	override def eventNotificationReceived(processIdentifier: primitive.UnsignedInteger,
			initiatingDevice: RemoteDevice, 
			eventObjectIdentifier: primitive.ObjectIdentifier,
			timeStamp: TimeStamp, 
			notificationClass: primitive.UnsignedInteger,
			priority: primitive.UnsignedInteger, 
			eventType: EventType, 
			messageText: primitive.CharacterString,
			notifyType: NotifyType, 
			ackRequired: primitive.Boolean, 
			fromState: EventState,
			toState: EventState, 
			eventValues: NotificationParameters) {
		println(listenerName+" eventNotificationReceived");
	}

	override def textMessageReceived(textMessageSourceDevice: RemoteDevice, messageClass: Choice,
			messagePriority: MessagePriority, message: primitive.CharacterString) {
		println(listenerName+" textMessageReceived");
	}

	override def privateTransferReceived(vendorId: primitive.UnsignedInteger, serviceNumber: primitive.UnsignedInteger,
			serviceParameters: Encodable) {
		println(listenerName+" privateTransferReceived");
	}

}