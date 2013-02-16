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

// AllJoyn core headers
#import "alljoyn/Status.h"

/**
 * This class is returned by the 
 * [joinOrCreateGroupWithName:]([PGMPeerGroupManager joinOrCreateGroupWithName:]) 
 * method of the peer group manager.
 *
 */
@interface PGMJoinOrCreateReturn : NSObject

/**
 * The AllJoyn status return from either calling 
 * [joinGroupWithName:]([PGMPeerGroupManager joinGroupWithName:])  or
 * [createGroupWithName:]([PGMPeerGroupManager createGroupWithName:]) 
 * in the PeerGroupManager.
 *
 */
@property (nonatomic, readonly) QStatus status;

/**
 * A flag denoting whether the user joined or created the group.
 *
 * YES if the caller joined the group, NO if the caller created
 *         the group
 */
@property (nonatomic, readonly) BOOL isJoiner;

/*
 * Initialize a PGMJoinOrCreateReturn object with the specified QStatus
 * and join flag.
 */
- (id)initWithStatus: (QStatus)status withJoinFlag: (BOOL)isJoiner;

@end

