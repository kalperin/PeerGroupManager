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

#import "PGMCreateGroupViewController.h"

@interface PGMCreateGroupViewController ()

@property (nonatomic, strong) NSMutableArray *randomNames;

@end

@implementation PGMCreateGroupViewController

@synthesize groupName = _groupName;
@synthesize lockStatus = _lockStatus;
@synthesize peerGroupManager = _peerGroupManager;
@synthesize randomNames = _randomNames;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (NSMutableArray *)randomNames
{
    if(!_randomNames)
    {
        _randomNames = [[NSMutableArray alloc] initWithObjects:@"Test",@"Testing",@"Sample",@"Example",@"Trial",@"Hello",@"AllJoyn",@"PGM",@"Peer",nil];
    }
    return _randomNames;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self.groupName setDelegate:self];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)createGroupButtonPressed:(UIButton *)sender
{
    BOOL lock = [self.lockStatus selectedSegmentIndex];
    NSLog(@"pressed create button");
    NSString *groupName = self.groupName.text;
    QStatus status = [self.peerGroupManager createGroupWithName:groupName andLock:lock];
    self.groupName.text = @"";
    [self.groupName resignFirstResponder];
    [self createdGroup:groupName WithStatus:status];
}
- (IBAction)createGroupWithRandomNameButtonPressed:(UIButton *)sender
{
    NSLog(@"Pressed Create Group With Random Name");
    NSInteger prefixIndex = arc4random()%self.randomNames.count;
    NSInteger suffix = 0;
    NSString *groupName = [NSString stringWithFormat:@"%@Group%u",[self.randomNames objectAtIndex:prefixIndex],suffix];
    BOOL lock = [self.lockStatus selectedSegmentIndex];
    QStatus status;
    while((status = [self.peerGroupManager createGroupWithName:groupName andLock:lock]) != ER_OK)
    {
        suffix++;
        groupName = [NSString stringWithFormat:@"%@Group%u",[self.randomNames objectAtIndex:prefixIndex],suffix];
        
        if(suffix == NSIntegerMax)
        {
            // No Group Could be Made, abort
            break;
        }
    }
    [self createdGroup:groupName WithStatus:status];
    
}

- (void)createdGroup:(NSString *)groupName WithStatus:(QStatus)status
{
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Created Group" message:[NSString stringWithFormat:@"%@ - %s", groupName, QCC_StatusText(status)] delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
    [alert show];
}

- (IBAction)hideKeyboard:(id)sender
{
    [self.groupName resignFirstResponder];
}

- (BOOL) textFieldShouldReturn:(UITextField *)textField
{
    [self.groupName resignFirstResponder];
    return YES;
}


@end
