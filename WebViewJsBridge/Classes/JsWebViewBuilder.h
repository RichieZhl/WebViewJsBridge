//
//  JsWebViewBuilder.h
//  WebViewJsBridge
//
//  Created by lylaut on 2022/6/21.
//

#import <Foundation/Foundation.h>
#import <WebKit/WebKit.h>

@interface JsWebViewBuilder : NSObject

/// 初始化
/// @param frame frame
/// @param namespaceStr 命名空间
- (instancetype)initWithFrame:(CGRect)frame
                           ns:(NSString *)namespaceStr
                     customJs:(NSString *)customJs;

/// 设置所在控制器
/// @param cvt 控制器
- (void)setSuperController:(UIViewController *)cvt;

/// 自定义WebKit代理实现类
/// @param delegate 代理实现类
- (void)setWKDelegate:(id)delegate;

/// 生成View
- (WKWebView *)build;

@end
