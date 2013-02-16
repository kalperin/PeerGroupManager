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

#import "PGMSetSessionPortViewController.h"

@interface PGMSetSessionPortViewController ()

@end

@implementation PGMSetSessionPortViewController

@synthesize peerGroupManager = _peerGroupManager;
@synthesize sessionPort = _sessionPort;
@synthesize setSessionPortButton = _setSessionPortButton;


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
	
    self.sessionPort.delegate = self;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)setSessionPortButtonPressed:(UIButton *)sender
{
    AJNSessionPort sessionPort = (uint16_t)[self.sessionPort.text integerValue];
    [self.peerGroupManager setSessionPort:sessionPort];
    
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Set Session Port" message:[NSString stringWithFormat:@"Setting Session Port to %@", self.sessionPort.text] delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
    [alert show];
    
    self.sessionPort.text = @"";
    [self.sessionPort resignFirstResponder];
}

- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string
{
    // Support the backspace key
    if([string isEqualToString:@""]) {
        return YES;
    }
    
    // Ensure that all the characters are numeric
    NSCharacterSet *numericCharacters = [NSCharacterSet decimalDigitCharacterSet];
    int i;
    for(i=0; i < [string length]; i++)
    {
        unichar character = [string characterAtIndex:i];
        if([numericCharacters characterIsMember:character])
        {
            return YES;
        }
    }
    return NO;
}

- (BOOL) textFieldShouldReturn:(UITextField *)textField
{
    [self.sessionPort resignFirstResponder];
    return YES;
}

- (IBAction)hideKeyboard:(id)sender
{
    [self.sessionPort resignFirstResponder];
}

@end
