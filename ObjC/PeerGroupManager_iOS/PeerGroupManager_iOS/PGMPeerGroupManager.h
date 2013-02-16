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

#import <Foundation/Foundation.h>
#import <Foundation/NSInvocation.h>

// AllJoyn Objective-C framework headers
#import "AllJoynFramework/AJNBusAttachment.h"
#import "AllJoynFramework/AJNProxyBusObject.h"
#import "AllJoynFramework/AJNBusObject.h"

#import "PGMPeerGroupDelegate.h"
#import "PGMJoinOrCreateReturn.h"
#import "PGMPeerGroupModule.h"

/**
 * The Peer Group Manager is a library that provides app developers with an
 * easy entry point into peer-to-peer programming using AllJoyn. A peer group
 * is simply an established connection between peers that allows them to
 * communicate over AllJoyn. With this library, developers can use AllJoyn with
 * minimum knowledge of AllJoyn concepts which is great for rapid prototyping.
 * The Peer Group Manager provides a mapping between high level AllJoyn
 * concepts and underlying implementation details abstracting out some of the
 * more subtle aspects that arenít always relevant to new developers of
 * AllJoyn. It also encapsulates a lot of boilerplate AllJoyn code for typical
 * applications including setting default parameters, enforcing a naming
 * convention for advertisements, etc. making the code simpler and cleaner.
 * This component only covers common use cases for typical AllJoyn applications
 * so if your project is advanced in nature, then this component might not be
 * right for you.
 */
@interface PGMPeerGroupManager: NSObject 

/**
 * Initialize the PeerGroupManager and register the provided PeerGroupDelegate
 * and bus objects. 
 *
 * The PeerGroupManager will automatically connect to the AllJoyn
 * bus and start discovering groups created by other
 * PeerGroupManagers that use the same group prefix.
 *
 * @param prefix       the prefix of the advertised name that will be used
 *                     for advertisement and discovery.
 * 
 * @param delegate     the PeerGroupDelegate to register. This can be null.
 *
 * @param busObjects   the bus objects to register. This can be nil. Bus
 *                     objects can be registered later via call to
 *                     registerBusObject:
 *
 * @warning            *NOTE:* Your PeerGroupManager will only be able to
 *                     communicate with other PeerGroupManagers that use
 *                     the same group prefix.
 */
- (id)initWithGroupPrefix:(NSString *)prefix withPeerGroupDelegate:(id <PGMPeerGroupDelegate>)delegate withBusObjects:(NSArray *)busObjects;

/** 
 * Permanently disconnect the PeerGroupManager from the AllJoyn daemon.
 *
 * Disconnecting includes leaving all currently joined groups,
 * destroying all currently hosted groups, unregistering all bus objects,
 * unregistering all listeners, unregistering all signal handlers,
 * disconnecting from the AllJoyn bus and releasing all resources.
 *
 * @warning *NOTE:* This renders the PeerGroupManager instance unusable and cannot be
 *          reversed. It is a programming error to call another method on the
 *          PeerGroupManager after the cleanup: method has been called. A new
 *          instance of the PeerGroupManager will need to be created if you wish to
 *          continue using it.
 */
- (void)cleanup;

/**
 * Create an unlocked group and advertise it for other peers to discover
 * and join. 
 *
 * Multiple groups can be created at any given time given that all the
 * group names are unique. The call will fail if createGroup: is called
 * with a group name of a group that already exists.
 *
 * @param groupName  the name of the group to create.
 *
 * @return  ER_OK if successful.
 *
 * @warning *NOTE:* You are inherently a peer of your own groups so you 
 *          do not need to call joinGroupWithName: on groups you create.
 */
- (QStatus)createGroupWithName:(NSString *)groupName;

/**
 * Create a locked or unlocked group and advertise it for other peers to discover
 * and join.
 *
 * Multiple groups can be created at any given time given that all the
 * group names are unique. The call will fail if createGroup: is called
 * with a group name of a group that already exists.
 *
 * @param groupName  the name of the group to create.
 *
 * @param flag       - YES - the group will be locked preventing
 *                   peers from discovering and joining the group until it
 *                   is unlocked.
 *                   - NO - the group will be
 *                   unlocked allowing peers to discover and join the
 *                   group.
 *
 * @return  ER_OK if successful.
 *
 * @warning *NOTE:* You are inherently a peer of your own groups so you
 *          do not need to call joinGroupWithName: on groups you create.
 */
- (QStatus)createGroupWithName:(NSString *)groupName andLock:(BOOL)flag;

/**
 * Stop the advertisement of a group you are hosting and destroy it.
 *
 * The advertised name is also released and the session port is unbound.
 * You can only destroy groups that you have created.
 *
 * @param groupName  the name of the group to destroy.
 *
 * @return  ER_OK if successful.
 */
- (QStatus)destroyGroupWithName:(NSString *)groupName;

/**
 * join an existing unlocked group.
 *
 * @param groupName  the name of the group to join.
 *
 * @return  ER_OK if successful.
 *
 * @warning *NOTE:* You cannot join groups that you have created because you are
 *          already inherently a peer in your own groups.
 */
- (QStatus)joinGroupWithName:(NSString *)groupName;

/**
 * leave the specified group you are currently joined to.
 *
 * @param groupName  the name of the group to leave.
 *
 * @return  ER_OK if successful.
 *
 * @warning *NOTE:* You cannot leave groups that you have created because you are
 *          inherently a peer in your own groups.
 */
- (QStatus)leaveGroupWithName:(NSString *)groupName;

/**
 * Join the specified group if it exists, otherwise create it.
 * 
 *
 * @param groupName  the name of the group to join or create.
 *
 * @return  PGMJoinOrCreateReturn  an object containing a flag to tell whether
 *                                 the method tried to join or create
 *                                 a group and the AllJoyn return status of
 *                                 the operation.
 *
 * @warning *NOTE:* There are opportunities for race conditions to occur during
 *          startup where two peers might not see each other and both create the
 *          group. This method is intended to be used for test and developmental
 *          purposes only.
 */
- (PGMJoinOrCreateReturn *)joinOrCreateGroupWithName:(NSString *)groupName;

/**
 * Unlock the specified group allowing peers to discover and join it. 
 *
 * You can only unlock groups that you have created and are currently hosting.
 *
 * @param groupName  the hosted group to unlock.
 *
 * @return  ER_OK if successful.
 *
 * @see lockGroupWithName:
 */
- (QStatus)unlockGroupWithName:(NSString *)groupName;

/**
 * Lock the specified group preventing peers from discovering and joining
 * it until unlockGroupWithName: is called. 
 *
 * You can only lock groups that you have created and are currently hosting.
 *
 * @param groupName  the hosted group to lock.
 *
 * @return  ER_OK if successful.
 *
 * @see unlockGroupWithName:
 */
- (QStatus)lockGroupWithName:(NSString *)groupName;

/**
 * List the group names of all of the discovered unlocked groups that you are not
 * currently participating in.
 *
 * Groups that you are hosting and groups that you are joined to are not included
 * in this list.
 *
 * @return  an NSArray of group names (NSStrings) of all the found groups.
 */
- (NSArray *)listFoundGroupNames;

/**
 * List the group names of all the groups you have created and are
 * currently hosting.
 *
 * @return  an NSArray of group names (NSStrings) of all the groups you are currently
 *          hosting.
 */
- (NSArray *)listHostedGroupNames;

/**
 * List the group names of all the groups you have successfully joined.
 *
 * @return  an NSArray of group names (NSStrings) of all the groups you are currently
 *          joined to.
 */
- (NSArray *)listJoinedGroupNames;

/**
 * List the group names of all the groups you are hosting that are
 * currently locked.
 *
 * @return  an NSArray of group names (NSStrings) of all the groups you are hosting
 *          that are currently locked.
 */
- (NSArray *)listLockedGroupNames;

/**
 * Get the PeerIds of all peers in the specified group. 
 *
 * You must be a participant in the group (either as a host or a joiner) to
 * be able to get the peers of a group.
 *
 * @param groupName  the name of the group to get the peers of.
 *
 * @return  an NSArray of PeerIds (NSStrings) of all peers in the group.
 */
- (NSArray *)getIdsOfPeersInGroup:(NSString *)groupName;

/**
 * Get the number of peers in the specified group. 
 *
 * You must be a participant in the group (either as a host or a joiner) to
 * be able to get the peers of a group.
 *
 * @param groupName  the name of the group to get the number of peers of.
 *
 * @return  the number of peers in the group or -1 if the
 *          groupName is invalid.
 */
- (int)getNumberOfPeersInGroup:(NSString *)groupName;

/**
 * Add an additional delegate to detect the didFindAdvertisedName:,
 * didLoseAdvertisedName:, groupWasLost:, didAddPeer:, and
 * didRemovePeer: signals. 
 *
 * Multiple delegates can be added at any time without affecting the 
 * previous delegates.
 *
 * @param delegate  the delegate with the desired call-back methods
 *                  to invoke.
 */
- (void)addPeerGroupDelegate:(id <PGMPeerGroupDelegate>)delegate;

/**
 * Remove a delegate so that it no longer detects the didFindAdvertisedName:,
 * didLoseAdvertisedName:, groupWasLost:, didAddPeer:, and
 * didRemovePeer: signals.
 *
 * @param delegate  the delegate to remove.
 */
- (void)removePeerGroupDelegate:(id <PGMPeerGroupDelegate>)delegate;

/**
 * Get a proxy bus object used for making remote method calls. 
 *
 * This object is only valid for the specified group.
 *
 * @param proxyClassName the name of the proxy bus object class generated 
 *                       by the codegen tool.
 *
 * @param peerId         the id of the peer to make the remote method calls on.
 *
 * @param groupName      the name of the group to communicate over.
 *
 * @param path     the object path of the remote bus object.
 *
 * @return  a proxy bus object to a remote bus object that can be used for making
 *          remote method calls or nil if an error occurred.
 */
- (AJNProxyBusObject *)getRemoteObjectWithClassName:(NSString *)proxyClassName forPeer:(NSString *)peerId inGroup:(NSString *)groupName onPath:(NSString *)path;

/**
 * Register a bus object on the bus.
 *
 * @param busObject  the bus object to register.
 *
 * @return  ER_OK if successful
 */
- (QStatus)registerBusObject:(AJNBusObject *)busObject;

/**
 * Register a module with the PeerGroupManager to allow it to communicate
 * over the specified group. 
 *
 * This will provide the module with the BusAttachment and the session Id for the group.
 *
 * @param module     the PGMPeerGroupModule to register.
 *
 * @param groupName  the name of the group to give the module access to.
 *
 * @return  ER_OK if successful
 */
- (QStatus)registerModule:(id <PGMPeerGroupModule>)module forGroup:(NSString *)groupName;

/**
 * Get the group prefix used for discovery when creating the PeerGroupManager.
 *
 * The PeerGroupManager will only discover other groups created by another 
 * PeerGroupManager using the same group prefix.
 *
 * @return  the group prefix used for discovery when creating the PeerGroupManager.
 */
- (NSString *)getGroupPrefix;

/**
 * Get the your own peer id. 
 *
 * This is the id that peers receive in the didAddPeer:, and didRemovePeer: call-back 
 * methods of the PeerGroupManagerDelegate when you join or leave a group.
 *
 * @return  your peer id or nil if an error occurred.
 */
- (NSString *)getMyPeerId;

/**
 * Get the your globally unique id. 
 *
 * The returned value may be appended to a group name in order to guarantee that the 
 * resulting name is globally unique.
 *
 * @return  your globally unique id.
 */
- (NSString *)getGUID;

/**
 * Get the peer id of host of the specified group.
 *
 * @param groupName  the name of the group to get the host's peer id from.
 *
 * @return  the peer id of the group host or nil if the group is not found.
 *
 * @warning *NOTE:* You must have been previously connected to the peer to 
 *          get the peer id.
 */
- (NSString *)getHostPeerIdOfGroup:(NSString *)groupName;

/**
 * Set the session port to be used for joining legacy AllJoyn applications 
 * that are not using the PeerGroupManager. 
 *
 * Groups that are not created by the PeerGroupManager might not follow
 * the same naming convention for their advertisements so this allows
 * your PeerGroupManager to infer a session port in case it can not 
 * obtain it from the advertisement. 
 *
 * @param sessionPort  the session port to use for joining
 *
 * @warning *NOTE:* This is an advanced feature. It will also start a new 
 *          mode of discovery that will allow the PeerGroupManager to 
 *          discover advertisements that do not follow the its 
 *          advertisement naming convention which includes the session port.
 */
- (void)setSessionPort:(AJNSessionPort)sessionPort;

@end

