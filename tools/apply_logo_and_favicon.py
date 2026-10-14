import re
import glob
import os

main_pages = [
    'index.html',
    'features.html',
    'pricing.html',
    'about.html',
    'privacy.html',
    'terms.html',
    'refund.html',
    'guides.html'
]

# Replacement block for Favicons in main pages (root level)
root_favicon_block = """    <!-- ── Favicon ────────────────────────────────────────────────── -->
    <link rel="shortcut icon" href="favicon.ico">
    <link rel="icon" type="image/png" sizes="32x32" href="assets/favicon-32x32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="assets/favicon-16x16.png">
    <link rel="icon" type="image/png" href="assets/favicon.png">
    <link rel="apple-touch-icon" sizes="180x180" href="assets/apple-touch-icon.png">"""

# Replacement block for Favicons in guides/ subfolder
guide_favicon_block = """    <!-- ── Favicon ────────────────────────────────────────────────── -->
    <link rel="shortcut icon" href="../favicon.ico">
    <link rel="icon" type="image/png" sizes="32x32" href="../assets/favicon-32x32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="../assets/favicon-16x16.png">
    <link rel="icon" type="image/png" href="../assets/favicon.png">
    <link rel="apple-touch-icon" sizes="180x180" href="../assets/apple-touch-icon.png">"""

# Unified navbar logo & brand for main pages
root_navbar_brand = """        <!-- Logo & Brand -->
        <a href="index.html" class="flex items-center gap-3 group">
            <div class="w-9 h-9 rounded-xl bg-asr-elevated border border-asr-border flex items-center justify-center p-1.5 group-hover:border-asr-sky/50 transition overflow-hidden">
                <img src="assets/barakah_logo.png" alt="Barakah Logo" class="w-full h-full object-contain">
            </div>
            <div>
                <span class="font-extrabold text-lg tracking-tight text-asr-text">Barakah</span>
                <span class="hidden sm:inline-block text-[10px] text-asr-sky font-semibold ml-1.5 px-1.5 py-0.5 rounded bg-asr-sky/10 border border-asr-sky/20">FOCUS</span>
            </div>
        </a>"""

# Unified navbar logo & brand for guides/ subfolder
guide_navbar_brand = """        <!-- Logo & Brand -->
        <a href="../index.html" class="flex items-center gap-3 group">
            <div class="w-9 h-9 rounded-xl bg-asr-elevated border border-asr-border flex items-center justify-center p-1.5 group-hover:border-asr-sky/50 transition overflow-hidden">
                <img src="../assets/barakah_logo.png" alt="Barakah Logo" class="w-full h-full object-contain">
            </div>
            <div>
                <span class="font-extrabold text-lg tracking-tight text-asr-text">Barakah</span>
                <span class="hidden sm:inline-block text-[10px] text-asr-sky font-semibold ml-1.5 px-1.5 py-0.5 rounded bg-asr-sky/10 border border-asr-sky/20">FOCUS</span>
            </div>
        </a>"""

# Unified footer brand logo
root_footer_brand = """                <div class="flex items-center gap-2.5 mb-3">
                    <div class="w-8 h-8 rounded-lg bg-asr-elevated border border-asr-border flex items-center justify-center p-1 overflow-hidden">
                        <img src="assets/barakah_logo.png" alt="Barakah Logo" class="w-full h-full object-contain">
                    </div>
                    <span class="font-extrabold text-lg text-asr-text">Barakah</span>
                </div>"""

guide_footer_brand = """                <div class="flex items-center gap-2.5 mb-3">
                    <div class="w-8 h-8 rounded-lg bg-asr-elevated border border-asr-border flex items-center justify-center p-1 overflow-hidden">
                        <img src="../assets/barakah_logo.png" alt="Barakah Logo" class="w-full h-full object-contain">
                    </div>
                    <span class="font-extrabold text-lg text-asr-text">Barakah</span>
                </div>"""

# 1. Update main root pages
for page in main_pages:
    if not os.path.exists(page): continue
    with open(page, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace favicon tags
    content = re.sub(
        r'(?:<!--[^\n]*Favicon[^\n]*-->\s*)?<link rel="(?:shortcut )?icon"[^>]*>(?:\s*<link rel="apple-touch-icon"[^>]*>)?',
        root_favicon_block,
        content,
        flags=re.IGNORECASE
    )
    # Also clean up any extra duplicate apple-touch-icon right after
    content = re.sub(
        r'(<link rel="apple-touch-icon"[^>]*>\s*)<link rel="apple-touch-icon"[^>]*>',
        r'\1',
        content
    )

    # Replace navbar brand:
    # Pattern 1: <a href="[^"]*" class="flex items-center[^"]*">\s*<img src="assets/(?:playstore_icon|asar_navbar_brand|asr_logo|barakah_logo)\.png"[^>]*>\s*</a>
    content = re.sub(
        r'<a href="index\.html" class="flex items-center group">\s*<img src="assets/[^"]+" alt="[^"]*" class="[^"]*">\s*</a>',
        root_navbar_brand,
        content
    )
    # Pattern 2: if index.html has <a href="#" class="flex items-center gap-3 group">
    content = re.sub(
        r'<a href="#" class="flex items-center gap-3 group">\s*<div class="w-9 h-9[^"]*">.*?</div>\s*<div>\s*<span class="font-extrabold[^>]*>.*?</span>.*?</div>\s*</a>',
        root_navbar_brand,
        content,
        flags=re.DOTALL
    )

    # Replace footer brand
    content = re.sub(
        r'<div class="flex items-center gap-2\.5 mb-3">\s*<div class="w-8 h-8[^"]*">.*?</div>\s*<span class="font-extrabold[^>]*>.*?</span>\s*</div>',
        root_footer_brand,
        content,
        flags=re.DOTALL
    )

    # Replace any leftover alt="ASAR Logo" or src="assets/playstore_icon.png" in brand logos
    content = content.replace('alt="ASAR Logo"', 'alt="Barakah Logo"')
    content = content.replace('ASAR Knowledge Base', 'Barakah Knowledge Base')

    with open(page, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Updated main page: {page}")

# 2. Update all guides in guides/
guide_files = glob.glob('guides/*.html')
print(f"Updating {len(guide_files)} guide files...")

for g in guide_files:
    with open(g, 'r', encoding='utf-8') as f:
        content = f.read()

    # Favicons
    content = re.sub(
        r'(?:<!--[^\n]*Favicon[^\n]*-->\s*)?<link rel="(?:shortcut )?icon"[^>]*>(?:\s*<link rel="apple-touch-icon"[^>]*>)?',
        guide_favicon_block,
        content,
        flags=re.IGNORECASE
    )

    # Navbar brand
    content = re.sub(
        r'<a href="\.\./index\.html" class="flex items-center group">\s*<img src="\.\./assets/[^"]+" alt="[^"]*" class="[^"]*">\s*</a>',
        guide_navbar_brand,
        content
    )
    content = re.sub(
        r'<a href="\.\./index\.html" class="flex items-center gap-3 group">.*?</a>',
        guide_navbar_brand,
        content,
        flags=re.DOTALL
    )

    # Footer brand
    content = re.sub(
        r'<div class="flex items-center gap-2\.5 mb-3">\s*<div class="w-8 h-8[^"]*">.*?</div>\s*<span class="font-extrabold[^>]*>.*?</span>\s*</div>',
        guide_footer_brand,
        content,
        flags=re.DOTALL
    )

    content = content.replace('alt="ASAR Logo"', 'alt="Barakah Logo"')
    content = content.replace('alt="ASAR"', 'alt="Barakah"')

    with open(g, 'w', encoding='utf-8') as f:
        f.write(content)

print("All guides updated successfully!")
