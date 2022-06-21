//
//  ErenLib
//
//  Created by lylaut on 2021/8/31.
//

#import "JsWidgetCollections.h"
#import "JsProBridge.h"

@implementation JsBaseWidgetProtocol

- (void)performWithController:(UIViewController *)controller data:(NSDictionary<NSString *, id> *)data callback:(WVJBResponseCallback)callback {
    [self fail:callback errorCode:-999 message:@"not implementation"];
}

- (NSDictionary *)createSuccessReponse:(BOOL)isDelete data:(id)data {
    NSMutableDictionary *dic = [NSMutableDictionary dictionary];
    dic[@"status"] = @1;
    dic[@"data"] = data;
    dic[@"JsBridgeIsDelete"] = @(isDelete);
    return dic;
}

- (NSDictionary *)createFailReponse:(BOOL)isDelete errorCode:(int)errorCode message:(id)message {
    NSMutableDictionary *dic = [NSMutableDictionary dictionary];
    dic[@"status"] = @-1;
    dic[@"data"] = @{@"errorCode": @(errorCode), @"message": message};
    dic[@"JsBridgeIsDelete"] = @(isDelete);
    return dic;
}

- (void)success:(WVJBResponseCallback)callback data:(id)data {
    if (callback) {
        callback([self createSuccessReponse:YES data:data]);
    }
}

- (void)successNotRemoveResponseId:(WVJBResponseCallback)callback data:(id)data {
    if (callback) {
        callback([self createSuccessReponse:NO data:data]);
    }
}

- (void)fail:(WVJBResponseCallback)callback errorCode:(int)errorCode message:(NSString *)message {
    if (callback) {
        callback([self createFailReponse:YES errorCode:errorCode message:message]);
    }
}

- (void)failNotRemoveResponseId:(WVJBResponseCallback)callback errorCode:(int)errorCode message:(NSString *)message {
    if (callback) {
        callback([self createFailReponse:NO errorCode:errorCode message:message]);
    }
}

@end

static NSMutableDictionary<NSString *, Class> *widgetCollectionMaps;

NSString *ProBridge_js(NSString *namespaceStr) {
    #define __pro_js_func__(x) #x
    
    __block NSMutableString *customScript = [NSMutableString string];
    [customScript appendFormat:@"if (window['%@'] == null) { window['%@'] = {}; }", namespaceStr, namespaceStr];
    [widgetCollectionMaps enumerateKeysAndObjectsUsingBlock:^(NSString * _Nonnull key, Class  _Nonnull obj, BOOL * _Nonnull stop) {
        NSArray<NSString *> *strings = [key componentsSeparatedByString:@"."];
        if (strings.count == 1) {
            [customScript appendFormat:@"generator.apply(window['%@'], ['%@']);", namespaceStr, key];
        } else {
            NSMutableString *cmdBuilder = [NSMutableString string];
            [cmdBuilder appendFormat:@"window['%@']", namespaceStr];
            for (int i = 0; i < strings.count - 1; ++i) {
                [cmdBuilder appendFormat:@"['%@']", strings[i]];
            }
            [customScript appendFormat:@"if (%@ == null) { %@ = {}; }", cmdBuilder, cmdBuilder];
            [customScript appendFormat:@"generator.apply(%@, ['%@']);\n", cmdBuilder, strings[strings.count - 1]];
        }
    }];
    
    
    static NSString *preprocessorJSCode = @__pro_js_func__(
(function() {
    function generator(method) {
        this[method] = function responseNoop(param) {
            var p = param || {};
            var successCallback = function (res) {
                console.log('默认成功回调', method, res);
            };
            var failCallback = function (err) {
                console.log('默认失败回调', method, err);
            };
            if (p.success) {
                successCallback = p.success;
                delete p.success;
            }
            if (p.fail) {
                failCallback = p.fail;
                delete p.fail;
            }
        
            //统一回调处理
            var callback = function (response) {
                console.log('response:' + response);
                const data = response || {};
                console.log('data:' + data);
                const status = data.status;
                console.log('status:' + status);
                const result = data.data;
                console.log('result:' + result);
                if (status >= 0) {
                    successCallback && successCallback.call(null, result);
                } else {
                    failCallback && failCallback.call(null, result);
                }
            };
        
            window.JsBridge.callHandler(method, p, callback);
        }
    }
    
    %@
})()
    );

    #undef __pro_js_func__
    return [NSString stringWithFormat:preprocessorJSCode, customScript];
};

@implementation JsWidgetCollections

+ (void)load {
    widgetCollectionMaps = [NSMutableDictionary dictionary];
}

+ (void)registerWidget:(NSString *)widgetName impClass:(Class)cls {
    widgetCollectionMaps[widgetName] = cls;
}

/// 获取所有的注册表
+ (NSDictionary<NSString *, Class> *)widgets {
    return widgetCollectionMaps.copy;
}

@end

