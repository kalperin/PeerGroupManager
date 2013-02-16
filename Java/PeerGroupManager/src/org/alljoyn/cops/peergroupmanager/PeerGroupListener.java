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

package org.alljoyn.cops.peergroupmanager;

/**
 * The PeerGroupListener is a listener that provides convenient empty
 * implementations of the PeerGroupListenerInterface methods, so that only the
 * desired call-back methods need to be overridden. The call-back methods are
 * equivalent to the foundAdvertisedname(), lostAdvertisedName(), sessionLost(),
 * sessionMemberAdded(), and sessionMemberRemoved() signals of AllJoyn.
 */
public class PeerGroupListener implements PeerGroupListenerInterface {
    
    /**
     * Called when a new group advertisement is found. This will not be
     * triggered for your own hosted groups.
     * 
     * @param groupName  the groupName that was found
     * @param transport  the transport that the groupName was discovered on
     */
    @Override
    public void foundAdvertisedName(String groupName, short transport) {
        return;
    }
    
    /**
     * Called when a group that was previously reported through 
     * foundAdvertisedName has become unavailable. This will not be triggered
     * for your own hosted groups.
     * 
     * @param groupName  the group name that has become unavailable
     * @param transport  the transport that stopped receiving the groupName 
     *                   advertisement
     */
    @Override
    public void lostAdvertisedName(String groupName, short transport) {
        return;
    }
    
    /**
     * Called when a group becomes disconnected.
     * 
     * @param groupName  the group that became disconnected
     */
    @Override
    public void groupLost(String groupName) {
        return;
    }
    
    /**
     * Called when a new peer joins a group.
     * 
     * @param peerId     the id of the peer that joined 
     * @param groupName  the group that the peer joined
     * @param numPeers   the current number of peers in the group
     */
    @Override
    public void peerAdded(String peerId, String groupName, int numPeers) {
        return;
    }
    
    /**
     * Called when a new peer leaves a group.
     * 
     * @param peerId     the id of the peer that left 
     * @param groupName  the group that the peer left
     * @param numPeers   the current number of peers in the group
     */
    @Override
    public void peerRemoved(String peerId, String groupName, int numPeers) {
        return;
    }
}
