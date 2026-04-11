import 'package:flutter/material.dart';

class AnnytunesApp extends StatelessWidget {
  const AnnytunesApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Annytunes',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.system,
      theme: ThemeData(
        colorSchemeSeed: Colors.deepPurple,
        useMaterial3: true,
        brightness: Brightness.light,
      ),
      darkTheme: ThemeData(
        colorSchemeSeed: Colors.deepPurple,
        useMaterial3: true,
        brightness: Brightness.dark,
      ),
      home: const Scaffold(
        body: Center(child: Text('Annytunes')),
      ),
    );
  }
}
