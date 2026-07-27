from PIL import Image, ImageOps

def process_logo(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    data = img.getdata()

    new_data = []
    for item in data:
        avg = (item[0] + item[1] + item[2]) // 3
        if avg > 180:
            new_data.append((255, 255, 255, 0))  # transparent
        else:
            alpha = 255 - int((avg / 180.0) * 100)
            new_data.append((255, 255, 255, min(255, alpha + 155)))

    img.putdata(new_data)

    # Auto-crop: remove transparent borders
    bbox = img.getbbox()
    if bbox:
        # Add small padding
        pad = 20
        bbox = (
            max(0, bbox[0] - pad),
            max(0, bbox[1] - pad),
            min(img.width, bbox[2] + pad),
            min(img.height, bbox[3] + pad),
        )
        img = img.crop(bbox)

    img.save(output_path, "PNG")
    print(f"Done! Cropped size: {img.size}")

process_logo("public/logo.png", "public/logo_transparent.png")
