import 'package:flutter/services.dart';

import 'lock_screen_notification_platform_interface.dart';

class LockScreenNotification {
  Future<int?> cancelNotification() {
    return LockScreenNotificationPlatform.instance.cancelNotification();
  }

  Future<int?> createNotification(String message) {
    return LockScreenNotificationPlatform.instance.createNotification(message);
  }

  Future<void> takePicture() {
    return LockScreenNotificationPlatform.instance.takePicture();
  }

  Future<void> launchApp() {
    return LockScreenNotificationPlatform.instance.launchApp();
  }

  Future<void> initialize(void Function(MethodCall) callback) async {
    LockScreenNotificationPlatform.instance.initialize(callback);
  }
}
