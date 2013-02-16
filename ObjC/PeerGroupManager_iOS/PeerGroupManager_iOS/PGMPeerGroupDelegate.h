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
#import "AllJoynFramework/AJNTransportMask.h"

/**
 * The PeerGroupDelegate is a protocol to be implemented if the app wishes to listen
 * to AllJoyn call-back methods. The call-back methods in this protocol are
 * equivalent to the didFindAdvertisedName:withTransportMask:namePrefix:, 
 * didLoseAdvertisedName:withTransportMask:namePrefix:, sessionWasLost:,
 * didAddMemberNamed:toSession:, and didRemoveMemberNamed:toSession: call-backs of AllJoyn.
 */
@protocol PGMPeerGroupDelegate <NSObject>

@optional
/**
 * Called when a new group advertisement is found. 
 *
 * This will not be triggered for locked groups or your own hosted groups.
 *
 * @param groupName  the groupName that was found.
 *
 * @param transport  the transport that the groupName was discovered on.
 */
- (void)didFindAdvertisedName:(NSString *)groupName withTransport:(AJNTransportMask)transport;

/**
 * Called when a group that was previously reported through
 * didFindAdvertisedName:withTransport: has become unavailable. 
 *
 * This will not be triggered for your own hosted groups. This will get
 * triggered for a group that was previously unlocked and has now become
 * locked.
 *
 * @param groupName  the group name that has become unavailable.
 *
 * @param transport  the transport that stopped receiving the groupName
 *                   advertisement.
 */
- (void)didLoseAdvertisedName:(NSString *)groupName withTransport:(AJNTransportMask)transport;

/**
 * Called when a group becomes disconnected.
 *
 * @param groupName  the group that became disconnected.
 */
- (void)groupWasLost:(NSString *)groupName;

/**
 * Called when a new peer joins a group.
 *
 * @param peerId     the id of the peer that joined.
 *
 * @param groupName  the group that the peer joined.
 *
 * @param numPeers   the current number of peers in the group.
 */
- (void)didAddPeer:(NSString *)peerId toGroup:(NSString *)groupName forATotalOf:(int)numPeers;

/**
 * Called when a new peer leaves a group.
 *
 * @param peerId     the id of the peer that left.
 *
 * @param groupName  the group that the peer left.
 *
 * @param numPeers   the current number of peers in the group.
 */
- (void)didRemovePeer:(NSString*)peerId fromGroup:(NSString *)groupName forATotalOf:(int)numPeers;
@end