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

#import "PGMMainMenuViewController.h"
#import "PGMTestObject.h"

@interface PGMMainMenuViewController ()

@property PGMTestObjectExtension *testObject;

@end

@implementation PGMMainMenuViewController

@synthesize peerGroupManager = _peerGroupManager;
@synthesize simpleObject = _simpleObject;
@synthesize testObject = _testObject;

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        
    }
    
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    PGMAppDelegate *appDelegate = (PGMAppDelegate *)[[UIApplication sharedApplication] delegate];
    appDelegate.mainMenu = self;
    
    
    if(!self.peerGroupManager)
    {
        // Create the PeerGroupManager
        NSLog(@"initializing peer group manager");
        self.peerGroupManager = [[PGMPeerGroupManager alloc] initWithGroupPrefix:@"org.alljoyn.PeerGroupManagerApp" withPeerGroupDelegate:self withBusObjects:nil];
        
        // Create the Bus Object
        NSLog(@"Creating bus object");
        self.simpleObject = [[PGMSimpleObjectExtension alloc] initWithPath:@"/SimpleService"];
        
        // Register the Bus Object
        [self.peerGroupManager registerBusObject:self.simpleObject];
        
        self.testObject = [[PGMTestObjectExtension alloc] initWithPath:@"/org/alljoyn/TestObject"];
        [self.peerGroupManager registerBusObject:self.testObject];
    }
    
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

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    UIViewController *controller = segue.destinationViewController;
    if([controller respondsToSelector:@selector(setPeerGroupManager:)])
    {
        ((id<PGMView>)controller).peerGroupManager = self.peerGroupManager;
    }
    if([controller respondsToSelector:@selector(setSimpleObject:)])
    {
       [controller performSelector:@selector(setSimpleObject:) withObject:self.simpleObject];
    }
}

- (void)cleanup
{
    [self.peerGroupManager cleanup];
}


#pragma mark - Table view data source

/*- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
#warning Potentially incomplete method implementation.
    // Return the number of sections.
    return 0;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
#warning Incomplete method implementation.
    // Return the number of rows in the section.
    return 0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *CellIdentifier = @"Cell";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier forIndexPath:indexPath];
    
    // Configure the cell...
    
    return cell;
}*/

/*
// Override to support conditional editing of the table view.
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Return NO if you do not want the specified item to be editable.
    return YES;
}
*/

/*
// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        // Delete the row from the data source
        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
    }   
    else if (editingStyle == UITableViewCellEditingStyleInsert) {
        // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
    }   
}
*/

/*
// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath
{
}
*/

/*
// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Return NO if you do not want the item to be re-orderable.
    return YES;
}
*/

#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
}

@end
