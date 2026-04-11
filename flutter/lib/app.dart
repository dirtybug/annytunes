import 'package:flutter/material.dart';

import 'theme/theme.dart';

class AnnytunesApp extends StatelessWidget {
  const AnnytunesApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Annytunes',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.system,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      home: const Scaffold(
        body: Center(child: Text('Annytunes')),
      ),
    );
  }
}
