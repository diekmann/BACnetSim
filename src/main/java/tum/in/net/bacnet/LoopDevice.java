package tum.in.net.bacnet;

//UNUSED!!


/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * Copyright (C) 2006-2009 Serotonin Software Technologies Inc. http://serotoninsoftware.com
 * @author Matthew Lohbihler
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 */
//package com.serotonin.bacnet4j.test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;

/**
 *
 * software only device default local loop ;-)
 *
 * @author mlohbihler
 * @author aploese
 */
public class LoopDevice implements Runnable {

    public static void JAVAmain(String[] args) throws Exception {
        LoopDevice ld = new LoopDevice("127.0.0.255", LocalDevice.DEFAULT_PORT + 1);
        Thread.sleep(12000*10); // wait 2*10 min
        ld.doTerminate();
    }
    private boolean terminate;
    private LocalDevice localDevice;
    private BACnetObject ai0;
    private BACnetObject ai1;
    private BACnetObject bi0;
    private BACnetObject bi1;
    private BACnetObject mso0;
    private BACnetObject ao0;

    public LoopDevice(String broadcastAddress, int port) throws BACnetServiceException, IOException {
        localDevice = new LocalDevice(1968, broadcastAddress);
        localDevice.setPort(port);
        localDevice.getConfiguration().setProperty(PropertyIdentifier.objectName, new CharacterString("BACnet LoopDevice corny"));
        localDevice.getEventHandler().addListener(new DeviceEventListener() {

	      		@Override
	      		public void synchronizeTime(DateTime dateTime, boolean utc){
	      			throw new NotImplementedException();
	      		}
	      		@Override
	      		public void reinitializeDevice(ReinitializedStateOfDevice reinitializedStateOfDevice){
	      			throw new NotImplementedException();
	      		}
      		
            public void listenerException(Throwable e) {
                System.out.println("loopDevice listenerException");
            }

            public void iAmReceived(RemoteDevice d) {
                System.out.println("loopDevice iAmReceived");
            }

            public boolean allowPropertyWrite(BACnetObject obj, PropertyValue pv) {
                System.out.println("loopDevice allowPropertyWrite");
                return true;
            }

            public void propertyWritten(BACnetObject obj, PropertyValue pv) {
                System.out.println("loopDevice propertyWritten");
                if (! pv.getPropertyIdentifier().equals(PropertyIdentifier.presentValue)){
                	System.out.println("\t something is strange: not writing to present value!!"+pv);
                }
                System.out.println("\twrote "+obj.getObjectName()+" new value: "+pv.getValue());
            }

            public void iHaveReceived(RemoteDevice d, RemoteObject o) {
                System.out.println("loopDevice iHaveReceived");
            }

            public void covNotificationReceived(UnsignedInteger subscriberProcessIdentifier, RemoteDevice initiatingDevice, ObjectIdentifier monitoredObjectIdentifier, UnsignedInteger timeRemaining, SequenceOf<PropertyValue> listOfValues) {
                System.out.println("loopDevice covNotificationReceived");
            }

            public void eventNotificationReceived(UnsignedInteger processIdentifier, RemoteDevice initiatingDevice, ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass, UnsignedInteger priority, EventType eventType, CharacterString messageText, NotifyType notifyType, Boolean ackRequired, EventState fromState, EventState toState, NotificationParameters eventValues) {
                System.out.println("loopDevice eventNotificationReceived");
            }

            public void textMessageReceived(RemoteDevice textMessageSourceDevice, Choice messageClass, MessagePriority messagePriority, CharacterString message) {
                System.out.println("loopDevice textMessageReceived");
            }

            public void privateTransferReceived(UnsignedInteger vendorId, UnsignedInteger serviceNumber, Encodable serviceParameters) {
                System.out.println("loopDevice privateTransferReceived");
            }

        });

        // Set up a few objects.
        ai0 = new BACnetObject(
                localDevice, localDevice.getNextInstanceObjectIdentifier(ObjectType.analogInput));
        ai0.setProperty(PropertyIdentifier.units, EngineeringUnits.centimeters);
        localDevice.addObject(getAi0());

        ai1 = new BACnetObject(
                localDevice, localDevice.getNextInstanceObjectIdentifier(ObjectType.analogInput));
        ai0.setProperty(PropertyIdentifier.units, EngineeringUnits.percentObscurationPerFoot);
        localDevice.addObject(getAi1());

        bi0 = new BACnetObject(
                localDevice, localDevice.getNextInstanceObjectIdentifier(ObjectType.binaryInput));
        localDevice.addObject(getBi0());
        bi0.setProperty(PropertyIdentifier.objectName, new CharacterString("Off and on"));
        bi0.setProperty(PropertyIdentifier.inactiveText, new CharacterString("Off"));
        bi0.setProperty(PropertyIdentifier.activeText, new CharacterString("On"));

        bi1 = new BACnetObject(
                localDevice, localDevice.getNextInstanceObjectIdentifier(ObjectType.binaryInput));
        localDevice.addObject(getBi1());
        bi1.setProperty(PropertyIdentifier.objectName, new CharacterString("Good and bad"));
        bi1.setProperty(PropertyIdentifier.inactiveText, new CharacterString("Bad"));
        bi1.setProperty(PropertyIdentifier.activeText, new CharacterString("Good"));

        mso0 = new BACnetObject(
                localDevice, localDevice.getNextInstanceObjectIdentifier(ObjectType.multiStateOutput));
        mso0.setProperty(PropertyIdentifier.objectName, new CharacterString("Vegetable"));
        mso0.setProperty(PropertyIdentifier.numberOfStates, new UnsignedInteger(4));
        mso0.setProperty(PropertyIdentifier.stateText, 1, new CharacterString("Tomato"));
        mso0.setProperty(PropertyIdentifier.stateText, 2, new CharacterString("Potato"));
        mso0.setProperty(PropertyIdentifier.stateText, 3, new CharacterString("Onion"));
        mso0.setProperty(PropertyIdentifier.stateText, 4, new CharacterString("Broccoli"));
        mso0.setProperty(PropertyIdentifier.presentValue, new UnsignedInteger(1));
        localDevice.addObject(getMso0());

        ao0 = new BACnetObject(
                localDevice, localDevice.getNextInstanceObjectIdentifier(ObjectType.analogOutput));
        ao0.setProperty(PropertyIdentifier.objectName, new CharacterString("Settable analog"));
        localDevice.addObject(getAo0());


        // Start the local device.
        try{
	        localDevice.initialize();
	        new Thread(this).start();
        } catch (java.net.BindException e) {
        	System.out.println(e);
        	System.out.println("not starting loop device, I assume it is already running");
        }
    }

    public void run() {
        try {
            System.out.println("LoopDevice start changing values" + this + "\n");

            // Let it go...
            float ai0value = 0;
            float ai1value = 0;
            boolean bi0value = false;
            boolean bi1value = false;

            getMso0().setProperty(PropertyIdentifier.presentValue, new UnsignedInteger(2));
            while (!isTerminate()) {
                System.out.print("Change values of LoopDevice " + this + "\n");
                // Change the values.
                ai0value += 0.1;
                ai1value += 0.7;
                bi0value = !bi0value;
                bi1value = !bi1value;


                // Update the values in the objects.
                ai0.setProperty(PropertyIdentifier.presentValue, new Real(ai0value));
                ai1.setProperty(PropertyIdentifier.presentValue, new Real(ai1value));
                bi0.setProperty(PropertyIdentifier.presentValue, bi0value ? BinaryPV.active : BinaryPV.inactive);
                bi1.setProperty(PropertyIdentifier.presentValue, bi1value ? BinaryPV.active : BinaryPV.inactive);

                synchronized (this) {
                    wait(1000*10); // 1*10 second or notified (faster exit then stupid wait for 1 second)
                }
            }
            System.out.println("Close LoopDevive " + this);
        } catch (Exception ex) {
        }
        localDevice.terminate();
        localDevice = null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (localDevice != null) {
            localDevice.terminate();
            localDevice = null;
        }
    }

    /**
     * @return the terminate
     */
    public boolean isTerminate() {
        return terminate;
    }

    /**
     * @param terminate the terminate to set
     */
    public void doTerminate() {
        this.terminate = true;
        synchronized (this) {
            this.notifyAll(); // we may wait for this in run() ...
        }
    }

    /**
     * @return the broadcastAddress
     */
    public String getBroadcastAddress() {
        return localDevice.getBroadcastAddress();
    }

    /**
     * @return the port
     */
    public int getPort() {
        return localDevice.getPort();
    }

    /**
     * @return the localDevice
     */
    public LocalDevice getLocalDevice() {
        return localDevice;
    }

    /**
     * @return the ai0
     */
    public BACnetObject getAi0() {
        return ai0;
    }

    /**
     * @return the ai1
     */
    public BACnetObject getAi1() {
        return ai1;
    }

    /**
     * @return the bi0
     */
    public BACnetObject getBi0() {
        return bi0;
    }

    /**
     * @return the bi1
     */
    public BACnetObject getBi1() {
        return bi1;
    }

    /**
     * @return the mso0
     */
    public BACnetObject getMso0() {
        return mso0;
    }

    /**
     * @return the ao0
     */
    public BACnetObject getAo0() {
        return ao0;
    }
}

