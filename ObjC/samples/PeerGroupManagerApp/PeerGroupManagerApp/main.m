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

#import <UIKit/UIKit.h>

#import "AllJoynFramework/AJNVersion.h"

#import "PGMAppDelegate.h"

int main(int argc, char *argv[])
{
    NSLog(@"PeerGroupManagerApp v2.0.0, built on AllJoyn %@", [AJNVersion versionInformation]);

    @autoreleasepool {
        return UIApplicationMain(argc, argv, nil, NSStringFromClass([PGMAppDelegate class]));
    }
}
