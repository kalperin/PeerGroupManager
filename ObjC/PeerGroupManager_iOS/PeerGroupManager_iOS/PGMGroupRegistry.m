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

#import "PGMGroupRegistry.h"

@interface PGMGroupRegistry ()

@property (nonatomic, strong)NSMutableDictionary *groupNamesToGroup;
@property (nonatomic, strong)NSMutableDictionary *sessionIdToGroup;

@end

@implementation PGMGroupRegistry

@synthesize groupNamesToGroup = _groupNamesToGroup;
@synthesize sessionIdToGroup = _sessionIdToGroup;

- (id) init
{
    self = [super init];
    self.groupNamesToGroup = [[NSMutableDictionary alloc] init];
    self.sessionIdToGroup = [[NSMutableDictionary alloc] init];
    
    return self;
}

- (void)addGroupWithName:(NSString *)groupName onPort:(AJNSessionPort)sessionPort ofStatus:(PGMStatus)status
{
    PGMGroup *newGroup = [[PGMGroup alloc] init];
    NSString *groupNameCopy = [groupName copy];
    newGroup.groupName = groupNameCopy;
    newGroup.sessionPort = sessionPort;
    newGroup.status = status;
    
    [self.groupNamesToGroup setObject:newGroup forKey:groupNameCopy];
}

- (void)removeGroupWithName:(NSString *)groupName
{
    if(groupName == nil)
    {
        return;
    }
    
    id groupToRemove = [self.groupNamesToGroup objectForKey:groupName];
    if([groupToRemove isKindOfClass:[PGMGroup class]])
    {
        [self.groupNamesToGroup removeObjectForKey:groupName];
        
        if(groupToRemove)
        {
            [self.sessionIdToGroup removeObjectForKey:[NSNumber numberWithUnsignedInt:((PGMGroup *)groupToRemove).sessionId]];
        }
    }
}

- (NSString *)getGroupNameFromSessionId:(AJNSessionId)sessionId
{
    id group = [self.sessionIdToGroup objectForKey:[NSNumber numberWithUnsignedInt:sessionId]];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        return ((PGMGroup *)group).groupName;
    }
    
    return nil;
}

- (NSString *)getHostedGroupNameFromSessionPort:(AJNSessionPort)sessionPort
{
    NSEnumerator *enumerator = [self.groupNamesToGroup objectEnumerator];
    id value;
    
    while((value = [enumerator nextObject]))
    {
        if([value isKindOfClass:[PGMGroup class]])
        {
            PGMGroup * group = (PGMGroup *) value;
            if(group.sessionPort == sessionPort && (group.status == PGMHostedAndLocked || group.status == PGMHostedAndUnlocked))
            {
                return ((PGMGroup *)value).groupName;
            }
        }
    }
    
    return nil;
}

- (AJNSessionId)getSessionIdOfGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        return ((PGMGroup *)group).sessionId;
    }
    
    return 0;
}

- (AJNSessionPort)getSessionPortOfGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        return ((PGMGroup *)group).sessionPort;
    }
    
    return 0;
}

- (PGMStatus)getStatusOfGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        return ((PGMGroup *)group).status;
    }
    
    return PGMInvalid;
}

- (BOOL)isLegacyGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        return ((PGMGroup *)group).isLegacyGroup;
    }
    
    return NO;
}

- (void)updateGroup:(NSString *)groupName withSessionId:(AJNSessionId)sessionId
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        PGMGroup *groupToUpdate = (PGMGroup *)group;
        AJNSessionId oldSessionId = groupToUpdate.sessionId;
        groupToUpdate.sessionId = sessionId;
        [self.sessionIdToGroup removeObjectForKey:[NSNumber numberWithUnsignedInt:oldSessionId]];
        [self.sessionIdToGroup setObject:groupToUpdate forKey:[NSNumber numberWithUnsignedInt:sessionId]];
    }
}

- (void)updateGroup:(NSString *)groupName withStatus:(PGMStatus)status
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        ((PGMGroup *)group).status = status;
    }
}

- (void)updateGroup:(NSString *)groupName withSessionPort:(AJNSessionPort)sessionPort
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        ((PGMGroup *)group).sessionPort = sessionPort;
    }
}

- (void)updateGroup:(NSString *)groupName withLegacyFlag:(BOOL)isLegacyGroup
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        ((PGMGroup *)group).isLegacyGroup = isLegacyGroup;
    }
}

- (void)changeGroupNameFrom:(NSString *)oldName to:(NSString *)newName
{
    id group = [self.groupNamesToGroup objectForKey:oldName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        PGMGroup *groupToUpdate = (PGMGroup *)group;
        [self.groupNamesToGroup removeObjectForKey:oldName];
        groupToUpdate.groupName = newName;
        [self.groupNamesToGroup setObject:groupToUpdate forKey:newName];
    }
}

- (void)addPeer:(NSString *)peerId toGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        [((PGMGroup *)group).idsOfPeers addObject:peerId];
    }
}

- (void)removePeer:(NSString *)peerId fromGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        [((PGMGroup *)group).idsOfPeers removeObject:peerId];
    }
}

- (void)clearPeersOfGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        [((PGMGroup *)group).idsOfPeers removeAllObjects];
    }
}

- (NSArray *)getIdsOfPeersInGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        return [((PGMGroup *)group).idsOfPeers copy];
    }
    
    return nil;
}

- (int)getNumberOfPeersInGroup:(NSString *)groupName
{
    id group = [self.groupNamesToGroup objectForKey:groupName];
    
    if([group isKindOfClass:[PGMGroup class]])
    {
        return ((PGMGroup *)group).idsOfPeers.count;
    }
    
    return -1;
}

- (BOOL)doesGroupExistWithName:(NSString *)groupName
{
    return ([self.groupNamesToGroup objectForKey:groupName] != nil);
}

- (NSArray *)listFoundGroupNames
{
    NSMutableArray *foundGroupNames = [[NSMutableArray alloc] init];
    NSEnumerator *enumerator = [self.groupNamesToGroup objectEnumerator];
    id value;
    
    while((value = [enumerator nextObject]))
    {
        if([value isKindOfClass:[PGMGroup class]] && ((PGMGroup *)value).status == PGMFound)
        {
            [foundGroupNames addObject:((PGMGroup *)value).groupName];
        }
    }
    
    [foundGroupNames sortUsingSelector:@selector(caseInsensitiveCompare:)];
    return [foundGroupNames copy];
}

- (NSArray *)listHostedGroupNames
{
    NSMutableArray *hostedGroupNames = [[NSMutableArray alloc] init];
    NSEnumerator *enumerator = [self.groupNamesToGroup objectEnumerator];
    id value;
    
    while((value = [enumerator nextObject]))
    {
        if([value isKindOfClass:[PGMGroup class]])
        {
            PGMStatus status = ((PGMGroup *)value).status;
            if(status == PGMHostedAndLocked || status == PGMHostedAndUnlocked)
            {
               [hostedGroupNames addObject:((PGMGroup *)value).groupName];
            }
        }
    }
    
    [hostedGroupNames sortUsingSelector:@selector(caseInsensitiveCompare:)];
    return [hostedGroupNames copy];
}

- (NSArray *)listJoinedGroupNames
{
    NSMutableArray *joinedGroupNames = [[NSMutableArray alloc] init];
    NSEnumerator *enumerator = [self.groupNamesToGroup objectEnumerator];
    id value;
    
    while((value = [enumerator nextObject]))
    {
        if([value isKindOfClass:[PGMGroup class]])
        {
            PGMStatus status = ((PGMGroup *)value).status;
            if(status == PGMJoinedAndLocked || status == PGMJoinedAndUnlocked)
            {
                [joinedGroupNames addObject:((PGMGroup *)value).groupName];
            }
        }
    }
    
    [joinedGroupNames sortUsingSelector:@selector(caseInsensitiveCompare:)];
    return [joinedGroupNames copy];
}

- (NSArray *)listLockedGroupNames
{
    NSMutableArray *lockedGroupNames = [[NSMutableArray alloc] init];
    NSEnumerator *enumerator = [self.groupNamesToGroup objectEnumerator];
    id value;
    
    while((value = [enumerator nextObject]))
    {
        if([value isKindOfClass:[PGMGroup class]] && ((PGMGroup *)value).status == PGMHostedAndLocked)
        {
            [lockedGroupNames addObject:((PGMGroup *)value).groupName];
        }
    }
    
    [lockedGroupNames sortUsingSelector:@selector(caseInsensitiveCompare:)];
    return [lockedGroupNames copy];
}

@end
