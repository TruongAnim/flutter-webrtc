import 'dart:typed_data';

import 'utils.dart';

class VideoFilterManager {
  static Future<void> setEnableFilter(bool enableFilter) async {
    await WebRTC.invokeMethod(
      'setEnableFilter',
      <String, dynamic>{'enableFilter': enableFilter},
    );
  }

  static Future<void> setFilterParams(
      {required double toneLevel, required double beautyLevel, required double brightLevel}) async {
    Float32List params = Float32List(3);
    params[0] = toneLevel;
    params[1] = beautyLevel;
    params[2] = brightLevel;
    await WebRTC.invokeMethod(
      'setFilterParams',
      <String, dynamic>{'params': params},
    );
  }

  static Future<void> setToneLevel(double toneLevel) async {
    await WebRTC.invokeMethod(
      'setToneLevel',
      <String, dynamic>{'toneLevel': toneLevel},
    );
  }

  static Future<void> setBeautyLevel(double beautyLevel) async {
    await WebRTC.invokeMethod(
      'setBeautyLevel',
      <String, dynamic>{'beautyLevel': beautyLevel},
    );
  }

  static Future<void> setBrightLevel(double brightLevel) async {
    await WebRTC.invokeMethod(
      'setBrightLevel',
      <String, dynamic>{'brightLevel': brightLevel},
    );
  }
}
