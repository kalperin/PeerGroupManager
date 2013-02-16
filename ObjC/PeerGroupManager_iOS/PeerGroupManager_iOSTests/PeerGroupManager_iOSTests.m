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

#import "AllJoynFramework/AJNSessionOptions.h"
#import "AllJoynFramework/AJNVersion.h"

#import "PGMPeerGroupDelegate.h"
#import "PGMPeerGroupManager.h"

#import "PeerGroupManager_iOSTests.h"

/*
 * The CountFoundDelegate counts found advertisements, and remembers
 * the latest added peer.
 */
@interface CountFoundDelegate : NSObject <PGMPeerGroupDelegate>
@property (nonatomic, strong) NSMutableArray *peerAddedPeerIDs;
@property (nonatomic) int countFoundAdvertisedName;
@end

@implementation CountFoundDelegate
@synthesize peerAddedPeerIDs = _peerAddedPeerIDs;
@synthesize countFoundAdvertisedName = _countFoundAdvertisedName;

- (id)init
{
    self = [super init];
    
    if (self)
    {
        self.peerAddedPeerIDs = [[NSMutableArray alloc] init];
        self.countFoundAdvertisedName = 0;
    }
    
    return self;
}

- (void)didFindAdvertisedName:(NSString *)groupName withTransport:(AJNTransportMask)transport
{
    self.countFoundAdvertisedName++;
}

- (void)didAddPeer:(NSString *)peerId toGroup:(NSString *)groupName forATotalOf:(int)numPeers
{
    if([self.peerAddedPeerIDs containsObject: peerId] == NO)
    {
        [self.peerAddedPeerIDs addObject: [NSString stringWithString: peerId]];
    }
}
@end


/*
 * The CountFoundAndLostDelegate does the same things as the PeerGroupTestFindDelegate,
 * but also counts lost advertisements.
 */
@interface CountFoundAndLostDelegate : CountFoundDelegate
@property (nonatomic) int countLostAdvertisedName;
@end

@implementation CountFoundAndLostDelegate
@synthesize countLostAdvertisedName = _countLostAdvertisedName;

- (id)init
{
    self = [super init];
    
    if (self)
    {
        self.countLostAdvertisedName = 0;
    }
    
    return self;
}

- (void)didLoseAdvertisedName:(NSString *)groupName withTransport:(AJNTransportMask)transport
{
    self.countLostAdvertisedName++;
}
@end


/*
 */
@interface SessionPortDelegate : NSObject< AJNSessionPortListener>
@property (nonatomic) int sessionID;
@end

@implementation SessionPortDelegate
@synthesize sessionID = _sessionID;

- (id)init
{
    self = [super init];
    
    if (self)
    {
        self.sessionID = 0;
    }
    
    return self;
}

- (BOOL)shouldAcceptSessionJoinerNamed:(NSString *)joiner onSessionPort:(AJNSessionPort)sessionPort withSessionOptions:(AJNSessionOptions *)options
{
    NSLog(@"[SessionPortDelegate shouldAcceptSessionJoinerNamed:] - accepted joiner %@", joiner);
    return YES;
}

- (void)didJoin:(NSString *)joiner inSessionWithId:(AJNSessionId)sessionId onSessionPort:(AJNSessionPort)sessionPort
{
    NSLog(@"[SessionPortDelegate didJoin:] - joiner %@", joiner);
    self.sessionID = sessionId;
}

@end


/*
 * Test class starts here.
 */
@interface PeerGroupManager_iOSTests()

// Properties
@property (nonatomic) NSInteger alljoynVersion;

@property (nonatomic, strong) CountFoundAndLostDelegate *delegate1;
@property (nonatomic, strong) PGMPeerGroupManager *pgm1;

@property (nonatomic, strong) CountFoundAndLostDelegate *delegate2;
@property (nonatomic, strong) PGMPeerGroupManager *pgm2;

@property (nonatomic, strong) AJNBusAttachment *bus;
@property (nonatomic, strong) SessionPortDelegate *spDelegate;

// Helper Methods
- (void)setUpPortTests: (AJNSessionPort) port;
- (void)tearDownPortTests;

- (QStatus)callPGM: (PGMPeerGroupManager *) pgm methodWithName: (NSString *)method withParameter: (NSString *)parameter;
- (QStatus)callPGM: (PGMPeerGroupManager *) pgm methodWithName: (NSString *)method withParameter: (NSString *)parameter withDelay: (NSTimeInterval)seconds;
- (BOOL)checkIfString: (NSString *)str isInArray: (NSArray *) arr;
- (void)delay: (NSTimeInterval)seconds;

@end

@implementation PeerGroupManager_iOSTests

// Constants 
static NSString * const kAppName = @"PeerGroupManager";
static NSString * const kGroupPrefix = @"test";
static NSString * const kGroupName = @"testSetPort";
static NSString * const kMethodCreateGroupWithName = @"createGroupWithName:";
static NSString * const kMethodJoinGroupWithName = @"joinGroupWithName:";
static NSString * const kMethodLeaveGroupWithName = @"leaveGroupWithName:";
static NSString * const kMethodDestroyGroupWithName = @"destroyGroupWithName:";
static NSString * const kMethodLockGroupWithName = @"lockGroupWithName:";
static NSString * const kMethodUnlockGroupWithName = @"unlockGroupWithName:";
static const int kContactPort = 47;

// Synthesize!
@synthesize alljoynVersion = _alljoynVersion;

@synthesize delegate1 = _delegate1;
@synthesize pgm1 = _pgm1;

@synthesize delegate2 = _delegate2;
@synthesize pgm2 = _pgm2;

@synthesize bus = _bus;
@synthesize spDelegate = _spDelegate;

- (void)setUp
{
    [super setUp];
    
    NSLog(@"Start of test");

    self.alljoynVersion = [AJNVersion versionNumber];
    
    self.delegate1 = [[CountFoundAndLostDelegate alloc] init];
    self.pgm1 = [[PGMPeerGroupManager alloc] initWithGroupPrefix: kGroupPrefix withPeerGroupDelegate: self.delegate1 withBusObjects: nil];

    self.delegate2 = [[CountFoundAndLostDelegate alloc] init];
    self.pgm2 = [[PGMPeerGroupManager alloc] initWithGroupPrefix: kGroupPrefix withPeerGroupDelegate: self.delegate2 withBusObjects: nil];
    
    [self delay: 0.5f];
}

- (void)setUpPortTests: (AJNSessionPort) port
{
    self.bus = [[AJNBusAttachment alloc] initWithApplicationName: kAppName allowRemoteMessages: YES];
    [self.bus start];
    [self.bus connectWithArguments: @"null:"];
    
    AJNSessionOptions *options = [[AJNSessionOptions alloc] init];
    [options setTrafficType: kAJNTrafficMessages];
    [options setIsMultipoint: YES];
    [options setProximity: kAJNProximityAny];
    [options setTransports: kAJNTransportMaskAny];
    
    self.spDelegate = [[SessionPortDelegate alloc] init];
    
    [self.bus bindSessionOnPort: port withOptions: options withDelegate: self.spDelegate];
    
    int flags = kAJNBusNameFlagReplaceExisting | kAJNBusNameFlagDoNotQueue;
    NSString *name = [NSString stringWithFormat: @"%@.%@", kGroupPrefix, kGroupName];
    
    [self.bus requestWellKnownName: name withFlags: flags];
    [self.bus advertiseName: name withTransportMask: kAJNTransportMaskAny];
    
    [self.pgm1 setSessionPort: kContactPort];
    [self delay: 0.5f];
}

- (void)tearDown
{
    [self.pgm1 cleanup];
    [self.pgm2 cleanup];
    
    self.delegate1 = nil;
    self.pgm1 = nil;

    self.delegate2 = nil;
    self.pgm2 = nil;
    
    [super tearDown];

    [self delay: 0.5f];
    
    NSLog(@"End of test");
}

- (void)tearDownPortTests
{
    [self.bus leaveSession:0];
    [self.bus destroy];
    
    self.bus = nil;
    self.spDelegate = nil;
    
    [self delay: 0.5f];
}

- (void)testCreateNormal
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);

    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    
    STAssertTrue([self checkIfString: groupName isInArray: [self.pgm1 listHostedGroupNames]], @"Hosted group not listed");
    STAssertFalse([self checkIfString: groupName isInArray: [self.pgm1 listFoundGroupNames]], @"Unfound group listed");
}

- (void)testCreateTwice
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);

    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName] == ER_FAIL, @"Group created twice" );
}

- (void)testCreateFound
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodCreateGroupWithName withParameter: groupName] == ER_FAIL, @"Group created by both PGMs" );
}

- (void)testCreateInput
{
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: nil] == ER_FAIL, @"Created nil group" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: @""] == ER_FAIL, @"Created empty group" );
}

- (void)testJoinNormal
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self.pgm1 getNumberOfPeersInGroup: groupName] == 1, @"Wrong peer count" );
    
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not joined" );
    STAssertTrue([self checkIfString: groupName isInArray: [self.pgm2 listJoinedGroupNames]], @"Joined group not listed");

    STAssertTrue([self.pgm1 getNumberOfPeersInGroup: groupName] == 2, @"Wrong peer count" );
}

- (void)testJoinNonexistant
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Joined nonexistant group" );
    STAssertFalse([self checkIfString: groupName isInArray: [self.pgm1 listJoinedGroupNames]], @"Unjoined group listed");
}

- (void)testJoinTwice
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not joined" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Joined group twice" );
}

- (void)testJoinHosting
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Joined hosted group" );
}

- (void)testJoinInput
{
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: nil withDelay: 0.5f] == ER_FAIL, @"Joined nil group" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: @"" withDelay: 0.5f] == ER_FAIL, @"Joined empty group" );
}

- (void)testDestroyNormal
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not joined" );

    STAssertTrue([self checkIfString: groupName isInArray: [self.pgm2 listJoinedGroupNames]], @"Joined group not listed");

    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodLeaveGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not left" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodDestroyGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not destroyed" );

    STAssertFalse([self checkIfString: groupName isInArray: [self.pgm1 listHostedGroupNames]], @"Destroyed group listed");
    STAssertFalse([self checkIfString: groupName isInArray: [self.pgm2 listFoundGroupNames]], @"Left group listed");
}

- (void)testDestroyNonexistant
{
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodDestroyGroupWithName withParameter: groupName] == ER_FAIL, @"Destroyed nonexistant group" );
}

- (void)testDestroyInput
{
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodDestroyGroupWithName withParameter: nil] == ER_FAIL, @"Destroyed nil group" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodDestroyGroupWithName withParameter: @""] == ER_FAIL, @"Destroyed empty group" );
}

- (void)testDestroyTwice
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodDestroyGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not destroyed" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodDestroyGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Destroyed group twice" );
}

- (void)testDestroyNotOwner
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );

    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodDestroyGroupWithName withParameter: groupName] == ER_FAIL, @"Destroyed found group" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not joined" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodDestroyGroupWithName withParameter: groupName] == ER_FAIL, @"Destroyed joined group" );
}


- (void)testLeaveNormal
{
    // These tests require AllJoyn 3.3 or higher
    if (self.alljoynVersion > 0x03020000)
    {
        // The group name is the same as the name of the test method
        NSString *groupName = NSStringFromSelector(_cmd);
        
        STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
        STAssertTrue([self.pgm1 getNumberOfPeersInGroup: groupName] == 1, @"Wrong peer count" );

        STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not joined" );
        STAssertTrue([self checkIfString: groupName isInArray: [self.pgm2 listJoinedGroupNames]], @"Joined group not listed");

        STAssertTrue([self.pgm1 getNumberOfPeersInGroup: groupName] == 2, @"Wrong peer count" );

        STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodLeaveGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not left" );
        STAssertFalse([self checkIfString: groupName isInArray: [self.pgm2 listJoinedGroupNames]], @"Left group listed");

        STAssertTrue([self.pgm1 getNumberOfPeersInGroup: groupName] == 1, @"Wrong peer count" );
    }
}

- (void)testLeaveNotJoined
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);

    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodLeaveGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Left unjoind group" );
}

- (void)testLeaveTwice
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );

    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not joined" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodLeaveGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not left" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodLeaveGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Group left twice" );
}

- (void)testLeaveHosting
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );

    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodLeaveGroupWithName withParameter: groupName] == ER_FAIL, @"Left hosted group" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not joined" );

    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodLeaveGroupWithName withParameter: groupName] == ER_FAIL, @"Left hosted group" );
}

- (void)testLeaveNonexistant
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodLeaveGroupWithName withParameter: groupName] == ER_FAIL, @"Left nonexistant group" );
}

- (void)testLeaveInput
{
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodLeaveGroupWithName withParameter: nil] == ER_FAIL, @"Left nil group" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodLeaveGroupWithName withParameter: @""] == ER_FAIL, @"Left empty group" );
}

- (void)testLockAndUnlockNormal
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodLockGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not locked" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Joined locked group" );

    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodUnlockGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not unlocked" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Unlocked group not joined" );
}

- (void)testLockAndUnlockNotOwner
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );

    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodLockGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Locked found group" );
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodUnlockGroupWithName withParameter: groupName withDelay: 0.5f] == ER_FAIL, @"Unlocked found group" );
}

- (void)testLockAndUnlockNonexistant
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodLockGroupWithName withParameter: groupName] == ER_FAIL, @"Locked nonexistant group" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodUnlockGroupWithName withParameter: groupName] == ER_FAIL, @"Unlocked nonexistant group" );
}


- (void)testUnlockAndLockInput
{
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodLockGroupWithName withParameter: nil] == ER_FAIL, @"Locked nil group" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodLockGroupWithName withParameter: @""] == ER_FAIL, @"Locked empty group" );

    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodUnlockGroupWithName withParameter: nil] == ER_FAIL, @"Unlocked nil group" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodUnlockGroupWithName withParameter: @""] == ER_FAIL, @"Unlocked empty group" );
}

- (void)testFoundAdvertisedName
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );

    STAssertTrue(self.delegate2.countFoundAdvertisedName == 1, @"Wrong found count");
}

- (void)testLostAdvertisedName
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
    
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodDestroyGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not destroyed" );

    STAssertTrue(self.delegate2.countLostAdvertisedName == 1, @"Wrong lost count");
}

- (void)testPeerAdded
{
    // The group name is the same as the name of the test method
    NSString *groupName = NSStringFromSelector(_cmd);
        
    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodCreateGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: groupName withDelay: 0.5f] == ER_OK, @"Group not joined" );
    
    STAssertTrue([self checkIfString: [self.pgm1 getMyPeerId] isInArray: self.delegate1.peerAddedPeerIDs], @"Missing peer ID");
    STAssertTrue([self checkIfString: [self.pgm2 getMyPeerId] isInArray: self.delegate2.peerAddedPeerIDs], @"Missing peer ID");
    
    // These tests require AllJoyn 3.3 or higher
    if (self.alljoynVersion > 0x03020000)
    {
        STAssertTrue([self checkIfString: [self.pgm1 getMyPeerId] isInArray: self.delegate2.peerAddedPeerIDs], @"Missing peer ID");
    }
}

- (void)testMultipleListeners
{    
    // The group name is the same as the name of the test method
    NSString *groupName1 = NSStringFromSelector(_cmd);
    NSString *groupName2 = [NSString stringWithFormat:@"%@2", groupName1];
    
    CountFoundDelegate *delegate3 = [[CountFoundDelegate alloc] init];
    CountFoundAndLostDelegate *delegate4 = [[CountFoundAndLostDelegate alloc] init];
        
    [self.pgm2 addPeerGroupDelegate: delegate3];
    [self.pgm2 addPeerGroupDelegate: delegate4];

    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodCreateGroupWithName withParameter: groupName1 withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodDestroyGroupWithName withParameter: groupName1 withDelay: 0.5f] == ER_OK, @"Group not destroyed" );

    STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodCreateGroupWithName withParameter: groupName2 withDelay: 0.5f] == ER_OK, @"Group not created" );
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: groupName2 withDelay: 0.5f] == ER_OK, @"Group not joined" );

    STAssertTrue((self.delegate2.countFoundAdvertisedName + delegate3.countFoundAdvertisedName + delegate4.countFoundAdvertisedName) == 3, @"Wrong found count");
    STAssertTrue((self.delegate2.countLostAdvertisedName + delegate4.countLostAdvertisedName) == 2, @"Wrong lost count");

    STAssertTrue([self checkIfString: [self.pgm1 getMyPeerId] isInArray: self.delegate1.peerAddedPeerIDs], @"Missing peer ID");
    STAssertTrue([self checkIfString: [self.pgm2 getMyPeerId] isInArray: self.delegate2.peerAddedPeerIDs], @"Missing peer ID");
    
    // These tests require AllJoyn 3.3 or higher
    if (self.alljoynVersion > 0x03020000)
    {
        STAssertTrue([self checkIfString: [self.pgm1 getMyPeerId] isInArray: self.delegate2.peerAddedPeerIDs], @"Missing peer ID");
    }
}


- (void)testSetPortNormal
{
    [self setUpPortTests: kContactPort];

    STAssertTrue([self checkIfString: kGroupName isInArray: [self.pgm1 listFoundGroupNames]], @"Group not listed");
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: kGroupName withDelay: 0.5f] == ER_OK, @"Group not joined" );
    STAssertTrue([self checkIfString: kGroupName isInArray: [self.pgm1 listJoinedGroupNames]], @"Group not listed");

    [self tearDownPortTests];
}

- (void)testSetPortWrongPort
{
    [self setUpPortTests: (kContactPort + 10)];

    STAssertTrue([self checkIfString: kGroupName isInArray: [self.pgm1 listFoundGroupNames]], @"Group not listed");
    STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: kGroupName withDelay: 0.5f] == ER_FAIL, @"Group not joined" );
    STAssertFalse([self checkIfString: kGroupName isInArray: [self.pgm1 listJoinedGroupNames]], @"Unjoined group listed");

    [self tearDownPortTests];
}

- (void)testSetPortWithTwoGroups
{
    // These tests require AllJoyn 3.3 or higher
    if (self.alljoynVersion > 0x03020000)
    {
        [self setUpPortTests: kContactPort];

        STAssertTrue([self checkIfString: kGroupName isInArray: [self.pgm1 listFoundGroupNames]], @"Group not listed");
        STAssertTrue([self callPGM: self.pgm2 methodWithName: kMethodCreateGroupWithName withParameter: kGroupName withDelay: 0.5f] == ER_OK, @"Group not created" );
        STAssertTrue([self checkIfString: kGroupName isInArray: [self.pgm1 listFoundGroupNames]], @"Group not listed");
        STAssertTrue([self callPGM: self.pgm1 methodWithName: kMethodJoinGroupWithName withParameter: kGroupName withDelay: 0.5f] == ER_OK, @"Group not joined" );

        // PGM2's group is different, so it should only see itself
        STAssertTrue([self.pgm2 getNumberOfPeersInGroup: kGroupName] == 1, @"Wrong peer count" );
        
        [self tearDownPortTests];
    }
}

// Helper Methods
- (QStatus)callPGM: (PGMPeerGroupManager *) pgm methodWithName: (NSString *)method withParameter: (NSString *)parameter
{
    return [self callPGM:pgm methodWithName:method withParameter:parameter withDelay: 0.0f];
}

- (QStatus)callPGM: (PGMPeerGroupManager *) pgm methodWithName: (NSString *)method withParameter: (NSString *)parameter withDelay: (NSTimeInterval)seconds
{
    QStatus status = ER_FAIL;
    SEL selector = NSSelectorFromString(method);
    
    if ([pgm respondsToSelector: selector])
    {
        status = (QStatus)[pgm performSelector: selector withObject: parameter];
    }
    
    if (seconds > 0.0f)
    {
        [self delay: seconds];
    }
    
    return status;
}

- (BOOL)checkIfString: (NSString *)string isInArray: (NSArray *) array
{
    for (NSString *element in array)
    {
        if ([string isEqual: element])
        {
            return YES;
        }
    }
    
    return NO;
}

-(void)delay: (NSTimeInterval)timeoutSeconds
{
    NSDate *timeoutDate = [NSDate dateWithTimeIntervalSinceNow: timeoutSeconds];
    BOOL timedOut = NO;
    
    while (!timedOut)
    {
        [[NSRunLoop currentRunLoop] runMode: NSDefaultRunLoopMode beforeDate: timeoutDate];
        timedOut = ([timeoutDate timeIntervalSinceNow] < 0.0) ? YES : NO;
    }
}

@end
