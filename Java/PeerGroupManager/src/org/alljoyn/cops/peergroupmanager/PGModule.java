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
