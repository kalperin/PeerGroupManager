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
