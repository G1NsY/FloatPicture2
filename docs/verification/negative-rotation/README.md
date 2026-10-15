# 逆时针角度输入

图片编辑页的角度滑块现以 0° 为中点，覆盖 -360° 至 360°，每格 45°。角度输入框支持负数和 0.01° 精度；负角度表示逆时针。输入保存后仍保持负数回显，悬浮图片按相应方向旋转。取消编辑保留原角度。

2026-10-14 在 Android 16 / API 36.1 模拟器验证：`RotationEditingTest` 8 项全部通过，耗时 34.2 秒。测试使用隔离缓存图库；覆盖负数输入、滑块逆时针旋转、保存重开和取消恢复。

`:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` 通过。

安装包：`FloatPicture-2.0.9.3-counterclockwise-rotation-debug.apk`。
SHA-256：`B2324C37489C81DC6712F23B6668BEB530DBB08429DCB858B66370A982B14442`。
