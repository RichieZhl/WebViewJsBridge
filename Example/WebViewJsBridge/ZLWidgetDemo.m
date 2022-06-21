//
//  ZLWidgetDemo.m
//  WebViewJsBridge_Example
//
//  Created by lylaut on 2022/6/21.
//  Copyright © 2022 richiezhl. All rights reserved.
//

#import "ZLWidgetDemo.h"

@implementation ZLWidgetDemo

- (void)performWithController:(UIViewController *)controller data:(NSDictionary<NSString *,id> *)data callback:(WVJBResponseCallback)callback {
    NSLog(@"%@", data);
    [self success:callback data:@"--------"];
}

@end
