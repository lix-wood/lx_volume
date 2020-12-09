import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

@immutable
class VolumeVal {
  final double vol;
  final int type;

  VolumeVal({
    @required this.vol,
    @required this.type,
  })  : assert(vol != null),
        assert(type != null);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
          (other is VolumeVal && hashCode == other.hashCode);

  @override
  int get hashCode => hashValues(vol, type);
}

class _VolumeValueNotifier extends ValueNotifier<VolumeVal> {
  _VolumeValueNotifier(VolumeVal value) : super(value);
}

typedef VolumeCallback = void Function(VolumeVal value);

class LxVolume {
  static const int STREAM_VOICE_CALL = 0;
  static const int STREAM_SYSTEM = 1;
  static const int STREAM_RING = 2;
  static const int STREAM_MUSIC = 3;
  static const int STREAM_ALARM = 4;

  static const double _step = 1.0 / 16.0;

  static const MethodChannel _channel = const MethodChannel('com.nowebx.lx_volume');

  static StreamSubscription _eventSubs;

  static _VolumeValueNotifier _notifier =
  _VolumeValueNotifier(VolumeVal(vol: 0, type: 0));

  static Future<bool> init({int type = STREAM_MUSIC}) async {
    return _channel.invokeMethod('init', { 'type': type });
  }

  static Future<double> get() async {
    final double volume = await _channel.invokeMethod('get');
    return volume;
  }
//
  static Future<double> set(double vol) async {
    final double volume = await _channel.invokeMethod('set', {"vol": vol});
    return volume;
  }

  static Future<double> disableUI() async {
    return _channel.invokeMethod('disable_ui');
  }

  static Future<double> enableUI() async {
    return _channel.invokeMethod('enable_ui');
  }

  static void enableWatcher() async {
    if (_eventSubs == null) {
      await _channel.invokeMethod("enable_watch");
      _eventSubs = EventChannel('com.nowebx.lx_volume/event')
          .receiveBroadcastStream()
          .listen(_eventListener, onError: _errorListener);
    }
  }

  static void disableWatcher() async {
    _eventSubs?.cancel();
    await _channel.invokeMethod("disable_watch");
    _eventSubs = null;
  }

  static void _eventListener(dynamic event) {
    final Map<dynamic, dynamic> map = event;
    switch (map['event']) {
      case 'vol':
        double vol = map['v'];
        int type = map['t'];
        _notifier.value = VolumeVal(vol: vol, type: type);
        break;
      default:
        break;
    }
  }

  static void _errorListener(Object obj) {
    print("errorListener: $obj");
  }

  static void addVolListener(VoidCallback listener) {
    _notifier.addListener(listener);
  }

  static void removeVolListener(VoidCallback listener) {
    _notifier.removeListener(listener);
  }

  static VolumeVal get value => _notifier.value;
}

class LxVolumeWatcher extends StatefulWidget {
  final VolumeCallback watcher;
  final Widget child;

  LxVolumeWatcher({
    @required this.watcher,
    @required this.child,
  })  : assert(child != null),
        assert(watcher != null);

  @override
  _LxVolumeWatcherState createState() => _LxVolumeWatcherState();
}

class _LxVolumeWatcherState extends State<LxVolumeWatcher> {
  @override
  void initState() {
    super.initState();
    LxVolume.enableWatcher();
    LxVolume.addVolListener(_volListener);
  }

  void _volListener() {
    VolumeVal value = LxVolume.value;
    widget.watcher(value);
  }

  @override
  void dispose() {
    super.dispose();
    LxVolume.removeVolListener(_volListener);
  }

  @override
  Widget build(BuildContext context) {
    return widget.child;
  }
}
