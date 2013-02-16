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

import org.alljoyn.bus.BusObject;

/**
 * This class is used to store bus objects with their corresponding object paths
 * 
 */
public class BusObjectData {
    private BusObject mBusObject;
    private String mObjectPath;
    
    /**
     * Constructs a BusObjectData instance.
     * 
     * @param busObj  the bus object
     * @param objPath the object path of the bus object
     */
    public BusObjectData (BusObject busObj, String objPath) {
        mBusObject = busObj;
        mObjectPath = objPath;
    }
    
    /**
     * Get the bus object.
     * 
     * @return the bus object
     */
    public BusObject getBusObject() {
        return mBusObject;
    }
    
    /**
     * Get the object patch of the bus object.
     * 
     * @return the object path of the bus object
     */
    public String getObjectPath() {
        return mObjectPath;
    }
}
