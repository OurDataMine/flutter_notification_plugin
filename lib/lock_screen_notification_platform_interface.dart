import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'lock_screen_notification_method_channel.dart';

abstract class LockScreenNotificationPlatform extends PlatformInterface {
  /// Constructs a LockScreenNotificationPlatform.
  LockScreenNotificationPlatform() : super(token: _token);

  static final Object _token = Object();

  static LockScreenNotificationPlatform _instance = MethodChannelLockScreenNotification();

  /// The default instance of [LockScreenNotificationPlatform] to use.
  ///
  /// Defaults to [MethodChannelLockScreenNotification].
  static LockScreenNotificationPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [LockScreenNotificationPlatform] when
  /// they register themselves.
  static set instance(LockScreenNotificationPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> initialize(void Function(MethodCall) callback) async {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<int?> cancelNotification() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<int?> createNotification(String message) async {
    throw UnimplementedError('createNotification() has not been implemented.');
  }

}
