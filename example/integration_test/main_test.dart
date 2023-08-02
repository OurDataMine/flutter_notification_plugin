import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lock_screen_notification_example/main.dart';
import 'package:patrol/patrol.dart';

void main() {
  patrolTest(
      'Take picture from notification',
      nativeAutomation: true,
      testNotification);
}

const package="com.ourdatamine.lock_screen_notification_example";

Future<void> testNotification(PatrolTester $) async {
  await $.pumpWidgetAndSettle(
      await buildMainApp([])
  );
  final notification = Selector(resourceId: "$package:id/notifcationText");
  final postview = Selector(resourceId: "$package:id/iv_capture");
  final preview = Selector(resourceId: "$package:id/viewFinder");

  $(#Permissions).tap();

  await $.native.grantPermissionWhenInUse();
  await $.native.grantPermissionWhenInUse();
  await $.native.grantPermissionWhenInUse();

  $(#CreateNotification).tap();

  await $.native.pressHome();

  await $.native.openNotifications();
  await $.native.tapOnNotificationBySelector(notification);

  expect(postview, findsNothing);
  await $.native.tap(preview);
  expect(postview, findsOneWidget);

}
