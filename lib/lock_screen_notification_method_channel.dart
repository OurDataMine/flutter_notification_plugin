import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'lock_screen_notification_platform_interface.dart';

/// An implementation of [LockScreenNotificationPlatform] that uses method channels.
class MethodChannelLockScreenNotification extends LockScreenNotificationPlatform {

  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('lock_screen_notification');

  /// The MethodChannel that is being used by this implementation of the plugin.
  @visibleForTesting
  MethodChannel get channel => _channel;

  static const CHANNEL_ID = "com.ourdatamine.heathtracking/notification_click";
  final MethodChannel _channel = const MethodChannel(CHANNEL_ID);
  late void Function(MethodCall) callback;

  Future<void> notificationHandler(MethodCall call) async {
    callback(call);
  }

  @override
  Future<void> initialize(void Function(MethodCall) callback) async {
    print("init call Handler");
    this.callback = callback;
    channel.setMethodCallHandler(notificationHandler);
  }

  @override
  Future<int?> cancelNotification() async {
    final int? ret = await channel.invokeMethod('cancelNotification');
    return ret;
  }

  @override
  Future<int?> createNotification(String message) async {
    final int? ret = await channel.invokeMethod('createNotification', message);
    return ret;
  }

  Future<void> launchApp() {
    channel.invokeMethod('launchApp');
    return Future(() => null);
  }
}


