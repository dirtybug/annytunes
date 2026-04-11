import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// A specialized text field for entering radio frequencies in MHz.
///
/// Converts between Hz (int, used internally) and MHz display (double).
/// Validates that the frequency is within the 100-520 MHz range.
class FrequencyInputField extends StatefulWidget {
  final int valueHz;
  final ValueChanged<int> onChanged;
  final String label;

  const FrequencyInputField({
    super.key,
    required this.valueHz,
    required this.onChanged,
    this.label = 'Frequency',
  });

  @override
  State<FrequencyInputField> createState() => _FrequencyInputFieldState();
}

class _FrequencyInputFieldState extends State<FrequencyInputField> {
  late final TextEditingController _controller;

  static const double _minMHz = 100.0;
  static const double _maxMHz = 520.0;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: _hzToMhzString(widget.valueHz));
  }

  @override
  void didUpdateWidget(FrequencyInputField oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.valueHz != widget.valueHz) {
      final newText = _hzToMhzString(widget.valueHz);
      if (_controller.text != newText) {
        _controller.text = newText;
      }
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  static String _hzToMhzString(int hz) {
    if (hz <= 0) return '';
    return (hz / 1000000).toStringAsFixed(5);
  }

  static int _mhzStringToHz(String text) {
    final mhz = double.tryParse(text);
    if (mhz == null) return 0;
    return (mhz * 1000000).round();
  }

  String? _validate(String? value) {
    if (value == null || value.isEmpty) return 'Required';
    final mhz = double.tryParse(value);
    if (mhz == null) return 'Invalid number';
    if (mhz < _minMHz || mhz > _maxMHz) {
      return '$_minMHz - $_maxMHz MHz';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: _controller,
      decoration: InputDecoration(
        labelText: widget.label,
        suffixText: 'MHz',
        border: const OutlineInputBorder(),
      ),
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [
        FilteringTextInputFormatter.allow(RegExp(r'[\d.]')),
      ],
      validator: _validate,
      onChanged: (text) {
        final hz = _mhzStringToHz(text);
        if (hz > 0) widget.onChanged(hz);
      },
    );
  }
}
