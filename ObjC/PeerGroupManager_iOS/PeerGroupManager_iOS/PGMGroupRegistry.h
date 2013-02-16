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

// AllJoyn Objective-C Framework headers
#import "AllJoynFramework/AJNSessionOptions.h"

#import "PGMGroup.h"

/* 
 * A class containing collections of groups. Encapsulates group data for the Peer Group Manager,
 * while also providing a single source to synchronize on for multithreading safety. 
 */
@interface PGMGroupRegistry : NSObject

/*
 * Initialize the group registry.
 *
 * @return the inialized group registry.
 */
- (id) init;

/*
 * Create and initialize a new group and add it to the registry.
 *
 * @param groupName the name of the group.
 *
 * @param sessionPort  the session port of the group.
 *
 * @param status the state that the group is in in respect to the user.
 */
- (void)addGroupWithName:(NSString *)groupName onPort:(AJNSessionPort)sessionPort ofStatus:(PGMStatus)status;

/*
 * Remove a group from the registry.
 *
 * @param groupName the name of the group to remove.
 */
- (void)removeGroupWithName:(NSString *)groupName;

/*
 * Get the group name of the group with the specified a session id.
 *
 * @param sessionId the session id of the group.
 *
 * @return the name of the group with the specified session id or nil if 
 *         the group was not found.
 */
- (NSString *)getGroupNameFromSessionId:(AJNSessionId)sessionId;

/*
 * Get the name of your hosted group with the specified session port.
 *
 * @param sessionPort the session port of the group.
 *
 * @return the name of your hosted group with the specified port or nil
 *         if the group was not found.
 */
- (NSString *)getHostedGroupNameFromSessionPort:(AJNSessionPort)sessionPort;

/*
 * Get the session id of the group with the specified group name.
 *
 * @param groupName the name of the group to get the session id of.
 *
 * @return the session id of the group or 0 if the group was not found.
 */
- (AJNSessionId)getSessionIdOfGroup:(NSString *)groupName;

/*
 * Get the session port of the group with the specified group name.
 *
 * @param groupName the name of the group to get the session id of.
 *
 * @return the session id of the group or 0 if the group was not found.
 */
- (AJNSessionPort)getSessionPortOfGroup:(NSString *)groupName;

/*
 * Get the PGMStatus (state) of the specified group.
 *
 * @param groupName the name of the group to get the PGMStatus of.
 *
 * @return the PGMStatus of the group.
 */
- (PGMStatus)getStatusOfGroup:(NSString *)groupName;

/*
 * Check if the specified group is a legacy group.
 *
 * A legacy group is a group that advertises a name which does not
 * conform to the Peer Group Manager's naming convention of including
 * the session port.
 *
 * @param groupName the name of the group to check.
 *
 * @return YES if the group is a legacy group, NO otherwise.
 */
- (BOOL)isLegacyGroup:(NSString *)groupName;

/*
 * Update the session id of the specified group.
 *
 * @param groupName the name of the group to update.
 *
 * @param sessionId the new sessionId of the group.
 */
- (void)updateGroup:(NSString *)groupName withSessionId:(AJNSessionId)sessionId;

/*
 * Update the PGMStatus of the group.
 *
 * @param groupName the name of the group to update.
 *
 * @param status the new PGMStatus of the group.
 */
- (void)updateGroup:(NSString *)groupName withStatus:(PGMStatus)status;

/*
 * Update the session port of the specified group.
 *
 * @param groupName the name of the group to update.
 *
 * @param sessionPort the new session port of the group.
 */
- (void)updateGroup:(NSString *)groupName withSessionPort:(AJNSessionPort)sessionPort;

/*
 * Update the legacy flag of the specified group.
 *
 * @param groupName the name of the group to update.
 *
 * @param isLegacyGroup YES if the group is a legacy group, NO otherwise.
 */
- (void)updateGroup:(NSString *)groupName withLegacyFlag:(BOOL)isLegacyGroup;

/*
 * Change the name of the specified group.
 *
 * @param oldName the name of the group to change.
 *
 * @param newName the new name to change the group to.
 */
- (void)changeGroupNameFrom:(NSString *)oldName to:(NSString *)newName;

/*
 * Add a peer to the specified group.
 *
 * @param peerId the id of the peer to add.
 *
 * @param groupName the name of the group to add the peer to.
 */
- (void)addPeer:(NSString *)peerId toGroup:(NSString *)groupName;

/*
 * Remove a peer from the specified group.
 *
 * @param peerId the id of the peer to remove.
 *
 * @param groupName the name of the group to remove the peer from.
 */
- (void)removePeer:(NSString *)peerId fromGroup:(NSString *)groupName;

/*
 * Remove all the peers of the specified group.
 *
 * @param groupName the name of the group to clear the peers of.
 */
- (void)clearPeersOfGroup:(NSString *)groupName;

/*
 * Get the peer ids of all the peers currently participating in the specified
 * group.
 *
 * @param groupName the name of the group to get the peer ids of.
 *
 * @return an NSArray of peer ids (NSStrings) of all the peers participating
 *         in the group.
 */
- (NSArray *)getIdsOfPeersInGroup:(NSString *)groupName;

/*
 * Get the number of peers currently participating in the specified group.
 *
 * @param groupName the name of the group to get the number of peers of.
 *
 * @return the number of peers currently participating in the group.
 */
- (int)getNumberOfPeersInGroup:(NSString *)groupName;

/*
 * Check if the specified group exists in the registry.
 *
 * @param groupName the name of the group to check.
 *
 * @return YES if the group exists in the registry, NO otherwise.
 */
- (BOOL)doesGroupExistWithName:(NSString *)groupName;

/*
 * List the group names of all of the discovered unlocked groups that you are not
 * currently participating in.
 *
 * Groups that you are hosting and groups that you are joined to are not included
 * in this list.
 *
 * @return  an NSArray of group names (NSStrings) of all the found groups.
 */
- (NSArray *)listFoundGroupNames;

/*
 * List the group names of all the groups you have created and are
 * currently hosting.
 *
 * @return  an NSArray of group names (NSStrings) of all the groups you are currently
 *          hosting.
 */
- (NSArray *)listHostedGroupNames;

/*
 * List the group names of all the groups you have successfully joined.
 *
 * @return  an NSArray of group names (NSStrings) of all the groups you are currently
 *          joined to.
 */
- (NSArray *)listJoinedGroupNames;

/*
 * List the group names of all the groups you are hosting that are
 * currently locked.
 *
 * @return  an NSArray of group names (NSStrings) of all the groups you are hosting
 *          that are currently locked.
 */
- (NSArray *)listLockedGroupNames;

@end
