#import "LockScreenNotificationPlugin.h"
#if __has_include(<lock_screen_notification/lock_screen_notification-Swift.h>)
#import <lock_screen_notification/lock_screen_notification-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "lock_screen_notification-Swift.h"
#endif

@implementation LockScreenNotificationPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftLockScreenNotificationPlugin registerWithRegistrar:registrar];
}
@end
