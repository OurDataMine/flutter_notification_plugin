import 'package:flutter_test/flutter_test.dart';
import 'package:lock_screen_notification/lock_screen_notification.dart';
import 'package:lock_screen_notification/lock_screen_notification_platform_interface.dart';
import 'package:lock_screen_notification/lock_screen_notification_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockLockScreenNotificationPlatform
    with MockPlatformInterfaceMixin
    implements LockScreenNotificationPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<int?> createNotification(int arg) {
    // TODO: implement createNotification
    throw UnimplementedError();
  }
}

void main() {
  final LockScreenNotificationPlatform initialPlatform = LockScreenNotificationPlatform.instance;

  test('$MethodChannelLockScreenNotification is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelLockScreenNotification>());
  });

  test('getPlatformVersion', () async {
    LockScreenNotification lockScreenNotificationPlugin = LockScreenNotification();
    MockLockScreenNotificationPlatform fakePlatform = MockLockScreenNotificationPlatform();
    LockScreenNotificationPlatform.instance = fakePlatform;

    expect(await lockScreenNotificationPlugin.getPlatformVersion(), '42');
  });
}
