//
//  JsBridgeWebView.h
//
//  Created by lylaut on 2021/2/25.
//

#import <WebKit/WebKit.h>

NS_ASSUME_NONNULL_BEGIN

typedef void (^WVJBResponseCallback)(id responseData);
typedef void (^WVJBHandler)(id data, WVJBResponseCallback responseCallback);

@interface JsBridgeWebView : WKWebView

/// 注册方法供JS调用
/// @param handlerName 方法名
/// @param handler 回调JS监听方法
- (void)registerHandler:(NSString *)handlerName handler:(WVJBHandler)handler;

/// 调用JS已注册的方法
/// @param handlerName construction
/// @param data 传给JS的数据
/// @param responseCallback JS回调回来的数据
- (void)callHandler:(NSString *)handlerName data:(id)data responseCallback:(WVJBResponseCallback)responseCallback;

/// 销毁资源，否则会导致内存泄漏
- (void)destory;

@end

NS_ASSUME_NONNULL_END
