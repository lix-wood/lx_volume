import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:lx_volume/lx_volume.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  double _platformVersion = 0;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    double platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      LxVolume.init(type: LxVolume.STREAM_RING);
      platformVersion = await LxVolume.get();
    } on PlatformException {
      platformVersion = 0;
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              LxVolumeWatcher(watcher: (watcher) {
                _platformVersion = watcher.vol;
                setState(() {});
              }, child: Text('Running on: $_platformVersion\n')),
              FlatButton(onPressed: () async {
                LxVolume.set(1);
                _platformVersion = await LxVolume.get();
                setState(() {});
              }, child: Text("set")),
              FlatButton(onPressed: () async {
                LxVolume.disableUI();
                setState(() {});
              }, child: Text("disableUI")),
              FlatButton(onPressed: () async {
                LxVolume.enableUI();
                setState(() {});
              }, child: Text("enableUI"))
            ],
          ),
        ),
      ),
    );
  }
}
