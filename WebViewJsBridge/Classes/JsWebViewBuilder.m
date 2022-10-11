//
//  JsWebViewBuilder.m
//  WebViewJsBridge
//
//  Created by lylaut on 2022/6/21.
//

#import "JsWebViewBuilder.h"
#import "JsBridgeWebView.h"
#import "JsWidgetCollections.h"
#import "JsProBridge.h"

@interface JsWebViewBuilder () <WKUIDelegate, WKNavigationDelegate>

@property (nonatomic, assign) CGRect frame;
@property (nonatomic, weak) UIViewController *superController;
@property (nonatomic, weak) JsBridgeWebView *webView;
@property (nonatomic, weak) id wkDelegate;
@property (nonatomic, copy) NSString *namespaceStr;
@property (nonatomic, copy) NSString *customJs;

@end

@implementation JsWebViewBuilder

- (WKWebView *)build {
    JsBridgeWebView *webView = [[JsBridgeWebView alloc] initWithFrame:self.frame configuration:[WKWebViewConfiguration new]];
    webView.configuration.allowsInlineMediaPlayback = YES; //是否允许内联(YES)或使用本机全屏控制器(NO)。默认值是否定的。
    webView.configuration.allowsAirPlayForMediaPlayback = YES;
    webView.configuration.mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeAll;
    
    webView.UIDelegate = self;
    webView.navigationDelegate = self;
    webView.allowsBackForwardNavigationGestures = YES;
    
    [webView.configuration.userContentController addUserScript:[[WKUserScript alloc] initWithSource:ProBridge_js(self.namespaceStr) injectionTime:WKUserScriptInjectionTimeAtDocumentStart forMainFrameOnly:YES]];
    
    if (self.customJs != nil && self.customJs.length > 0) {
        [webView.configuration.userContentController addUserScript:[[WKUserScript alloc] initWithSource:self.customJs injectionTime:WKUserScriptInjectionTimeAtDocumentStart forMainFrameOnly:YES]];
    }
    
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wundeclared-selector"
    [[JsWidgetCollections performSelector:@selector(widgets)] enumerateKeysAndObjectsUsingBlock:^(NSString * _Nonnull key, Class  _Nonnull obj, BOOL * _Nonnull stop) {
        [webView registerHandler:key handler:^(id data, WVJBResponseCallback responseCallback) {
            id target = [[obj alloc] init];
            if ([target isKindOfClass:[JsBaseWidgetProtocol class]]) {
                [target performWithController:self.superController data:data callback:responseCallback];
            }
        }];
    }];
#pragma clang diagnostic pop

    self.webView = webView;
    return webView;
}

- (instancetype)initWithFrame:(CGRect)frame
                           ns:(NSString *)namespaceStr
                     customJs:(NSString *)customJs {
    if (self = [super init]) {
        self.frame = frame;
        self.namespaceStr = namespaceStr;
        self.customJs = customJs;
    }
    return self;
}

- (void)setSuperController:(UIViewController *)cvt {
    _superController = cvt;
}

- (void)setWKDelegate:(id)delegate {
    _wkDelegate = delegate;
}

- (void)webView:(WKWebView *)webView didFinishNavigation:(WKNavigation *)navigation {
    if (webView.canGoBack) {
        // 禁用返回手势
        self.superController.navigationController.interactivePopGestureRecognizer.enabled = NO;
    } else {
        self.superController.navigationController.interactivePopGestureRecognizer.enabled = YES;
    }
    if (self.wkDelegate != nil && [self.wkDelegate respondsToSelector:@selector(webView:didFinishNavigation:)]) {
        [self.wkDelegate webView:webView didFinishNavigation:navigation];
    }
}

- (WKWebView *)webView:(WKWebView *)webView createWebViewWithConfiguration:(WKWebViewConfiguration *)configuration forNavigationAction:(WKNavigationAction *)navigationAction windowFeatures:(WKWindowFeatures *)windowFeatures {
    //如果目标主视图不为空,则允许导航
    if (!navigationAction.targetFrame.isMainFrame) {
        [webView loadRequest:navigationAction.request];
    }
    if (self.wkDelegate != nil && [self.wkDelegate respondsToSelector:@selector(webView:createWebViewWithConfiguration:forNavigationAction:windowFeatures:)]) {
        return [self.wkDelegate webView:webView
  createWebViewWithConfiguration:configuration
             forNavigationAction:navigationAction
                  windowFeatures:windowFeatures];
    }
    return nil;
}

- (void)webView:(WKWebView *)webView runJavaScriptAlertPanelWithMessage:(NSString *)message initiatedByFrame:(WKFrameInfo *)frame completionHandler:(void (^)(void))completionHandler {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:message message:@"" preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        completionHandler();
    }]];
    [self.superController presentViewController:alert animated:YES completion:nil];
}

- (void)webView:(WKWebView *)webView runJavaScriptConfirmPanelWithMessage:(NSString *)message initiatedByFrame:(WKFrameInfo *)frame completionHandler:(void (^)(BOOL))completionHandler {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:message message:@"" preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        completionHandler(YES);
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        completionHandler(NO);
    }]];
    [self.superController presentViewController:alert animated:YES completion:nil];
}

- (void)webView:(WKWebView *)webView decidePolicyForNavigationAction:(WKNavigationAction *)navigationAction decisionHandler:(void (^)(WKNavigationActionPolicy))decisionHandler {
    if (navigationAction.request.URL) {
        NSString *scheme = navigationAction.request.URL.scheme;
        if ([scheme isEqualToString:@"tel"]) {
            [[UIApplication sharedApplication] openURL:navigationAction.request.URL options:@{} completionHandler:nil];
        }
    }
    
    NSString *url = [navigationAction.request.URL.absoluteString stringByRemovingPercentEncoding];
    if ([url hasPrefix:@"weixin://"] || [url containsString:@"itunes.apple.com"]) {
        decisionHandler(WKNavigationActionPolicyCancel);
        if ([[UIApplication sharedApplication] canOpenURL:navigationAction.request.URL]) {
            [[UIApplication sharedApplication] openURL:navigationAction.request.URL options:@{} completionHandler:nil];
        }
    } else {
        decisionHandler(WKNavigationActionPolicyAllow);
    }
}

- (void)webView:(WKWebView *)webView decidePolicyForNavigationResponse:(WKNavigationResponse *)navigationResponse decisionHandler:(void (^)(WKNavigationResponsePolicy))decisionHandler {
    if (self.wkDelegate != nil && [self.wkDelegate respondsToSelector:@selector(webView:decidePolicyForNavigationResponse:decisionHandler:)]) {
        return [self.wkDelegate webView:webView
      decidePolicyForNavigationResponse:navigationResponse
                        decisionHandler:decisionHandler];
    } else {
        decisionHandler(WKNavigationResponsePolicyAllow);
    }
}

- (void)webView:(WKWebView *)webView didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition, NSURLCredential * _Nullable))completionHandler {
    if ([challenge.protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodServerTrust]) {
        if (challenge.previousFailureCount == 0) {
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), ^{
                NSURLCredential *credential = [NSURLCredential credentialForTrust:challenge.protectionSpace.serverTrust];
                completionHandler(NSURLSessionAuthChallengeUseCredential, credential);
            });
            return;
        }
    }
    completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, nil);
}

- (void)webViewWebContentProcessDidTerminate:(WKWebView *)webView {
    if (webView.URL) {
        [webView reload];
    }
    if (self.wkDelegate != nil && [self.wkDelegate respondsToSelector:@selector(webViewWebContentProcessDidTerminate:)]) {
        [self.wkDelegate webViewWebContentProcessDidTerminate:webView];
    }
}

@end
