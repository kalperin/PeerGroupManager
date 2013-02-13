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

import org.alljoyn.bus.Status;

/**
 * This class is returned by the joinOrCreate() method of the peer group manager
 * 
 */
public class JoinOrCreateReturn {
    private Status status;
    private boolean isJoiner;
    
    public JoinOrCreateReturn(Status status, boolean isJoiner) {
        this.status = status;
        this.isJoiner = isJoiner;
    }
    
    /**
     * Get the AllJoyn status return from calling joinOrCreateGroup().
     * 
     * @return the AllJoyn status return from joinOrCreateGroup()
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Get a flag denoting whether the user joined or created the group.
     * 
     * @return true if the caller joined the group, false if the caller created
     *         the group
     */
    public boolean isJoiner() {
        return isJoiner;
    }
}
