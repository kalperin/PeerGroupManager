/******************************************************************************
 * Copyright 2013, Qualcomm Innovation Center, Inc.
 *
 *    All rights reserved.
 *    This file is licensed under the 3-clause BSD license in the NOTICE.txt
 *    file for this project. A copy of the 3-clause BSD license is found at:
 *
 *        http://opensource.org/licenses/BSD-3-Clause.
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the license is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the license for the specific language governing permissions and
 *    limitations under the license.
 ******************************************************************************/

package org.alljoyn.cops.peergroupmanagerapp;

import java.util.ArrayList;
import java.util.List;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

import org.alljoyn.cops.peergroupmanager.PeerGroupManager;
import org.alljoyn.cops.peergroupmanager.BusObjectData;
import org.alljoyn.cops.peergroupmanager.JoinOrCreateReturn;
import org.alljoyn.cops.peergroupmanager.PeerGroupListener;


//import groupmanager.BusObjectData;
//import groupmanager.GroupManager;
//import groupmanager.GroupManagerListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class BusHandler extends Handler {
    private static final String SERVICE_NAME = "org.alljoyn.PeerGroupManagerApp";
    
    private MainActivity    mActivity;
    private UIHandler       mUIHandler;
    
    private SimpleService   mSimpleService = new SimpleService();
    private PeerGroupManager  mGroupManager;

    /* These are the messages sent to the BusHandler from the UI. */
    public static final int INIT = 1;
    public static final int CREATE_GROUP = 2;
    public static final int DESTROY_GROUP = 3;
    public static final int JOIN_GROUP = 4;
    public static final int LEAVE_GROUP = 5;
    public static final int UNLOCK_GROUP = 6;
    public static final int LOCK_GROUP = 7;
    public static final int SET_PORT = 8;
    public static final int JOIN_OR_CREATE = 9;
    public static final int DISCONNECT = 10;
    public static final int PING = 13;

    public BusHandler(Looper looper, UIHandler uiHandler, MainActivity activity) {
        super(looper);
        mUIHandler = uiHandler;
        mActivity = activity;
    }
    
    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
        case INIT: {
            doInit();
            break;
        }
        case CREATE_GROUP: {
            doCreateGroup((String) msg.obj);
            break;
        }
        case DESTROY_GROUP : {
            doDestroyGroup((String) msg.obj);
            break;
        }
        case JOIN_GROUP: {
            doJoinGroup((String) msg.obj);
            break;
        }
        case LEAVE_GROUP : {
            doLeaveGroup((String) msg.obj);
            break;
        }
        case UNLOCK_GROUP : {
        	doUnlockGroup((String) msg.obj);
        	break;
        }
        case LOCK_GROUP : {
        	doLockGroup((String) msg.obj);
        	break;
        }
        case SET_PORT : {
        	doSetPort((Short) msg.obj);
        	break;
        }
        case JOIN_OR_CREATE : {
        	doJoinOrCreate((String) msg.obj);
        	break;
        }
        case PING: {
            Bundle data = msg.getData();
            String groupName = data.getString("groupName");
            String pingString = data.getString("pingString");
            doPing(groupName, pingString);
            break;
        }
        case DISCONNECT: {
            doDisconnect();
            break;
        }
        default:
            break;
        }
    }

    

	private void doInit() {
		PeerGroupListener pgListener = new PeerGroupListener() {
        	@Override
            public void foundAdvertisedName(String groupName, short transport) {
                updatePingGroupList();
            }
            

            @Override
            public void lostAdvertisedName(String groupName, short transport) {
                updatePingGroupList();
            }
            
            @Override
            public void groupLost(String groupName) {
                updatePingGroupList();
            }
            
            @Override
            public void peerAdded(String busId, String groupName, int numParticipants){
            	System.out.println(busId);
            	updatePingGroupList();
            }
        };
        
        ArrayList<BusObjectData> busObjects = new ArrayList<BusObjectData>();
        busObjects.add(new BusObjectData(mSimpleService, "/SimpleService"));
        mGroupManager = new PeerGroupManager(SERVICE_NAME, pgListener, busObjects);
        mGroupManager.registerSignalHandlers(this);
        updatePingGroupList();
    }
    
    private void doDisconnect() {
        mGroupManager.cleanup();
        getLooper().quit();
        sendUiMessage(UIHandler.TOAST_MSG, "Disconnected");
    }

    private void doCreateGroup(String groupName) {
        mUIHandler.logInfo("GroupManager.createGroup(" + groupName + ")");
        Status status = mGroupManager.createGroup(groupName);
        mUIHandler.logStatus("GroupManager.createGroup()", status);
        if (status == Status.OK) {
            updatePingGroupList();
            sendUiMessage(UIHandler.TOAST_MSG, "Hosting Group: " + groupName);
        }
    }

    private void doDestroyGroup(String groupName) {
        mUIHandler.logInfo("About to end group - " + groupName);
        
        Status status = mGroupManager.destroyGroup(groupName);
        
        mUIHandler.logStatus("GroupManager.destroyGroup()", status);  
        if(status == Status.OK){
        	updatePingGroupList();
        	sendUiMessage(UIHandler.TOAST_MSG, "Ended Group: " + groupName);
    	}
    }

    private void doJoinGroup(String groupName) {
        mUIHandler.logInfo("GroupManager.joinGroup(" + groupName + ")");
        
        Status status = mGroupManager.joinGroup(groupName);
        
        mUIHandler.logStatus("GroupManager.joinGroup()", status);     
        if (status == Status.OK) {    
            updatePingGroupList();
            sendUiMessage(UIHandler.TOAST_MSG, "Joined Group: " + groupName);
        }
    }

    private void doLeaveGroup(String groupName) {
        Status status = mGroupManager.leaveGroup(groupName);
        if (status == Status.OK) {  
            updatePingGroupList();
            sendUiMessage(UIHandler.TOAST_MSG, "Leaving Group: " + groupName);
        }
    }
    
    private void doUnlockGroup(String groupName) {
    	Status status = mGroupManager.unlockGroup(groupName);
    	if (status == Status.OK) {  
    		updatePingGroupList();
            sendUiMessage(UIHandler.TOAST_MSG, "Unlocking Group: " + groupName);
        }
		
	}
    
    private void doLockGroup(String groupName) {
    	Status status = mGroupManager.lockGroup(groupName);
    	if (status == Status.OK) {  
    		updatePingGroupList();
            sendUiMessage(UIHandler.TOAST_MSG, "Locking Group: " + groupName);
        }
	}
    
    private void doSetPort(short sessionPort){
    	mGroupManager.setSessionPort(sessionPort);
    	sendUiMessage(UIHandler.TOAST_MSG, "Set Session port: " + sessionPort);
    	updatePingGroupList();
    }
    
    private void doJoinOrCreate(String groupName){
    	mUIHandler.logInfo("GroupManager.joinOrCreateGroup(" + groupName + ")");
        JoinOrCreateReturn status = mGroupManager.joinOrCreateGroup(groupName);
        mUIHandler.logStatus("GroupManager.createGroup()", status.getStatus());
        if (status.getStatus() == Status.OK) {
            updatePingGroupList();
            if (status.isJoiner())
            	sendUiMessage(UIHandler.TOAST_MSG, "Joined Group: " + groupName);
            else
            	sendUiMessage(UIHandler.TOAST_MSG, "Hosting Group: " + groupName);
        }
    }

    private void doPing(String groupName, String message) {
    	// TODO once getSignalInterface is done
        SimpleInterface simpleInterface = mGroupManager.getSignalInterface(groupName, mSimpleService, SimpleInterface.class);

        try {
            if(simpleInterface != null) {
                simpleInterface.Ping(message);
            }
        } catch (BusException e) {
            e.printStackTrace();
        }
    }
    
    /* Helper function to send a message to the UI thread. */
    private void sendUiMessage(int what, Object obj) {
        mUIHandler.sendMessage(mUIHandler.obtainMessage(what, obj));
    }
    
    private void updatePingGroupList() {
    	
        List<String> availableGroups = mGroupManager.listFoundGroups();
        List<String> hostedGroups = mGroupManager.listHostedGroups();
        List<String> joinedGroups = mGroupManager.listJoinedGroups();
        List<String> lockedGroups = mGroupManager.listLockedGroups();
        
        availableGroups.removeAll(hostedGroups);
        availableGroups.removeAll(joinedGroups);
        
        String[] availableGroupArray = availableGroups.toArray(new String[0]);
        String[] hostedGroupArray = hostedGroups.toArray(new String[0]);
        String[] joinedGroupArray = joinedGroups.toArray(new String[0]);
        String[] lockGroupArray = lockedGroups.toArray(new String[0]);

        Message msg = mUIHandler.obtainMessage(UIHandler.UPDATE_GROUP_LIST_SPINNER);
        Bundle data = new Bundle();
        
        data.putStringArray("availableGroupList", availableGroupArray);
        data.putStringArray("hostedGroupList", hostedGroupArray);
        data.putStringArray("joinedGroupList", joinedGroupArray);
        data.putStringArray("lockedGroupList", lockGroupArray);
        
        msg.setData(data);
        mUIHandler.sendMessage(msg);
    }
    
    /*
     * Method calls called by the dialogs
     */
    public ArrayList<String> listGroups() {
        return mGroupManager.listFoundGroups();
    }
    
    public ArrayList<String> listHostedGroups() {
        return mGroupManager.listHostedGroups();
    }
    
    public ArrayList<String> listJoinedGroups() {
        return mGroupManager.listJoinedGroups();
    }
    
    public ArrayList<String> listLockedGroups() {
		return mGroupManager.listLockedGroups();
	}
    
    public ArrayList<String> getParticipants(String groupName) {
        return mGroupManager.getPeers(groupName);
    }
    
    

    /*
     * Simple class with the empty Ping signal
     */
    class SimpleService implements SimpleInterface, BusObject {
        public void Ping(String Str) {
        }        
    }
    
    /*
     * Signal Handler for the Ping signal
     */
    @BusSignalHandler(iface = "org.alljoyn.bus.samples.simple.SimpleInterface", signal = "Ping")
    public void Ping(String str) {
    	// TODO once getSenderBusId() is done
        sendUiMessage(UIHandler.TOAST_MSG, "User" + mGroupManager.getSenderPeerId() + "  Message: " + str);
    }

	
}
