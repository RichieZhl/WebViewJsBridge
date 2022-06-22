//
//  ZLViewController.m
//  WebViewJsBridge
//
//  Created by richiezhl on 02/25/2021.
//  Copyright (c) 2021 richiezhl. All rights reserved.
//

#import "ZLViewController.h"
#import <WebViewJsBridge/JsWebViewBuilder.h>
#import <WebViewJsBridge/JsWidgetCollections.h>

@interface ZLViewController () <WKNavigationDelegate>

@property (nonatomic, strong) JsWebViewBuilder *builder;

@end

@implementation ZLViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    [JsWidgetCollections registerWidget:@"navigateTo" impClass:NSClassFromString(@"ZLWidgetDemo")];
    [JsWidgetCollections registerWidget:@"a.navigateTo" impClass:NSClassFromString(@"ZLWidgetDemo")];
    
    JsWebViewBuilder *builder = [[JsWebViewBuilder alloc] initWithFrame:self.view.bounds ns:@"eren" customJs:@"window.eren.token='asdfsf'"];
    [builder setSuperController:self];
    [builder setWKDelegate:self];
    WKWebView *webView = [builder build];
    self.builder = builder;
    [self.view addSubview:webView];
    [webView loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:@"https://www.baidu.com"]]];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)webView:(WKWebView *)webView didFinishNavigation:(WKNavigation *)navigation {
    NSLog(@"finished");
}

@end
