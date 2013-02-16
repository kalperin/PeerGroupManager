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

import java.util.ArrayList;

import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Status;

interface PeerGroupManagerInterface {
    
    public Status createGroup (String groupName);
    
    public Status createGroup (String groupName, boolean locked); 
    
    public Status destroyGroup (String groupName);
    
    public Status joinGroup (String groupName);
    
    public Status leaveGroup (String groupName);
    
    public JoinOrCreateReturn joinOrCreateGroup(String groupName); 
    
    public void cleanup();
    
    public Status unlockGroup(String groupName);
    
    public Status lockGroup(String groupName);
    
    public ArrayList<String> listFoundGroups();
    
    public ArrayList<String> listHostedGroups();
    
    public ArrayList<String> listJoinedGroups();
    
    public ArrayList<String> listLockedGroups();
    
    public ArrayList<String> getPeers (String groupName);
    
    public int getNumPeers(String groupName);
    
    public void addPeerGroupListener(PeerGroupListenerInterface peerGroupListener);

    public <T> T getRemoteObjectInterface(String peerId, String groupName, String objectPath, Class<T> iface);
    
    public <T> T getSignalInterface(String groupName, BusObject busObject, Class<T> iface);
    
    public <T> T getSignalInterface(String peerId, String groupName, BusObject busObject, Class<T> iface);
    
    public Status registerModule(PGModule module, String groupName);
    
    public String getGroupPrefix();
    
    public Status registerSignalHandlers(Object classWithSignalHandlers);
    
    public String getMyPeerId();
    
    public String getGUID();
    
    public String getSenderPeerId();
    
    public String getGroupHostPeerId(String groupName);
    
    public void setSessionPort(short sessionPort);
}
