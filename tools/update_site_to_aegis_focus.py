import re
import glob
import os

pages = ['index.html', 'features.html', 'pricing.html', 'about.html', 'privacy.html', 'terms.html', 'refund.html', 'guides.html']

for page in pages:
    if not os.path.exists(page): continue
    with open(page, 'r', encoding='utf-8') as f:
        content = f.read()

    # Title & Metadata
    content = re.sub(r'Barakah — Protect Your Time\. Multiply Barakah\.', 'Aegis Focus — Unbreakable Digital Discipline & Focus', content)
    content = re.sub(r'Barakah — Protect Your Time', 'Aegis Focus — Unbreakable App Blocker', content)
    content = re.sub(r'Protect Your Time\. Multiply Barakah\.', 'Unbreakable Digital Discipline. Protect Your Attention.', content)
    content = re.sub(r'Barakah Pro', 'Aegis Focus Pro', content)
    content = re.sub(r'Barakah', 'Aegis Focus', content)

    # Assets & Download links
    content = re.sub(r'Barakah-Release\.apk', 'AegisFocus-Latest.apk', content)
    content = re.sub(r'ASAR-Latest\.apk', 'AegisFocus-Latest.apk', content)
    content = re.sub(r'assets/barakah_logo\.png', 'assets/aegis_logo.png', content)
    content = re.sub(r'assets/barakah_logo_transparent\.png', 'assets/aegis_logo_transparent.png', content)

    with open(page, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Updated: {page}")

# Update all guide pages
guides = glob.glob('guides/*.html')
for g in guides:
    with open(g, 'r', encoding='utf-8') as f:
        content = f.read()

    content = re.sub(r'Barakah — Protect Your Time\. Multiply Barakah\.', 'Aegis Focus — Unbreakable Digital Discipline & Focus', content)
    content = re.sub(r'Barakah — Protect Your Time', 'Aegis Focus — Unbreakable App Blocker', content)
    content = re.sub(r'Protect Your Time\. Multiply Barakah\.', 'Unbreakable Digital Discipline. Protect Your Attention.', content)
    content = re.sub(r'Barakah-Release\.apk', 'AegisFocus-Latest.apk', content)
    content = re.sub(r'ASAR-Latest\.apk', 'AegisFocus-Latest.apk', content)
    content = re.sub(r'\.\./assets/barakah_logo\.png', '../assets/aegis_logo.png', content)
    content = re.sub(r'assets/barakah_logo\.png', 'assets/aegis_logo.png', content)
    content = re.sub(r'Barakah', 'Aegis Focus', content)

    with open(g, 'w', encoding='utf-8') as f:
        f.write(content)

print(f"Updated {len(guides)} guide pages!")
