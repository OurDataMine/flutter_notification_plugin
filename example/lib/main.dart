import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:lock_screen_notification/lock_screen_notification.dart';
import 'package:quick_actions/quick_actions.dart';
import 'package:receive_intent/receive_intent.dart';

Map<String, dynamic> intent_extras = {};

Future<void> _initReceiveIntent() async {
  // Platform messages may fail, so we use a try/catch PlatformException.
  try {
    final receivedIntent = await ReceiveIntent.getInitialIntent();
    intent_extras = receivedIntent?.extra ?? {};
    // print("Intent Extras1: $intent_extras");
    // Validate receivedIntent and warn the user, if it is not correct,
    // but keep in mind it could be `null` or "empty"(`receivedIntent.isNull`).
  } on PlatformException {
    // Handle exception
  }
}

void main(List<String> args) async {
  WidgetsFlutterBinding.ensureInitialized();

  await _initReceiveIntent();
  print("Intent Extras2: $intent_extras");

  if (args.isNotEmpty) {
    print("main: $args");
    print("Got ${args.length} Args, not starting app.");
    // If we return, then we don't exist to get the callback function.
    // the engine keeps running, but we aren't listening.
    // return;
  }
  String initialRoute =  "/";
  if (intent_extras.containsKey("path_to_photo")) {
    initialRoute = "/photo_edit";
  }
  runApp(MyApp(initialRoute: initialRoute));
}

class MyApp extends StatelessWidget {
  final String initialRoute;

  const MyApp({this.initialRoute="/", super.key});

  @override
  Widget build(BuildContext context) {

    return MaterialApp(
      home: const MyAppScreen(),
      initialRoute: initialRoute,
      routes: {
        // "/": (context) => const MyAppScreen(),
        "/photo_edit": (context) => const EditScreen(),
      },
    );
  }
}


class EditScreen extends StatelessWidget {
  const EditScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final file = File(intent_extras["path_to_photo"]);
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
          leading: InkWell(
            onTap: () {
              Navigator.pop(context);
            },
            child: const Icon(
              Icons.arrow_back_ios,
              color: Colors.black54,
            ),
          ),
        ),
        body: Column(
          children: [
            Text('Current route = ${ModalRoute.of(context)?.settings.name}'),
            Image(image: Image.file(file).image)
          ],
        ),
      ),
    );
  }
}

class MyAppScreen extends StatefulWidget {
  const MyAppScreen({super.key});

  @override
  State<MyAppScreen> createState() => _MyAppScreenState();
}

class _MyAppScreenState extends State<MyAppScreen> {
  final _lockScreenNotificationPlugin = LockScreenNotification();
  int _ret = 0;
  String _method = "";
  List<Object?> _args = [""];


  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    _lockScreenNotificationPlugin.initialize(defaultHandler);
    callFunction();
  }

  void defaultHandler(MethodCall call) {

    print("Reached dart callback as: ${call.method}(${call.arguments})");

    if (call.method == "feelings_event") {
      final args = call.arguments as List<dynamic>;
      _lockScreenNotificationPlugin.createNotification("How are you Feeling now? (Last was ${args[1]})");
      print("TODO: Record ${args[0]} in database");
    } else if (call.method == "picture_event") {
      final arg = call.arguments as String;
      print("TODO: Read picture from $arg and record it in the database");
    } else if (call.method == "edit_picture") {
      final arg = call.arguments as String;
      print("TODO: Verify picture from $arg is in database and open a deep link");
    }

    setState(() {
      _method = call.method;
      if (call.arguments is List) {
        _args = call.arguments;
      } else {
        _args[0] = call.arguments;
      }
    });

  }

  void callFunction() async {
    final ret = await _lockScreenNotificationPlugin.createNotification("How are you Feeling?");
    setState(() {
      _ret = ret ?? -1;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
            children: [
              Text('Current route = ${ModalRoute.of(context)?.settings.name}'),
              ElevatedButton(onPressed: callFunction,
                  child: const Text("Create Notification")),
              ElevatedButton(onPressed: () => _lockScreenNotificationPlugin.cancelNotification(),
                  child: const Text("Cancel Notification")),
              Text('Notification returned $_ret\n'),
              Text('Method: $_method(${_args})'),
              Text('Extras: $intent_extras'),
            ]
        ),
      ),
    );
  }
}
