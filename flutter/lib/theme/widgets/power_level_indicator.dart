import 'package:flutter/material.dart';

import '../../core/models/enums.dart';

/// Displays a power level as an icon with a text label.
class PowerLevelIndicator extends StatelessWidget {
  const PowerLevelIndicator({
    super.key,
    required this.level,
  });

  final PowerLevel level;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final filledBars = switch (level) {
      PowerLevel.low => 1,
      PowerLevel.med => 2,
      PowerLevel.high => 3,
      PowerLevel.turbo => 4,
    };

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _buildBars(filledBars, theme.colorScheme),
        const SizedBox(width: 6),
        Text(
          level.label,
          style: theme.textTheme.labelSmall,
        ),
      ],
    );
  }

  Widget _buildBars(int filled, ColorScheme colorScheme) {
    const totalBars = 4;
    const barWidth = 4.0;
    const barSpacing = 2.0;

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.end,
      children: List.generate(totalBars, (i) {
        final height = 6.0 + (i * 4.0);
        final isFilled = i < filled;
        return Padding(
          padding: EdgeInsets.only(right: i < totalBars - 1 ? barSpacing : 0),
          child: Container(
            width: barWidth,
            height: height,
            decoration: BoxDecoration(
              color: isFilled
                  ? colorScheme.primary
                  : colorScheme.outlineVariant,
              borderRadius: BorderRadius.circular(1),
            ),
          ),
        );
      }),
    );
  }
}
