import os
import glob
import re

def update_file(filepath, replacements):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content
    for pattern, repl in replacements:
        content = re.sub(pattern, repl, content)

    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated: {filepath}")

# Process all guides
guides = glob.glob('guides/*.html')
print(f"Processing {len(guides)} guide files...")

guide_replacements = [
    (r'ASAR-Latest\.apk', 'Barakah-Release.apk'),
    (r'ASAR — Turn Time Into Progress', 'Barakah — Protect Your Time. Multiply Barakah.'),
    (r'ASAR — Be Productive', 'Barakah — Protect Your Time'),
    (r'Turn Time Into Progress', 'Protect Your Time. Multiply Barakah.'),
    (r'ASAR', 'Barakah'),
    (r'ASR', 'Barakah'),
    (r'assets/asar_logo_transparent_white\.png', 'assets/playstore_icon.png'),
    (r'assets/asar_logo\.png', 'assets/playstore_icon.png'),
    (r'assets/asar_navbar_brand\.png', 'assets/playstore_icon.png'),
]

for g in guides:
    update_file(g, guide_replacements)

