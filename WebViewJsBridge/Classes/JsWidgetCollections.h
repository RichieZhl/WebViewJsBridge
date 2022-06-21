//
//  ErenLib
//
//  Created by lylaut on 2021/8/31.
//

#import <UIkit/UIkit.h>
#import <objc/runtime.h>

typedef void (^WVJBResponseCallback)(id responseData);
typedef void (^WVJBHandler)(id data, WVJBResponseCallback responseCallback);

@interface JsBaseWidgetProtocol : NSObject

- (void)performWithController:(UIViewController *)controller data:(NSDictionary<NSString *, id> *)data callback:(WVJBResponseCallback)callback;

- (void)success:(WVJBResponseCallback)callback data:(id)data;
- (void)successNotRemoveResponseId:(WVJBResponseCallback)callback data:(id)data;
- (void)fail:(WVJBResponseCallback)callback errorCode:(int)errorCode message:(NSString *)message;
- (void)failNotRemoveResponseId:(WVJBResponseCallback)callback errorCode:(int)errorCode message:(NSString *)message;

@end

@interface JsWidgetCollections : NSObject


/// 注册实现
/// @param widgetName widget名
/// @param cls 实现BaseWidgetProtocol协议的类Class
+ (void)registerWidget:(NSString *)widgetName impClass:(Class)cls;

@end
