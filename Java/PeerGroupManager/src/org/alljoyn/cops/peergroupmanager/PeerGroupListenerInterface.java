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

/**
 * The PeerGroupListenerInterface defines an interface that is used to
 * receive notification of changes to the peer group. The call-backs are
 * equivalent to the foundAdvertisedname(), lostAdvertisedName(), sessionLost(),
 * sessionMemberAdded(), and sessionMemberRemoved() signals of AllJoyn.
 */
public interface PeerGroupListenerInterface {
	
    /**
     * Called when a new group advertisement is found. This will not be
     * triggered for your own hosted groups.
     * 
     * @param groupName  the groupName that was found
     * @param transport  the transport that the groupName was discovered on
     */
    public void foundAdvertisedName(String groupName, short transport);
    
    /**
     * Called when a group that was previously reported through 
     * foundAdvertisedName has become unavailable. This will not be triggered
     * for your own hosted groups.
     * 
     * @param groupName  the group name that has become unavailable
     * @param transport  the transport that stopped receiving the groupName 
     *                   advertisement
     */
    public void lostAdvertisedName(String groupName, short transport);
    
    /**
     * Called when a group becomes disconnected.
     * 
     * @param groupName  the group that became disconnected
     */
    public void groupLost(String groupName);
    
    /**
     * Called when a new peer joins a group.
     * 
     * @param peerId     the id of the peer that joined 
     * @param groupName  the group that the peer joined
     * @param numPeers   the current number of peers in the group
     */
    public void peerAdded(String peerId, String groupName, int numPeers);
    
    /**
     * Called when a peer leaves a group.
     * 
     * @param peerId     the id of the peer that left 
     * @param groupName  the group that the peer left
     * @param numPeers   the current number of peers in the group
     */
    public void peerRemoved(String peerId, String groupName, int numPeers);
}
