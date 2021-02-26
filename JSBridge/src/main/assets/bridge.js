(function() {
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
		window.Android.postMessage(JSON.stringify(message));
	}
	
	function _handleMessageFromNative(messageJSON) {
		var message = JSON.parse(messageJSON);
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
})()