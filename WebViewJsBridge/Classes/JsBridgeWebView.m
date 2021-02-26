//
//  JsBridgeWebView.m
//
//  Created by lylaut on 2021/2/25.
//

#import "JsBridgeWebView.h"

NSString *ZLJavascriptBridge_js(void);

@interface ZLJsScriptHandler : NSObject <WKScriptMessageHandler> {
    long _uniqueId;
    __weak WKWebView *_webView;
}

@property (strong, nonatomic) NSMutableDictionary* responseCallbacks;
@property (strong, nonatomic) NSMutableDictionary* messageHandlers;

@end

@implementation ZLJsScriptHandler

- (instancetype)initWithWebView:(WKWebView *)webView {
    if (self = [super init]) {
        _webView = webView;
        _uniqueId = 1;
        self.responseCallbacks = [NSMutableDictionary dictionary];
        self.messageHandlers = [NSMutableDictionary dictionary];
    }
    return self;
}

- (void)registerHandler:(NSString *)handlerName handler:(WVJBHandler)handler {
    self.messageHandlers[handlerName] = [handler copy];
}

- (void)callHandler:(NSString *)handlerName data:(id)data responseCallback:(WVJBResponseCallback)responseCallback {
    NSMutableDictionary* message = [NSMutableDictionary dictionary];
    
    if (data) {
        message[@"data"] = data;
    }
    
    if (responseCallback) {
        NSString* callbackId = [NSString stringWithFormat:@"objc_cb_%ld", ++_uniqueId];
        self.responseCallbacks[callbackId] = [responseCallback copy];
        message[@"callbackId"] = callbackId;
    }
    
    if (handlerName) {
        message[@"handlerName"] = handlerName;
    }
    [self _dispatchMessage:message];
}

- (void)_dispatchMessage:(NSDictionary *)message {
    NSString *messageJSON = [[NSString alloc] initWithData:[NSJSONSerialization dataWithJSONObject:message options:0 error:nil] encoding:NSUTF8StringEncoding];
    
    NSString *javascriptCommand = [NSString stringWithFormat:@"window.JsBridge._handleMessageFromNative(%@);", messageJSON];
    if ([[NSThread currentThread] isMainThread]) {
        [_webView evaluateJavaScript:javascriptCommand completionHandler:nil];
    } else {
        dispatch_sync(dispatch_get_main_queue(), ^{
            [self->_webView evaluateJavaScript:javascriptCommand completionHandler:nil];
        });
    }
}

- (void)userContentController:(nonnull WKUserContentController *)userContentController didReceiveScriptMessage:(nonnull WKScriptMessage *)smessage {
    NSDictionary *message = smessage.body;
    NSString* responseId = message[@"responseId"];
    if (responseId) {
        WVJBResponseCallback responseCallback = _responseCallbacks[responseId];
        responseCallback(message[@"responseData"]);
        [self.responseCallbacks removeObjectForKey:responseId];
    } else {
        WVJBResponseCallback responseCallback = NULL;
        NSString* callbackId = message[@"callbackId"];
        if (callbackId) {
            responseCallback = ^(id responseData) {
                if (responseData == nil) {
                    responseData = [NSNull null];
                }
                
                NSDictionary *msg = @{ @"responseId": callbackId, @"responseData": responseData };
                [self _dispatchMessage:msg];
            };
        } else {
            responseCallback = ^(id ignoreResponseData) {
                // Do nothing
            };
        }
        
        WVJBHandler handler = self.messageHandlers[message[@"handlerName"]];
        
        if (!handler) {
            NSLog(@"WVJBNoHandlerException, No handler for message from JS: %@", message);
            return;
        }
        
        handler(message[@"data"], responseCallback);
    }
}

@end

@interface JsBridgeWebView ()

@property (nonatomic, readwrite, weak) ZLJsScriptHandler *nativeJsHandler;

- (void)setJsHandler:(ZLJsScriptHandler *)jsHandler;

@end

@implementation JsBridgeWebView

- (instancetype)initWithFrame:(CGRect)frame configuration:(WKWebViewConfiguration *)configuration {
    if (self = [super initWithFrame:frame configuration:configuration]) {
        [self setUp:self.configuration];
    }
    return self;
}

- (instancetype)initWithCoder:(NSCoder *)coder {
    if (self = [super initWithCoder:coder]) {
        [self setUp:self.configuration];
    }
    return self;
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self setUp:self.configuration];
    }
    return self;
}

- (void)setUp:(WKWebViewConfiguration *)configuration {
    [self.configuration.userContentController addUserScript:[[WKUserScript alloc] initWithSource:ZLJavascriptBridge_js() injectionTime:WKUserScriptInjectionTimeAtDocumentStart forMainFrameOnly:YES]];
    
    ZLJsScriptHandler *jsScriptHandler = [[ZLJsScriptHandler alloc] initWithWebView:self];
#pragma clang diagnostic push

#pragma clang diagnostic ignored "-Wundeclared-selector"
    [self performSelector:@selector(setJsHandler:) withObject:jsScriptHandler];
#pragma clang diagnostic pop
}

- (void)setJsHandler:(ZLJsScriptHandler *)jsHandler {
    self.nativeJsHandler = jsHandler;
    [self.configuration.userContentController addScriptMessageHandler:jsHandler name:@"JsBridge"];
}

- (void)registerHandler:(NSString *)handlerName handler:(WVJBHandler)handler {
    [self.nativeJsHandler registerHandler:handlerName handler:handler];
}

- (void)callHandler:(NSString *)handlerName data:(id)data responseCallback:(WVJBResponseCallback)responseCallback {
    [self.nativeJsHandler callHandler:handlerName data:data responseCallback:responseCallback];
}

- (void)destory {
    [self.configuration.userContentController removeAllUserScripts];
    [self.configuration.userContentController removeScriptMessageHandlerForName:@"JsBridge"];
}

@end


NSString *ZLJavascriptBridge_js(void) {
#define __wvjb_js_func__(x) #x

static NSString *preprocessorJSCode = @__wvjb_js_func__(
;(function() {
 if (window.JsBridge) {
     return;
 }

 window.JsBridge = {
     registerHandler: registerHandler,
     callHandler: callHandler,
     _handleMessageFromNative: _handleMessageFromNative
 };

 var messageHandlers = {};
 
 var responseCallbacks = {};
 var uniqueId = 1;

 function registerHandler(handlerName, handler) {
     messageHandlers[handlerName] = handler;
 }
 
 function callHandler(handlerName, data, responseCallback) {
     if (arguments.length === 2 && typeof data == 'function') {
         responseCallback = data;
         data = null;
     }
     _doSend({ handlerName:handlerName, data:data }, responseCallback);
 }
 
 function _doSend(message, responseCallback) {
     if (responseCallback) {
         var callbackId = 'cb_'+(uniqueId++)+'_'+new Date().getTime();
         responseCallbacks[callbackId] = responseCallback;
         message['callbackId'] = callbackId;
     }
     window.webkit.messageHandlers.JsBridge.postMessage(message)
 }
 
 function _handleMessageFromNative(message) {
     var responseCallback;
     
     if (message.responseId) {
         responseCallback = responseCallbacks[message.responseId];
         if (!responseCallback) {
             return;
         }
         const isDelete = message.responseData == null || (message.responseData.JsBridgeIsDelete == null || message.responseData.JsBridgeIsDelete);
         if (message.responseData != null) {
             delete message.responseData.JsBridgeIsDelete;
         }
         
         responseCallback(message.responseData);
         if (isDelete) {
             delete responseCallbacks[message.responseId];
         }
     } else {
         if (message.callbackId) {
             var callbackResponseId = message.callbackId;
             responseCallback = function(responseData) {
                 _doSend({ handlerName:message.handlerName, responseId:callbackResponseId, responseData:responseData });
             };
         }
         
         var handler = messageHandlers[message.handlerName];
         if (!handler) {
             console.log("JsBridge: WARNING: no handler for message from ObjC:", message);
         } else {
             handler(message.data, responseCallback);
         }
     }
 }
})();
);

#undef __wvjb_js_func__
return preprocessorJSCode;
}
