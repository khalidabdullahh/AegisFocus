import os
from PIL import Image, ImageDraw

def create_circular_icon(img):
    mask = Image.new('L', img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, img.size[0], img.size[1]), fill=255)
    
    output = Image.new('RGBA', img.size, (0, 0, 0, 0))
    output.paste(img, (0, 0))
    output.putalpha(mask)
    return output

def create_adaptive_foreground(img, size):
    # Adaptive foreground canvas is `size` x `size`, inner icon should occupy ~70% to be in safe zone
    fg = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    icon_size = int(size * 0.72)
    resized_img = img.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    
    offset = (size - icon_size) // 2
    fg.paste(resized_img, (offset, offset))
    return fg

def main():
    source_path = '/Users/khalidabdullah/.gemini/antigravity/brain/2e5f9ff2-0ef5-4c03-bd2b-ff59edfae324/.user_uploaded/media_1791168383682.jpg'
    base_dir = '/Users/khalidabdullah/AntiGravity/aegis-focus'
    
    src = Image.open(source_path).convert('RGBA')
    
    # Res directories
    mipmaps = {
        'mipmap-mdpi': (48, 108),
        'mipmap-hdpi': (72, 162),
        'mipmap-xhdpi': (96, 216),
        'mipmap-xxhdpi': (144, 324),
        'mipmap-xxxhdpi': (192, 432)
    }
    
    for folder, (size, fg_size) in mipmaps.items():
        folder_path = os.path.join(base_dir, 'app/src/main/res', folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # ic_launcher.png (Square / Rounded corners)
        launcher_img = src.resize((size, size), Image.Resampling.LANCZOS)
        launcher_img.save(os.path.join(folder_path, 'ic_launcher.png'), 'PNG')
        
        # ic_launcher_round.png
        round_img = create_circular_icon(launcher_img)
        round_img.save(os.path.join(folder_path, 'ic_launcher_round.png'), 'PNG')
        
        # ic_launcher_foreground.png
        fg_img = create_adaptive_foreground(src, fg_size)
        fg_img.save(os.path.join(folder_path, 'ic_launcher_foreground.png'), 'PNG')
        print(f"Generated icons for {folder}")
        
    # Drawable logo
    drawable_path = os.path.join(base_dir, 'app/src/main/res/drawable-nodpi')
    os.makedirs(drawable_path, exist_ok=True)
    src.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(drawable_path, 'ic_asr_logo.png'), 'PNG')
    
    # Assets
    assets_dir = os.path.join(base_dir, 'assets')
    os.makedirs(assets_dir, exist_ok=True)
    
    src.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, 'playstore_icon.png'), 'PNG')
    src.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, 'iap_icon_512x512.png'), 'PNG')
    src.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, 'asar_logo.png'), 'PNG')
    src.resize((114, 114), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, 'iap_icon_114x114.png'), 'PNG')
    src.resize((114, 114), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, 'amazon_icon_114.png'), 'PNG')
    src.resize((114, 114), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, 'amazon_icon_114_light.png'), 'PNG')
    src.resize((64, 64), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, 'favicon.png'), 'PNG')
    print("All assets generated successfully!")

if __name__ == '__main__':
    main()
