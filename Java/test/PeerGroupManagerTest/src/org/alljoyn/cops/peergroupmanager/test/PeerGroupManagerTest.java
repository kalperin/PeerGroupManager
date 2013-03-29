/*******************************************************************************
* Copyright 2012 - 2013, Qualcomm Innovation Center, Inc.
*
*    Licensed under the Apache License, Version 2.0 (the "License");
*    you may not use this file except in compliance with the License.
*    You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
******************************************************************************/

package org.alljoyn.cops.peergroupmanager.test;

import java.lang.reflect.Method;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.cops.peergroupmanager.PeerGroupListener;
import org.alljoyn.cops.peergroupmanager.PeerGroupManager;

import android.test.AndroidTestCase;
import android.util.Log;

public class PeerGroupManagerTest extends AndroidTestCase {

	
	private PeerGroupManager mPeerGroupManager1;
	private PeerGroupManager mPeerGroupManager2;
	private BusAttachment busForSetPortTest;
	
	private final String TAG = "PeerGroupManagerTest";
	private final String SERVICE_PREFIX = "test";
	private final String SERVICE_NAME = "testSetPort";
	private final short CONTACT_PORT = 47;	
	
	private String peerAddedPeerId;
	private int countFoundAdvertisedName = 0;
	private int countLostAdvertisedName = 0;
	private int sessionId = 0;
	
	/*
	 * PeerGroupListener used in testing setPort functionality
	 */
	private PeerGroupListener pgListener = new PeerGroupListener() {
    	@Override
        public void foundAdvertisedName(String groupName, short transport) {
            countFoundAdvertisedName++;
        };
        

        @Override
        public void lostAdvertisedName(String groupName, short transport) {
        	countLostAdvertisedName++;
        };
        
        @Override
        public void peerAdded(String busId, String groupName, int numParticipants){
        	peerAddedPeerId = busId;
        }
    };

	public PeerGroupManagerTest(){
		super();		
	}
	
	/*
	 * setUp function creates two PeerGroupManagers to be used in tests 
	 * and also resets some checked variables to default values
	 */
	protected void setUp(){
		mPeerGroupManager1 = new PeerGroupManager("test", new PeerGroupListener(), null);
		mPeerGroupManager2 = new PeerGroupManager("test", new PeerGroupListener(), null);
		
		countFoundAdvertisedName = 0;
		countLostAdvertisedName = 0;
		peerAddedPeerId = "";

		logInfo("start of test");
	}
	
	/*
	 * tearDown function calls cleanup on the PeerGroupManager, 
	 * which releases all the PeerGroupManager's resources, 
	 * and then sets the references to null for faster garbage collection
	 */
	protected void tearDown(){	
		mPeerGroupManager1.cleanup();
		mPeerGroupManager2.cleanup();
		
		mPeerGroupManager1 = null;
		mPeerGroupManager2 = null;	
		
		logInfo("end of test");
	}
	
	/*
	 * Testing createGroup
	 */
	public void testCreateNormal(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testCreateNormal") == Status.OK);

		assertTrue(mPeerGroupManager1.listHostedGroups().contains("testCreateNormal"));
		
		assertTrue(mPeerGroupManager2.listFoundGroups().contains("testCreateNormal"));
	}
	
	public void testCreateTwice(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testCreateTwice") == Status.OK);

		assertTrue(mPeerGroupManager1.createGroup("testCreateTwice") == Status.FAIL);
	}
	
	public void testCreateFound(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testCreateFound") == Status.OK);

		assertTrue(mPeerGroupManager2.createGroup("testCreateFound") == Status.FAIL);
	}
	
	public void testCreateInput(){
		assertTrue(mPeerGroupManager1.createGroup(null) == Status.FAIL);
		
		assertTrue(mPeerGroupManager1.createGroup("") == Status.FAIL);
	}
	
	
	/*
	 * Testing joinGroup
	 */
	public void testJoinNormal(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testJoinNormal") == Status.OK);
		
		assertTrue(mPeerGroupManager1.getNumPeers("testJoinNormal") == 1);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "joinGroup", "testJoinNormal") == Status.OK);

		assertTrue( mPeerGroupManager2.listJoinedGroups().contains("testJoinNormal"));
		
		assertTrue(mPeerGroupManager1.getNumPeers("testJoinNormal") == 2);
	}
	
	public void testJoinNonexistant(){
		assertTrue( mPeerGroupManager1.joinGroup("testJoinNonexistant") == Status.FAIL);	

		assertFalse(mPeerGroupManager1.listJoinedGroups().contains("testJoinNonexistant"));
	}
	
	public void testJoinTwice(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testJoinTwice") == Status.OK);

		assertTrue(callMethodAndWait(mPeerGroupManager2, "joinGroup", "testJoinTwice") == Status.OK);
		
		assertTrue(mPeerGroupManager2.joinGroup("testJoinTwice") == Status.ALLJOYN_JOINSESSION_REPLY_ALREADY_JOINED);
	}
	
	public void testJoinHosting(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testJoinHosting") == Status.OK);

		assertTrue(mPeerGroupManager1.joinGroup("testJoinHosting") == Status.FAIL);
	}
	
	public void testJoinInput(){
		assertTrue(mPeerGroupManager1.joinGroup(null) == Status.FAIL);

		assertTrue(mPeerGroupManager1.joinGroup("") == Status.FAIL);
	}
	
	
	/*
	 * Testing destroyGroup
	 */	
	public void testDestroyNormal(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testDestroyNormal") == Status.OK);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "joinGroup", "testDestroyNormal") == Status.OK);

		assertTrue(mPeerGroupManager2.listJoinedGroups().contains("testDestroyNormal"));
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "leaveGroup", "testDestroyNormal") == Status.OK);
		
		assertTrue(callMethodAndWait(mPeerGroupManager1, "destroyGroup", "testDestroyNormal") == Status.OK);
		
		assertFalse(mPeerGroupManager1.listHostedGroups().contains("testDestroyNormal"));
		
		assertFalse(mPeerGroupManager2.listFoundGroups().contains("testDestroyNormal"));
	}
	
	public void testDestroyNonexistant(){
		assertTrue(mPeerGroupManager1.destroyGroup("testDestroyNonexistant") == Status.FAIL);	
	}
	
	public void testDestroyInput(){
		assertTrue(mPeerGroupManager1.destroyGroup(null) == Status.FAIL);
		
		assertTrue(mPeerGroupManager1.destroyGroup("") == Status.FAIL);
	}
	
	public void testDestroyTwice(){
		assertTrue(mPeerGroupManager1.createGroup("testDestroyTwice") == Status.OK);

		assertTrue(mPeerGroupManager1.destroyGroup("testDestroyTwice") == Status.OK);

		assertTrue(mPeerGroupManager1.destroyGroup("testDestroyTwice") == Status.FAIL);
	}
	
	public void testDestroyNotOwner(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testDestroyNotOwner") == Status.OK);
	
		assertTrue(mPeerGroupManager2.destroyGroup("testDestroyNotOwner") == Status.FAIL);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "joinGroup", "testDestroyNotOwner") == Status.OK);

		assertTrue(mPeerGroupManager2.destroyGroup("testDestroyNotOwner") == Status.FAIL);
	}
	
	
	/*
	 * Testing leaveGroup
	 */
	public void testLeaveNormal(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testLeaveNormal") == Status.OK);

		assertTrue(mPeerGroupManager1.getNumPeers("testLeaveNormal") == 1);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "joinGroup", "testLeaveNormal") == Status.OK);

		assertTrue(mPeerGroupManager2.listJoinedGroups().contains("testLeaveNormal"));
		
		assertTrue(mPeerGroupManager1.getNumPeers("testLeaveNormal") == 2);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "leaveGroup", "testLeaveNormal") == Status.OK);
		
		assertFalse(mPeerGroupManager2.listJoinedGroups().contains("testLeaveNormal"));
		
		assertTrue(mPeerGroupManager1.getNumPeers("testLeaveNormal") == 1);
	}
	
	public void testLeaveNotJoined(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testLeaveNotJoined") == Status.OK);

		assertTrue(mPeerGroupManager2.leaveGroup("testLeaveNotJoined") == Status.ALLJOYN_LEAVESESSION_REPLY_NO_SESSION);
	}
	
	public void testLeaveTwice(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testLeaveTwice") == Status.OK);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "joinGroup", "testLeaveTwice") == Status.OK);
		
		assertTrue(mPeerGroupManager2.leaveGroup("testLeaveTwice") == Status.OK);
		
		assertTrue(mPeerGroupManager2.leaveGroup("testLeaveTwice") == Status.ALLJOYN_LEAVESESSION_REPLY_NO_SESSION);
	}
	
	public void testLeaveHosting(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testLeaveHosting") == Status.OK);

		assertTrue(mPeerGroupManager1.leaveGroup("testLeaveHosting") == Status.FAIL);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "joinGroup", "testLeaveHosting") == Status.OK);
		
		assertTrue(mPeerGroupManager1.leaveGroup("testLeaveHosting") == Status.FAIL);
	}
	
	public void testLeaveNonexistant(){
		assertTrue(mPeerGroupManager2.leaveGroup("testLeaveNonexistant") == Status.ALLJOYN_LEAVESESSION_REPLY_NO_SESSION);
	}
	
	public void testLeaveInput(){
		assertTrue(mPeerGroupManager1.leaveGroup(null) == Status.FAIL);

		assertTrue(mPeerGroupManager1.leaveGroup("") == Status.FAIL);
	}
	
	
	/*
	 * Testing lockGroup and unlockGroup
	 */
	public void testLockAndUnlockNormal(){
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testLockAndUnlockNormal") == Status.OK);		

		assertTrue(callMethodAndWait(mPeerGroupManager1, "lockGroup", "testLockAndUnlockNormal") == Status.OK);		
		
		Status status = mPeerGroupManager2.joinGroup("testLockAndUnlockNormal");
		assertTrue(status == Status.ALLJOYN_JOINSESSION_REPLY_REJECTED || status == Status.FAIL);

		assertTrue(callMethodAndWait(mPeerGroupManager1, "unlockGroup", "testLockAndUnlockNormal") == Status.OK);		

		assertTrue(mPeerGroupManager2.joinGroup("testLockAndUnlockNormal") == Status.OK);
	}
	
	public void testLockAndUnlockNotOwner(){
		assertTrue(mPeerGroupManager1.createGroup("testLockAndUnlockNotOwner") == Status.OK);
	
		assertTrue(mPeerGroupManager2.lockGroup("testLockAndUnlockNotOwner") == Status.FAIL);

		assertTrue(mPeerGroupManager2.unlockGroup("testLockAndUnlockNotOwner") == Status.FAIL);
	}
	
	public void testLockAndUnlockNonexistant(){
		assertTrue(mPeerGroupManager1.lockGroup("testLockAndUnlockNonexistant") == Status.FAIL);
		
		assertTrue(mPeerGroupManager1.unlockGroup("testLockAndUnlockNonexistant") == Status.FAIL);
	}

	
	public void testUnlockAndLockInput(){
		assertTrue(mPeerGroupManager1.lockGroup(null) == Status.FAIL);

		assertTrue(mPeerGroupManager1.lockGroup("") == Status.FAIL);

		assertTrue(mPeerGroupManager1.unlockGroup(null) == Status.FAIL);

		assertTrue(mPeerGroupManager1.unlockGroup("") == Status.FAIL);
	}
	
	
	/*
	 * Testing PeerGroupListener callbacks
	 */
	public void testFoundAdvertisedName(){
		mPeerGroupManager2.addPeerGroupListener(pgListener);
		
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testFoundAdvertisedName") == Status.OK);
		
		assertTrue(countFoundAdvertisedName == 1);
	}
	
	public void testLostAdvertisedName(){
		mPeerGroupManager2.addPeerGroupListener(pgListener);
		
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testLostAdvertisedName") == Status.OK);
		
		assertTrue(callMethodAndWait(mPeerGroupManager1, "destroyGroup", "testLostAdvertisedName") == Status.OK);
		
		assertTrue(countLostAdvertisedName == 1);
	}
	
	public void testPeerAdded(){
		mPeerGroupManager2.addPeerGroupListener(pgListener);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "createGroup", "testPeerAdded") == Status.OK);
		
		assertTrue(callMethodAndWait(mPeerGroupManager1, "joinGroup", "testPeerAdded") == Status.OK);

		assertTrue(peerAddedPeerId.equals(mPeerGroupManager1.getMyPeerId()));
	}
	
	public void testMultipleListeners(){
		
		PeerGroupListener pgListenerFoundAndLostAdvertised = new PeerGroupListener() {
	    	@Override
	        public void foundAdvertisedName(String groupName, short transport) {
	            countFoundAdvertisedName++;
	        };
	        

	        @Override
	        public void lostAdvertisedName(String groupName, short transport) {
	        	countLostAdvertisedName++;
	        };
	    };
	    
	    PeerGroupListener pgListenerFoundAdvertised = new PeerGroupListener() {
	    	@Override
	        public void foundAdvertisedName(String groupName, short transport) {
	            countFoundAdvertisedName++;
	        };
	    };
	    
		mPeerGroupManager2.addPeerGroupListener(pgListener);
		mPeerGroupManager2.addPeerGroupListener(pgListenerFoundAndLostAdvertised);
		mPeerGroupManager2.addPeerGroupListener(pgListenerFoundAdvertised);
		
		assertTrue(callMethodAndWait(mPeerGroupManager1, "createGroup", "testMultipleListeners") == Status.OK);
		
		assertTrue(callMethodAndWait(mPeerGroupManager1, "destroyGroup", "testMultipleListeners") == Status.OK);
		
		assertTrue(callMethodAndWait(mPeerGroupManager2, "createGroup", "testMultipleListeners2") == Status.OK);
		
		assertTrue(callMethodAndWait(mPeerGroupManager1, "joinGroup", "testMultipleListeners2") == Status.OK);
		
		assertTrue(countFoundAdvertisedName == 3);
		assertTrue(countLostAdvertisedName == 2);
		assertTrue(peerAddedPeerId.equals(mPeerGroupManager1.getMyPeerId()));
	}
	
	
	/*
	 * Testing setPort functionality
	 */
	public void testSetPortNormal(){
		setPortSetUp((short)(CONTACT_PORT));
        
        assertTrue(mPeerGroupManager1.listFoundGroups().contains(SERVICE_NAME));
        
        assertTrue(callMethodAndWait(mPeerGroupManager1, "joinGroup", SERVICE_NAME) == Status.OK);
        
        assertTrue(mPeerGroupManager1.listJoinedGroups().contains(SERVICE_NAME));
        
        setPortCleanUp(busForSetPortTest);
	}
	
	public void testSetPortWrongPort(){
        setPortSetUp((short)(CONTACT_PORT + 10));
        
        assertTrue(mPeerGroupManager1.listFoundGroups().contains(SERVICE_NAME));
        
        assertTrue(callMethodAndWait(mPeerGroupManager1, "joinGroup", SERVICE_NAME) == Status.ALLJOYN_JOINSESSION_REPLY_UNREACHABLE);
        
        assertFalse(mPeerGroupManager1.listJoinedGroups().contains(SERVICE_NAME));
        
        setPortCleanUp(busForSetPortTest);
	}
	
	public void testSetPortWithTwoGroups(){
        setPortSetUp((short)(CONTACT_PORT));
        
        assertTrue(mPeerGroupManager1.listFoundGroups().contains(SERVICE_NAME));
        
        assertTrue(callMethodAndWait(mPeerGroupManager2, "createGroup", SERVICE_NAME) == Status.OK);
        
        assertTrue(mPeerGroupManager1.listFoundGroups().contains(SERVICE_NAME));
        
        assertTrue(callMethodAndWait(mPeerGroupManager1, "joinGroup", SERVICE_NAME) == Status.OK);
        
        assertTrue(mPeerGroupManager2.getNumPeers(SERVICE_NAME) == 2);
        
        setPortCleanUp(busForSetPortTest);
	}
	
	
	/*
	 * Private helper functions
	 */
	private Status callMethodAndWait(PeerGroupManager instance, String methodName, String parameter){
		Class[] parameterTypes = new Class[1];
		parameterTypes[0] = String.class;
		
		Object[] parameters = new Object[1];
		parameters[0] = parameter;
		
		try {
			Method method = PeerGroupManager.class.getMethod(methodName, parameterTypes);
			Status status = (Status) method.invoke(instance, parameters);
			Thread.sleep(400);
			return status;
			
		} catch (Exception e) {
			logError(e.getClass().getName() + " - " + e.getMessage());
			
		}
		
		return null;
	}
	
	private void logError(String message) {
        String msg = getName() + ": " + message;
        Log.e(TAG, msg);
        
    }
	
	private void logInfo(String message) {
		String msg = getName() + ": " + message;
        Log.i(TAG, msg);
        
    }
	
	private void setPortSetUp(short port){
        busForSetPortTest = new BusAttachment("PeerGroupManager", BusAttachment.RemoteMessage.Receive);            
        busForSetPortTest.connect();
        
        Mutable.ShortValue contactPort = new Mutable.ShortValue(port);
        
        SessionOpts sessionOpts = new SessionOpts();
        sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
        sessionOpts.isMultipoint = true;
        sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
        sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
        
        busForSetPortTest.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() 
        {
            @Override
            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts)
            {                
               return true;
            }
            
            @Override
            public void sessionJoined(short sessionPort, int id, String joiner) 
            {
            	sessionId = id;
            }
        });
        
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;          
        busForSetPortTest.requestName(SERVICE_PREFIX + "." + SERVICE_NAME, flag);
        busForSetPortTest.advertiseName(SERVICE_PREFIX + "." + SERVICE_NAME, SessionOpts.TRANSPORT_ANY);
        
        mPeerGroupManager1.setSessionPort(CONTACT_PORT);
        
        try {
            Thread.sleep(400);
        } catch (Exception e) {
            logError(e.getClass().getName() + " - " + e.getMessage());       
        }
	}
	
	private void setPortCleanUp(BusAttachment bus){
		bus.leaveSession(sessionId);
		bus.disconnect();
		bus.release();
	}
	
	
}

