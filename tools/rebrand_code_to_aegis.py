import re

files_to_update = [
    ("app/src/main/java/com/aegisfocus/app/presentation/onboarding/OnboardingScreen.kt", [
        (r'"ENTER BARAKAH"', r'"ENTER AEGIS FOCUS"'),
        (r'Barakah requires', r'Aegis Focus requires')
    ]),
    ("app/src/main/java/com/aegisfocus/app/presentation/onboarding/OnboardingViewModel.kt", [
        (r'Barakah cannot be uninstalled', r'Aegis Focus cannot be uninstalled')
    ]),
    ("app/src/main/java/com/aegisfocus/app/presentation/focus/FocusViewModel.kt", [
        (r'Barakah cannot be uninstalled', r'Aegis Focus cannot be uninstalled')
    ]),
    ("app/src/main/java/com/aegisfocus/app/presentation/settings/SettingsViewModel.kt", [
        (r'Barakah during strict focus', r'Aegis Focus during strict focus')
    ]),
    ("app/src/main/java/com/aegisfocus/app/presentation/settings/SettingsScreen.kt", [
        (r'Barakah Pro Membership', r'Aegis Focus Pro'),
        (r'Barakah operates completely', r'Aegis Focus operates completely'),
        (r'Barakah • v1\.1\.0', r'Aegis Focus • v1.1.0'),
        (r'BARAKAH-12M-XXXX-XXXX', r'AEGIS-12M-XXXX-XXXX')
    ]),
    ("app/src/main/java/com/aegisfocus/app/presentation/paywall/PaywallScreen.kt", [
        (r'BARAKAH-12M-XXXX-XXXX', r'AEGIS-12M-XXXX-XXXX')
    ]),
    ("app/src/main/java/com/aegisfocus/app/domain/subscription/LicenseKeyValidator.kt", [
        (r'parts\[0\] != "BARAKAH"', r'parts[0] != "AEGIS" && parts[0] != "BARAKAH"'),
        (r'Expected: BARAKAH-XXXX-XXXX-XXXX', r'Expected: AEGIS-XXXX-XXXX-XXXX'),
        (r'return "BARAKAH-\$tier-\$payload-\$sig"', r'return "AEGIS-$tier-$payload-$sig"')
    ]),
    ("tools/generate_license.py", [
        (r'prefix = "BARAKAH"', r'prefix = "AEGIS"')
    ])
]

for filepath, replacements in files_to_update:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    for pat, rep in replacements:
        content = re.sub(pat, rep, content)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Updated: {filepath}")

