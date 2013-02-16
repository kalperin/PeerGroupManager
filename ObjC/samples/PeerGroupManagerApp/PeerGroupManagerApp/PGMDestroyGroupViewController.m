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

#import "PGMDestroyGroupViewController.h"

@interface PGMDestroyGroupViewController ()

- (void)alertScreen:(NSMutableString*)string;

@end


@implementation PGMDestroyGroupViewController

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
    
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
 
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
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
    NSInteger numRows = [self.peerGroupManager listHostedGroupNames].count;
    if(numRows > 0)
    {
        return numRows;
    }
    
    return 1;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *CellIdentifier = @"DestroyGroupCell";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier forIndexPath:indexPath];
    NSArray *hostedGroups = [self.peerGroupManager listHostedGroupNames];
    
    // Configure the cell...
    if (hostedGroups.count == 0 && indexPath.row == 0)
    {
        cell.textLabel.text = @"No hosted groups";
    }
    else
    {
        cell.textLabel.text = [hostedGroups objectAtIndex:indexPath.row];
    }
    return cell;
}



#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView didDeselectRowAtIndexPath:(NSIndexPath *)indexPath
{

}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{

}

- (void)tableView:(UITableView *)tableView didHighlightRowAtIndexPath:(NSIndexPath *)indexPath
{
}

- (void)tableView:(UITableView *)tableView didUnhighlightRowAtIndexPath:(NSIndexPath *)indexPath
{
}

#pragma mark - Button Actions

- (IBAction)destroySelectedGroupsButtonPressed:(id)sender
{
    NSArray *selectedRows = self.tableView.indexPathsForSelectedRows;
    NSMutableString *destroyedGroups = [[NSMutableString alloc] init];
    for (NSIndexPath *indexPath in selectedRows)
    {
        UITableViewCell *cell = [self.tableView cellForRowAtIndexPath:indexPath];
        QStatus status = [self.peerGroupManager destroyGroupWithName:cell.textLabel.text];
        [destroyedGroups appendString:[NSString stringWithFormat:@"%@ - %s\n",cell.textLabel.text,QCC_StatusText(status)]];
    }
    [self.tableView reloadData];
    
    [self alertScreen:destroyedGroups];
}

- (IBAction)destroyAllGroupsButtonPressed:(id)sender
{
    NSArray *hostedGroups = [self.peerGroupManager listHostedGroupNames];
    NSMutableString *destroyedGroups = [[NSMutableString alloc] init];
    for (NSString *groupName in hostedGroups)
    {
        QStatus status = [self.peerGroupManager destroyGroupWithName:groupName];
        [destroyedGroups appendString:[NSString stringWithFormat:@"%@ - %s\n",groupName,QCC_StatusText(status)]];
        
    }
    [self.tableView reloadData];
    
    [self alertScreen:destroyedGroups];
}

- (void)alertScreen:(NSMutableString*)string
{
    if(string.length > 0)
    {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Destroyed Groups" message:string delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
        [alert show];
    }
}

@end
