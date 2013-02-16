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

import org.alljoyn.cops.peergroupmanagerapp.R;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {
    /* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }

    // UI Buttons
    private Button mHostButton;
    private Button mDestroyButton;
    private Button mJoinButton;
    private Button mLeaveButton;
    private Button mUnlockButton;
    private Button mLockButton;
    private Button mPortButton;
    private Button mJoinOrCreateButton;
    private Button mParticipantsButton;
    private EditText mPingText;
    private Spinner mPingGroupList;
    private ArrayAdapter<String> spinnerAdapter;
    
    private Menu menu;
    
    /* Handler used to make calls to AllJoyn methods. See onCreate(). */
    private BusHandler mBusHandler;
    private UIHandler  mUIHandler;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mUIHandler = new UIHandler(this);
        
        /* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper(), mUIHandler, this);
        
        mHostButton = (Button) findViewById(R.id.hostButton);
        mDestroyButton = (Button) findViewById(R.id.destroyButton);
        mJoinButton = (Button) findViewById(R.id.joinButton);
        mLeaveButton = (Button) findViewById(R.id.leaveButton);
        mUnlockButton = (Button) findViewById(R.id.unlockButton);
        mLockButton = (Button) findViewById(R.id.lockButton);
        mPortButton = (Button) findViewById(R.id.portButton);
        mJoinOrCreateButton = (Button) findViewById(R.id.joinOrCreateButton);
        mParticipantsButton = (Button) findViewById(R.id.peersButton);
        mPingText = (EditText) findViewById(R.id.pingText);
        mPingGroupList = (Spinner) findViewById(R.id.pingGroupList);

        
        mHostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                  showDialog(DIALOG_CREATE_GROUP_ID);
            }
        });
        
        mDestroyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_DESTROY_GROUP_ID);
            }
        });
        
        mJoinButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_JOIN_GROUP_ID);
            }
        });
        
        mLeaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_LEAVE_GROUP_ID);
            }
        });
        
        mUnlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_UNLOCK_GROUP_ID);
            }
        });
        
        mLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_LOCK_GROUP_ID);
            }
        });
        
        mPortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_SET_PORT_ID);
            }
        });
        
        mJoinOrCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_JOIN_OR_CREATE_ID);
            }
        });
        
        mParticipantsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_SELECT_GET_PEER_GROUP_ID);
            }
        });
        
        // Hide the soft keyboard until the edit text is selected
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        mPingText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_UP) {
                    
                    /* Call the remote object's Ping method. */
                    String pingString = mPingText.getText().toString();
                    if(pingString.trim().equals("")) {
                        mUIHandler.sendMessage(mUIHandler.obtainMessage(UIHandler.TOAST_MSG, "Please enter a valid message"));
                        return true;
                    }
                    Message msg = mBusHandler.obtainMessage(BusHandler.PING);
                    Bundle data = new Bundle();
                    data.putString("groupName", (String) mPingGroupList.getSelectedItem());
                    data.putString("pingString", pingString);
                    msg.setData(data);
                    mBusHandler.sendMessage(msg);
                }
                return true;
            }
        });
        
        String[] groups = {"No Groups Available"};
        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, groups);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPingGroupList.setAdapter(spinnerAdapter);
        
        /* Connect to an AllJoyn object. */
        mBusHandler.sendEmptyMessage(BusHandler.INIT);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        this.menu = menu;
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.quit:
	    	finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        /* Disconnect to prevent resource leaks. */
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    }

    /*
     * Non-Blocking Method calls used by the dialogs
     */
    public void createGroup(String groupName) {
        Message msg = mBusHandler.obtainMessage(BusHandler.CREATE_GROUP, groupName);
        mBusHandler.sendMessage(msg);
    }
    
    public void joinGroup(String groupName) {
        Message msg = mBusHandler.obtainMessage(BusHandler.JOIN_GROUP, groupName);
        mBusHandler.sendMessage(msg);
    }
    
    public void destroyGroup(String groupName) {
        Message msg = mBusHandler.obtainMessage(BusHandler.DESTROY_GROUP, groupName);
        mBusHandler.sendMessage(msg);
    }
    
    public void leaveGroup(String groupName) {
        Message msg = mBusHandler.obtainMessage(BusHandler.LEAVE_GROUP, groupName);
        mBusHandler.sendMessage(msg);
    }
    
    public void unlockGroup(String groupName) {
        Message msg = mBusHandler.obtainMessage(BusHandler.UNLOCK_GROUP, groupName);
        mBusHandler.sendMessage(msg);
    }
    
    public void lockGroup(String groupName) {
        Message msg = mBusHandler.obtainMessage(BusHandler.LOCK_GROUP, groupName);
        mBusHandler.sendMessage(msg);
    }
    
    public void setPort(short sessionPort){
    	Message msg = mBusHandler.obtainMessage(BusHandler.SET_PORT, sessionPort);
        mBusHandler.sendMessage(msg);
    }
    
    public void joinOrCreateGroup(String groupName) {
    	Message msg = mBusHandler.obtainMessage(BusHandler.JOIN_OR_CREATE, groupName);
    	mBusHandler.sendMessage(msg);
    }
    
    public void updateGroupListSpinner(String[] availableGroups, String[] hostedGroups, String[] joinedGroups, String[] lockedGroups) {
        // enable and disable buttons based on listed groups
    	System.out.println("Available: " + availableGroups.length 
    			+ ", Hosted: " + hostedGroups.length 
    			+ ", Joined: " + joinedGroups.length 
    			+ ", Locked: " + lockedGroups.length);
    	
    	if(availableGroups.length == 0){
        	mJoinButton.setEnabled(false);
        }
        else{
        	mJoinButton.setEnabled(true);
        }
        
        if(hostedGroups.length == 0){
        	mDestroyButton.setEnabled(false);
        	mUnlockButton.setEnabled(false);
            mLockButton.setEnabled(false);
        }
        else{
        	mDestroyButton.setEnabled(true);
        	mUnlockButton.setEnabled(true);
            mLockButton.setEnabled(true);
        }
        
        if(joinedGroups.length == 0){
        	mLeaveButton.setEnabled(false);
        }
        else{
        	mLeaveButton.setEnabled(true);
        }
        
        if(lockedGroups.length == 0){
        	mUnlockButton.setEnabled(false);
        }
        else{
        	mUnlockButton.setEnabled(true);
        }
        
        if(hostedGroups.length == lockedGroups.length){
        	mLockButton.setEnabled(false);
        }
        else{
        	mLockButton.setEnabled(true);
        }
        
    	
        // Combine list of hosted and joined groups
    	String[] groups = new String[hostedGroups.length + joinedGroups.length];
    	System.arraycopy(hostedGroups, 0, groups, 0, hostedGroups.length);
    	
    	int i = hostedGroups.length;
    	for(String group : joinedGroups){
    		groups[i++] = group;
    	}
    	
    	if(groups.length == 0) {
            groups = new String[1];
            groups[0] = "No Groups Available";
            mParticipantsButton.setEnabled(false);
        }
    	else{
    		mParticipantsButton.setEnabled(true);
    	}
    	
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, groups);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPingGroupList.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();
    }
    
    /*
     * Create the dialogs for each function
     */
    public static final int DIALOG_CREATE_GROUP_ID = 0;
    public static final int DIALOG_DESTROY_GROUP_ID = 1;
    public static final int DIALOG_JOIN_GROUP_ID = 2;
    public static final int DIALOG_LEAVE_GROUP_ID = 3;
    public static final int DIALOG_UNLOCK_GROUP_ID = 4;
    public static final int DIALOG_LOCK_GROUP_ID = 5;
    public static final int DIALOG_SELECT_GET_PEER_GROUP_ID = 6;
    public static final int DIALOG_SET_PORT_ID = 7;
    public static final int DIALOG_JOIN_OR_CREATE_ID = 8;
    public static final int DIALOG_GET_PEERS_ID = 9;
    public static final int DIALOG_ALLJOYN_ERROR_ID = 10;
    
    protected Dialog onCreateDialog(int id, Bundle args) {
        mUIHandler.logInfo("onCreateDialog(" + id + ")");
        Dialog result = null;
        switch(id) {
        case DIALOG_CREATE_GROUP_ID:
            { 
                DialogBuilder builder = new DialogBuilder();
                result = builder.createCreateGroupDialog(this, mUIHandler);
            }           
            break; 
        case DIALOG_DESTROY_GROUP_ID:
            { 
                DialogBuilder builder = new DialogBuilder();
                result = builder.createDestroyGroupDialog(this, mBusHandler);
            }           
            break;
        case DIALOG_JOIN_GROUP_ID:
            { 
                DialogBuilder builder = new DialogBuilder();
                result = builder.createJoinGroupDialog(this, mBusHandler, mUIHandler);
            }           
            break;
        case DIALOG_LEAVE_GROUP_ID:
            { 
                DialogBuilder builder = new DialogBuilder();
                result = builder.createLeaveGroupDialog(this, mBusHandler);
            }           
            break;
        case DIALOG_UNLOCK_GROUP_ID:
	        {
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createUnlockGroupDialog(this, mBusHandler);
	        }
	        break;
        case DIALOG_LOCK_GROUP_ID:
	        {
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createLockGroupDialog(this, mBusHandler);
	        }
	        break;
        case DIALOG_SET_PORT_ID:
	        {
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createSetPortDialog(this, mUIHandler);
	        }
	        break; 
        case DIALOG_JOIN_OR_CREATE_ID:
	        {
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createJoinOrCreateDialog(this, mUIHandler);
	        }
        	break;
        case DIALOG_GET_PEERS_ID:
            { 
                DialogBuilder builder = new DialogBuilder();
                String groupName = args.getString("groupName");
                result = builder.createParticipantsDialog(this, mBusHandler, groupName);
            }           
            break;
        case DIALOG_SELECT_GET_PEER_GROUP_ID:
            { 
                DialogBuilder builder = new DialogBuilder();
                result = builder.createSelectGroupDialog(this, mBusHandler);
            }           
            break;
        }
        return result;
    }
    
   
}
