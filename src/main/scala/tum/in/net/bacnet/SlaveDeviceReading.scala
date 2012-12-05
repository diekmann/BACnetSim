package tum.in.net.bacnet

import com.serotonin.bacnet4j._
import com.serotonin.bacnet4j.`type`.constructed._
import com.serotonin.bacnet4j.`type`.primitive
import com.serotonin.bacnet4j.`type`.enumerated._
import com.serotonin.bacnet4j.`type`.notificationParameters._
import com.serotonin.bacnet4j.`type`.Encodable
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest._
import com.serotonin.bacnet4j.obj._
import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.TIMEOUT

class SlaveDeviceReading (
    val localDevice: MyLocalDevice,
    val otherDevices: List[String]
    ) extends Actor
{
  val random = new scala.util.Random()
  
  /* Property references */
	var refs: Map[RemoteDevice, util.PropertyReferences] = Map()
	
	def getPropertyRef(d: RemoteDevice): util.PropertyReferences = refs.get(d) match {
	  case Some(x) => x
	  case None => println("Error: did not find reference for `"+d+"' "+ 
	      d.getAddress().toIpPortString()+" in refs")
	  	println("\tremoteDevices: "+localDevice.getRemoteDevices())
	  	println("\tknownRemoteDevices: "+knownOtherDevices())
	  	println("\trefs: "+refs.keys.toList)
	  	throw new NoSuchElementException
	}
	
  /* Property values, cached. Things like name of an Output. Not updated.
   * Do not get present value from those! */
	var refsValues: Map[RemoteDevice, util.PropertyValues] = Map()
	
	/**
	 * Use this instead of localDevice.getRemoteDevices.
	 * Only returns devices where we have a reference for
	 */
	def knownOtherDevices(): List[RemoteDevice] = localDevice.getRemoteDevices().filter(d => refs.contains(d))
	
	// actor must be running
	protected def initializeRemoteDevices(): Unit = {
		import scala.collection.JavaConversions._
		
		println("initilaizing remote devices: "+otherDevices)

		// get to know all other devices
		otherDevices.foreach(localDevice.sendUnconfirmedRequest(_, localDevice.getIAm))
		
		// get notified if iAm is received
		localDevice.addEventListener(this)
		
	  def waitForOtherDevices(remaining: Set[String], start: Long): Unit = {
	    if (remaining.isEmpty){
	    	println("waitForOtherDevices: waited for iAmReceived: " + (System.currentTimeMillis() - start) + " ms")
	    } else receiveWithin(10000){
			  case BACListeneriAmReceived(d) =>
			    println("waitForOtherDevices: received iAm from "+d.getAddress().toIpPortString())
			    waitForOtherDevices(remaining - d.getAddress().toIpString(), start)
			  case TIMEOUT =>
			    println("waitForOtherDevices: timeout. Missing devices: "+remaining)
			    waitForOtherDevices(Set(), start)
			}
	  }
		waitForOtherDevices(otherDevices.toSet, System.currentTimeMillis())

		val remoteDevices = localDevice.getRemoteDevices()
		println("Registered remote devices: "+remoteDevices.map(s => "\t("+s.getInstanceNumber()+", "+
		    s.getAddress().toIpPortString()+", "+s.getName()+")\n").mkString)
		
		assert(remoteDevices.length > 0, "found at least one remote device")

		// Get extended information for all remote devices.
		for (d <- remoteDevices) {
			localDevice.getExtendedDeviceInformation(d);
			val objRefs = new util.PropertyReferences()

			println("remote name: "+d.getName())
			val oidsEncoded = localDevice.sendReadPropertyAllowNull(d, 
					d.getObjectIdentifier(), PropertyIdentifier.objectList)
			assert (oidsEncoded.isInstanceOf[SequenceOf[primitive.ObjectIdentifier]])
			val oids = oidsEncoded.asInstanceOf[SequenceOf[primitive.ObjectIdentifier]].getValues()

			for (oid <- oids) {
				addPropertyReferences(objRefs, oid)
			}


			localDevice.getLocalDevice(true).readProperties(d, objRefs);
			//println("Values of d: " + d);
			// Send the read request.
			val values: util.PropertyValues = localDevice.getLocalDevice(true).readProperties(d, objRefs);
			
			// add to refsValues
			// later on, we use this to retrive the name of a sensor
			val objRefsValues = new util.PropertyValues()
			for (oid <- oids) {
				val propertyReferences = objRefs.getProperties().get(oid)
				for (pr <- propertyReferences.filter(p => p.getPropertyIdentifier() != PropertyIdentifier.presentValue)){
					values.filter(v => v.getPropertyIdentifier() == pr.getPropertyIdentifier() && 
					    v.getObjectIdentifier() == oid) match {
					  case Nil => println("no value for "+pr+" "+pr.getPropertyIdentifier()+" returned")
					  case List(opr) => 
					    val encodable: Encodable = values.getNoErrorCheck(oid, pr)
					    //println("storing "+pr.getPropertyIdentifier()+" for oid "+oid+" value "+encodable)
					    objRefsValues.add(oid, 
					        pr.getPropertyIdentifier(), 
					        pr.getPropertyArrayIndex(), 
					        encodable)
					  case l => println("ambiguous: "+l)
					}
					
				}
			}
					
			def printMyRemoteObjects(): Unit = try {

				// Dereference the property values back into the points.
				for (oid <- oids) {
					printObject(oid, objRefs, values)
				}
			} catch {
			case e: exception.BACnetException => println("event.bacnet.readDevice ADDRESS: " + e.getMessage());
			}
			
			
			println("savind objRef for "+d)
			refs = refs.updated(d, objRefs)
			refsValues = refsValues.updated(d, objRefsValues)

			printMyRemoteObjects()
		}
	}


  /**
   * register for value changes in all binary inputs
   */
	protected def registerCOV(): Unit = {
			import scala.collection.JavaConversions._  


			val remoteDevices = knownOtherDevices()
			
			//			BAD! analogue values can change all the time
			//			ObjectCovSubscription.addSupportedObjectType(ObjectType.analogValue)
			//			ObjectCovSubscription.addSupportedObjectType(ObjectType.analogInput)
			//			ObjectCovSubscription.addSupportedObjectType(ObjectType.analogOutput)
			
			for (d <- remoteDevices) {
			  
			  val objRefs = getPropertyRef(d)
			  
			  val noAnalogue = objRefs.getProperties().filter(o => o._1.getObjectType() != ObjectType.analogValue &&
			      o._1.getObjectType() != ObjectType.analogInput && o._1.getObjectType() != ObjectType.analogOutput)
			      
				println("registered values (without analogue):"+noAnalogue.keys)
			  
			  for (remoteObj <- noAnalogue) {
					val objectIdentifier: primitive.ObjectIdentifier = remoteObj._1
					val propertyReferences: List[`type`.constructed.PropertyReference] = remoteObj._2.toList
					
					println("Setting up COV request for oid: "+objectIdentifier+" propId: "+propertyReferences.head.getPropertyIdentifier())
					
					val subscriberProcessIdentifier = new primitive.UnsignedInteger(0) //TODO??
					val issueConfirmedNotifications = new primitive.Boolean(true)
					val lifetime = new primitive.UnsignedInteger(0)
					val covReq: service.confirmed.SubscribeCOVRequest = new service.confirmed.SubscribeCOVRequest(
							subscriberProcessIdentifier, objectIdentifier,
							issueConfirmedNotifications, lifetime)
					try {
						if (localDevice.getLocalDevice(true).send(d, covReq) ne null) println("send always returned null. error?")
						println("subsciption of " + objectIdentifier.getObjectType()
								+ " " + objectIdentifier.getInstanceNumber() + " done")
					} catch {
					  case e: exception.BACnetServiceException => println("BACnetServiceException: unable to send <SubscribeCOVRequest> for the Object "
							+ objectIdentifier.getObjectType()+  "-"+objectIdentifier.getInstanceNumber()+
							"\n\t"+ e.getMessage())
							println(e)
					  case e:exception.BACnetException =>
					  println("BACnetException: unable to send <SubscribeCOVRequest> for the Object "
							+ objectIdentifier.getObjectType()+  "-"+objectIdentifier.getInstanceNumber()+
							"\n\t"+ e.getMessage())
							println(e)
					}
			  }
			}
	}
	
	/**
	 * read all analogoue in and outputs, read digital outputs
	 */
	protected def readAnalogueValues(): Unit = {
	  import scala.collection.JavaConversions._
		

		val remoteDevices = knownOtherDevices()
		for (d <- remoteDevices) {
		  val objRefs = getPropertyRef(d)
		  
		  // added digital outputs
		  val onlyAnalogue = objRefs.getProperties().filter(o => o._1.getObjectType() == ObjectType.analogValue ||
		      o._1.getObjectType() == ObjectType.analogInput || o._1.getObjectType() == ObjectType.analogOutput ||
		      o._1.getObjectType() == ObjectType.binaryOutput)
		      
			var str = "reading analogue values and digital outputs of "+ d.getAddress().toIpPortString() +": "+onlyAnalogue.keys + 
				"\n"
		  
		  for (remoteObj <- onlyAnalogue) {
				val objectIdentifier: primitive.ObjectIdentifier = remoteObj._1
				val propertyReferences: List[`type`.constructed.PropertyReference] = remoteObj._2.toList
				val value: util.PropertyValues = localDevice.getLocalDevice(true).readOidPresentValues(d, List(objectIdentifier))
				
				assert(objectIdentifier ne null)
				assert(value ne null)
				
				
				def getCachedPropertyValue(pid: PropertyIdentifier, default: String): String = {
					val objRefLst = propertyReferences.filter(r => r.getPropertyIdentifier() == pid)
					assert(objRefLst.length <= 1)
					val objStr = if (objRefLst.isEmpty)
					  	default
						else
						{
						  val v = refsValues.get(d).get
							val enc = v.get(objectIdentifier, pid)
							enc.toString
						}
					objStr
				}
				val objName = getCachedPropertyValue(PropertyIdentifier.objectName, "unknownValue")
				val objUnits = getCachedPropertyValue(PropertyIdentifier.units, "")
				
				str += "["+d.getName()+"] "+objName+" "
        str += "("+objectIdentifier.toString()+")" + " = "
        str += value.getString(objectIdentifier, PropertyIdentifier.presentValue, "N/A") + " "+objUnits
        str += "\n"
		  }
		  
		  println(str)
		}
	}

	
	/**
	 * write a binary or analogue output
	 */
	protected def changeValue(): Unit = {
			import scala.collection.JavaConversions._  

			val remoteDevices = knownOtherDevices()
			for (d <- remoteDevices) {
				val objRefs= getPropertyRef(d)
				
				// change binary or analog output
				val changeBinary = random.nextBoolean()
				
				val propertyRefs = if (changeBinary)
					  objRefs.getProperties().toList.filter(o => o._1.getObjectType() == ObjectType.binaryOutput)
					else
					  objRefs.getProperties().toList.filter(o => o._1.getObjectType() == ObjectType.analogOutput)
				
				val newValue = if (changeBinary)
					  new primitive.UnsignedInteger(random.nextInt(20)) // not so binary, but works. simulating some wtf
					else
					  new primitive.Real(random.nextInt(40)+random.nextFloat())
				
				assert(propertyRefs.length > 0, "have an output to change")
				val prop = propertyRefs(random.nextInt(propertyRefs.length)) // random oid
				val objectIdentifier = prop._1
				
				println("Writing a property of "+d.getAddress().toIpPortString()+" oid `"+objectIdentifier+"'")
				
				val priority: primitive.UnsignedInteger = null
				val propertyArrayIndex: primitive.UnsignedInteger = null
				val writeReq = new service.confirmed.WritePropertyRequest(objectIdentifier,
						PropertyIdentifier.presentValue,
						propertyArrayIndex, 
						newValue,
						priority)
				try {
					if (localDevice.getLocalDevice(true).send(d, writeReq) ne null) 
					  println("ERROR: send always returns null")
					
					println("writing of " + objectIdentifier.getObjectType()
							+ "-" + objectIdentifier.getInstanceNumber() + " done")
							println("New value: "+newValue)
				} catch {
				case e:exception.BACnetException =>
				println("unable to send <WritePropertyRequest> for the Object "
						+ objectIdentifier.getObjectType()+  "-"+objectIdentifier.getInstanceNumber()+
						"\n\t"+ e.getMessage())
						println(e)
				}
			}
	}
	
	def act(): Unit = {
	  initializeRemoteDevices()
	  
	  registerCOV()
	  
	  loop{
		  Thread.sleep((random.nextInt(7)+3)*1000) // sleep about 3 to 10 sec
		  if (random.nextBoolean())
		  	readAnalogueValues()
		  else
		    changeValue()
	  }
	}

  
  private def addPropertyReferences(refs: util.PropertyReferences, oid: primitive.ObjectIdentifier) {
        refs.add(oid, PropertyIdentifier.objectName);

        val otype: ObjectType = oid.getObjectType();
        if (ObjectType.accumulator.equals(otype)) {
            refs.add(oid, PropertyIdentifier.units);
        } else if (ObjectType.analogInput.equals(otype) ||
                ObjectType.analogOutput.equals(otype) ||
                ObjectType.analogValue.equals(otype) ||
                ObjectType.pulseConverter.equals(otype)) {
            refs.add(oid, PropertyIdentifier.units);
        } else if (ObjectType.binaryInput.equals(otype) ||
                ObjectType.binaryOutput.equals(otype) ||
                ObjectType.binaryValue.equals(otype)) {
            refs.add(oid, PropertyIdentifier.inactiveText);
            refs.add(oid, PropertyIdentifier.activeText);
        } else if (ObjectType.lifeSafetyPoint.equals(otype)) {
            refs.add(oid, PropertyIdentifier.units);
        } else if (ObjectType.loop.equals(otype)) {
            refs.add(oid, PropertyIdentifier.outputUnits);
        } else if (ObjectType.multiStateInput.equals(otype) ||
                ObjectType.multiStateOutput.equals(otype) ||
                ObjectType.multiStateValue.equals(otype)) {
            refs.add(oid, PropertyIdentifier.stateText);
        } else {
            return;
        }

        refs.add(oid, PropertyIdentifier.presentValue);
    }
  
  private def printObject(oid: primitive.ObjectIdentifier, refs: util.PropertyReferences, 
      values: util.PropertyValues) {
        println("\t" + oid);
        
        // print properties
        import scala.collection.JavaConversions._
        
    		val prsjava = refs.getProperties().get(oid)
        val prs: List[PropertyReference] = prsjava.toList
        
        for (pr: PropertyReference <- prs) {
            val encodable: Encodable = values.getNoErrorCheck(oid, pr);
            if (encodable == null) {
                System.out.println("event.bacnet.readError: no value returned");
            } else if (encodable.isInstanceOf[BACnetError]) {
                System.out.println("event.bacnet.readError NAME: " + (encodable.asInstanceOf[BACnetError]).getErrorCode());
            } else {
                System.out.println(String.format("\t\t %s = %s", pr.getPropertyIdentifier(), encodable.toString()));
            }
        }
    }
}