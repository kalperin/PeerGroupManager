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

package org.alljoyn.cops.peergroupmanagerapp;

import org.alljoyn.bus.Status;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class UIHandler extends Handler {
    
    private static final String TAG = "GroupManagerApp";

    /* UI Handler Codes */
    public static final int TOAST_MSG = 0;
    public static final int TOGGLE_DISCOVERY_BUTTONS = 1;
    public static final int UPDATE_GROUP_LIST_SPINNER = 2;
    
    private MainActivity mActivity;
    
    public UIHandler (MainActivity activity) {
        mActivity = activity;
    }
    
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case TOAST_MSG:
            Toast.makeText(mActivity.getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
            break;
        case UPDATE_GROUP_LIST_SPINNER:
            Bundle data = msg.getData();
            mActivity.updateGroupListSpinner(data.getStringArray("availableGroupList"), 
            		data.getStringArray("hostedGroupList"), 
            		data.getStringArray("joinedGroupList"), 
            		data.getStringArray("lockedGroupList"));
            break;
        default:
            break;
        }
    }
    
    public void logInfo(String msg) {
        Log.i(TAG, msg);
    }
    
    public void logStatus(String msg, Status status) {
        String log = String.format("%s: %s", msg, status);
        if (status == Status.OK) {
            Log.i(TAG, log);
        } else {
            Message toastMsg = obtainMessage(TOAST_MSG, log);
            sendMessage(toastMsg);
            Log.e(TAG, log);
        }
    }

    public void logError(String msg) {
        Message toastMsg = obtainMessage(TOAST_MSG, msg);
        sendMessage(toastMsg);
        Log.e(TAG, msg);
    }

    public void logException(String msg, Exception ex) {
        String log = String.format("%s: %s", msg, ex);
        Message toastMsg = obtainMessage(TOAST_MSG, log);
        sendMessage(toastMsg);
        Log.e(TAG, log, ex);
    }
}
