import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lock_screen_notification/lock_screen_notification_method_channel.dart';

void main() {
  MethodChannelLockScreenNotification platform = MethodChannelLockScreenNotification();
  const MethodChannel channel = MethodChannel('lock_screen_notification');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
