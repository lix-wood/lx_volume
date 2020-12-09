#import "LxVolumePlugin.h"
#if __has_include(<lx_volume/lx_volume-Swift.h>)
#import <lx_volume/lx_volume-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "lx_volume-Swift.h"
#endif

@implementation LxVolumePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftLxVolumePlugin registerWithRegistrar:registrar];
}
@end
