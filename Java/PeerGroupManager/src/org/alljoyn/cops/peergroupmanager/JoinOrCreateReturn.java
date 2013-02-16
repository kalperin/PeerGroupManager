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
