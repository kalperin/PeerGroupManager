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

#import "PGMJoinOrCreateViewController.h"

@interface PGMJoinOrCreateViewController ()

@end

@implementation PGMJoinOrCreateViewController

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
	   
    [self.groupName setDelegate:self];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)joinOrCreateButtonPressed:(UIButton *)sender
{
    NSString * groupName = self.groupName.text;
    PGMJoinOrCreateReturn *jocReturn = [self.peerGroupManager joinOrCreateGroupWithName:groupName];
    
    NSString *alertTitle = jocReturn.isJoiner ? @"Joined Group" : @"Created Group";
    
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:alertTitle message:[NSString stringWithFormat:@"%@ - %s", groupName, QCC_StatusText(jocReturn.status)] delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
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
