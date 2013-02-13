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

package org.alljoyn.cops.peergroupmanager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;


/**
 * The Peer Group Manager is a library that provides app developers with an
 * easy entry point into peer-to-peer programming using AllJoyn. A peer group
 * is simply an established connection between peers that allows them to
 * communicate over AllJoyn. With this library, developers can use AllJoyn with
 * minimum knowledge of AllJoyn concepts which is great for rapid prototyping.
 * The Peer Group Manager provides a mapping between high level AllJoyn
 * concepts and underlying implementation details abstracting out some of the
 * more subtle aspects that aren’t always relevant to new developers of
 * AllJoyn. It also encapsulates a lot of boilerplate AllJoyn code for typical
 * applications including setting default parameters, enforcing a naming
 * convention for advertisements, etc. making the code simpler and cleaner.
 * This component only covers common use cases for typical AllJoyn applications
 * so if your project is advanced in nature, then this component might not be
 * right for you.
 */
public class PeerGroupManager implements PeerGroupManagerInterface {
	/* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }
    
    private final String TAG = "PeerGroupManager";
    private final short INVALID_SESSION_PORT = -1;
    private BusAttachment bus = null;
    private BusListener pgBusListener;
    private SessionListener pgSessionListener;
    
    // Flag for turning on or off debug messages
    private boolean debugOn = true;
    // Holds the service name (group prefix) for the PeerGroupManager object
    private final String groupPrefix;
    // The default SessionOpts used for all group communication
    private SessionOpts defaultSessionOpts;
    // The default session port set by setSessionPort() to be used for joining
    // legacy AllJoyn apps
    private short defaultSessionPort;
    
    // Groups currently Hosted
    private ArrayList<String> hostedGroups = new ArrayList<String>();
    // Groups currently joined
    private ArrayList<String> joinedGroups = new ArrayList<String>();
    // Stores the available groups with the interested well known name prefix
    private HashMap<String,Short> foundGroups = new HashMap<String,Short>(); 
    // All the groups that are locked
    private ArrayList<String> lockedGroups = new ArrayList<String>();
    // Stores the group names created by JoinOrCreate()
    private ArrayList<String> jocGroups = new ArrayList<String>();
    
    // Stores the PeerGroupListeners
    private ArrayList<PeerGroupListenerInterface> peerGroupListeners = new ArrayList<PeerGroupListenerInterface>();
    // Stores the bus objects registered on the bus
    private ArrayList<BusObject> registeredBusObjects = new ArrayList<BusObject>();
    // Stores classes which contain signal handlers to be registered
    private ArrayList<Object> classesWithSignalHandlers = new ArrayList<Object>();
    
    // Map session Ids to peers
    private HashMap<Integer,ArrayList<String>> sessionIdToPeers = new HashMap<Integer,ArrayList<String>>();
    
    // Map group names to session Ids
    private HashMap<String,Integer> groupNameToSessionId = new HashMap<String,Integer>();
    // Map session ids to group names
    private HashMap<Integer,String> sessionIdToGroupName = new HashMap<Integer,String>();
    
    // HashMaps to map hosted group names to session ports
    private HashMap<String,Short> groupNameToSessionPort = new HashMap<String,Short>();
    // HashMaps to map hosted session ports to group names
    private HashMap<Short,String> sessionPortToGroupName = new HashMap<Short,String>();
    
    private static HandlerThread callbackHandler = HandlerThread.create();
    
    private static class HandlerThread extends Thread {
        private Handler handler;
        
        public static HandlerThread create() {
            HandlerThread handlerThread = new HandlerThread();
            
            handlerThread.setPriority(Thread.MAX_PRIORITY);
            handlerThread.start();

            return handlerThread;
        }
        
        public void run() {
            Looper.prepare();
            handler = new Handler();
            Looper.loop();
        }
        
        public void post(Runnable r) {
            handler.post(r);
        }
    }
        
    /*------------------------------------------------------------------------*
     * Constructors
     *------------------------------------------------------------------------*/
	/**
     * Construct a PeerGroupManager and register the provided PeerGroupListener 
     * and bus objects. The PeerGroupManager will also automatically connect to
     * the AllJoyn bus and start discovering groups created by other 
     * PeerGroupManagers that use the same group prefix.
     * 
     * @param groupPrefix  the prefix of the advertised name that will be used 
     *                     for advertisement and discovery.
     *                     NOTE: Your PeerGroupManager will only be able to 
     *                     communicate with other PeerGroupManagers that use 
     *                     the same group prefix.
     * @param pgListener   the PeerGroupListener to register. This can be null.
     * @param busObjects   the bus objects to register. This can be null. If 
     *                     null, then this instance of the PeerGroupManager
     *                     will be a pure client with no bus objects since bus 
     *                     objects cannot be registered beyond this point.
     */
    public PeerGroupManager (String pgPrefix, PeerGroupListenerInterface pgListener, ArrayList<BusObjectData> busObjects) {
        String methodName = "Constructor()";
        if(isInvalidStringParam(pgPrefix)) {
            logError(methodName, "Invalid name prefix");
            throw new IllegalArgumentException("Invalid name prefix");
        }
        this.bus = new BusAttachment(pgPrefix, BusAttachment.RemoteMessage.Receive);
        this.groupPrefix = pgPrefix.trim() + ".sp";
        addPeerGroupListener(pgListener);
        this.pgBusListener = new PGBusListener();
        this.pgSessionListener = new PGSessionListener();
        this.defaultSessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
        this.defaultSessionPort = INVALID_SESSION_PORT;
        registerBusObjects(busObjects);
        Status status = connectBus();
        if(status != Status.OK) {
            logError(methodName, "Failed to connect: " + status.toString());
            throw new IllegalArgumentException("Failed to connect: " + status.toString());
        }
    }
    
    
    /*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
    /**
     * cleanup
     * will permanently disconnect the PeerGroupManager from the AllJoyn 
     * daemon. Disconnecting includes leaving all currently joined groups, 
     * destroying all currently hosted groups, unregistering all bus objects, 
     * unregistering all listeners, unregistering all signal handlers, 
     * disconnecting from the AllJoyn bus and releasing all resources.
     * NOTE: This renders the PeerGroupManager instance unusable and cannot be
     * reversed. It is a programming error to call another method on the 
     * PeerGroupManager after the cleanup() method has been called. A new 
     * instance of the PeerGroupManager will need to be created if you wish to
     * continue using it. 
     */
    @Override
    public synchronized void cleanup() {
        String methodName = "cleanup()";
        logInfo(methodName, "");
        
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return;
        }
        
        // Leave all joined groups
        logInfo(methodName, "Leaving all joined groups");
        ArrayList<String> currentlyJoinedGroups = listJoinedGroups();
        if (!currentlyJoinedGroups.isEmpty()) {
            for (String groupName : currentlyJoinedGroups) {
                leaveGroup(groupName);
            }
        }
        joinedGroups.clear();
        
        // Destroy all hosted group
        logInfo(methodName, "Destroying all hosted groups");
        ArrayList<String> currentlyHostedGroups = listHostedGroups();
        if(!currentlyHostedGroups.isEmpty()) {
            for (String groupName : currentlyHostedGroups) {
                destroyGroup(groupName);
            }
        }
        hostedGroups.clear();
        lockedGroups.clear();
        
        // Unregister the all app defined bus objects
        logInfo(methodName, "Unregistering All Bus Objects");
        unregisterAllBusObjects();
        
        // Unregister the bus listener
        logInfo(methodName, "Unregistering Bus Listener");
        bus.unregisterBusListener(pgBusListener);
        peerGroupListeners.clear();
        
        // Unregister all signal handlers
        logInfo(methodName, "Unregistering Signal Handlers");
        for(Object classObj : classesWithSignalHandlers) {
            bus.unregisterSignalHandlers(classObj);
        }
        classesWithSignalHandlers.clear();
        
        // Stop Discovery
        if(defaultSessionPort == INVALID_SESSION_PORT) {
            // If the default session port was never set, stop discovery on groupPrefix.sp
            bus.cancelFindAdvertisedName(groupPrefix);
        }
        else {
            // If the default session port was set, stop discovery on groupPrefix
            bus.cancelFindAdvertisedName(groupPrefix.substring(0, groupPrefix.lastIndexOf(".")));
        }
        logInfo(methodName, "Stopping Discovery");
        foundGroups.clear(); 
        
        // Disconnect the bus attachment
        bus.disconnect();
        logInfo(methodName, "Disconnecting Bus Attachment");
        
        // Release all AllJoyn resources immediately
        bus.release();
        logInfo(methodName, "Releasing Resources");
        
        // Clean up all of our lists
        jocGroups.clear();
        registeredBusObjects.clear();
        sessionIdToPeers.clear();
        groupNameToSessionId.clear();
        sessionIdToGroupName.clear();
        groupNameToSessionPort.clear();
        sessionPortToGroupName.clear();
        
        bus = null;
    }
    
    /**
     * createGroup
     * creates an unlocked group and advertises it for other peers to discover 
     * and join. You can create as many groups as you want given that all the 
     * group names are unique. If you try to create a group with a name that 
     * already exists, the call will fail. 
     * NOTE: You are inherently a peer of your own groups so you do not need to
     * call joinGroup() on groups you create.
     * 
     * @param groupName  the name of the group to create
     * @return  OK if successful
     */
    @Override
    public synchronized Status createGroup(String groupName) {
        return createGroup(groupName, false);
    }

    /**
     * createGroup
     * creates a locked or unlocked group. You can create as many groups as you
     * want given that all the group names are unique. If you try to create a 
     * group with a name that already exists, the call will fail. 
     * NOTE: You are inherently a peer of your own groups so you do not need to
     * call joinGroup() on groups you create.
     * 
     * @param groupName  the name of the group to create.
     * @param locked     If set to true, the group will be locked preventing 
     *                   peers from discovering and joining the group until it
     *                   is unlocked. If set to false, the group will be 
     *                   unlocked allowing peers to discover and join the 
     *                   group.
     * @return  OK if successful
     */
    @Override
    public synchronized Status createGroup(String groupName, boolean locked) {      
        String methodName = "createGroup()";
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return Status.FAIL;
        }
        
        logInfo("createGroup(" + groupName + "," + locked + ")", "");
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return Status.FAIL;
        }
        
        Mutable.ShortValue sessionPort = new Mutable.ShortValue(BusAttachment.SESSION_PORT_ANY);
        PGSessionPortListener pgSessionPortListener = new PGSessionPortListener();
        
        // Bind the session Port
        Status status = bus.bindSessionPort(sessionPort, defaultSessionOpts, pgSessionPortListener);
        logInfo(methodName, "Binding Session Port " + sessionPort.value + " - " + status.toString());
        if(status != Status.OK) {
            return status;
        }
        
        // Make sure the group name isn't already taken
        if(listFoundGroups().contains(groupName) || listHostedGroups().contains(groupName) 
                || listJoinedGroups().contains(groupName)) {
            logInfo(methodName, groupName + " is already taken");
            status = Status.FAIL;
        }
        else {
            logInfo(methodName, groupName + " is available");
            // Build the advertised name with the session port 
            String advertisedName = getAdvertisedName(groupName, sessionPort.value);
            // Request the Advertised Name
            int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
            status = bus.requestName(advertisedName, flag);
            logInfo(methodName, "Requesting name " + advertisedName + " - " + status.toString());
            if(status == Status.OK) {
                // Add the new group to the list of hosted groups
                hostedGroups.add(groupName);
                // Lock the group if told to do so
                if(locked) {
                    lockedGroups.add(groupName);
                }
                // Otherwise advertise the group
                else {
                    // Advertise the well known name of the group
                    status = bus.advertiseName(advertisedName, defaultSessionOpts.transports);
                    logInfo(methodName, "Advertising name " + advertisedName + " - " + status.toString());
                }
                // The status check is here in case we advertise the session
                if(status == Status.OK) {
                    // Map the newly created group to the session port
                    addGroupNameToSessionPort(groupName, sessionPort.value);
                    // Successful return here
                    return status;
                }
                // Fall through and cleanup on failure
                hostedGroups.remove(groupName);
                bus.releaseName(advertisedName);
            }
        }
        bus.unbindSessionPort(sessionPort.value);
        return status;
    }

    /**
     * destroyGroup
     * stops the advertisement of a group you are hosting and destroys it.
     * 
     * @param groupName  the name of the group to destroy
     * @return  OK if successful
     */
    @Override
    public synchronized Status destroyGroup(String groupName) {
        String methodName = "destroyGroup()";
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return Status.FAIL;
        }
        
        logInfo("destroyGroup(" + groupName + ")", "");
        Status status = Status.FAIL;
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return status;
        }
        
        if(groupNameToSessionPort.containsKey(groupName)) {
            short sessionPort = groupNameToSessionPort.get(groupName);
            String advertisedName = getAdvertisedName(groupName, sessionPort);
            
            // If the group is locked, we don't need to cancel the advertisement
            if(!lockedGroups.contains(groupName)) {
                // Stop advertising the group
                status = bus.cancelAdvertiseName(advertisedName, defaultSessionOpts.transports);
                logInfo(methodName, "Canceling Advertised Name " + advertisedName);
                if(status != Status.OK) {
                    return status;
                }
            }
            
            // Release the well known name
            status = bus.releaseName(advertisedName);
            logInfo(methodName, "Releasing Name " + advertisedName);
            if(status != Status.OK) {
                // Restore the advertisement if the group isn't locked
                if(!lockedGroups.contains(groupName)) {
                    bus.advertiseName(advertisedName, defaultSessionOpts.transports);
                }
                return status;
            }

            // Unbind the session port
            status = bus.unbindSessionPort(sessionPort);
            logInfo(methodName, "Unbinding Session Port " + sessionPort);
            if(status != Status.OK) {
                // Re-request the advertised name
                int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
                bus.requestName(advertisedName, flag);
                // Restore the advertisement if the group isn't locked 
                if(!lockedGroups.contains(groupName)) {
                    bus.advertiseName(advertisedName, defaultSessionOpts.transports);
                }
                return status;
            }
            
            // Remove the group from the list of locked groups
            lockedGroups.remove(groupName);
            
            if(groupNameToSessionId.containsKey(groupName)) {
                int sessionId = groupNameToSessionId.get(groupName);
                // Clear the list of participants for this session
                clearPeers(sessionId);
                removeGroupNameToSessionId(groupName);
            }
            
            // Remove the group from the list of hosted groups
            hostedGroups.remove(groupName); 
            // Remove the group from the group name to session port mapping
            removeGroupNameToSessionPort(groupName);
            
            /* 
             * If the group is a JoinOrCreate group, then remove it from
             * the list of JoC groups 
             */ 
            removeJoCGroup(groupName);
        }

        return status;
    }
    
    /**
     * unlockGroup
     * unlocks the specified group allowing peers to discover and join it. You
     * can only unlock groups that you have created and are currently hosting.
     * 
     * @param groupName  the hosted group to unlock
     * @return  OK if successful     
     */
    @Override
    public synchronized Status unlockGroup(String groupName) {
        String methodName = "unlockGroup()";
        Status status = Status.FAIL;
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return status;
        }
        logInfo("unlockGroup(" + groupName + ")", "");
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return status;
        }
        
        // You can only unlock groups you are hosting
        if(!listHostedGroups().contains(groupName)) {
            logInfo(methodName, "You are not hosting the group - " + groupName);
            return status;
        }
        
        // If the group is already unlocked then do nothing
        if(!lockedGroups.contains(groupName)) {
            return Status.OK;
        }
        
        if(groupNameToSessionPort.containsKey(groupName)) {
            String advertisedName = getAdvertisedName(groupName, groupNameToSessionPort.get(groupName));
            // Advertise the well known name of the group
            status = bus.advertiseName(advertisedName, defaultSessionOpts.transports);
            if(status == Status.OK) {
                // Unlock the group if it was previously locked
                lockedGroups.remove(groupName);
            }
            logInfo(methodName, advertisedName + " - " + status.toString());
        }
        return status;
    }
    
    /**
     * lockGroup
     * locks the specified group preventing peers from discovering and joining 
     * it until unlock() is called. You can only lock groups that you have 
     * created and are currently hosting.
     * 
     * @param groupName  the hosted group to lock
     * @return  OK if successful     
     */
    @Override
    public synchronized Status lockGroup(String groupName) {
        String methodName = "lockGroup()";
        Status status = Status.FAIL;
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return status;
        }
        logInfo("lockGroup(" + groupName + ")", "");
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return status;
        }
        
        
        // You can only lock groups you are hosting
        if(!listHostedGroups().contains(groupName)) {
            logInfo(methodName, "You are not hosting the group - " + groupName);
            return status;
        }
        
        // If the group is already locked then do nothing
        if(lockedGroups.contains(groupName)) {
            return Status.OK;
        }
        
        if(groupNameToSessionPort.containsKey(groupName)) {
            String advertisedName = getAdvertisedName(groupName, groupNameToSessionPort.get(groupName));
            // Stop advertising the well known name of the group
            status = bus.cancelAdvertiseName(advertisedName, defaultSessionOpts.transports);
            if(status == Status.OK) {
                // Add the group to the list of locked groups
                lockedGroups.add(groupName);
            }
            logInfo(methodName, advertisedName + " - " + status.toString());
        }
        return status;
    }

    /**
     * joinGroup
     * joins an existing unlocked group.
     * NOTE: You cannot join groups that you have created because you are 
     * already inherently a peer in your own groups.
     * 
     * @param groupName  the name of the group to join
     * @return  OK if successful
     */
    @Override
    public synchronized Status joinGroup (String groupName) {
        String methodName = "joinGroup()";
        Status status = Status.FAIL;
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return status;
        }
        logInfo("joinGroup(" + groupName + ")", "");
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return status;
        }
        
        // You should not join your own group because you are implicitly a peer
        if(hostedGroups.contains(groupName)) {
            logInfo(methodName, "You are already a peer of your own group - " + groupName);
            return status;
        }
        
        // Check if the group is being advertised
        if(!foundGroups.containsKey(groupName)) {
            logInfo(methodName, "Group not found - " + groupName);
            return status;
        }
        
        // Get the advertised name of the group
        short sessionPort = foundGroups.get(groupName);
        String advertisedName = getAdvertisedName(groupName, sessionPort);
        Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
        
        // Join the group
        status = bus.joinSession(advertisedName, sessionPort, sessionId, defaultSessionOpts, pgSessionListener);
        logInfo(methodName, "joinSession(" + advertisedName + ", " + sessionPort + ")" + " - " + status.toString());
        if(status == Status.OK) {
            /* 
             * Add yourself to your list of peers for the group because 
             * sessionMemberAdded wont be triggered for yourself
             */
            addPeer(sessionId.value, bus.getUniqueName());
            // Add the group to the list of joined groups
            joinedGroups.add(groupName);
            addGroupNameToSessionId(groupName, sessionId.value);
        }
        // Try joining with the default session port
        else if(defaultSessionPort != INVALID_SESSION_PORT){
            Status prevStatus = status;
            advertisedName = getLegacyAdvertisedName(groupName);
            // Try joining with the defaultSessionPort
            status = bus.joinSession(advertisedName, defaultSessionPort, sessionId, defaultSessionOpts, pgSessionListener);
            logInfo(methodName, "joinSession(" + advertisedName + ", " + defaultSessionPort + ")" + " - " + status.toString());
            if(status == Status.OK) {
                /* 
                 * Add yourself to your list of peers for the group because 
                 * sessionMemberAdded wont be triggered for yourself
                 */
                addPeer(sessionId.value, bus.getUniqueName());
                // Add the group to the list of joined groups
                joinedGroups.add(groupName);
                addGroupNameToSessionId(groupName, sessionId.value);
            }
            else {
                // Return the original failed status if both join attempts fail
                status = prevStatus;
            }
        }
        return status;
    }

    /**
     * leaveGroup
     * leaves the specified group you are joined to.
     * NOTE: You cannot leave groups that you have created because you are 
     * inherently a peer in your own groups.
     * 
     * @param groupName  the name of the group to leave
     * @return  OK if successful
     */
    @Override
    public synchronized Status leaveGroup (String groupName) {
        String methodName = "leaveGroup()";
        Status status = Status.FAIL;
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return status;
        }
        logInfo("leaveGroup(" + groupName + ")", "");
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return status;
        }
        
        // You cannot leave your own group
        if(hostedGroups.contains(groupName)) {
            logInfo(methodName, "You cannot leave your own group - " + groupName);
            return status;
        }
        
        // Get the sessionId of the group
        int sessionId = getSessionId(groupName);
        // Leave the group
        status = bus.leaveSession(sessionId);
        if(status == Status.OK) {
            removeGroupNameToSessionId(groupName);
            clearPeers(sessionId);
            joinedGroups.remove(groupName);
        }
        logInfo(methodName, "leaveSession(" + sessionId + ") - " + status.toString());
        return status;
    }

    /**
     * joinOrCreateGroup
     * joins the specified group if it exists, otherwise creates it.
     * NOTE: There are opportunities for race conditions to occur during 
     * startup where two peers might not see each other and both create the 
     * group. This method is intended to be used for test and developmental 
     * purposes only.
     * 
     * @param groupName  the name of the group to join or create
     * @return  JoinOrCreateReturn  an object containing a flag to tell whether
     *                              the method tried to join or create 
     *                              a group and the AllJoyn return status of 
     *                              the operation
     */
    @Override
    public synchronized JoinOrCreateReturn joinOrCreateGroup(String groupName) {
        String methodName = "joinOrCreateGroup()";
        Status status = Status.FAIL;
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return new JoinOrCreateReturn(status, true);
        }
        logInfo("joinOrCreateGroup(" + groupName + ")", "");
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return new JoinOrCreateReturn(status, true);
        }
        
        String masterGroupName = getJoCMasterGroupName(groupName);
        String guidGroupName = getJoCGuidGroupName(groupName);
        
        // If we're hosting the master group we're already joined
        if(listHostedGroups().contains(masterGroupName)) {
            logInfo(methodName, "Master group already created");
            return new JoinOrCreateReturn(Status.FAIL, false);
        }
        
        // If we see the master group then just join it
        if(listFoundGroups().contains(masterGroupName)) {
            logInfo(methodName, "Joining master group");
            status = joinGroup(masterGroupName);
            return new JoinOrCreateReturn(status, true);
        }
        
        // Create a group with your GUID if the main group wasn't found
        status = createGroup(guidGroupName);
        logInfo(methodName, "Creating group with GUID - " + status.toString());
        if(status != Status.OK) {
            return new JoinOrCreateReturn(status, false);
        }
        // Store the JoC group name
        addJoCGroup(groupName);
        
        // Pause the thread for 2 seconds to let the foundAdvertiseName() signals come through
        logInfo(methodName, "Pausing Thread");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logError(methodName, e.toString());
        }
        
        ArrayList<String> availableGroups = listFoundGroups();
        
        // Join the master group if it exists and we are not hosting it
        if(availableGroups.contains(masterGroupName) && !listHostedGroups().contains(masterGroupName)) {
            logInfo(methodName, "Joining master group");
            status = joinGroup(masterGroupName);
            // Clean up your GUID group
            destroyGroup(guidGroupName);
            return new JoinOrCreateReturn(status, true);
        }
        
        // Determine the group with the Highest GUID
        String groupWithHighestGuid = guidGroupName;
        for(String group : availableGroups) {
            // Only check GUID groups that are for this JoC group
            if(group.matches(groupName + ".JoC-\\S+")) {
                if(groupWithHighestGuid.compareToIgnoreCase(group) < 0) {
                    groupWithHighestGuid = group;
                }
            }
        }
        logInfo(methodName, "Highest GUID group is " + groupWithHighestGuid);
        
        // Check for master group again
        availableGroups = listFoundGroups();
        
        // Join the master group if it exists and we are not hosting it
        if(availableGroups.contains(masterGroupName) && !listHostedGroups().contains(masterGroupName)) {
            logInfo(methodName, "Joining master group");
            status = joinGroup(masterGroupName);
            // Clean up your GUID group
            destroyGroup(guidGroupName);
            return new JoinOrCreateReturn(status, true);
        }
        
        // Return if someone has joined your group
        if(listHostedGroups().contains(masterGroupName)) {
            logInfo(methodName, "Someone joined my group. I am the Host");
            return new JoinOrCreateReturn(Status.OK, false);
        }
        
        /* 
         * Advertise the master group name and return if you have the highest 
         * GUID group and no one has joined it yet
         */
        if(groupWithHighestGuid.equals(guidGroupName)) {
            logInfo(methodName, "I have the highest Guid. I am the Host");
            changeGroupName(guidGroupName, masterGroupName);
            return new JoinOrCreateReturn(Status.OK, false);
        }
        else {
            // Join the group with the highest GUID
            logInfo(methodName, "Joining group " + groupWithHighestGuid);
            status = joinGroup(groupWithHighestGuid);

            // Swap out the GUID group name for the master group name
            joinedGroups.remove(groupWithHighestGuid);
            joinedGroups.add(masterGroupName);
            int sessionId = groupNameToSessionId.get(groupWithHighestGuid);
            removeGroupNameToSessionId(groupWithHighestGuid);
            addGroupNameToSessionId(masterGroupName, sessionId);
            
            // Clean up your GUID group
            logInfo(methodName, "Destroying my GUID group");
            destroyGroup(guidGroupName);
            return new JoinOrCreateReturn(status, true);
        }
    }
    
    /** 
     * getPeers
     * gets the PeerIds of all peers in the specified group. You must be a
     * participant in the group (you either created or joined the group) to 
     * be able to get the peers of that group.
     * 
     * @param groupName  the name of the group to get the peers of
     * @return  a list of PeerIds of all peers in the group
     */
    @Override
    public synchronized ArrayList<String> getPeers(String groupName) {
        String methodName = "getPeers()";
        ArrayList<String> clone = new ArrayList<String>();
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return clone;
        }
        int sessionId = getSessionId(groupName);
        logInfo("getPeers(" + groupName + ")", "SessionID - " + sessionId);
        
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return clone;
        }
        
        /* 
         * If we are hosting the group, but there is no session Id yet then we are
         * the only ones in the group
         */
        if(listHostedGroups().contains(groupName) && sessionId == -1) {
            clone.add(bus.getUniqueName());
            return clone;
        }
        // Get the list of peers for the group
        if(sessionIdToPeers.containsKey(sessionId)) {
            ArrayList<String> peerList = sessionIdToPeers.get(sessionId);
            if(peerList != null) {
                // Create and return a deep copy of the peer list for the specified group
                for (String peer : peerList) {
                    clone.add(new String(peer));
                }
            }
        }
        logInfo(methodName, clone.toString());
        return clone;
    }
    
    /** 
     * getNumPeers
     * gets the number of peers in the specified group. You must be a
     * participant in the group (you either created or joined the group) to 
     * be able to get the number of peers of that group.
     * 
     * @param groupName  the name of the group to get the number of peers of
     * @return  the number of peers in the group or -1 if the 
     *          groupName is invalid
     */
    @Override
    public synchronized int getNumPeers(String groupName) {
        int size = -1;
        if(isInvalidStringParam(groupName)) {
            logInfo("getNumPeers()", "Invalid group name");
            return size;
        }
        
        size = getPeers(groupName).size();
        logInfo("getNumPeers(" + groupName + ")", "numPeers - " + size);
        return size;
    }

    /** 
     * listFoundGroups
     * lists the group names of all of the discovered unlocked groups excluding
     * your own hosted groups.
     * 
     * @return  a list of group names of all the found group
     */
    @Override
    public synchronized ArrayList<String> listFoundGroups() {
        // Create and return a deep copy of the group list
        ArrayList<String> clone = new ArrayList<String>(foundGroups.size());
        for (String groupName : foundGroups.keySet()) {
            clone.add(new String(groupName));
        }
        logInfo("listFoundGroups()", clone.toString());
        return clone;
    }
    
    /**
     * listHostedGroups
     * lists the group names of all the groups you have created and are 
     * currently hosting.
     * 
     * @return  a list of the group names of all the groups you are currently 
     *          hosting
     */
    @Override
    public synchronized ArrayList<String> listHostedGroups() {
        // Create and return a deep copy of the hosted group list
        ArrayList<String> clone = new ArrayList<String>(hostedGroups.size());
        for (String groupName : hostedGroups) {
            clone.add(new String(groupName));
        }
        logInfo("listHostedGroups()", clone.toString());
        return clone;
    }
    
    /**
     * listJoinedGroups
     * lists the group names of all the groups you have successfully joined.
     * 
     * @return  a list of the group names of all the groups you are currently 
     *          joined to
     */
    @Override
    public synchronized ArrayList<String> listJoinedGroups() {
        // Create and return a deep copy of the joined group list
        ArrayList<String> clone = new ArrayList<String>(joinedGroups.size());
        for (String groupName : joinedGroups) {
            clone.add(new String(groupName));
        }
        logInfo("listJoinedGroups()", clone.toString());
        return clone;
    }
    
    /**
     * listLockedGroups
     * lists the group names of all the groups you are hosting that are 
     * currently locked.
     * 
     * @return  a list of the group names of all the groups you are hosting
     *          that are currently locked
     */
    @Override
    public synchronized ArrayList<String> listLockedGroups() {
        // Create and return a deep copy of the locked group list
        ArrayList<String> clone = new ArrayList<String>(lockedGroups.size());
        for (String groupName : lockedGroups) {
            clone.add(new String(groupName));
        }
        logInfo("listLockedGroups()", clone.toString());
        return clone;
    }
    
    /**
     * registerSignalHandlers
     * registers all annotated signal handlers in the specified class.
     * 
     * @param classWithSignalHandlers  the class containing the signal handlers 
     *                                 to be registered
     * @return  OK if successful 
     */
    @Override
    public synchronized Status registerSignalHandlers(Object classWithSignalHandlers) {
        // Register all the signal handlers in the given class
        Status status = Status.FAIL;
        
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo("registerSignalHandlers()", "Bus Attachment has already been disconnected");
            return status;
        }
        
        if(classWithSignalHandlers != null) {
            status = bus.registerSignalHandlers(classWithSignalHandlers);
            if(status == Status.OK && !classesWithSignalHandlers.contains(classWithSignalHandlers)) {
                classesWithSignalHandlers.add(classWithSignalHandlers);
            }
        }
        return status;
    }
    
    /**
     * addPeerGroupListener
     * adds an additional listener to detect the foundAdvertisedName(), 
     * lostAdvertisedName(), groupLost(), peerAdded(), and 
     * peerRemoved() signals. Multiple listeners can be added at any time 
     * without affecting the previous listeners.
     * 
     * @param peerGroupListener  the listener with the desired call-back methods 
     *                           to invoke
     */
    @Override
    public synchronized void addPeerGroupListener(PeerGroupListenerInterface peerGroupListener) {
        if(peerGroupListener != null) {
            peerGroupListeners.remove(peerGroupListener);
            peerGroupListeners.add(peerGroupListener);
        }
    }
    
    /**
     * getRemoteObjectInterface
     * gets a proxy to be used for making remote method calls. This is a proxy 
     * to a remote bus object implementing the specified interface. This object
     * is only valid for the specified group.
     * 
     * @param peerId      the id of the peer to make the remote method calls on
     * @param groupName   the name of the group to communicate over
     * @param objectPath  the object path of the remote bus object
     * @param iface       the interface object defining the remote method call
     * @return  a proxy to a remote bus object that can be used for making 
     *          remote method calls or null if an error occurred
     */
    @Override
    public synchronized <T> T getRemoteObjectInterface(String peerId, String groupName, String objectPath, Class<T> iface) {
        String methodName = "getRemoteObjectInterface()";
        logInfo(methodName, "");
        
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return null;
        }
        
        if(isInvalidStringParam(peerId)) {
            logInfo(methodName, "Invalid peer Id");
            return null;
        }
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return null;
        }
        if(objectPath == null) {
            logInfo(methodName, "Invalid object path");
            return null;
        }
        if(iface == null) {
            logInfo(methodName, "Invalid interface");
            return null;
        }
        ProxyBusObject proxy = bus.getProxyBusObject(peerId, objectPath, getSessionId(groupName), new Class<?>[] {iface});
        return proxy.getInterface(iface);
    }
    
    /**
     * getSignalInterface
     * gets the interface object from a bus object to be used for emitting  
     * signals to a group. This object is only valid for the specified group.
     * 
     * @param groupName  the name of the group to emit signals over
     * @param busObject  the bus object that implements the interface 
     *                   containing the signals
     * @param iface      the interface object defining the signal
     * @return  an interface object that can be used to emit signals to a group
     *          or null if an error occurred 
     */
    @Override
    public synchronized <T> T getSignalInterface(String groupName, BusObject busObject, Class<T> iface) {
        String methodName = "getSignalInterface()";
        logInfo(methodName, "");
        
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return null;
        }
        
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return null;
        }
        if(busObject == null) {
            logInfo(methodName, "Invalid bus object");
            return null;
        }
        if(iface == null) {
            logInfo(methodName, "Invalid interface");
            return null;
        }
        SignalEmitter emitter = new SignalEmitter(busObject, getSessionId(groupName), SignalEmitter.GlobalBroadcast.Off);
        return emitter.getInterface(iface);
    }
    
    /**
     * getSignalInterface
     * gets the interface object from a bus object to be used for emitting
     * directed signals to a peer. This object is only valid over the specified 
     * group.
     * 
     * @param peerId     the id of the peer to send signals to
     * @param groupName  the name of the group to emit signals over
     * @param busObject  the bus object that implements the interface 
     *                   containing the signals
     * @param iface      the interface object defining the signal
     * @return  an interface object that can be used to emit directed signals 
     *          to a peer or null if an error occurred
     */
    @Override
    public synchronized <T> T getSignalInterface(String peerId, String groupName, BusObject busObject, Class<T> iface) {
        String methodName = "getSignalInterface()";
        logInfo(methodName, "");
        
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return null;
        }
        
        if(isInvalidStringParam(peerId)) {
            logInfo(methodName, "Invalid peer Id");
            return null;
        }
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return null;
        }
        if(busObject == null) {
            logInfo(methodName, "Invalid bus object");
            return null;
        }
        if(iface == null) {
            logInfo(methodName, "Invalid interface");
            return null;
        }
        SignalEmitter emitter = new SignalEmitter(busObject, peerId, getSessionId(groupName), SignalEmitter.GlobalBroadcast.Off);
        return emitter.getInterface(iface);
    }
    
    /**
     * registerModule
     * registers a module with the PeerGroupManager to allow it to communicate
     * over the specified group. This will provide the module with the
     * BusAttachment and the session Id for the group.
     * 
     * @param module     the module to register
     * @param groupName  the name of the group to give the module access to
     * @return  OK if successful
     */
    @Override
    public synchronized Status registerModule(PGModule module, String groupName) { 
        String methodName = "registerModule()";
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return Status.FAIL;
        }
        logInfo("registerModule(" + groupName + ")", "");
        
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return Status.FAIL;
        }
        
        return module.register(bus, getSessionId(groupName));
    }
    
    /**
     * getGroupPrefix
     * gets the group prefix used when creating the PeerGroupManager.
     * 
     * @return  the group prefix used when creating the PeerGroupManager
     */
    @Override
    public String getGroupPrefix() {
        return groupPrefix.substring(0, groupPrefix.lastIndexOf("."));
    }
    
    /**
     * getMyPeerId
     * returns the user's own peer id. This is the id that peers receive in 
     * the peerAdded() and peerRemoved() call-back methods of the 
     * PeerGroupManagerListener when you join or leave a group.
     * 
     * @return  the user's peer id or null if an error occurred
     */
    @Override
    public synchronized String getMyPeerId() {
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo("getMyPeerId()", "Bus Attachment has already been disconnected");
            return null;
        }
        return bus.getUniqueName();
    }
    
    /**
     * getGUID
     * returns the user's globally unique id. The returned value may be 
     * appended to a group name in order to guarantee that the resulting name 
     * is globally unique.
     * 
     * @return  the user's globally unique id
     */
    @Override
    public synchronized String getGUID() {
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo("getGUID()", "Bus Attachment has already been disconnected");
            return null;
        }
        return bus.getGlobalGUIDString();
    }
    
    /**
     * getSenderPeerId
     * gets the peer id of the peer that triggered the currently executing
     * method or signal handler. This is to be used in the context of a bus 
     * method or a signal handler.
     * 
     * @return  the peer id of the peer that triggered the currently executing
     *          method or signal handler or null if used out of context.
     */
    @Override
    public synchronized String getSenderPeerId() {
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo("getSenderPeerId()", "Bus Attachment has already been disconnected");
            return null;
        }
        return bus.getMessageContext().sender;
    }
    
    /**
     * getGroupHostPeerId
     * gets the peer id of the group host.
     * 
     * @param groupName  the name of the group to get the host's peer id from
     * @return  the peer id of the group host or null if the group is not found 
     */
    @Override
    public synchronized String getGroupHostPeerId(String groupName) {
        String methodName = "getGroupHostPeerId()";
        String peerId = null;
        if(isInvalidStringParam(groupName)) {
            logInfo(methodName, "Invalid group name");
            return peerId;
        }
        logInfo("getGroupHostPeerId(" + groupName + ")", "");
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return null;
        }
        
        if(foundGroups.containsKey(groupName) || joinedGroups.contains(groupName)) {
            try {
                peerId = bus.getDBusProxyObj().GetNameOwner(getAdvertisedName(groupName, foundGroups.get(groupName)));
            } catch (BusException e) {
                e.printStackTrace();
                logError(methodName, e.toString());
                /* 
                 * If the groupPrefix.sp.groupName advertisement is not owned, 
                 * try getting the host of the groupPrefix.groupName 
                 * advertisement for legacy AllJoyn advertisements
                 */ 
                if(defaultSessionPort != INVALID_SESSION_PORT) {
                    try {
                        peerId = bus.getDBusProxyObj().GetNameOwner(getLegacyAdvertisedName(groupName));
                    } catch (BusException ex) {
                        ex.printStackTrace();
                        logError(methodName, ex.toString());
                    }
                }
            }
        }
        // If you are hosting the group, return your own peer id
        else if(hostedGroups.contains(groupName)) {
            peerId = getMyPeerId();
        }
        else {
            logInfo(methodName, "Group not found");
        }
        return peerId;
    }
    
    /**
     * setSessionPort
     * sets the session port for joining legacy AllJoyn applications that are
     * not using the PeerGroupManager. When joinGroup() is called it will
     * first attempt to retrieve the session port from the group advertisement
     * and join with that. If it fails then it will try to join using the 
     * session port that was set in this method.
     * NOTE: This is an advanced feature. It will also start a new mode of 
     * discovery that will allow the PeerGroupManager to discover 
     * advertisements that do not follow the group advertisement naming 
     * convention and include the session port.
     * 
     * @param sessionPort  the session port to use for joining
     */
    @Override
    public synchronized void setSessionPort(short sessionPort) {
        String methodName = "setSessionPort()";
        logInfo("setSessionPort(" + sessionPort + ")", "");
        
        // Make sure the bus attachment is set up
        if(isBusInvalid()) {
            logInfo(methodName, "Bus Attachment has already been disconnected");
            return;
        }
        
        if(defaultSessionPort == INVALID_SESSION_PORT) {
            /*
             * Set the session port before starting the new discovery so that
             * it is set before the foundAdvertisedName() signal comes through.
             */
            defaultSessionPort = sessionPort;
            String discoveryName = groupPrefix.substring(0, groupPrefix.lastIndexOf("."));
            Status status = bus.findAdvertisedName(discoveryName);
            logInfo(methodName, "findAdvertisedName(" + discoveryName + ") - " + status);
            if(status != Status.OK) {
                defaultSessionPort = INVALID_SESSION_PORT;
                return;
            }
            status = bus.cancelFindAdvertisedName(groupPrefix);
            logInfo(methodName, "cancelFindAdvertisedName(" + groupPrefix + ") - " + status);
        }
        else {
            defaultSessionPort = sessionPort;
        }
    }
    
    
    /*------------------------------------------------------------------------*
     * Private Listener Classes
     *------------------------------------------------------------------------*/
    private class PGBusListener extends BusListener {
        /**
         * This method is called when AllJoyn discovers a remote attachment
         * that is hosting a group.
         */
        @Override
        public void foundAdvertisedName(final String name, final short transport, final String namePrefix) {
            String methodName = "PGBusListener.foundAdvertisedName()";
            logInfo(methodName, "Name - " + name + "  Transport - " + transport);
		    callbackHandler.post(new Runnable() {
		        public void run() {
		            synchronized(PeerGroupManager.this) {
    		            onFoundAdvertisedName(name, transport, namePrefix);
		            }
		        }
		    });
        }
        
        private void onFoundAdvertisedName(String name, short transport, String namePrefix)
        {
            String methodName = "PGBusListener.onFoundAdvertisedName"; 
            logInfo(methodName, "(" + name + ", " + transport + ")");
            String groupName = getGroupName(name);
            // Don't trigger foundAdvertisedName() for our own group advertisements
            if(!listHostedGroups().contains(groupName)) {
                // Check if the newly found group is the master JoC group for a GUID group
                for(String joinedGroup : listJoinedGroups()) {
                    if(joinedGroup.startsWith(groupName) && joinedGroup.contains(".JoC-")) {
                        joinedGroups.remove(joinedGroup);
                        joinedGroups.add(groupName);
                        int sessionId = groupNameToSessionId.get(joinedGroup);
                        removeGroupNameToSessionId(joinedGroup);
                        addGroupNameToSessionId(groupName, sessionId);
                    }
                }
                // Store the group name if it follows the naming convention that
                // includes the session port in the advertisement
                if(name.matches(groupPrefix + "\\d+.\\S+")) {
                    logInfo(methodName, "Triggering foundAdvertisedName() on PeerGroupListeners");
                    // Store the advertised name
                    addFoundGroup(groupName, getSessionPort(name));
                    // Call the listeners on the group name
                    for(PeerGroupListenerInterface listener : peerGroupListeners) {
                        listener.foundAdvertisedName(groupName, transport);
                    }
                }
                // Store the group name if we have the defaultSessionPort set
                else if(defaultSessionPort != INVALID_SESSION_PORT) {
                    logInfo(methodName, "Session Port is set");
                    groupName = name.replaceFirst(groupPrefix.substring(0, groupPrefix.lastIndexOf(".") + 1), "");
                    // Don't trigger foundAdvertisedName() for our own advertisements
                    if(!listHostedGroups().contains(groupName)) {
                        logInfo(methodName, "Triggering foundAdvertisedName() on PeerGroupListeners");
                        // Store the advertised name
                        addFoundGroup(groupName, defaultSessionPort);
                        // Call the listeners on the group name
                        for(PeerGroupListenerInterface listener : peerGroupListeners) {
                            listener.foundAdvertisedName(groupName, transport);
                        }
                    }
                }
            }
        }
        
        /**
         * This method is called when AllJoyn decides that a remote bus
         * attachment that is hosting a group is no longer available.
         */
        @Override
        public void lostAdvertisedName(final String name, final short transport, final String namePrefix) {
            String methodName = "PGBusListener.lostAdvertisedName()"; 
            logInfo(methodName, "Name - " + name + "  Transport - " + transport);
            callbackHandler.post(new Runnable() {
                public void run() {
                    synchronized(PeerGroupManager.this) {
                        onLostAdvertisedName(name, transport, namePrefix);
                    }
                }
            });
        }
        
        private void onLostAdvertisedName(String name, short transport, String namePrefix) {
            String methodName = "PGBusListener.onLostAdvertisedName"; 
            logInfo(methodName, "(" + name + ", " + transport + ")");
            String groupName = getGroupName(name);
            if(!listHostedGroups().contains(groupName)) {
                logInfo(methodName, "Triggering lostAdvertisedName() on PeerGroupListeners");
                // Remove the full advertised name
                removeFoundGroup(groupName);
                // Call the listeners on the group name
                for(PeerGroupListenerInterface listener : peerGroupListeners) {
                    listener.lostAdvertisedName(groupName, transport);
                }
            }
        }
    }
    
    private class PGSessionPortListener extends SessionPortListener {     
        /**
         * This method is called when a peer tries to join the group
         * we have hosted.  It asks us if we want to accept the peer into
         * our group.
         */
        @Override
        public boolean acceptSessionJoiner(final short sessionPort, final String joiner, final SessionOpts sessionOpts) {
            synchronized(PeerGroupManager.this) {
                return onAcceptSessionJoiner(sessionPort, joiner, sessionOpts);
            }
        }
        
        private boolean onAcceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
            logInfo("PGSessionPortListener.onAcceptSessionJoiner(" + sessionPort + "," + joiner + ")", "");
            String groupName = sessionPortToGroupName.get(sessionPort);
         
            if(groupName != null) {
                // Reject joiners if the group is locked from a 
                // lockGroup() call
                if(lockedGroups.contains(groupName)) {
                    return false;
                }
            }
            
            // Allow everyone to join
            return true;
        }
        
        /**
         * If we return true in acceptSessionJoiner, we admit a new peer
         * into our group.  The group does not really exist until a 
         * peer joins, at which time the group is created and a session
         * ID is assigned.  This method communicates to us that this event
         * has happened, and provides the new session ID for us to use.
         */
        @Override
        public void sessionJoined(final short sessionPort, final int id, final String joiner) {
            String methodName = "PGSessionPortListener.sessionJoined()";
            logInfo("PGSessionPortListener.sessionJoined(" + sessionPort + "," + joiner + ")", "");
            callbackHandler.post(new Runnable() {
                public void run() {
                    synchronized(PeerGroupManager.this) {
                        onSessionJoined(sessionPort, id, joiner);
                    }
                }            
            });
        }
        
        private void onSessionJoined(short sessionPort, int id, String joiner) {
            String methodName = "PGSessionPortListener.onSessionJoined";
            String groupName = sessionPortToGroupName.get(sessionPort);
            if(groupName != null) {
                addGroupNameToSessionId(groupName, id);
                if(groupName.contains(".JoC-")) {
                    addGroupNameToSessionId(groupName.substring(0, groupName.lastIndexOf(".")), id);
                }
                
                // Add the host to the list of participants. Should only happen once
                if(!getPeers(groupName).contains(bus.getUniqueName())) {
                    logInfo(methodName, "Host adding self to Participant list");
                    addPeer(id, bus.getUniqueName());
                    logInfo(methodName, "Setting Session Listener");
                    bus.setSessionListener(id, pgSessionListener); 
                    // Explicitly trigger SessionMemberAdded for the first Joiner in the Host 
                    pgSessionListener.sessionMemberAdded(id, joiner);
                    
                    // If someone joins your JoinOrCreate group, then you are now
                    // the master and should start advertising the master group.
                    if(jocGroups.contains(groupName)) {
                        changeGroupName(getJoCGuidGroupName(groupName), 
                                        getJoCMasterGroupName(groupName));
                    }
                }
            }
            else {
                logInfo(methodName, "No group name for port " + sessionPort);
            }
        }             
    }
    
    private class PGSessionListener extends SessionListener {
        @Override
        public void sessionLost(final int sessionId) {
            logInfo("PGSessionListener.sessionLost(" + sessionId + ")", "");
            callbackHandler.post(new Runnable() {
                public void run() {
                    synchronized(PeerGroupManager.this) {
                        onSessionLost(sessionId);
                    }
                }
            });
        }
        
        private void onSessionLost(int sessionId) {
            String methodName = "PGSessionPortListener.onSessionLost";
            logInfo(methodName, "(" + sessionId + ")");
            String groupName = sessionIdToGroupName.get(sessionId);
            // Remove the group from your list of joined groups
            joinedGroups.remove(groupName);
            // Remove the session id from the group
            removeGroupNameToSessionId(groupName);
            // Clear the peers for the group
            sessionIdToPeers.remove(sessionId);
            logInfo("PGSessionListener.groupLost(" + groupName + ")", "");
            for(PeerGroupListenerInterface listener : peerGroupListeners) {
                listener.groupLost(groupName);
            }
        }
        
        @Override 
        public void sessionMemberAdded(final int sessionId, final String uniqueName) {
            logInfo("PGSessionListener.sessionMemberAdded(" + sessionId + ", " + uniqueName + ")", "");
            callbackHandler.post(new Runnable() {
                public void run() {
                    synchronized(PeerGroupManager.this) {                    
                        onSessionMemberAdded(sessionId, uniqueName);
                    }
                }
            });
        }
        
        private void onSessionMemberAdded(int sessionId, String uniqueName) {
            String methodName = "PGSessionPortListener.onSessionMemberAdded";
            logInfo(methodName, "(" + sessionId + ", " + uniqueName + ")");
            // Add the new peer to the list of peers for the group
            addPeer(sessionId, uniqueName);
            String groupName = sessionIdToGroupName.get(sessionId);
            int numParticipants = getNumPeers(groupName);
            for(PeerGroupListenerInterface listener : peerGroupListeners) {
                listener.peerAdded(uniqueName, groupName, numParticipants);
            }
        }
        
        @Override
        public void sessionMemberRemoved(final int sessionId, final String uniqueName) {
            logInfo("PGSessionListener.sessionMemberRemoved(" + sessionId + ", " + uniqueName + ")", "");
            callbackHandler.post(new Runnable() {
                public void run() {
                    synchronized(PeerGroupManager.this) {                    
                        onSessionMemberRemoved(sessionId, uniqueName);
                    }
                }
            });
        }
        
        private void onSessionMemberRemoved(int sessionId, String uniqueName) {
            String methodName = "PGSessionPortListener.onSessionMemberRemoved";
            logInfo(methodName, "(" + sessionId + ", " + uniqueName + ")");       
            // Remove the peer from the list of peers for the group
            removePeer(sessionId, uniqueName);
            String groupName = sessionIdToGroupName.get(sessionId);
            int numParticipants = getNumPeers(groupName);
            for(PeerGroupListenerInterface listener : peerGroupListeners) {
                listener.peerRemoved(uniqueName, groupName, numParticipants);
            }
        }
    }

    
    /*------------------------------------------------------------------------*
     * Helper Methods
     *------------------------------------------------------------------------*/
    /**
     * connectBus
     * registers the bus listener, connects the bus attachment to the bus, 
     * starts discovery, and registers signal handlers. 
     * 
     * @return  OK if successful
     */
    private Status connectBus() {
        String methodName = "connectBus()";
        logInfo(methodName, "");
        Status status;
        
        // Register the bus listener
        bus.registerBusListener(pgBusListener);
        logInfo(methodName, "Registering Bus Listener");
        
        // Connect the bus
        status = bus.connect();
        logInfo(methodName, "Connecting Bus Attachment - " + status.toString());
        if(status != Status.OK) {
            return status;
        }
        
        // Start Discovery
        status = bus.findAdvertisedName(groupPrefix);
        logInfo(methodName, "FindAdvertisedName(" + groupPrefix + ") - " + status.toString());
        if(status != Status.OK) {
            return status;
        }
        
        // Pause the thread for 20 milliseconds to let the foundAdvertiseName() signals come through
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logError(methodName, e.toString());
        }

        return status; 
    }
    
    /**
     * setDebug
     * enables or disables debug messages.
     * 
     * @param debug  true to turn debug message on, false otherwise.
     */
    public void setDebug (boolean debug) {
        debugOn = debug;
    }
    
    /**
     * logInfo
     * logs the message of the executing method.
     * 
     * @param method   the currently executing method
     * @param message  the message to log
     */
    private void logInfo(String method, String message) {
        String msg = method + ": " + message;
        if(debugOn) {
            Log.i(TAG, msg);
        }
    }
    
    /**
     * logError
     * logs the error of the executing method.
     * 
     * @param method   the currently executing method
     * @param message  the message to log
     */
    private void logError(String method, String message) {
        String msg = method + ": " + message;
        Log.e(TAG, msg);
    }
    
    /**
     * getSessionId
     * gets the session Id of the group with the specified group name.
     * 
     * @param groupName  the name of the group to get the session id of
     * @return  the session id of the group if currently participating in it,
     *          otherwise -1
     */
    private int getSessionId(String groupName) {
        if(groupName != null) {
              if(groupNameToSessionId.containsKey(groupName)) {
                  logInfo("getSessionId(" + groupName + ")", groupNameToSessionId.get(groupName) + "");
                  return groupNameToSessionId.get(groupName);
              }
        }
        logInfo("getSessionId()", "Session Id not found");
        return -1;
    }
    
    /**
     * getGroupName
     * gets the group name (well known name suffix) of the given advertised
     * well known name.
     * 
     * @param advertisedName  the full well known name
     * @return  the group name (well known name suffix) of the given well 
     *          known name
     */
    private String getGroupName(String advertisedName) {
        return advertisedName.replaceFirst(groupPrefix + "\\d+.", "");
    }
    
    private short getSessionPort(String advertisedName) {
        String shortStr = advertisedName.replaceFirst(groupPrefix, "");
        shortStr = shortStr.substring(0, shortStr.indexOf("."));
        try {
            return Short.valueOf(shortStr);
        }
        catch (NumberFormatException e) {
            return INVALID_SESSION_PORT;
        }
    }
    
    /**
     * getAdvertisedName
     * generates the groupPrefix.sp.groupName string used for advertisement.
     * 
     * @param groupName    the name of the group
     * @param sessionPort  the port the group is hosted on
     * @return  the groupPrefix.sp.groupName string used for advertisement
     */
    private String getAdvertisedName(String groupName, short sessionPort) {
        String sessionPortStr = Short.toString(sessionPort);
        return groupPrefix + sessionPortStr + "." + groupName;
    }
    
    /**
     * getAdvertisedName
     * generates the groupPrefix.groupName string used for advertisement by
     * legacy AllJoyn apps not using the groupPrefix.sp.groupName naming 
     * convention containing the session port.
     * 
     * @param groupName    the name of the group
     * @return  the groupPrefix.groupName string used for advertisement
     */
    private String getLegacyAdvertisedName(String groupName) {
        return groupPrefix.substring(0,groupPrefix.lastIndexOf(".")) + "." + groupName;
    }
    
    /**
     * getJoCMasterGroupName
     * gets the master group name for a JoinOrCreate group.
     * 
     * @param groupName  the name of the JoinOrCreate group
     * @return  the master group name for a JoinOrCreate group
     */
    private String getJoCMasterGroupName(String groupName) {
        return groupName;
    }
    
    /**
     * getJoCGuidGroupName
     * gets the GUID group name for a JoinOrCreate group.
     * 
     * @param groupName the name of the JoinOrCreate group
     * @return  the GUID group name for a JoinOrCreate group
     */
    private String getJoCGuidGroupName(String groupName) {
        return groupName + ".JoC-" + bus.getGlobalGUIDString();
    }
    
    /**
     * changeGroupName
     * changes the advertisement of an available group when switching from
     * your GUID JoinOrCreate group name to the master JoinOrCreateGroupName.
     * 
     * @param prevGroupName  the existing group name
     * @param newGroupName   the new group name to change to
     * @return  OK if successful
     */
    private Status changeGroupName(String prevGroupName, String newGroupName) {
        Status status = Status.FAIL;
        String methodName = "changeGroupName()";
        logInfo("changeGroupName(" + newGroupName + ", " + prevGroupName + ")", "");
        // Make sure the new group name isn't already taken
        if(listFoundGroups().contains(newGroupName) || listHostedGroups().contains(newGroupName) 
                || listJoinedGroups().contains(newGroupName)) {
            logInfo(methodName, newGroupName + " is already being advertised");
            return status;
        }
        logInfo(methodName, newGroupName + " is available");
        
        // Make sure the group is valid
        if(!listHostedGroups().contains(prevGroupName)) {
            logInfo(methodName, "You are not the host of the group " + prevGroupName);
            return status;
        }
        
        Short sessionPort = groupNameToSessionPort.get(prevGroupName);
        String newAdvertisedName = getAdvertisedName(newGroupName, sessionPort);
        // Request the Well Known Name
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        status = bus.requestName(newAdvertisedName, flag);
        logInfo(methodName, "Requesting name " + newAdvertisedName + " - " + status.toString());
        hostedGroups.add(newGroupName);
        if(status == Status.OK) {
            // Advertise the Well Known Name
            status = bus.advertiseName(newAdvertisedName, defaultSessionOpts.transports);
            logInfo(methodName, "Advertising name " + newAdvertisedName + " - " + status.toString());
            if(status == Status.OK) {
                String prevAdvertisedName = getAdvertisedName(prevGroupName, sessionPort);
                // Cancel the old advertisement
                bus.cancelAdvertiseName(prevAdvertisedName, defaultSessionOpts.transports);
                // Update all of our mappings
                removeGroupNameToSessionPort(prevGroupName);
                addGroupNameToSessionPort(newGroupName, sessionPort);
                hostedGroups.remove(prevGroupName);
                return status;
            }
            // Fall through and cleanup on failure
            hostedGroups.remove(newGroupName);
            bus.releaseName(newAdvertisedName);
        }
        return status;
    }
    
    /**
     * registerBusObjects
     * registers a list of bus object at their object paths on the bus.
     * 
     * @param busObjects  the list of bus objects and their corresponding 
     *                    object paths to register
     * @return  OK if successful
     */
    private void registerBusObjects(ArrayList<BusObjectData> busObjects) {
        if(busObjects == null) {
            return;
        }
            
        // Register all the bus objects in the list of given bus objects
        for (BusObjectData busObjectData : busObjects) {
            Status status = bus.registerBusObject(busObjectData.getBusObject(), busObjectData.getObjectPath());
            logInfo("registerBusObjects()", "Registering bus object at " + busObjectData.getObjectPath() 
                    + " - " + status.toString());
            if(status == Status.OK) {
                registeredBusObjects.add(busObjectData.getBusObject());
            }
        }
    }

    /**
     * unregisterAllBusObjects
     * unregisters all busObjects currently registered to the bus. This will 
     * result in a disconnecting of the bus attachment and therefore should 
     * be used during cleanup if not using disconnectBus().
     */
    private void unregisterAllBusObjects() {
        logInfo("unregisterAllBusObjects()", "");
        // Unregister all bus objects that are still registered
        for(BusObject busObj : registeredBusObjects) {
            bus.unregisterBusObject(busObj);
        }
        registeredBusObjects.clear();
    }
    
    /**
     * isInvalidStringParam
     * checks the given string to see if it is invalid or not. An invalid
     * string is null or an empty string.
     * 
     * @param string  the string to check
     * @return  true if the string is invalid, false otherwise
     */
    private boolean isInvalidStringParam(String string) {
        boolean ret = false;
        if(string == null) {
            ret = true;
        }
        else if(string.trim() == "") {
            ret = true;
        }
        return ret;
    }
    
    /**
     * isBusInvalid
     * checks to see if the bus attachment managed by the PeerGroupManager
     * is invalid or not.
     * 
     * @return  true if the bus attachment is invalid or false otherwise
     */
    private boolean isBusInvalid() {
        if(bus == null) {
            return true;
        }
        return false;
    }
    
    
    /*------------------------------------------------------------------------*
     * Private Thread Safe Methods
     *------------------------------------------------------------------------*/
    // Accessing the list of found groups
    private void addFoundGroup(String groupName, short sessionPort) {
        foundGroups.remove(groupName);
        foundGroups.put(groupName, sessionPort);
    }
    
    private void removeFoundGroup(String groupName) {
        foundGroups.remove(groupName);
    }
    
    // Accessing the lists of peers
    private void addPeer(int sessionId, String peerId) {
        logInfo("addPeer(" + sessionId + ", " + peerId + ")", "");
        if(sessionIdToPeers.containsKey(sessionId)) {
            sessionIdToPeers.get(sessionId).remove(peerId);
        }
        else {
            sessionIdToPeers.put(sessionId, new ArrayList<String>());
        }
        sessionIdToPeers.get(sessionId).add(peerId);
    }
    
    private void removePeer(int sessionId, String peerId) {
        logInfo("removePeer(" + sessionId + ", " + peerId + ")", "");
        if(sessionIdToPeers.containsKey(sessionId)) {
            sessionIdToPeers.get(sessionId).remove(peerId);
        }
    }
    
    private void clearPeers(int sessionId) {
        logInfo("clearPeers(" + sessionId + ")", "");
        if(sessionIdToPeers.containsKey(sessionId)) {
            sessionIdToPeers.get(sessionId).clear();
            sessionIdToPeers.remove(sessionId);
        }
    }
    
    // Accessing the maps of group names to session ports
    private void addGroupNameToSessionPort(String groupName, short sessionPort) {
        groupNameToSessionPort.remove(groupName);
        groupNameToSessionPort.put(groupName, sessionPort);
        sessionPortToGroupName.remove(sessionPort);
        sessionPortToGroupName.put(sessionPort, groupName);
    }
    
    private void removeGroupNameToSessionPort(String groupName) {
        if(groupNameToSessionPort.containsKey(groupName)) {
            short sessionPort = groupNameToSessionPort.get(groupName);
            sessionPortToGroupName.remove(sessionPort);
            groupNameToSessionPort.remove(groupName);
        }
    }
    
    // Accessing the maps of group names to session Ids
    private void addGroupNameToSessionId(String groupName, int sessionId) {
        groupNameToSessionId.remove(groupName);
        groupNameToSessionId.put(groupName, sessionId);
        sessionIdToGroupName.remove(sessionId);
        sessionIdToGroupName.put(sessionId, groupName);
    }
    
    private void removeGroupNameToSessionId(String groupName) {
        Integer sessionId = groupNameToSessionId.remove(groupName);
        if(sessionId != null) {
            sessionIdToGroupName.remove(sessionId);
        }
    }
    
    // Accessing the list of JoinOrCreate groups
    private void addJoCGroup(String groupName) {
        logInfo("addJoCGroup(" + groupName + ")", "");
        jocGroups.remove(groupName);
        jocGroups.add(groupName);
    }
    
    private void removeJoCGroup(String groupName) {
        logInfo("removeJoCGroup(" + groupName + ")", "");
        jocGroups.remove(groupName);
    }
}


