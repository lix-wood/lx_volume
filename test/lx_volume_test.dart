import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lx_volume/lx_volume.dart';

void main() {
  const MethodChannel channel = MethodChannel('lx_volume');

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
    expect(await LxVolume.get(), '42');
  });
}
