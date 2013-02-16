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

import android.app.Dialog;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.alljoyn.cops.peergroupmanagerapp.R;

public class DialogBuilder {
    private static final String TAG = "PeerGroupManagerApp";
    
    public Dialog createCreateGroupDialog(final MainActivity activity, final UIHandler uiHandler) {
        Log.i(TAG, "createCreateGroupDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.creategroupdialog);
        
        final EditText groupNameText = (EditText) dialog.findViewById(R.id.createGroupName);
        
        Button create = (Button) dialog.findViewById(R.id.createGroupButton);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupName = groupNameText.getText().toString();
                
                // Make sure the input is valid
                if(groupName.contains(" ") || groupName.equals("")) {
                    uiHandler.sendMessage(uiHandler.obtainMessage(UIHandler.TOAST_MSG, "Please enter a valid Group Name"));
                    return;
                }
                // Call Create Group
                activity.createGroup(groupName);
                // Hide the soft keyboard
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                activity.removeDialog(MainActivity.DIALOG_CREATE_GROUP_ID);
            }
        });
                
        Button cancel = (Button) dialog.findViewById(R.id.cancelCreateGroupButton);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Hide the soft keyboard
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                activity.removeDialog(MainActivity.DIALOG_CREATE_GROUP_ID);
            }
        });
        
        return dialog;
    }

    
    public Dialog createJoinGroupDialog(final MainActivity activity, final BusHandler busHandler, final UIHandler uiHandler) {
    	Log.i(TAG, "createJoinGroupDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.joingroupdialog);
        
        ArrayAdapter<String> groupListAdapter = new ArrayAdapter<String>(activity, android.R.layout.test_list_item);
        final ListView groupList = (ListView)dialog.findViewById(R.id.joinGroupList);
        groupList.setAdapter(groupListAdapter);
        
        List<String> groups = busHandler.listGroups();
        
        //remove all already hosted and joined groups
        groups.removeAll(busHandler.listHostedGroups());
        groups.removeAll(busHandler.listJoinedGroups());
        
        //Transfer groups from the ArrayList given by the GroupManager to the local ArrayAdapter
        for (String group : groups) {
            groupListAdapter.add(group);
        }
        groupListAdapter.notifyDataSetChanged();
                        
        groupList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Call join group on the selected advertised group
                String name = groupList.getItemAtPosition(position).toString();
                activity.joinGroup(name);
                /*
                 * Android likes to reuse dialogs for performance reasons.  If
                 * we reuse this one, the list of channels will eventually be
                 * wrong since it can change.  We have to tell the Android
                 * application framework to forget about this dialog completely.
                 */
                activity.removeDialog(MainActivity.DIALOG_JOIN_GROUP_ID);
            }
        });
        
        Button ok = (Button)dialog.findViewById(R.id.cancelJoinGroupButton);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                activity.removeDialog(MainActivity.DIALOG_JOIN_GROUP_ID);
            }
        });
        
        return dialog;
    }
    
    
    public Dialog createSelectGroupDialog(final MainActivity activity, final BusHandler busHandler) {
        Log.i(TAG, "createParticipantsDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.selectgroupdialog);
        
        ArrayAdapter<String> groupListAdapter = new ArrayAdapter<String>(activity, android.R.layout.test_list_item);
        final ListView groupList = (ListView)dialog.findViewById(R.id.groupList);
        groupList.setAdapter(groupListAdapter);
        
        List<String> groups = new ArrayList<String>(busHandler.listHostedGroups());
        List<String> joinedGroups = busHandler.listJoinedGroups();
        
        // Combine the list of Hosted Groups and Joined Groups
        for (String group : joinedGroups) {
            if(!groups.contains(group)) {
                groups.add(group);
            }
        }
        
        //Transfer groups from the ArrayList given by the GroupManager to the local ArrayAdapter
        for (String group : groups) {
            groupListAdapter.add(group);
        }
        groupListAdapter.notifyDataSetChanged();
        
        groupList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Create the get peers dialog after the user selects the group
                String name = groupList.getItemAtPosition(position).toString();
                Bundle args = new Bundle();
                args.putString("groupName", name);
                activity.showDialog(MainActivity.DIALOG_GET_PEERS_ID, args);
                /*
                 * Android likes to reuse dialogs for performance reasons.  If
                 * we reuse this one, the list of channels will eventually be
                 * wrong since it can change.  We have to tell the Android
                 * application framework to forget about this dialog completely.
                 */
                activity.removeDialog(MainActivity.DIALOG_SELECT_GET_PEER_GROUP_ID);
            }
        });
                        
        Button ok = (Button)dialog.findViewById(R.id.cancelSelectGroupButton);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                activity.removeDialog(MainActivity.DIALOG_SELECT_GET_PEER_GROUP_ID);
            }
        });
        
        return dialog;
    }
    
    public Dialog createParticipantsDialog(final MainActivity activity, final BusHandler busHandler, String groupName) {
        Log.i(TAG, "createParticipantsDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.peersdialog);
        
        ArrayAdapter<String> peersListAdapter = new ArrayAdapter<String>(activity, android.R.layout.test_list_item);
        final ListView peersList = (ListView)dialog.findViewById(R.id.peerList);
        peersList.setAdapter(peersListAdapter);
        
        // Call getParticipants
        List<String> peers = busHandler.getParticipants(groupName);
        for (String peer : peers) {
            peersListAdapter.add(peer);
        }
        peersListAdapter.notifyDataSetChanged();
                        
        Button ok = (Button)dialog.findViewById(R.id.closeButton);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                activity.removeDialog(MainActivity.DIALOG_GET_PEERS_ID);
            }
        });
        
        return dialog;
    }
    
    public Dialog createDestroyGroupDialog(final MainActivity activity, final BusHandler busHandler) {
        Log.i(TAG, "createDestroyGroupDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.destroygroupdialog);
        
        ArrayAdapter<String> groupListAdapter = new ArrayAdapter<String>(activity, android.R.layout.test_list_item);
        final ListView groupList = (ListView)dialog.findViewById(R.id.hostedGroupList);
        groupList.setAdapter(groupListAdapter);
        
        List<String> groups = busHandler.listHostedGroups();

        //Transfer groups from the ArrayList given by the GroupManager to the local ArrayAdapter
        for (String group : groups) {
            groupListAdapter.add(group);
        }
        groupListAdapter.notifyDataSetChanged();
                        
        groupList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Call destroy group on the selected hosted group
                String name = groupList.getItemAtPosition(position).toString();
                activity.destroyGroup(name);
                /*
                 * Android likes to reuse dialogs for performance reasons.  If
                 * we reuse this one, the list of channels will eventually be
                 * wrong since it can change.  We have to tell the Android
                 * application framework to forget about this dialog completely.
                 */
                activity.removeDialog(MainActivity.DIALOG_DESTROY_GROUP_ID);
            }
        });
        
        Button ok = (Button)dialog.findViewById(R.id.cancelDestroyGroupButton);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                activity.removeDialog(MainActivity.DIALOG_DESTROY_GROUP_ID);
            }
        });
        
        return dialog;
    }
    
    public Dialog createLeaveGroupDialog(final MainActivity activity, final BusHandler busHandler) {
        Log.i(TAG, "createLeaveGroupDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.leavegroupdialog);
        
        ArrayAdapter<String> groupListAdapter = new ArrayAdapter<String>(activity, android.R.layout.test_list_item);
        final ListView groupList = (ListView)dialog.findViewById(R.id.joinedGroupList);
        groupList.setAdapter(groupListAdapter);
        
        List<String> groups = busHandler.listJoinedGroups();

        //Transfer groups from the ArrayList given by the GroupManager to the local ArrayAdapter
        for (String group : groups) {
            groupListAdapter.add(group);
        }
        groupListAdapter.notifyDataSetChanged();
        
        groupList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Call leave group on the selected joined group
                String name = groupList.getItemAtPosition(position).toString();
                activity.leaveGroup(name);
                /*
                 * Android likes to reuse dialogs for performance reasons.  If
                 * we reuse this one, the list of channels will eventually be
                 * wrong since it can change.  We have to tell the Android
                 * application framework to forget about this dialog completely.
                 */
                activity.removeDialog(MainActivity.DIALOG_LEAVE_GROUP_ID);
            }
        });       
                        
        Button ok = (Button)dialog.findViewById(R.id.cancelLeaveGroupButton);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                activity.removeDialog(MainActivity.DIALOG_LEAVE_GROUP_ID);
            }
        });
        
        return dialog;
    }


	public Dialog createUnlockGroupDialog(final MainActivity activity, final BusHandler busHandler) {
		Log.i(TAG, "createUnlockGroupDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.unlockgroupdialog);
        
        ArrayAdapter<String> groupListAdapter = new ArrayAdapter<String>(activity, android.R.layout.test_list_item);
        final ListView groupList = (ListView)dialog.findViewById(R.id.hostedGroupList);
        groupList.setAdapter(groupListAdapter);
        
        List<String> groups = busHandler.listHostedGroups();
        List<String> lockedGroups = busHandler.listLockedGroups();
        
        
        //Transfer unloackable groups from the ArrayList given by the GroupManager to the local ArrayAdapter
        for (String group : groups) {
        	if(lockedGroups.contains(group)){
        		groupListAdapter.add(group);
        	}
        }
        groupListAdapter.notifyDataSetChanged();
                        
        groupList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Call unlock group on the selected hosted group
                String name = groupList.getItemAtPosition(position).toString();
                activity.unlockGroup(name);
                /*
                 * Android likes to reuse dialogs for performance reasons.  If
                 * we reuse this one, the list of channels will eventually be
                 * wrong since it can change.  We have to tell the Android
                 * application framework to forget about this dialog completely.
                 */
                activity.removeDialog(MainActivity.DIALOG_UNLOCK_GROUP_ID);
            }
        });
        
        Button ok = (Button)dialog.findViewById(R.id.cancelUnlockGroupButton);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                activity.removeDialog(MainActivity.DIALOG_UNLOCK_GROUP_ID);
            }
        });
        
        return dialog;
	}


	public Dialog createLockGroupDialog(final MainActivity activity, final BusHandler busHandler) {
		Log.i(TAG, "createLockGroupDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.lockgroupdialog);
        
        ArrayAdapter<String> groupListAdapter = new ArrayAdapter<String>(activity, android.R.layout.test_list_item);
        final ListView groupList = (ListView)dialog.findViewById(R.id.hostedGroupList);
        groupList.setAdapter(groupListAdapter);
        
        List<String> groups = busHandler.listHostedGroups();
        List<String> lockedGroups = busHandler.listLockedGroups();
        
        groups.removeAll(lockedGroups);
        
        //Transfer groups from the ArrayList given by the GroupManager to the local ArrayAdapter
        for (String group : groups) {
            groupListAdapter.add(group);
        }
        groupListAdapter.notifyDataSetChanged();
                        
        groupList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Call lock group on the selected hosted group
                String name = groupList.getItemAtPosition(position).toString();
                activity.lockGroup(name);
                /*
                 * Android likes to reuse dialogs for performance reasons.  If
                 * we reuse this one, the list of channels will eventually be
                 * wrong since it can change.  We have to tell the Android
                 * application framework to forget about this dialog completely.
                 */
                activity.removeDialog(MainActivity.DIALOG_LOCK_GROUP_ID);
            }
        });
        
        Button ok = (Button)dialog.findViewById(R.id.cancelLockGroupButton);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                activity.removeDialog(MainActivity.DIALOG_LOCK_GROUP_ID);
            }
        });
        
        return dialog;
	}
	
	public Dialog createSetPortDialog(final MainActivity activity, final UIHandler uiHandler) {
        Log.i(TAG, "createSetPortDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.setportdialog);
        
        final EditText sessionPortText = (EditText) dialog.findViewById(R.id.createSessionPort);
        Button create = (Button) dialog.findViewById(R.id.setPortButton);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sessionPortString = sessionPortText.getText().toString();
                
                // Make sure the input is valid
                if(sessionPortString.equals("")) {
                    uiHandler.sendMessage(uiHandler.obtainMessage(UIHandler.TOAST_MSG, "Please enter a valid Session Name and Port"));
                    return;
                }
                // Call Create Group
                short sessionPort = Short.valueOf(sessionPortString);
                activity.setPort(sessionPort);
                // Hide the soft keyboard
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                activity.removeDialog(MainActivity.DIALOG_SET_PORT_ID);
            }
        });
                
        Button cancel = (Button) dialog.findViewById(R.id.cancelSetPortButton);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Hide the soft keyboard
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                activity.removeDialog(MainActivity.DIALOG_SET_PORT_ID);
            }
        });
        
        return dialog;
    }
	
	public Dialog createJoinOrCreateDialog(final MainActivity activity, final UIHandler uiHandler) {
        Log.i(TAG, "createJoinOrCreateDialog()");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.creategroupdialog);
        
        final EditText groupNameText = (EditText) dialog.findViewById(R.id.createGroupName);
        
        Button create = (Button) dialog.findViewById(R.id.createGroupButton);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupName = groupNameText.getText().toString();
                
                // Make sure the input is valid
                if(groupName.contains(" ") || groupName.equals("")) {
                    uiHandler.sendMessage(uiHandler.obtainMessage(UIHandler.TOAST_MSG, "Please enter a valid Group Name"));
                    return;
                }
                // Call Create Group
                activity.joinOrCreateGroup(groupName);
                // Hide the soft keyboard
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                activity.removeDialog(MainActivity.DIALOG_JOIN_OR_CREATE_ID);
            }
        });
                
        Button cancel = (Button) dialog.findViewById(R.id.cancelCreateGroupButton);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Hide the soft keyboard
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                activity.removeDialog(MainActivity.DIALOG_JOIN_OR_CREATE_ID);
            }
        });
        
        return dialog;
    }
    
    

}
