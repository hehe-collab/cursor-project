import requests
import json
import logging
import re

logger = logging.getLogger("AI_Drama")

class TranslatorEngine:
    def __init__(self, model="llama3", host="http://localhost:11434"):
        self.model = model
        self.host = host
        self.api_url = f"{host}/api/chat"

    def is_mostly_chinese(self, text):
        return len(re.findall(r'[\u4e00-\u9fff]', text)) > 0

    def translate(self, text, target_lang="Thai"):
        """
        Few-Shot 强化版：通过示例强制 AI 学习短剧语境
        """
        # 1. 定义不同语言的职场映射示例
        examples = ""
        if "indonesian" in target_lang.lower() or "印尼" in target_lang.lower():
            examples = """
Examples of correct mapping:
- "李总好！" -> "Halo, Pak Li!" (NOT "Mr. Li is fine")
- "你作为公司的董事长" -> "Anda sebagai Direktur Utama perusahaan"
- "一口一个冰冰" -> "Memanggil Bingbing terus-menerus"
- "就这小白脸？" -> "Hanya gigolo ini?"
"""
        elif "thai" in target_lang.lower() or "泰" in target_lang.lower():
            examples = """
Examples of correct mapping:
- "李总好！" -> "สวัสดีครับ คุณหลี่!" (NOT "Mr. Li is fine")
- "你作为公司的董事长" -> "คุณในฐานะประธานกรรมการของบริษัท"
- "一口一个冰冰" -> "เรียกปิงปิงไม่หยุดปาก"
- "就这小白脸？" -> "แค่แมงดาคนนี้เหรอ?"
"""

        # 2. 构建极致严厉的 System Prompt
        persona_prompt = f"""You are a high-end subtitle translator for Chinese CEO-themed Short Dramas.
Translate into idiomatic {target_lang}.

CRITICAL RULES:
1. GREETING RULE: '[Name] + 总 + 好' is a GREETING.
   - Indonesian: 'Halo, Pak [Name]'
   - Thai: 'สวัสดีครับ คุณ [Name]'
   - NEVER translate as '[Name] is fine/good'.
2. TITLE RULE: '总' is 'CEO/President'. Use 'Pak' or 'Direktur' (ID) / 'Khun' (TH).
3. NO CHINESE: Your response must contain 0% Chinese characters.
4. FORMAT: Output ONLY the translated text. Match the line count exactly.

{examples}

Text to translate:
{text}"""

        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": "You are a translation engine that follows few-shot examples strictly."},
                {"role": "user", "content": persona_prompt}
            ],
            "stream": False,
            "options": {
                "temperature": 0.1, # 进一步调低随机性，使其死板地遵守示例
                "top_p": 0.9,
                "num_predict": 1024
            }
        }
        
        try:
            proxies = {"http": None, "https": None}
            response = requests.post(self.api_url, json=payload, timeout=60, proxies=proxies)
            if response.status_code == 200:
                result = response.json()
                raw_res = result.get("message", {}).get("content", "").strip()
                
                # 深度清洗
                clean_res = re.sub(r'```[a-zA-Z]*\n?', '', raw_res).replace('```', '')
                lines = clean_res.split('\n')
                # 过滤可能的 AI 废话
                filtered = []
                for l in lines:
                    low = l.lower()
                    if low.startswith("here is") or low.startswith("translation") or "sure" in low[:10]:
                        continue
                    filtered.append(l)
                
                final_res = "\n".join(filtered).strip()
                # 汉字残留检测
                if self.is_mostly_chinese(final_res): return ""
                return final_res
            return ""
        except Exception as e:
            logger.error(f"Ollama 连接失败: {e}")
            return ""
