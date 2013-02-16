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

#import "PGMPingGroupViewController.h"
#import <QuartzCore/QuartzCore.h>

@interface PGMPingGroupViewController ()

@property (nonatomic, strong) NSString *selectedGroup;
@property (nonatomic, strong) NSMutableDictionary *chatLogs;

@end

@implementation PGMPingGroupViewController

@synthesize peerGroupManager = _peerGroupManager;
@synthesize tableView = _tableView;
@synthesize pingTextView = _pingTextView;
@synthesize logTextView = _logTextView;
@synthesize selectedGroup = _selectedGroup;
@synthesize handle = _handle;
@synthesize simpleObject = _simpleObject;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
	self.tableView.delegate = self;
    self.tableView.dataSource = self;
    self.pingTextView.layer.cornerRadius = 8;
    
    if(!self.chatLogs)
    {
        self.chatLogs = [[NSMutableDictionary alloc] init];
    }
    
    [self.peerGroupManager addPeerGroupDelegate:self];
    [self.simpleObject registerSimpleDelegateSignalHandler:self];
    self.logTextView.text = @"No Group Selected";
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table View data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    // Return the number of sections.
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    // Return the number of rows in the section.
    NSInteger numRows = [self.peerGroupManager listJoinedGroupNames].count + [self.peerGroupManager listHostedGroupNames].count;
    if(numRows > 0)
    {
        return numRows;
    }
    return 1;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *CellIdentifier = @"PingGroupCell";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier forIndexPath:indexPath];
    
    NSMutableArray *hostedAndJoinedGroups = [[self.peerGroupManager listHostedGroupNames] mutableCopy];
    [hostedAndJoinedGroups addObjectsFromArray:[self.peerGroupManager listJoinedGroupNames]];
    [hostedAndJoinedGroups sortUsingSelector:@selector(caseInsensitiveCompare:)];
    
    // Configure the cell...
    if (hostedAndJoinedGroups.count == 0 && indexPath.row == 0)
    {
        cell.textLabel.text = @"No hosted or joined groups";
    }
    else
    {
        cell.textLabel.text = [hostedAndJoinedGroups objectAtIndex:indexPath.row];
    }
    return cell;
}

#pragma mark - Table View delegate

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    self.selectedGroup = [self.tableView cellForRowAtIndexPath:indexPath].textLabel.text;
    self.logTextView.text = [self.chatLogs objectForKey:self.selectedGroup];
    
    // Go to the bottom of the chat log if the chat is larger than the textview size
    CGPoint bottomOffset = CGPointMake(0, self.logTextView.contentSize.height - self.logTextView.bounds.size.height);
    if(bottomOffset.y > 0)
    {
        [self.logTextView setContentOffset:bottomOffset animated:YES];
    }
}


#pragma mark - Button Actions
- (IBAction)sendButtonPressed:(id)sender
{
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setTimeStyle:NSDateFormatterMediumStyle];
    [dateFormatter setDateStyle:NSDateFormatterNoStyle];
    
    if(self.selectedGroup != nil) {
        [self.simpleObject sendPingString:self.pingTextView.text group:self.selectedGroup toDestination:nil];
        
        if([self.chatLogs objectForKey:self.selectedGroup])
        {
            [[self.chatLogs objectForKey:self.selectedGroup] appendString:[NSString stringWithFormat:@"[%@] %@: %@\n", [dateFormatter stringFromDate:[NSDate date]], self.peerGroupManager.getMyPeerId, self.pingTextView.text]];
        }
        else
        {
            [self.chatLogs setObject:[NSMutableString stringWithFormat:@"[%@] %@: %@\n", [dateFormatter stringFromDate:[NSDate date]], self.peerGroupManager.getMyPeerId, self.pingTextView.text] forKey:self.selectedGroup];
        }
        
        self.logTextView.text = [self.chatLogs objectForKey:self.selectedGroup];
        
        // Go to the bottom of the chat log if the chat is larger than the textview size
        CGPoint bottomOffset = CGPointMake(0, self.logTextView.contentSize.height - self.logTextView.bounds.size.height);
        if(bottomOffset.y > 0)
        {
            [self.logTextView setContentOffset:bottomOffset animated:YES];
        }
    }
    
    self.pingTextView.text = @"";
    [self.pingTextView resignFirstResponder];
}

#pragma mark - Simple Object Signal Handler methods
-(void)didReceivePingString:(NSString *)Str group:(NSString *)groupName fromSender:(NSString *)sender
{
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setTimeStyle:NSDateFormatterMediumStyle];
    [dateFormatter setDateStyle:NSDateFormatterNoStyle];
    
    if([self.chatLogs objectForKey:groupName])
    {
        [[self.chatLogs objectForKey:groupName] appendString:[NSString stringWithFormat:@"[%@] %@: %@\n", [dateFormatter stringFromDate:[NSDate date]], sender, Str]];
    }
    else
    {
        [self.chatLogs setObject:[NSMutableString stringWithFormat:@"[%@] %@: %@\n", [dateFormatter stringFromDate:[NSDate date]], sender, Str] forKey:groupName];
    }
    
    if([self.selectedGroup isEqualToString:groupName])
    {
        // Go to the bottom of the chat log if the chat is larger than the textview size
        self.logTextView.text = [self.chatLogs objectForKey:self.selectedGroup];
        CGPoint bottomOffset = CGPointMake(0, self.logTextView.contentSize.height - self.logTextView.bounds.size.height);
        if(bottomOffset.y > 0)
        {
            [self.logTextView setContentOffset:bottomOffset animated:YES];
        }
    }
}

@end
