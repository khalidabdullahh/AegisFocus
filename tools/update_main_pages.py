import re

def update_main_pages():
    pages = ['index.html', 'features.html', 'pricing.html', 'about.html', 'privacy.html', 'terms.html', 'refund.html', 'guides.html']
    
    for page in pages:
        with open(page, 'r', encoding='utf-8') as f:
            content = f.read()

        # Update Titles & Meta
        content = re.sub(r'ASAR — Turn Time Into Progress', 'Barakah — Protect Your Time. Multiply Barakah.', content)
        content = re.sub(r'ASAR — Be Productive', 'Barakah — Protect Your Time', content)
        content = re.sub(r'Turn Time Into Progress', 'Protect Your Time. Multiply Barakah.', content)
        content = re.sub(r'ASAR-Latest\.apk', 'Barakah-Release.apk', content)
        content = re.sub(r'assets/asar_logo_transparent_white\.png', 'assets/playstore_icon.png', content)
        content = re.sub(r'assets/asar_navbar_brand\.png', 'assets/playstore_icon.png', content)
        content = re.sub(r'assets/asr_logo_transparent_white\.png', 'assets/playstore_icon.png', content)
        content = re.sub(r'assets/asar_logo\.png', 'assets/playstore_icon.png', content)
        
        # Replace brand text where appropriate
        content = content.replace('>ASAR</span>', '>Barakah</span>')
        content = content.replace('>ASAR Pro<', '>Barakah Pro<')
        content = content.replace('ASAR is ', 'Barakah is ')
        content = content.replace('ASAR gives ', 'Barakah gives ')
        content = content.replace('ASAR works ', 'Barakah works ')
        content = content.replace('ASAR replaces ', 'Barakah replaces ')
        content = content.replace('Experience ASAR', 'Experience Barakah')
        content = content.replace('How ASAR Works', 'How Barakah Works')
        content = content.replace('HOW ASAR WORKS', 'HOW BARAKAH WORKS')
        content = content.replace('Who is ASAR for', 'Who is Barakah for')
        content = content.replace('WHO IS ASAR FOR', 'WHO IS BARAKAH FOR')
        content = content.replace('Why Choose ASAR', 'Why Choose Barakah')
        content = content.replace('About ASAR', 'About Barakah')
        content = content.replace('How ASAR Compares', 'How Barakah Compares')
        content = content.replace('© 2025 ASAR', '© 2026 Barakah')
        content = content.replace('© 2026 ASAR', '© 2026 Barakah')
        
        with open(page, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated main page: {page}")

update_main_pages()
