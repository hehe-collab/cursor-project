import cv2
import imagehash
from PIL import Image
import sys

def crop_subtitle_area(image_cv, crop_ratio=0.2):
    """
    裁剪图片底部区域以消除字幕干扰
    crop_ratio: 裁剪掉底部的比例，默认20%（0.2）
    """
    height, width = image_cv.shape[:2]
    crop_height = int(height * (1 - crop_ratio))
    return image_cv[:crop_height, :]

def calculate_phash(image_cv):
    """
    计算图片的感知哈希(pHash)
    """
    # OpenCV 默认读取为 BGR，转换为 RGB
    image_rgb = cv2.cvtColor(image_cv, cv2.COLOR_BGR2RGB)
    # imagehash 需要 PIL 的 Image 对象
    pil_image = Image.fromarray(image_rgb)
    return imagehash.phash(pil_image)

def test_similarity(img1_path, img2_path, crop_ratio=0.2):
    print(f"正在读取图片: {img1_path} 和 {img2_path}...")
    img1 = cv2.imread(img1_path)
    img2 = cv2.imread(img2_path)
    
    if img1 is None or img2 is None:
        print("错误: 无法读取图片，请检查路径是否正确。")
        return
        
    print(f"[{img1_path}] 原始尺寸: {img1.shape[:2]}")
    print(f"[{img2_path}] 原始尺寸: {img2.shape[:2]}")
    
    # 1. 裁剪底部字幕区
    print(f"\n--- 步骤 1: 裁剪底部 {int(crop_ratio * 100)}% 的区域（去除字幕） ---")
    cropped_img1 = crop_subtitle_area(img1, crop_ratio)
    cropped_img2 = crop_subtitle_area(img2, crop_ratio)
    
    print(f"裁剪后尺寸: {cropped_img1.shape[:2]}")
    
    # 将裁剪后的图片保存以便人工确认
    cv2.imwrite("cropped_chinese.jpg", cropped_img1)
    cv2.imwrite("cropped_indo.jpg", cropped_img2)
    print("已保存裁剪后的预览图: cropped_chinese.jpg, cropped_indo.jpg")
    
    # 2. 计算 pHash
    print("\n--- 步骤 2: 提取图像特征 (pHash) ---")
    hash1 = calculate_phash(cropped_img1)
    hash2 = calculate_phash(cropped_img2)
    
    print(f"图1 pHash: {hash1}")
    print(f"图2 pHash: {hash2}")
    
    # 3. 计算汉明距离
    print("\n--- 步骤 3: 计算汉明距离 (Hamming Distance) ---")
    # 距离越小，图片越相似。通常距离 <= 5 可以认为是非常相似的画面
    distance = hash1 - hash2
    print(f"两张图片的汉明距离为: {distance}")
    
    print("\n结论:")
    if distance == 0:
        print("✅ 画面完美匹配！(像素级一致)")
    elif distance <= 5:
        print("✅ 画面成功匹配！(几乎相同，可能存在轻微的压缩、调色或分辨率变化)")
    elif distance <= 10:
        print("⚠️ 画面相似，可能是同一个镜头的不同帧，或者经过了大幅度的裁剪/调色。")
    else:
        print("❌ 画面不匹配，这是两个不同的镜头。")

if __name__ == "__main__":
    if len(sys.argv) == 3:
        test_similarity(sys.argv[1], sys.argv[2])
    else:
        print("使用方法: python test_image_match.py <图片1路径> <图片2路径>")
        print("示例: python test_image_match.py dummy_chinese.jpg dummy_indo.jpg")
