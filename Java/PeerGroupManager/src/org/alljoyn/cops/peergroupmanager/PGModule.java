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

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.Status;

/**
 * The interface a module needs to implement if it wants to interact with 
 * the groups created by the PeerGroupManager.
 *
 */
public interface PGModule {
    /**
     * register will be called by the PeerGroupManager when the 
     * registerModule() method of the PeerGroupManager is called to provide
     * the module with the AllJoyn components necessary to interact with
     * its groups
     * 
     * @param bus        the bus attachment created and used by the 
     *                   PeerGroupManager
     * @param sessionId  the session id of the group
     * @return  OK if successful
     */
    public Status register(BusAttachment bus, int sessionId);
}
