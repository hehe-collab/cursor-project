"""
QQ 邮箱 SMTP 发信
"""
from typing import List, Union

import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.image import MIMEImage
from pathlib import Path


def send_screenshot_email(
    smtp_user: str,
    smtp_password: str,
    recipient: str,
    screenshot_path: Union[str, List[str]],
    subject: str = "不力看板截图",
    body: str = "请查收看板截图。",
) -> bool:
    """
    通过 QQ 邮箱 SMTP 发送带截图的邮件。
    screenshot_path 可为单个路径或路径列表，多个截图作为附件一起发送。
    """
    msg = MIMEMultipart()
    msg["From"] = smtp_user
    msg["To"] = recipient
    msg["Subject"] = subject

    msg.attach(MIMEText(body, "plain", "utf-8"))

    paths = [screenshot_path] if isinstance(screenshot_path, str) else screenshot_path
    for p in paths:
        path = Path(p)
        if path.exists():
            with open(path, "rb") as f:
                img = MIMEImage(f.read())
                img.add_header("Content-Disposition", "attachment", filename=path.name)
                msg.attach(img)

    try:
        with smtplib.SMTP_SSL("smtp.qq.com", 465) as server:
            server.login(smtp_user, smtp_password)
            server.sendmail(smtp_user, recipient, msg.as_string())
        return True
    except Exception as e:
        raise RuntimeError(f"邮件发送失败: {e}")
