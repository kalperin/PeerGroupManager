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

#import "PGMLeaveGroupViewController.h"

@interface PGMLeaveGroupViewController ()

- (void)alertScreen:(NSMutableString*)string;

@end

@implementation PGMLeaveGroupViewController

@synthesize peerGroupManager = _peerGroupManager;
@synthesize tableView = _tableView;

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
    
	self.tableView.delegate = self;
    self.tableView.dataSource = self;
    
    [self.peerGroupManager addPeerGroupDelegate:self];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    // Return the number of sections.
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    // Return the number of rows in the section.
    NSInteger numRows = [self.peerGroupManager listJoinedGroupNames].count;
    if(numRows > 0)
    {
        return numRows;
    }
    
    return 1;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *CellIdentifier = @"LeaveGroupCell";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier forIndexPath:indexPath];
    NSArray *joinedGroups = [self.peerGroupManager listJoinedGroupNames];
    
    // Configure the cell...
    if (joinedGroups.count == 0 && indexPath.row == 0)
    {
        cell.textLabel.text = @"No joined groups";
    }
    else
    {
        cell.textLabel.text = [joinedGroups objectAtIndex:indexPath.row];
    }
    return cell;
}

#pragma mark - Button Actions

- (IBAction)leaveSelectedGroupsButtonPressed:(id)sender
{
    NSArray *selectedRows = self.tableView.indexPathsForSelectedRows;
    NSMutableString *groupsLeft = [[NSMutableString alloc] init];
    for (NSIndexPath *indexPath in selectedRows)
    {
        // Join group for each selected row
        UITableViewCell *cell = [self.tableView cellForRowAtIndexPath:indexPath];
        QStatus status = [self.peerGroupManager leaveGroupWithName:cell.textLabel.text];
        [groupsLeft appendString:[NSString stringWithFormat:@"%@ - %s\n",cell.textLabel.text,QCC_StatusText(status)]];
    }
    
    [self.tableView reloadData];
    
    [self alertScreen:groupsLeft];
}

- (IBAction)leaveAllGroupsButtonPressed:(id)sender {
    NSArray *joinedGroups = [self.peerGroupManager listJoinedGroupNames];
    NSMutableString *groupsLeft = [[NSMutableString alloc] init];
    for (NSString *groupName in joinedGroups)
    {
        QStatus status = [self.peerGroupManager leaveGroupWithName:groupName];
        [groupsLeft appendString:[NSString stringWithFormat:@"%@ - %s\n",groupName,QCC_StatusText(status)]];
    }
    
    [self.tableView reloadData];
    
    [self alertScreen:groupsLeft];
}

- (void)alertScreen:(NSMutableString*)string
{
    if(string.length > 0)
    {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Left Groups" message:string delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
        [alert show];
    }
}

#pragma mark - PeerGroupDelegate methods

- (void)groupWasLost:(NSString *)groupName
{
    // Refresh the table when a group is lost
    [self.tableView reloadData];
}


@end
