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

#import "PGMPeerGroupManager.h"
#import "PGMGroupRegistry.h"

// AllJoyn Objective-C Framework headers
#import "AllJoynFramework/AJNInterfaceDescription.h"

// AllJoyn C++ Framework headers
#import "alljoyn/Message.h"
#import "alljoyn/ProxyBusObject.h"
#import "alljoyn/BusAttachment.h"

@interface PGMPeerGroupManager () <AJNBusListener, AJNSessionListener, AJNSessionPortListener>

@property (nonatomic, strong) AJNBusAttachment  *bus;
@property (nonatomic, strong) PGMGroupRegistry  *groupRegistry;
@property (nonatomic, strong) NSMutableArray    *registeredBusObjects;
@property (nonatomic, strong) NSMutableArray    *delegates;
@property (nonatomic, strong) NSString          *prefix;
@property (nonatomic, strong) AJNSessionOptions *defaultSessionOpts;
@property (nonatomic) AJNSessionPort            defaultSessionPort;

/*
 * Register the bus listener, connect the bus attachment to the bus,
 * start discovery, and register signal handlers.
 *
 * @return  OK if successful
 */
- (QStatus)connectBus;

/*
 * Check the given string to see if it is invalid or not. 
 *
 * An invalid string is nil or an empty string.
 *
 * @param stringParameter  the string to check.
 *
 * @return  YES if the string is invalid, NO otherwise.
 */
- (BOOL)isInvalidStringParameter:(NSString *)stringParameter;

/*
 * Log the given message from the specified function.
 *
 * @param message      the message to log.
 *
 * @param functionName the currently executing method.
 */
- (void)PGMLogMessage:(NSString *)message inFunction:(const char[])functioName;

/*
 * Log the given message and status from the specified function.
 *
 * @param message      the message to log.
 *
 * @param status       the QStatus to log.
 *
 * @param functionName the currently executing method.
 */
- (void)PGMLogMessage:(NSString *)message withStatus:(QStatus)status inFunction:(const char[])functionName;

/*
 * Check if the specified advertised name follows the naming convention of the 
 * Peer Group Manager.
 *
 * The advertised name is a Peer Group Manager advertised name if it follows the
 * following naming convention: group prefix + ".sp" + sessionPort + "." + groupName.
 *
 * @param advertisedName  the advertised name to check.
 *
 * @return YES if the the advertised name is a Peer Group Manager advertised name,
 *         NO otherwise.
 */
- (BOOL)isPeerGroupManagerAdvertisedName:(NSString *)advertisedName;

/*
 * Get the group name given the advertisedName.
 * 
 * The advertised name is expected to be a Peer Group Manager advertised name.
 *
 * @param advertisedName the advertised name to get the group name from.
 *
 * @return the group name that was extracted from the advertised name.
 *
 * @see isPeerGroupManagerAdvertisedName:
 */
- (NSString *)getGroupNameFromAdvertisedName:(NSString *)advertisedName;

/*
 * Generate the Peer Group Manager advertised name from the given group name.
 *
 * @param groupName the group name with which to generate the advertised name.
 *
 * @return the Peer Group Manager advertised name.
 * 
 * @see isPeerGroupManagerAdvertisedName:
 */
- (NSString *)getAdvertisedNameFromGroupName:(NSString *)groupName;

/*
 * Generate the legacy advertised name from the given group name.
 *
 * A legacy advertised name does not follow the Peer Group Manager's
 * naming convention of including the session port in the advertisement.
 *
 * @param groupName the group name with which to generate the advertised name.
 *
 * @return the Peer Group Manager advertised name.
 */
- (NSString *)getLegacyAdvertisedNameFromGroupName:(NSString *)groupName;

/*
 * Extract the session port out of the Peer Group Manager advertised name.
 *
 * @param advertisedName the advertised name to extract the session port from.
 *
 * @return the session port found in the Peer Group Manager advertised name.
 */
- (AJNSessionPort)getSessionPortFromAdvertisedName:(NSString *) advertisedName;

/*
 * Extract the group name from the given GUID advertised name used in
 * the joinOrCreateGroupWithName: method of the Peer Group Manager.
 *
 * @param GUIDName the GUID advertised name.
 *
 * @return the groupName found in the GUID advertised name.
 */
- (NSString *)getGroupNameFromGUIDName:(NSString *)GUIDName;

/*
 * Generate the GUID advertised name used in the joinOrCreateGroupWithName:
 * method of the Peer Group Manager given a group name
 *
 * @param groupName the group name to generate the GUID advertisement with.
 *
 * @return the GUID advertised name.
 */
- (NSString *)getGUIDNameFromGroupName:(NSString *)groupName;

/*
 * Check if the given group name is a GUID group that has your
 * GUID appended to the end.
 *
 * @param groupName the group name to check.
 *
 * @return YES if the group name is one of your GUID groups,
 *         NO otherwise.
 */
- (BOOL)isMyGUIDName:(NSString *)groupName;

/*
 * Check if the given group name is a GUID group that has a GUID
 * appended to the end.
 *
 * @param groupName the group name to check.
 *
 * @return YES if the group name is a GUID group, NO otherwise.
 */
- (BOOL)isAGUIDName:(NSString *)groupName;

/*
 * Change the name of a group.
 *
 * This includes updating the group in the group registry, changing the groups
 * advertisement and cleaning up the old advertisement.
 *
 * @param prevGroupName the name of the group to change.
 *
 * @param newGroupName  the name to change the group to.
 *
 * @return ER_OK if successful.
 */
- (QStatus)changeGroupName:(NSString *) prevGroupName toName:(NSString *)newGroupName;

@end


@implementation PGMPeerGroupManager

static volatile int32_t globalPendingJoins = 0;

@synthesize bus = _bus;
@synthesize groupRegistry = _groupRegistry;
@synthesize registeredBusObjects = _registeredBusObjects;
@synthesize delegates = _delegates;
@synthesize prefix = _prefix;
@synthesize defaultSessionOpts = _defaultSessionOpts;
@synthesize defaultSessionPort = _defaultSessionPort;

- (id)initWithGroupPrefix:(NSString *)prefix withPeerGroupDelegate:(id <PGMPeerGroupDelegate>)delegate withBusObjects:(NSArray *)busObjects
{
    self = [super init];
    if(self)
    {
        if([self isInvalidStringParameter:prefix])
        {
            return nil;
        }
        
        self.prefix = [[prefix stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] stringByAppendingString:@".sp"];
        self.groupRegistry = [[PGMGroupRegistry alloc] init];
        self.bus = [[AJNBusAttachment alloc] initWithApplicationName:prefix allowRemoteMessages:YES];
        self.registeredBusObjects = [[NSMutableArray alloc] init];
        self.defaultSessionOpts = [[AJNSessionOptions alloc] initWithTrafficType:kAJNTrafficMessages supportsMultipoint:YES proximity:kAJNProximityAny transportMask:kAJNTransportMaskAny];
        self.defaultSessionPort = PGMBadSessionPort;
            
        // Add the PGM Delegate
        self.delegates = [[NSMutableArray alloc] init];
        if(delegate != nil)
        {
            [self.delegates addObject:delegate];
        }
        
        QStatus status;
        // Add the Bus Objects
        for(AJNBusObject *busObject in busObjects)
        {
            status = [self registerBusObject:busObject];
        }
        
        // Connect the bus
        status = [self connectBus];
        if(status != ER_OK)
        {
            return nil;
        }
    }
    return self;
}

- (void)cleanup
{
    [self PGMLogMessage:@"doing cleanup" inFunction:__PRETTY_FUNCTION__];
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return;
    }

    // Leave all joined groups
    NSArray *joinedGroupNames = [self.groupRegistry listJoinedGroupNames];
    for (NSString *groupName in joinedGroupNames)
    {
        [self leaveGroupWithName:groupName];
    }

    // Destroy all hosted groups
    NSArray *hostedGroupNames = [self.groupRegistry listHostedGroupNames];
    for (NSString *groupName in hostedGroupNames)
    {
        [self destroyGroupWithName:groupName];
    }

    // Unregister all bus obejct
    for (AJNBusObject *busObject in self.registeredBusObjects)
    {
        [self.bus unregisterBusObject:busObject];
    }

    // Unregister peer group manager from being the busAttachments listener
    [self.bus unregisterBusListener:self];

    // Check if the default session port was set
    if(self.defaultSessionPort == PGMBadSessionPort)
    {
        // Cancel discovery on the group prefix + ".sp"
        [self.bus cancelFindAdvertisedName:self.prefix];
    }
    else
    {
        // Cancel discovery on the group prefix
        [self.bus cancelFindAdvertisedName:[self.prefix substringToIndex:(self.prefix.length - 2)]];
    }
    
    [self.bus disconnectWithArguments:@"null:"];
}

- (PGMJoinOrCreateReturn *)joinOrCreateGroupWithName:(NSString *)groupName
{
    QStatus status = ER_FAIL;
    
    // Check for a valid group prefix
    if([self isInvalidStringParameter:groupName])
    {
        [self PGMLogMessage:@"invalid string parameter" inFunction:__PRETTY_FUNCTION__];
        return [[PGMJoinOrCreateReturn alloc] initWithStatus:status withJoinFlag:YES];
    }
    
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return [[PGMJoinOrCreateReturn alloc] initWithStatus:status withJoinFlag:YES];
    }
    
    // If the group exists, join it
    if([self.groupRegistry doesGroupExistWithName:groupName])
    {
        status = [self joinGroupWithName:groupName];
        return [[PGMJoinOrCreateReturn alloc] initWithStatus:status withJoinFlag:YES];
    }
    
    NSString *guidName = [self getGUIDNameFromGroupName:groupName];
    
    // Create your JoC GUID Group
    status = [self createGroupWithName:guidName];
    
    if(status != ER_OK)
    {
        return [[PGMJoinOrCreateReturn alloc] initWithStatus:status withJoinFlag:NO];
    }
    
    // Block for 2 seconds to let the foundAdvertisedName() signals to come through
    NSDate *timeoutDate = [NSDate dateWithTimeIntervalSinceNow:2];
    BOOL block = YES;
    do {
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:timeoutDate];
        if ([timeoutDate timeIntervalSinceNow] < 0.0) {
            block = NO;
        }
    } while (block);
    
    // Check for the master session
    if([self.groupRegistry doesGroupExistWithName:groupName])
    {
        PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
        if(groupStatus == PGMFound)
        {
            // If the group has been found, join it
            status = [self joinGroupWithName:groupName];
            [self destroyGroupWithName:guidName];
            return [[PGMJoinOrCreateReturn alloc] initWithStatus:status withJoinFlag:YES];
        }
        else if(PGMHostedAndUnlocked)
        {
            // If we are hosting the master session (someone else joined our GUID session), return OK
            return [[PGMJoinOrCreateReturn alloc] initWithStatus:ER_OK withJoinFlag:NO];
        }
    }
    
    // Find the group with the highest GUID
    NSString *groupWithHighestGuid = guidName;
    
    for(NSString *group in [self listFoundGroupNames])
    {
        NSError *error = nil;
        NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:[NSString stringWithFormat:@"%@.JoC-\\S+",groupName] options:0 error:&error];
        if(error)
        {
            // TODO: handle error case
        }
        NSUInteger numberOfMatches = [regex numberOfMatchesInString:group options:0 range:NSMakeRange(0, group.length)];
        
        // If it's a JoC GUID group
        if(numberOfMatches > 0)
        {
            NSComparisonResult result = [group compare:groupWithHighestGuid];
            
            if(result == NSOrderedDescending)
            {
                groupWithHighestGuid = group;
            }
        }
    }
    
    // Check for the master session
    if([self.groupRegistry doesGroupExistWithName:groupName])
    {
        PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
        if(groupStatus == PGMFound)
        {
            // If the group has been found, join it
            status = [self joinGroupWithName:groupName];
            [self destroyGroupWithName:guidName];
            return [[PGMJoinOrCreateReturn alloc] initWithStatus:status withJoinFlag:YES];
        }
        else if(PGMHostedAndUnlocked)
        {
            // If we are hosting the master session (someone else joined our GUID session), return OK
            return [[PGMJoinOrCreateReturn alloc] initWithStatus:ER_OK withJoinFlag:NO];
        }
    }
    
    // Check who had the highest GUID
    if([groupWithHighestGuid isEqualToString:guidName])
    {
        // If we have the highest GUID, change the GUID session to a master session
        QStatus status = [self changeGroupName:guidName toName:groupName];
        return [[PGMJoinOrCreateReturn alloc] initWithStatus:status withJoinFlag:NO];
    }
    else
    {
        // Join the highest GUID session
        status = [self joinGroupWithName:groupWithHighestGuid];
        // Change name of the joined group to the master session name
        [self.groupRegistry changeGroupNameFrom:groupWithHighestGuid to:groupName];
        [self destroyGroupWithName:guidName];
        return [[PGMJoinOrCreateReturn alloc] initWithStatus:status withJoinFlag:YES];
    }
}

- (QStatus)createGroupWithName:(NSString *)groupName
{
    return [self createGroupWithName:groupName andLock:NO];
}

- (QStatus)createGroupWithName:(NSString *)groupName andLock:(BOOL)flag
{
    QStatus status = ER_FAIL;
    // Check for a valid group prefix
    if([self isInvalidStringParameter:groupName])
    {
        [self PGMLogMessage:@"invalid string parameter" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Bind the session port
    AJNSessionPort sessionPort = [self.bus bindSessionOnAnyPortWithOptions:self.defaultSessionOpts withDelegate:self];
    
    // Make sure the session port was bound successfully
    if(sessionPort == kAJNSessionPortAny)
    {
        [self PGMLogMessage:@"failed to bind port" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Make sure the group name isn't already taken
    if([self.groupRegistry doesGroupExistWithName:groupName])
    {
        [self PGMLogMessage:[NSString stringWithFormat:@"group with name %@ already exists", groupName] inFunction:__PRETTY_FUNCTION__];
    }
    else
    {
        // The group name is available
        [self PGMLogMessage:[NSString stringWithFormat:@"group name %@ is available", groupName] inFunction:__PRETTY_FUNCTION__];
        
        // Request the advertised name
        NSString *advertisedName = [self.prefix stringByAppendingString:[NSString stringWithFormat:@"%u.%@",sessionPort,groupName]];
        status = [self.bus requestWellKnownName:advertisedName withFlags:kAJNBusNameFlagDoNotQueue];
        [self PGMLogMessage:[NSString stringWithFormat:@"requested name %@",advertisedName] withStatus:status inFunction:__PRETTY_FUNCTION__];
        if(status == ER_OK)
        {
            // Add the group to the registry before advertising so we can filter out our own advertisements
            [self.groupRegistry addGroupWithName:groupName onPort:sessionPort ofStatus:(flag ? PGMHostedAndLocked : PGMHostedAndUnlocked)];
            if(!flag)
            {
                // Advertise the group if it is unlocked
                status = [self.bus advertiseName:advertisedName withTransportMask:self.defaultSessionOpts.transports];
                [self PGMLogMessage:[NSString stringWithFormat:@"advertised name %@",advertisedName] withStatus:status inFunction:__PRETTY_FUNCTION__];
            }
            
            if(status == ER_OK)
            {
                // Successful return
                return status;
            }
            // Release the advertised name
            [self.bus releaseWellKnownName:advertisedName];
            // Remove the group from the registry
            [self.groupRegistry removeGroupWithName:groupName];
        }
    }
    // Unbind the session port
    [self.bus  unbindSessionFromPort:sessionPort];
    return status;
}

- (QStatus)destroyGroupWithName:(NSString *)groupName
{
    QStatus status = ER_FAIL;
    // Check for a valid group prefix
    if([self isInvalidStringParameter:groupName])
    {
        [self PGMLogMessage:@"invalid string parameter" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return status;
    }

    // Get status of the group
    PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
    
    // Check if we own the group
    if(groupStatus == PGMHostedAndLocked || groupStatus == PGMHostedAndUnlocked)
    {
        // Get necessary group info
        AJNSessionPort sessionPort = [self.groupRegistry getSessionPortOfGroup:groupName];
        NSString *advertisedName = [self getAdvertisedNameFromGroupName:groupName];
        
        if(groupStatus == PGMHostedAndUnlocked)
        {
            // Stop advertising
            status = [self.bus cancelAdvertisedName:advertisedName withTransportMask:self.defaultSessionOpts.transports];
            [self PGMLogMessage:[NSString stringWithFormat:@"cancelled advertised name %@",advertisedName] withStatus:status inFunction:__PRETTY_FUNCTION__];
            if(status != ER_OK)
            {
                return status;
            }
        }
    
        // Release the advertised name
        status = [self.bus releaseWellKnownName:advertisedName];
        [self PGMLogMessage:[NSString stringWithFormat:@"released advertised name %@",advertisedName] withStatus:status inFunction:__PRETTY_FUNCTION__];
        if(status != ER_OK)
        {
            // Restore the advertisement if the group is unlocked
            if(groupStatus == PGMHostedAndUnlocked)
            {
                [self.bus advertiseName:advertisedName withTransportMask:self.defaultSessionOpts.transports];
            }
            return status;
        }
    
        // Unbind the session port
        status = [self.bus unbindSessionFromPort:sessionPort];
        [self PGMLogMessage:[NSString stringWithFormat:@"unbound session port %@",advertisedName] withStatus:status inFunction:__PRETTY_FUNCTION__];
        if(status != ER_OK)
        {
            // Re-request the advertised name
            [self.bus requestWellKnownName:advertisedName withFlags:kAJNBusNameFlagDoNotQueue];
            // Restore the advertisement if the group is unlocked
            if(groupStatus == PGMHostedAndUnlocked)
            {
                [self.bus advertiseName:advertisedName withTransportMask:self.defaultSessionOpts.transports];
            }
            return status;
        }
    
        // Remove group from the registry
        [self.groupRegistry removeGroupWithName:groupName];
    }
    else
    {
        [self PGMLogMessage:[NSString stringWithFormat:@"not hosting a group named %@",groupName] inFunction:__PRETTY_FUNCTION__];
    }
    return status;
}

- (QStatus)joinGroupWithName:(NSString *)groupName
{
    QStatus status = ER_FAIL;
    
    // Check for a valid group prefix
    if([self isInvalidStringParameter:groupName])
    {
        [self PGMLogMessage:@"invalid string parameter" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Get status of the group
    PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
    

    // Check if the group is avaialble to join
    if(groupStatus == PGMFound)
    {
        AJNSessionId sessionId = 0;
        AJNSessionPort sessionPort = [self.groupRegistry getSessionPortOfGroup:groupName];
        
        // Make sure we have a session port for the group
        if(sessionPort != PGMBadSessionPort)
        {
            // Get the advertised name
            NSString *advertisedName;
            if([self.groupRegistry isLegacyGroup:groupName])
            {
                advertisedName = [self getLegacyAdvertisedNameFromGroupName:groupName];
            }
            else
            {
                advertisedName = [self getAdvertisedNameFromGroupName:groupName];
            }
            // Connect with port found in advertisement
            [self PGMLogMessage:[NSString stringWithFormat:@"joining group %@ on port %hu with advertised name %@",groupName, sessionPort,advertisedName] inFunction:__PRETTY_FUNCTION__];
            
            int32_t pendingJoins = OSAtomicIncrement32Barrier(&globalPendingJoins);
            assert(pendingJoins == 1);
            
            sessionId = [self.bus joinSessionWithName:advertisedName onPort:sessionPort withDelegate:self options:self.defaultSessionOpts];
            
            pendingJoins = OSAtomicDecrement32Barrier(&globalPendingJoins);
            assert(pendingJoins == 0);

            if(sessionId <= 0) {
                sessionId = [self.bus joinSessionWithName:advertisedName onPort:self.defaultSessionPort withDelegate:self options:self.defaultSessionOpts];
                if(sessionId > 0) {
                    [self.groupRegistry updateGroup:groupName withSessionPort:self.defaultSessionPort];
                }
            }
        }
    
        if(sessionId > 0)
        {
            // Succesfully joined, so update sessionId and status of group
            status = ER_OK;
            [self.groupRegistry updateGroup:groupName withSessionId:sessionId];
            [self.groupRegistry updateGroup:groupName withStatus:PGMJoinedAndUnlocked];
            
            // Add yourself to the list of peers since sessionMemberAdded isn't triggered
            [self didAddMemberNamed:[self getMyPeerId] toSession:sessionId];
        }
        [self PGMLogMessage:[NSString stringWithFormat:@"joined group %@",groupName] withStatus:status inFunction:__PRETTY_FUNCTION__];
    }
    else
    {
        [self PGMLogMessage:[NSString stringWithFormat:@"no joinable group named %@",groupName] inFunction:__PRETTY_FUNCTION__];
    }
    return status;
}

- (QStatus)leaveGroupWithName:(NSString *)groupName
{
    QStatus status = ER_FAIL;

    // Check for a valid group prefix
    if([self isInvalidStringParameter:groupName])
    {
        [self PGMLogMessage:@"invalid string parameter" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Get status of the group
    PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
    
    // Check if the group was joined
    if(groupStatus == PGMJoinedAndUnlocked || groupStatus == PGMJoinedAndLocked)
    {
        AJNSessionId sessionId = [self.groupRegistry getSessionIdOfGroup:groupName];
        status = [self.bus leaveSession:sessionId];
        if(status == ER_OK)
        {
            if(groupStatus == PGMJoinedAndUnlocked)
            {
                // Group is still being advertised, so clear stale group info but keep group in registry
                [self.groupRegistry updateGroup:groupName withStatus:PGMFound];
                [self.groupRegistry updateGroup:groupName withSessionId:0];
                [self.groupRegistry clearPeersOfGroup:groupName];
            }
            else
            {
                // Group is no longer advertised or joinable, so remove group from registry
                [self.groupRegistry removeGroupWithName:groupName];
            }
        }
        [self PGMLogMessage:[NSString stringWithFormat:@"left group %@", groupName] withStatus:status inFunction:__PRETTY_FUNCTION__];
    }
    else
    {
        [self PGMLogMessage:[NSString stringWithFormat:@"have not joined a group named %@",groupName] inFunction:__PRETTY_FUNCTION__];
    }
    return status;
}

- (QStatus)unlockGroupWithName:(NSString *)groupName
{
    QStatus status = ER_FAIL;
    
    // Check for a valid group prefix
    if([self isInvalidStringParameter:groupName])
    {
        [self PGMLogMessage:@"invalid string parameter" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Get status of the group
    PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
    
    // Check if hosting the group
    if(groupStatus == PGMHostedAndLocked)
    {
        // Restart advertisement
        status = [self.bus advertiseName:[self getAdvertisedNameFromGroupName:groupName] withTransportMask:self.defaultSessionOpts.transports];
        [self PGMLogMessage:[NSString stringWithFormat:@"started advertising %@",[self getAdvertisedNameFromGroupName:groupName]] withStatus:status inFunction:__PRETTY_FUNCTION__];
        if(status == ER_OK)
        {
            // Update group to unlocked
            [self.groupRegistry updateGroup:groupName withStatus:PGMHostedAndUnlocked];
        }
    }
    else if(groupStatus == PGMHostedAndUnlocked)
    {
        // Group already unlocked, return OK status
        status = ER_OK;
    }
    else
    {
        [self PGMLogMessage:[NSString stringWithFormat:@"not hosting a group named %@",groupName] inFunction:__PRETTY_FUNCTION__];
    }
    
    [self PGMLogMessage:[NSString stringWithFormat:@"unlocked group %@",groupName] withStatus:status inFunction:__PRETTY_FUNCTION__];
    return status;
}

- (QStatus)lockGroupWithName:(NSString *)groupName
{
    QStatus status = ER_FAIL;
    
    // Check for a valid group prefix
    if([self isInvalidStringParameter:groupName])
    {
        [self PGMLogMessage:@"invalid string parameter" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Get status of the group
    PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
    
    // Check if hosting the groupß
    if(groupStatus == PGMHostedAndUnlocked)
    {
        // Stop advertisement
        status = [self.bus cancelAdvertisedName:[self getAdvertisedNameFromGroupName:groupName] withTransportMask:self.defaultSessionOpts.transports];
        [self PGMLogMessage:[NSString stringWithFormat:@"stopped advertising %@",[self getAdvertisedNameFromGroupName:groupName]] withStatus:status inFunction:__PRETTY_FUNCTION__];
        if(status == ER_OK)
        {
            // Update group to locked
            [self.groupRegistry updateGroup:groupName withStatus:PGMHostedAndLocked];
        }
    }
    else if(groupStatus == PGMHostedAndLocked)
    {
        // Group already locked, return OK status
        status = ER_OK;
    }
    else
    {
        [self PGMLogMessage:[NSString stringWithFormat:@"not hosting a group named %@",groupName] inFunction:__PRETTY_FUNCTION__];
    }
    
    [self PGMLogMessage:[NSString stringWithFormat:@"locked group %@",groupName] withStatus:status inFunction:__PRETTY_FUNCTION__];
    return status;
}

- (NSArray *)listFoundGroupNames
{
    //[self PGMLogMessage:[NSString stringWithFormat:@"found groups: %@",[self.groupRegistry listFoundGroupNames]] inFunction:__PRETTY_FUNCTION__];
    return [self.groupRegistry listFoundGroupNames];
}

- (NSArray *)listHostedGroupNames
{
    //[self PGMLogMessage:[NSString stringWithFormat:@"hosted groups: %@",[self.groupRegistry listHostedGroupNames]] inFunction:__PRETTY_FUNCTION__];
    return [self.groupRegistry listHostedGroupNames];
}

- (NSArray *)listJoinedGroupNames
{
    //[self PGMLogMessage:[NSString stringWithFormat:@"joined groups: %@",[self.groupRegistry listJoinedGroupNames]] inFunction:__PRETTY_FUNCTION__];
    return [self.groupRegistry listJoinedGroupNames];
}

- (NSArray *)listLockedGroupNames
{
    //[self PGMLogMessage:[NSString stringWithFormat:@"locked groups: %@",[self.groupRegistry listLockedGroupNames]] inFunction:__PRETTY_FUNCTION__];
    return [self.groupRegistry listLockedGroupNames];
}

- (NSArray *)getIdsOfPeersInGroup:(NSString *)groupName
{
    NSArray *idsOfPeers = [self.groupRegistry getIdsOfPeersInGroup:groupName];
    PGMStatus status = [self.groupRegistry getStatusOfGroup:groupName];
    
    // If the session is not formed yet, you are the only participant in the group
    if(idsOfPeers.count == 0 && (status == PGMHostedAndLocked || status == PGMHostedAndUnlocked))
    {
        return [[NSArray alloc] initWithObjects:[self getMyPeerId],nil];
    }
    
    return idsOfPeers;
}

- (int)getNumberOfPeersInGroup:(NSString *)groupName
{
    NSInteger numPeers = [self.groupRegistry getNumberOfPeersInGroup:groupName];
    PGMStatus status = [self.groupRegistry getStatusOfGroup:groupName];
    
    // If the session is not formed yet, you are the only participant in the group
    if(numPeers == 0 && (status == PGMHostedAndLocked || status == PGMHostedAndUnlocked))
    {
        return 1;
    }
    
    return numPeers;
}

- (void)addPeerGroupDelegate:(id <PGMPeerGroupDelegate>)delegate
{
    [self.delegates addObject:delegate];
}

- (void)removePeerGroupDelegate:(id <PGMPeerGroupDelegate>)delegate
{
    [self.delegates removeObject:delegate];
}

- (AJNProxyBusObject *)getRemoteObjectWithClassName:(NSString *)proxyClassName forPeer:(NSString *)peerId inGroup:(NSString *)groupName onPath:(NSString *)path
{
    // Get class from string argument, then alloc and init proxy bus object
    return [[NSClassFromString(proxyClassName) alloc] initWithBusAttachment:self.bus serviceName:peerId objectPath:path sessionId:[self.groupRegistry getSessionIdOfGroup:groupName]];
}

- (QStatus)registerBusObject:(AJNBusObject *)busObject
{
    QStatus status = ER_FAIL;
    // Register the bus object's interfaces 
    if([busObject respondsToSelector:@selector(registerInterfacesWithBus:)])
    {
        [self PGMLogMessage:@"Bus Object responds to selector registerInterfacesWithBus:" inFunction:__PRETTY_FUNCTION__];
        NSInvocation *method = [NSInvocation invocationWithMethodSignature:[[busObject class] instanceMethodSignatureForSelector:@selector(registerInterfacesWithBus:)]];
        [method setSelector:@selector(registerInterfacesWithBus:)];
        [method setTarget:busObject];
        AJNBusAttachment *bus = self.bus;
        [method setArgument:&bus atIndex:2];
        [method invoke];
        [method getReturnValue:&status];
        [self PGMLogMessage:@"Registering bus object interfaces" withStatus:status inFunction:__PRETTY_FUNCTION__];
    }
    
    if(status == ER_OK)
    {
        // Register the bus object
        status = [self.bus registerBusObject:busObject];
        [self PGMLogMessage:@"registered bus object" withStatus:status inFunction:__PRETTY_FUNCTION__];
        if(status == ER_OK)
        {
            // Store the bus object
            [self.registeredBusObjects addObject:busObject];
        }
    }
    
    // set a block to look up group name by session id
    if([busObject respondsToSelector:@selector(setGroupNameLookup:)])
    {
        __weak PGMPeerGroupManager *weakSelf = self;
        [busObject performSelector:@selector(setGroupNameLookup:) withObject:(^(AJNSessionId sessionId){
            return [weakSelf.groupRegistry getGroupNameFromSessionId:sessionId];
        })];
    }
    
    // set a block to look up session id by group name
    if([busObject respondsToSelector:@selector(setSessionIdLookup:)])
    {
        __weak PGMPeerGroupManager *weakSelf = self;
        [busObject performSelector:@selector(setSessionIdLookup:) withObject:(^(NSString *groupName){
            return [weakSelf.groupRegistry getSessionIdOfGroup:groupName];
        })];
    }
    return status;
}

- (QStatus)registerModule:(id <PGMPeerGroupModule>)module forGroup:(NSString *)groupName
{
    return [module registerOnBus:self.bus withSessionId:[self.groupRegistry getSessionIdOfGroup:groupName]];
}

- (NSString *)getGroupPrefix
{
    return [self.prefix substringToIndex:(self.prefix.length - 3)];
}

- (NSString *)getMyPeerId
{
    return self.bus.uniqueName;
}

- (NSString *)getGUID
{
    return self.bus.uniqueIdentifier;
}

- (NSString *)getHostPeerIdOfGroup:(NSString* )groupName
{
    // Create a C++ MsgArg that contains the advertised name
    ajn::MsgArg inArgs[1];
    inArgs[0].Set("s", [[self getAdvertisedNameFromGroupName:groupName] UTF8String]);

    // Grab the C++ Bus Attachment handle
    ajn::BusAttachment *busHandle = (ajn::BusAttachment*) self.bus.handle;
    
    // Pass the C++ BusAttachment handle to the C++ Message constructor
    ajn::Message reply(*busHandle);

    // Grab the C++ proxy object and call GetNameOwner
    QStatus status = busHandle->GetDBusProxyObj().MethodCall([@"org.freedesktop.DBus" UTF8String], "GetNameOwner", inArgs, 1, reply, 5000);

    [self PGMLogMessage:[NSString stringWithFormat:@"got name owner of group %@", groupName] withStatus:status inFunction:__PRETTY_FUNCTION__];
    if(status != ER_OK)
    {
        if(status == ER_BUS_REPLY_IS_ERROR_MESSAGE)
        {
            [self PGMLogMessage:[NSString stringWithFormat:@"ERROR - %s", reply->GetErrorDescription().c_str()] inFunction:__PRETTY_FUNCTION__];
        }
        return nil;
    }

    // Get the peerId of the owner from the reply Message
    NSString *hostPeerId = [NSString stringWithCString:reply->GetArg()->v_string.str encoding:NSUTF8StringEncoding];
    
    [self PGMLogMessage:[NSString stringWithFormat:@"Owner of group %@ is %@", groupName, hostPeerId] inFunction:__PRETTY_FUNCTION__];

    return hostPeerId;
    
    /* Pure Objective-C implementation */
//    NSString *advertisedName = [[NSString alloc] init];
//    NSString *hostPeerId = nil;
//    QStatus status = ER_FAIL;
//    
//    // Get the advertised name
//    if([self.groupRegistry isLegacyGroup:groupName]) {
//        advertisedName = [self getLegacyAdvertisedNameFromGroupName:groupName];
//    }
//    else
//    {
//        advertisedName = [self getAdvertisedNameFromGroupName:groupName];
//    }
//    
//    // Pack the advertised name in a MessageArg
//    AJNMessageArgument *firstArg = [[AJNMessageArgument alloc] init];
//    [firstArg setValue:@"s", [advertisedName UTF8String]];
//    // Pack the input arguments
//    NSArray *inArgs = [[NSArray alloc] initWithObjects:firstArg, nil];
//    
//    // Create the output message
//    AJNMessage *reply;
//    
//    // Call the method on the DBus Proxy Bus Object
//    status = [self.bus.dbusProxyObject callMethodWithName:@"GetNameOwner" onInterfaceWithName:@"org.freedesktop.DBus" withArguments:inArgs methodReply:&reply];
//    [reply performSelector:@selector(setShouldDeleteHandleOnDealloc:) withObject:NO];
//    
//    if(status == ER_OK)
//    {
//        [self PGMLogMessage:@"GETNAMEOWNER SUCCESS" inFunction:__PRETTY_FUNCTION__];
//    }
//    else{
//        [self PGMLogMessage:[NSString stringWithFormat:@"GETNAMEOWNER Failed with status - %s", QCC_StatusText(status)] inFunction:__PRETTY_FUNCTION__];
//        if(status == ER_BUS_REPLY_IS_ERROR_MESSAGE)
//        {
//            [self PGMLogMessage:[NSString stringWithFormat:@"ERROR - %@", reply.errorName] inFunction:__PRETTY_FUNCTION__];
//        }
//    }
//    
//    // Unpack the reply message
//    if(reply.arguments.count > 0) {
//        char *temp = NULL;
//        // Retrieve the MessageArg from the reply message
//        AJNMessageArgument *replyArg = [reply.arguments objectAtIndex:0];
//        // Unpack the string from the MessageArg
//        status = [replyArg value:@"s", &temp];
//        
//        if(status == ER_OK)
//        {
//            // Copy the string
//            hostPeerId = [NSString stringWithUTF8String:temp];
//        }
//    }
//
//    return hostPeerId;
}

- (void)setSessionPort:(AJNSessionPort)sessionPort
{
    QStatus status = ER_FAIL;
    if(self.defaultSessionPort == PGMBadSessionPort)
    {
        // Check for a valid bus attachment
        if(!self.bus)
        {
            [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
            return; 
        }

        // Look for a new prefix without the ".sp" limiter, opening up the found advertised names
        NSString *newAdvertisedName = [self.prefix substringToIndex:(self.prefix.length - 3)];
        status = [self.bus findAdvertisedName:newAdvertisedName];
        if(status == ER_OK)
        {
            // Stop looking for advertisements containing ".sp"
            self.defaultSessionPort = sessionPort;
            [self PGMLogMessage:[NSString stringWithFormat:@"Session Port set to %hu", sessionPort] inFunction:__PRETTY_FUNCTION__];
            [self.bus cancelFindAdvertisedName:self.prefix];
        }
    }
    else
    {
        self.defaultSessionPort = sessionPort;
        [self PGMLogMessage:[NSString stringWithFormat:@"Session Port set to %hu", sessionPort] inFunction:__PRETTY_FUNCTION__];
    }
}


#pragma mark - private methods

-(QStatus)connectBus
{
    QStatus status;
    
    // Register the bus listener
    [self.bus registerBusListener:self];
    [self PGMLogMessage:@"registered bus listener" inFunction:__PRETTY_FUNCTION__];
    
    // Start the bus
    status = [self.bus start];
    [self PGMLogMessage:@"started bus" withStatus:status inFunction:__PRETTY_FUNCTION__];
    if(status != ER_OK)
    {
        return status;
    }
    
    // Connect the bus
    status = [self.bus connectWithArguments:@"null:"];
    [self PGMLogMessage:@"connected bus" withStatus: status inFunction:__PRETTY_FUNCTION__];
    if(status != ER_OK)
    {
        return status;
    }

    // Start discovery
    status = [self.bus findAdvertisedName:self.prefix];
    [self PGMLogMessage:@"looking for advertised name" withStatus: status inFunction:__PRETTY_FUNCTION__];
    if(status != ER_OK)
    {
        return status;
    }
    
    // Block for 20 milliseconds to let the foundAdvertisedName() signals to come through
    NSDate *timeoutDate = [NSDate dateWithTimeIntervalSinceNow:0.2];
    BOOL block = YES;
    do {
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:timeoutDate];
        if ([timeoutDate timeIntervalSinceNow] < 0.0) {
            block = NO;
        }
    } while (block);
    
    return status;
}

- (BOOL)isInvalidStringParameter:(NSString *)stringParameter
{
    if(stringParameter == nil)
        return YES;
    if(stringParameter.length == 0)
        return YES;
    return NO;
}

- (void)PGMLogMessage:(NSString *)message inFunction:(const char[])functionName
{
    NSLog(@"%s - %@", functionName, message);
}

- (void)PGMLogMessage:(NSString *)message withStatus:(QStatus)status inFunction:(const char[])functionName
{
    NSLog(@"%s - %@: %s", functionName, message, QCC_StatusText(status));
}

- (BOOL)isPeerGroupManagerAdvertisedName:(NSString *)advertisedName
{
    NSError *error = nil;
    NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:[self.prefix stringByAppendingString:@"\\d+.\\S+"] options:0 error:&error];
    if(error)
    {
        // TODO: handle error case
    }
    NSRange match = [regex rangeOfFirstMatchInString:advertisedName options:0 range:NSMakeRange(0, advertisedName.length)];
    return (match.location == 0 && match.length == advertisedName.length);
}

- (NSString *)getGroupNameFromAdvertisedName:(NSString *)advertisedName
{
    if([self isPeerGroupManagerAdvertisedName:advertisedName])
    {
        NSError *error = nil;
        NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:[self.prefix stringByAppendingString:@"\\d+."] options:0 error:&error];
        if(error)
        {
            return nil;
        }
        return [regex stringByReplacingMatchesInString:advertisedName options:0 range:NSMakeRange(0, advertisedName.length) withTemplate:@""];
    }
    else
    {
        // Replace "prefix." with "" in "prefix.groupName"
        return [advertisedName stringByReplacingOccurrencesOfString:[self.prefix substringToIndex:(self.prefix.length - 2)] withString:@""];
    }
}

- (NSString *)getAdvertisedNameFromGroupName:(NSString *)groupName
{
    AJNSessionPort port = [self.groupRegistry getSessionPortOfGroup:groupName];
    return [NSString stringWithFormat:@"%@%hu.%@", self.prefix, port, groupName];
}

- (NSString *)getLegacyAdvertisedNameFromGroupName:(NSString *)groupName
{
    NSString *legacyPrefix = [self getGroupPrefix];
    return [NSString stringWithFormat:@"%@.%@", legacyPrefix, groupName];
}

- (AJNSessionPort)getSessionPortFromAdvertisedName:(NSString *) advertisedName
{
    if([self isPeerGroupManagerAdvertisedName:advertisedName])
    {
        NSString *sessionPortString = [advertisedName substringFromIndex:self.prefix.length];
        sessionPortString = [sessionPortString substringToIndex:[sessionPortString rangeOfString:@"."].location];
        return [sessionPortString integerValue];
    }
    else
    {
        return self.defaultSessionPort;
    }
}

- (NSString *)getGroupNameFromGUIDName:(NSString *)GUIDName
{
    return [GUIDName stringByReplacingOccurrencesOfString:[NSString stringWithFormat:@".JoC-%@", [self getGUID]] withString:@""];
}

- (NSString *)getGUIDNameFromGroupName:(NSString *)groupName
{
    return [NSString stringWithFormat:@"%@.JoC-%@",groupName,[self getGUID]];
}

- (BOOL)isMyGUIDName:(NSString *)groupName
{
    return [groupName hasSuffix:[NSString stringWithFormat:@".JoC-%@", [self getGUID]]];
}

- (BOOL)isAGUIDName:(NSString *)groupName
{
    return [groupName hasSuffix:[NSString stringWithFormat:@".JoC-%@", [self getGUID]]];
}

- (QStatus)changeGroupName:(NSString *)prevGroupName toName:(NSString *)newGroupName
{
    QStatus status = ER_FAIL;
    
    // Check for a valid group prefix
    if([self isInvalidStringParameter:newGroupName])
    {
        [self PGMLogMessage:@"invalid string parameter" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Check for a valid bus attachment
    if(!self.bus)
    {
        [self PGMLogMessage:@"bus is nil" inFunction:__PRETTY_FUNCTION__];
        return status;
    }
    
    // Make sure the new group name isn't already taken
    if([self.groupRegistry doesGroupExistWithName:newGroupName])
    {
        [self PGMLogMessage:[NSString stringWithFormat:@"group with name %@ already exists", newGroupName] inFunction:__PRETTY_FUNCTION__];
    }
    else
    {
        // The new group name is available
        [self PGMLogMessage:[NSString stringWithFormat:@"group name %@ is available", newGroupName] inFunction:__PRETTY_FUNCTION__];
        
        // Request the new advertised name
        AJNSessionPort sessionPort = [self.groupRegistry getSessionPortOfGroup:prevGroupName];
        NSString *newAdvertisedName = [NSString stringWithFormat:@"%@%hu.%@", self.prefix, sessionPort, newGroupName];
        status = [self.bus requestWellKnownName:newAdvertisedName withFlags:kAJNBusNameFlagDoNotQueue];
        [self PGMLogMessage:[NSString stringWithFormat:@"requested name %@",newAdvertisedName] withStatus:status inFunction:__PRETTY_FUNCTION__];
        
        if(status == ER_OK)
        {
            // Switch names in the group registry in preparation for the foundAdvertisedName call on the new group name
            [self.groupRegistry changeGroupNameFrom:prevGroupName to:newGroupName];

            // Advertise the new group first to avoid a gap in advertisement
            status = [self.bus advertiseName:newAdvertisedName withTransportMask:self.defaultSessionOpts.transports];
            [self PGMLogMessage:[NSString stringWithFormat:@"advertised name %@",newAdvertisedName] withStatus:status inFunction:__PRETTY_FUNCTION__];

            
            if(status == ER_OK)
            {
                // Stop advertising the old group name
                NSString *prevAdvertisedName = [NSString stringWithFormat:@"%@%hu.%@", self.prefix, sessionPort, prevGroupName];
                status = [self.bus cancelAdvertisedName:prevAdvertisedName withTransportMask:self.defaultSessionOpts.transports];
                [self PGMLogMessage:[NSString stringWithFormat:@"stopped advertising %@",prevAdvertisedName] withStatus:status inFunction:__PRETTY_FUNCTION__];
                // Successful return
                return status;
            }
            
            // Return to state before this method call
            // Change the group name back
            [self.groupRegistry changeGroupNameFrom:newGroupName to:prevGroupName];
            // Release the advertised name
            [self.bus releaseWellKnownName:newAdvertisedName];
        }
    }
    return status;
}



#pragma mark - AJNBusListener delegate methods

-(void) didFindAdvertisedName:(NSString *)name withTransportMask:(AJNTransportMask)transport namePrefix:(NSString *)namePrefix
{
    [self PGMLogMessage:[NSString stringWithFormat:@"found advertised name %@", name] inFunction:__PRETTY_FUNCTION__];
    NSString *groupName = [self getGroupNameFromAdvertisedName:name];
    PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:[self getGroupNameFromAdvertisedName:name]];
    
    // Ignore our own advertisements
    // If we found the name for the first time AND it either follows our naming convention OR we have a default session port set
    if(groupStatus == PGMInvalid && ([self isPeerGroupManagerAdvertisedName:name] || self.defaultSessionPort != PGMBadSessionPort))
    {
        AJNSessionPort sessionPort = [self getSessionPortFromAdvertisedName:name];
        [self PGMLogMessage:[NSString stringWithFormat:@"adding group %@ to the group registry on port %u", groupName,sessionPort] inFunction:__PRETTY_FUNCTION__];
        [self.groupRegistry addGroupWithName:groupName onPort:sessionPort ofStatus:PGMFound];
        if(sessionPort == self.defaultSessionPort){
            [self.groupRegistry updateGroup:groupName withLegacyFlag:YES];
        }

        // Trigger PGM delegates
        for (id<PGMPeerGroupDelegate> delegate in self.delegates)
        {
            if([delegate respondsToSelector:@selector(didFindAdvertisedName: withTransport:)])
            {
                [delegate didFindAdvertisedName:groupName withTransport:transport];
            }
        }
    }
    else if(groupStatus == PGMJoinedAndLocked)
    {
        // Update the status of the group from locked to unlocked
        [self PGMLogMessage:[NSString stringWithFormat:@"updating group %@ from joined and locked to joined and unlocked", groupName] inFunction:__PRETTY_FUNCTION__];
        [self.groupRegistry updateGroup:groupName withStatus:PGMJoinedAndUnlocked];
    }
}

-(void) didLoseAdvertisedName:(NSString *)name withTransportMask:(AJNTransportMask)transport namePrefix:(NSString *)namePrefix
{
    NSString *groupName = [self getGroupNameFromAdvertisedName:name];
    PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
    [self PGMLogMessage:[NSString stringWithFormat:@"lost advertised name %@", name] inFunction:__PRETTY_FUNCTION__];

    // Act depending on group status
    switch (groupStatus) {
        case PGMFound:
            // If the group is only found but not joined, remove it from the registry
            [self PGMLogMessage:[NSString stringWithFormat:@"removing group %@ from the group registry", groupName] inFunction:__PRETTY_FUNCTION__];
            [self.groupRegistry removeGroupWithName:groupName];
            break;
        case PGMJoinedAndUnlocked:
            // If the group has been joined and just now stopped advertising,
            // mark it as locked so it can be remove from the registry when leaving the group
            [self PGMLogMessage:[NSString stringWithFormat:@"updating group %@ from joined and unlocked to joined and locked", groupName] inFunction:__PRETTY_FUNCTION__];
            [self.groupRegistry updateGroup:groupName withStatus:PGMJoinedAndLocked];
            break;
        default:
            break;
    }
    
    // Ignore our own advertisements
    // If the advertised name follows our naming convention OR we have a default session port set
    if((groupStatus == PGMFound || groupStatus == PGMJoinedAndUnlocked) && ([self isPeerGroupManagerAdvertisedName:name] || self.defaultSessionPort != PGMBadSessionPort))
    {
        // Trigger PGM Delegates
        for (id<PGMPeerGroupDelegate> delegate in self.delegates)
        {
            if([delegate respondsToSelector:@selector(didLoseAdvertisedName: withTransport:)])
            {
                [delegate didLoseAdvertisedName:groupName withTransport:transport];
            }
        }
    }
}


#pragma mark - AJNSessionListener delegate methods

-(void) didAddMemberNamed:(NSString *)memberName toSession:(AJNSessionId)sessionId
{
    NSString *groupName = [self.groupRegistry getGroupNameFromSessionId:sessionId];
    
    [self PGMLogMessage:[NSString stringWithFormat:@"adding peer %@ to group %@ with sessionId %u", memberName, groupName, sessionId] inFunction:__PRETTY_FUNCTION__];
    // Add the peer to the group
    [self.groupRegistry addPeer:memberName toGroup:groupName];
    
    // Call the Peer Group delegates
    for (id<PGMPeerGroupDelegate> delegate in self.delegates)
    {
        if([delegate respondsToSelector:@selector(didAddPeer: toGroup: forATotalOf:)])
        {
            [delegate didAddPeer:memberName toGroup:groupName forATotalOf:[self.groupRegistry getNumberOfPeersInGroup:groupName]];
        }
    }
}

-(void) didRemoveMemberNamed:(NSString *)memberName fromSession:(AJNSessionId)sessionId
{
    NSString *groupName = [self.groupRegistry getGroupNameFromSessionId:sessionId];
    
    [self PGMLogMessage:[NSString stringWithFormat:@"removing peer %@ from group %@ with sessionId %u", memberName, groupName, sessionId] inFunction:__PRETTY_FUNCTION__];
    // Remove the peer from the group
    [self.groupRegistry removePeer:memberName fromGroup:groupName];
    
    // Call the Peer Group delegates
    for (id<PGMPeerGroupDelegate> delegate in self.delegates)
    {
        if([delegate respondsToSelector:@selector(didRemovePeer: fromGroup: forATotalOf:)])
        {
            [delegate didRemovePeer:memberName fromGroup:groupName forATotalOf:[self.groupRegistry getNumberOfPeersInGroup:groupName]];
        }
    }
}

-(void) sessionWasLost:(AJNSessionId)sessionId
{
    NSString *groupName = [self.groupRegistry getGroupNameFromSessionId:sessionId];
    PGMStatus groupStatus = [self.groupRegistry getStatusOfGroup:groupName];
    
    [self PGMLogMessage:[NSString stringWithFormat:@"lost group %@ with sessionId %u", groupName, sessionId] inFunction:__PRETTY_FUNCTION__];
    // Remove the group from the registry if you are a joiner
    if(groupStatus == PGMJoinedAndLocked)
    {
        [self.groupRegistry removeGroupWithName:groupName];
    }
    else if(groupStatus == PGMJoinedAndUnlocked)
    {
        [self.groupRegistry updateGroup:groupName withStatus:PGMFound];
    }

    // Call the Peer Group delegates
    for (id<PGMPeerGroupDelegate> delegate in self.delegates)
    {
        if([delegate respondsToSelector:@selector(groupWasLost:)])
        {
            [delegate groupWasLost:groupName];
        }
    }
}


#pragma mark - AJNSessionPortListener delegate methods

-(void) didJoin:(NSString *)joiner inSessionWithId:(AJNSessionId)sessionId onSessionPort:(AJNSessionPort)sessionPort
{
    // Get the groupName
    NSString *groupName = [self.groupRegistry getHostedGroupNameFromSessionPort:sessionPort];
    [self PGMLogMessage:[NSString stringWithFormat:@"peer %@ has joined group %@", joiner, groupName] inFunction:__PRETTY_FUNCTION__];
    
    if(!groupName)
    {
        return;
    }
    
    // Set the group's session id
    [self.groupRegistry updateGroup:groupName withSessionId:sessionId];
    
    NSArray *peers = [self.groupRegistry getIdsOfPeersInGroup:groupName];
    NSString *myPeerId = [self getMyPeerId];
    if(![peers containsObject:myPeerId])
    {
        // Explicitly add yourself as a peer in the group
        [self didAddMemberNamed:myPeerId toSession:sessionId];
        // Explicitly add the first joiner
        [self didAddMemberNamed:joiner toSession:sessionId];
        // Set the session listener for the session
        [self.bus bindSessionListener:self toSession:sessionId];
        // If the group is our JoC GUID session, change it to the master session
        if([self isMyGUIDName:groupName])
        {
            // Strip off the .JoC-GUID suffix from the advertisement
            NSString *newGroupName = [self getGroupNameFromGUIDName:groupName];
            // Change the group name and its advertisement
            QStatus status = [self changeGroupName:groupName toName:newGroupName];
            [self PGMLogMessage:[NSString stringWithFormat:@"changed name of join or create group from %@ to %@", groupName, newGroupName] withStatus:status inFunction:__PRETTY_FUNCTION__];
        }
    }
    
}

-(BOOL) shouldAcceptSessionJoinerNamed:(NSString *)joiner onSessionPort:(AJNSessionPort)sessionPort withSessionOptions:(AJNSessionOptions *)options
{
    // Variables to be used in the block defined below
    __weak __typeof__(self) weakSelf = self;
    __block BOOL shouldAccept = NO;
    __block dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    
    // Ths callback occurs on an AllJoyn thread, so access to the group registry has
    // to be done carefully to ensure thread safety. So we define a block to do the
    // work, and then later decide if the block can be executed immediately, or if it
    // needs to be queued up in the main dispatcher thread.
    dispatch_block_t dispatchBlock = ^{
        if (weakSelf)
        {
            NSString *groupName = [weakSelf.groupRegistry getHostedGroupNameFromSessionPort:sessionPort];
            PGMStatus groupStatus = [weakSelf.groupRegistry getStatusOfGroup:groupName];
            
            // Check if the group is unlocked
            if(groupStatus == PGMHostedAndUnlocked)
            {
                // Accept the joiner if the group is unlocked
                [self PGMLogMessage:[NSString stringWithFormat:@"accepted joiner %@ in group %@", joiner, groupName] inFunction:"shouldAcceptSessionJoinerNamed"];
                shouldAccept = YES;
            }
            else
            {
                // Reject the joiner if the group is locked
                [self PGMLogMessage:[NSString stringWithFormat:@"rejected joiner %@ in group %@", joiner, groupName] inFunction:"shouldAcceptSessionJoinerNamed"];
                shouldAccept = NO;
            }
        }
        
        dispatch_semaphore_signal(semaphore);
    };
    
    // Check the join count to see if this callback was triggered by an attempt
    // within our address space to join a PGM group.
    int32_t pendingJoins = OSAtomicOr32Barrier(0, (uint32_t *)&globalPendingJoins);
    
    if (pendingJoins == 1)
    {
        // The pending join count should only be 1 if a call to joinSessionWithName is
        // occurring within our address space (and therefore necessarily blocking the
        // main thread). This primarily occurs when running the unit tests. In this
        // case, we cannot place the block on the main queue because it is waiting for
        // us to return (if we did, the wait on the semaphore below would time out and
        // we would always reject any join requests). Fortunately, however, we don't
        // have to worry about race conditions in accessing the group registry, because
        // all user code calls to this class must originate on the main thread (which
        // is blocked). So we can go ahead and safely invoke the dispatch block
        // immediately.
        dispatchBlock();
    }
    else if (pendingJoins == 0)
    {
        // The pending join count should be 0 if a call to joinSessionWithName from
        // outside our address space triggered this callback. In this case, the main
        // thread is not known to be blocked. So to ensure thread safety, we must
        // dispatch the block to the main queue (which serializes access to this class)
        // and wait for the result.
        dispatch_async(dispatch_get_main_queue(), dispatchBlock);
    }
    else
    {
        // A pending join count of something other than 0 or 1 means something is wrong.
        assert(NO);

        [self PGMLogMessage:@"invalid join count" inFunction:"shouldAcceptSessionJoinerNamed"];
        
        dispatch_semaphore_signal(semaphore);
    }

    // Wait up to 2 seconds for the results (this should only actually delay execution
    // when the pending join count is 0). If we dispatched immediately, or if there
    // was an error, the semaphore should already be signaled.
    if (dispatch_semaphore_wait(semaphore, dispatch_time(DISPATCH_TIME_NOW, 2LL * NSEC_PER_SEC)))
    {
        [self PGMLogMessage:[NSString stringWithFormat:@"timeout checking joiner %@ on port %d", joiner, sessionPort] inFunction:"shouldAcceptSessionJoinerNamed"];        
    }

    // The call to dispatch_release() here would be required if the iOS version
    // was below 6.0. This project currently has a minimum target of 6.1, so we
    // don't need to do the release.
    //
    //dispatch_release(semaphore);
    
    return shouldAccept;
}

@end
