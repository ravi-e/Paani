import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class PaaniTheme {
  static ThemeData get lightTheme {
    final baseTextTheme = GoogleFonts.atkinsonHyperlegibleTextTheme();

    final customTextTheme = baseTextTheme.copyWith(
      headlineLarge: GoogleFonts.atkinsonHyperlegible(
        fontSize: 32,
        fontWeight: FontWeight.w800,
        letterSpacing: -0.01 * 32,
        color: const Color(0xFF191C1E),
      ),
      headlineMedium: GoogleFonts.atkinsonHyperlegible(
        fontSize: 28,
        fontWeight: FontWeight.w700,
        color: const Color(0xFF191C1E),
      ),
      bodyLarge: GoogleFonts.atkinsonHyperlegible(
        fontSize: 22,
        fontWeight: FontWeight.w400,
        height: 32 / 22,
        color: const Color(0xFF191C1E),
      ),
      bodyMedium: GoogleFonts.atkinsonHyperlegible(
        fontSize: 18,
        fontWeight: FontWeight.w400,
        height: 28 / 18,
        color: const Color(0xFF191C1E),
      ),
      labelLarge: GoogleFonts.atkinsonHyperlegible(
        fontSize: 20,
        fontWeight: FontWeight.w700,
        letterSpacing: 0.02 * 20,
        color: const Color(0xFF191C1E),
      ),
      labelMedium: GoogleFonts.atkinsonHyperlegible(
        fontSize: 16,
        fontWeight: FontWeight.w700,
        color: const Color(0xFF191C1E),
      ),
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: const ColorScheme(
        brightness: Brightness.light,
        primary: const Color(0xFF00327D),
        primaryContainer: const Color(0xFF0047AB),
        secondary: const Color(0xFF006D35),
        secondaryContainer: const Color(0xFF8DF9A8),
        surface: const Color(0xFFFCF9F4),
        onPrimary: Colors.white,
        onSecondary: Colors.white,
        onSurface: const Color(0xFF1C1C19),
        onError: Colors.white,
        error: const Color(0xFFBA1A1A),
        outline: const Color(0xFF737784),
        outlineVariant: const Color(0xFFC3C6D5),
        tertiary: const Color(0xFF363636),
        tertiaryContainer: const Color(0xFF4D4D4D),
      ),
      scaffoldBackgroundColor: const Color(0xFFFCF9F4),
      textTheme: customTextTheme,
      
      // Component Overrides
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          minimumSize: const Size(88, 64),
          elevation: 0,
          backgroundColor: const Color(0xFF00327D),
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8.0),
          ),
          textStyle: GoogleFonts.atkinsonHyperlegible(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            letterSpacing: 0.02 * 20,
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(88, 64),
          foregroundColor: const Color(0xFF00327D),
          side: const BorderSide(color: Color(0xFF737784), width: 1.5),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8.0),
          ),
          textStyle: GoogleFonts.atkinsonHyperlegible(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            letterSpacing: 0.02 * 20,
          ),
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(
          minimumSize: const Size(56, 56),
        ),
      ),
      listTileTheme: const ListTileThemeData(
        minVerticalPadding: 16.0,
        minTileHeight: 72.0,
        contentPadding: EdgeInsets.symmetric(horizontal: 24.0, vertical: 8.0),
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith<Color?>((states) {
          if (states.contains(WidgetState.selected)) {
            return const Color(0xFF006D35); // Secondary/Vitality Green
          }
          return const Color(0xFF737784); // Outline
        }),
        trackColor: WidgetStateProperty.resolveWith<Color?>((states) {
          if (states.contains(WidgetState.selected)) {
            return const Color(0xFF8DF9A8); // Secondary Container
          }
          return const Color(0xFFC3C6D5); // Outline Variant
        }),
      ),
      inputDecorationTheme: InputDecorationTheme(
        floatingLabelBehavior: FloatingLabelBehavior.never,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 18.0),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8.0),
          borderSide: const BorderSide(color: Color(0xFF737784), width: 1.5),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8.0),
          borderSide: const BorderSide(color: Color(0xFF737784), width: 1.5),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8.0),
          borderSide: const BorderSide(color: Color(0xFF00327D), width: 2.0),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8.0),
          borderSide: const BorderSide(color: Color(0xFFBA1A1A), width: 1.5),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8.0),
          borderSide: const BorderSide(color: Color(0xFFBA1A1A), width: 2.0),
        ),
        errorStyle: GoogleFonts.atkinsonHyperlegible(
          fontSize: 16,
          color: const Color(0xFFBA1A1A),
        ),
      ),
    );
  }
}
