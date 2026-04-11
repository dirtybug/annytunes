import 'package:flutter/material.dart';

/// A small chip that displays "DMR" or "Analog" with a distinct color.
class ChannelModeChip extends StatelessWidget {
  const ChannelModeChip({
    super.key,
    required this.digital,
  });

  /// When true, displays "DMR"; otherwise displays "Analog".
  final bool digital;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    final Color backgroundColor;
    final Color foregroundColor;
    final String label;

    if (digital) {
      backgroundColor = colorScheme.primaryContainer;
      foregroundColor = colorScheme.onPrimaryContainer;
      label = 'DMR';
    } else {
      backgroundColor = colorScheme.tertiaryContainer;
      foregroundColor = colorScheme.onTertiaryContainer;
      label = 'Analog';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: theme.textTheme.labelSmall?.copyWith(
              color: foregroundColor,
              fontWeight: FontWeight.w600,
            ),
      ),
    );
  }
}
