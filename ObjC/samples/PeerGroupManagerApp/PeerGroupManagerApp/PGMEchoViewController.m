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

#import "PGMEchoViewController.h"

@interface PGMEchoViewController ()

@end

@implementation PGMEchoViewController

@synthesize peerGroupManager = _peerGroupManager;
@synthesize echoString = _echoString;
@synthesize echoButton = _echoButton;
@synthesize echoReply = _echoReply;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	
    self.echoString.delegate = self;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)echoButtonPressed:(id)sender
{
    NSString *remotePeer;
    NSString *groupName;
    
    NSMutableArray *hostedAndJoinedGroups = [[self.peerGroupManager listHostedGroupNames] mutableCopy];
    [hostedAndJoinedGroups addObjectsFromArray:[self.peerGroupManager listJoinedGroupNames]];
    
    BOOL complete = NO;
    for(NSString *group in hostedAndJoinedGroups)
    {
        if([self.peerGroupManager getNumberOfPeersInGroup:group] > 1)
        {
            NSArray *peers = [self.peerGroupManager getIdsOfPeersInGroup:group];
            for(NSString *peer in peers)
            {
                if(![peer isEqualToString:[self.peerGroupManager getMyPeerId]])
                {
                    remotePeer = [peer copy];
                    groupName = [group copy];
                    complete = YES;
                    break;
                }
            }
        }
        if(complete)
        {
            break;
        }
    }
    
    TestObjectProxy *proxy = (TestObjectProxy *) [self.peerGroupManager getRemoteObjectWithClassName:@"TestObjectProxy" forPeer:remotePeer inGroup:groupName onPath:@"/org/alljoyn/TestObject"];
    
    NSLog(@"Getting Proxy for remote peer %@ in group %@", remotePeer, groupName);
    
    NSString *replyString = [proxy echoString:self.echoString.text];
    self.echoReply.text = [NSString stringWithFormat:@"Echo from %@ - %@", remotePeer, replyString];
}

- (BOOL) textFieldShouldReturn:(UITextField *)textField
{
    [self.echoString resignFirstResponder];
    return YES;
}

@end
