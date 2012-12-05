package tum.in.net.bacnet;

// UNUSED!!

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

import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.type.constructed.BACnetError;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.util.PropertyValues;

/**
 * Discovers and devices and print all properties of all objects found.
 *
 *
 *
 * @author Matthew Lohbihler
 * @author Arne Plöse
 */
public class DiscoveryTest {

    public static String BROADCAST_ADDRESS = "127.0.0.255";
    private LoopDevice loopDevice;
    private LocalDevice localDevice;

    public DiscoveryTest(String broadcastAddress, int port) throws IOException {
        localDevice = new LocalDevice(1234, broadcastAddress);
        localDevice.setPort(port);
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
                System.out.println("DiscoveryTest listenerException");
            }

            public void iAmReceived(RemoteDevice d) {
                System.out.println("DiscoveryTest iAmReceived");
                synchronized (DiscoveryTest.this) {
                    DiscoveryTest.this.notifyAll();
                }
            }

            public boolean allowPropertyWrite(BACnetObject obj, PropertyValue pv) {
                System.out.println("DiscoveryTest allowPropertyWrite");
                return true;
            }

            public void propertyWritten(BACnetObject obj, PropertyValue pv) {
                System.out.println("DiscoveryTest propertyWritten");
            }

            public void iHaveReceived(RemoteDevice d, RemoteObject o) {
                System.out.println("DiscoveryTest iHaveReceived");
            }

            public void covNotificationReceived(UnsignedInteger subscriberProcessIdentifier, RemoteDevice initiatingDevice, ObjectIdentifier monitoredObjectIdentifier, UnsignedInteger timeRemaining, SequenceOf<PropertyValue> listOfValues) {
                System.out.println("DiscoveryTest covNotificationReceived");
            }

            public void eventNotificationReceived(UnsignedInteger processIdentifier, RemoteDevice initiatingDevice, ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass, UnsignedInteger priority, EventType eventType, CharacterString messageText, NotifyType notifyType, Boolean ackRequired, EventState fromState, EventState toState, NotificationParameters eventValues) {
                System.out.println("DiscoveryTest eventNotificationReceived");
            }

            public void textMessageReceived(RemoteDevice textMessageSourceDevice, Choice messageClass, MessagePriority messagePriority, CharacterString message) {
                System.out.println("DiscoveryTest textMessageReceived");
            }

            public void privateTransferReceived(UnsignedInteger vendorId, UnsignedInteger serviceNumber, Encodable serviceParameters) {
                System.out.println("DiscoveryTest privateTransferReceived");
            }
        });
        localDevice.initialize();

    }

    public void doDiscover() throws Exception {
        // Who is
        System.out.println("Send Broadcast WhoIsRequest() ");
        // Send the broadcast to the correct port of the LoopDevice !!!
        localDevice.sendBroadcast(loopDevice.getPort(), new WhoIsRequest(null, null));

        // wait for notification in iAmReceived() Timeout 2 sec
        synchronized (this) {
            final long start = System.currentTimeMillis();
            this.wait(2000);
            System.out.println(" waited for iAmReceived: " + (System.currentTimeMillis() - start) + " ms");
        }


        System.out.println("Start iterating remote devices");

        // Get extended information for all remote devices.
        for (RemoteDevice d : localDevice.getRemoteDevices()) {

            localDevice.getExtendedDeviceInformation(d);

            List<ObjectIdentifier> oids = ((SequenceOf<ObjectIdentifier>) localDevice.sendReadPropertyAllowNull(
                    d, d.getObjectIdentifier(), PropertyIdentifier.objectList)).getValues();

            PropertyReferences refs = new PropertyReferences();
            for (ObjectIdentifier oid : oids) {
                addPropertyReferences(refs, oid);
            }
            

            localDevice.readProperties(d, refs);
            System.out.println("Values of d: " + d);
            try {
                // Send the read request.
                PropertyValues values = localDevice.readProperties(d, refs);

                // Dereference the property values back into the points.
                for (ObjectIdentifier oid : oids) {
                    printObject(oid, refs, values);
                }
            } catch (BACnetException e) {
                System.out.println("event.bacnet.readDevice ADDRESS: " + e.getMessage());
            }

        }

        System.out.println("Remote devices done...");
    }

    /**
     * Note same Bropadcast address, but different ports!!!
     * @param args
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    public static void JAVAmain(String[] args) throws Exception {
        DiscoveryTest dt = new DiscoveryTest(BROADCAST_ADDRESS, LocalDevice.DEFAULT_PORT);
        dt.setLoopDevice(new LoopDevice(BROADCAST_ADDRESS, LocalDevice.DEFAULT_PORT + 1));
        try {
            dt.doDiscover();
        } finally {
            dt.localDevice.terminate();
            System.out.println("Cleanup loopDevice");
            dt.getLoopDevice().doTerminate();
        }
    }

    private void addPropertyReferences(PropertyReferences refs, ObjectIdentifier oid) {
        refs.add(oid, PropertyIdentifier.objectName);

        ObjectType type = oid.getObjectType();
        if (ObjectType.accumulator.equals(type)) {
            refs.add(oid, PropertyIdentifier.units);
        } else if (ObjectType.analogInput.equals(type) ||
                ObjectType.analogOutput.equals(type) ||
                ObjectType.analogValue.equals(type) ||
                ObjectType.pulseConverter.equals(type)) {
            refs.add(oid, PropertyIdentifier.units);
        } else if (ObjectType.binaryInput.equals(type) ||
                ObjectType.binaryOutput.equals(type) ||
                ObjectType.binaryValue.equals(type)) {
            refs.add(oid, PropertyIdentifier.inactiveText);
            refs.add(oid, PropertyIdentifier.activeText);
        } else if (ObjectType.lifeSafetyPoint.equals(type)) {
            refs.add(oid, PropertyIdentifier.units);
        } else if (ObjectType.loop.equals(type)) {
            refs.add(oid, PropertyIdentifier.outputUnits);
        } else if (ObjectType.multiStateInput.equals(type) ||
                ObjectType.multiStateOutput.equals(type) ||
                ObjectType.multiStateValue.equals(type)) {
            refs.add(oid, PropertyIdentifier.stateText);
        } else {
            return;
        }

        refs.add(oid, PropertyIdentifier.presentValue);
    }

    /**
     * @return the loopDevice
     */
    public LoopDevice getLoopDevice() {
        return loopDevice;
    }

    /**
     * @param loopDevice the loopDevice to set
     */
    public void setLoopDevice(LoopDevice loopDevice) {
        this.loopDevice = loopDevice;
    }

    private void printObject(ObjectIdentifier oid, PropertyReferences refs, PropertyValues values) {
        System.out.println("\t" + oid);
        // print propertie
        for (PropertyReference pr : refs.getProperties().get(oid)) {
            Encodable encodable = values.getNoErrorCheck(oid, pr);
            if (encodable == null) {
                System.out.println("event.bacnet.readError: no value returned");
            } else if (encodable instanceof BACnetError) {
                System.out.println("event.bacnet.readError NAME: " + ((BACnetError) encodable).getErrorCode());
            } else {
                System.out.println(String.format("\t\t %s = %s", pr.getPropertyIdentifier(), encodable.toString()));
            }
        }
    }
}
