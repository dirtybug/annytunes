import 'package:flutter/material.dart';

/// Displays a frequency value (in Hz) as a formatted MHz string.
///
/// Example: 145500000 Hz -> "145.50000 MHz"
class FrequencyText extends StatelessWidget {
  const FrequencyText({
    super.key,
    required this.frequencyHz,
    this.style,
  });

  /// Frequency in Hz.
  final int frequencyHz;

  /// Optional text style override. A monospace font is applied by default.
  final TextStyle? style;

  @override
  Widget build(BuildContext context) {
    final mhz = frequencyHz / 1e6;
    final formatted = '${mhz.toStringAsFixed(5)} MHz';

    final baseStyle =
        style ?? Theme.of(context).textTheme.bodyMedium ?? const TextStyle();

    return Text(
      formatted,
      style: baseStyle.copyWith(fontFamily: 'monospace'),
    );
  }
}
