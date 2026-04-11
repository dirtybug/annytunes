import 'package:flutter/material.dart';

import '../spacing.dart';

/// A semi-transparent overlay that shows progress during long operations.
///
/// Intended to be shown as a stack overlay on top of existing content.
class ProgressOverlay extends StatelessWidget {
  const ProgressOverlay({
    super.key,
    required this.progress,
    this.message,
  });

  /// Progress value from 0.0 to 1.0.
  final double progress;

  /// Optional message displayed above the progress bar.
  final String? message;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final percent = (progress * 100).round();

    return ColoredBox(
      color: Colors.black54,
      child: Center(
        child: Card(
          margin: const EdgeInsets.all(AppSpacing.xl),
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (message != null) ...[
                  Text(message!, style: theme.textTheme.titleMedium),
                  const SizedBox(height: AppSpacing.md),
                ],
                LinearProgressIndicator(value: progress),
                const SizedBox(height: AppSpacing.sm),
                Text('$percent%', style: theme.textTheme.bodySmall),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
