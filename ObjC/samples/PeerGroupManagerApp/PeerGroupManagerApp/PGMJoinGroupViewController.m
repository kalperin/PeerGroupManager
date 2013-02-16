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

#import "PGMJoinGroupViewController.h"

@interface PGMJoinGroupViewController ()

- (void)alertScreen:(NSMutableString*)string;

@end

@implementation PGMJoinGroupViewController

@synthesize tableView = _tableView;
@synthesize peerGroupManager = _peerGroupManager;

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
    NSInteger numRows = [self.peerGroupManager listFoundGroupNames].count;
    if(numRows > 0)
    {
        return numRows;
    }
    
    return 1;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *CellIdentifier = @"JoinGroupCell";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier forIndexPath:indexPath];
    NSArray *foundGroups = [self.peerGroupManager listFoundGroupNames];
    
    // Configure the cell...
    if (foundGroups.count == 0 && indexPath.row == 0)
    {
        cell.textLabel.text = @"No found groups";
    }
    else
    {
        cell.textLabel.text = [foundGroups objectAtIndex:indexPath.row];
        //cell.detailTextLabel.text = [self.peerGroupManager getHostPeerIdOfGroup:cell.textLabel.text];
    }
    return cell;
}


#pragma mark - Button Actions

- (IBAction)joinSelectedGroupsButtonPressed:(id)sender
{
    NSArray *selectedRows = self.tableView.indexPathsForSelectedRows;
    NSMutableString *joinedGroups = [[NSMutableString alloc] init];
    for (NSIndexPath *indexPath in selectedRows)
    {
        // Join group for each selected row
        UITableViewCell *cell = [self.tableView cellForRowAtIndexPath:indexPath];
        QStatus status = [self.peerGroupManager joinGroupWithName:cell.textLabel.text];
        [joinedGroups appendString:[NSString stringWithFormat:@"%@ - %s\n",cell.textLabel.text,QCC_StatusText(status)]];
    }
    [self.tableView reloadData];
    
    [self alertScreen:joinedGroups];
}

- (void)alertScreen:(NSMutableString*)string
{
    if(string.length > 0)
    {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Joined Groups" message:string delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
        [alert show];
    }
}

#pragma mark - PeerGroupDelegate methods

- (void)didFindAdvertisedName:(NSString *)groupName withTransport:(AJNTransportMask)transport
{
    // Refresh the table when a new advertised name is found
    [self.tableView reloadData];
}

- (void)didLoseAdvertisedName:(NSString *)groupName withTransport:(AJNTransportMask)transport
{
    // Refresh the table when an advertised name is lost
    [self.tableView reloadData];
}


@end
