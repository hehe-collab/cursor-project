#!/usr/bin/env python3
"""
短剧出海后台 - 解析导出的 xlsx 数据

用法：
  python parse_export.py <xlsx 文件路径>
  python parse_export.py                    # 解析 data/ 目录下最新的 xlsx
  python parse_export.py -o data/xxx.csv    # 指定输出路径
  python parse_export.py --profile 周投放数据  # 按数据类型规则解析

默认输出到 data/statistics.csv（固定命名，便于分析时自动识别）
"""
import argparse
import re
import sys
from pathlib import Path

import pandas as pd

# 固定输出文件名，分析时优先读取此文件
DEFAULT_CSV_NAME = "statistics.csv"

# 默认数据类型（不指定 --profile 时使用）
DEFAULT_PROFILE = "周投放数据"


def _apply_profile_周投放数据(df: pd.DataFrame) -> pd.DataFrame:
    """
    周投放数据规则：三投放组 ABC，印尼+泰国
    - 组别：推广名称含 A-/B-/C- → A组/B组/C组，不含则剔除
    - 国家：剧名含 -IDN/-THAI → 印尼/泰国，不含则剔除（大小写不敏感）
    - 剧名（纯）：去掉 -IDN、-THAI 后缀
    """
    if df.empty:
        return df
    # 兼容列名
    promo_col = None
    for c in df.columns:
        if "推广" in str(c) and "名称" in str(c):
            promo_col = c
            break
    drama_col = None
    for c in df.columns:
        if str(c).strip() == "剧名":
            drama_col = c
            break
    if not promo_col or not drama_col:
        print("警告：未找到「推广名称」或「剧名」列，跳过规则处理", file=sys.stderr)
        return df

    # 组别
    def _group(s):
        if pd.isna(s):
            return ""
        s = str(s).strip()
        if "A-" in s:
            return "A组"
        if "B-" in s:
            return "B组"
        if "C-" in s:
            return "C组"
        return ""

    df["组别"] = df[promo_col].apply(_group)

    # 国家：-IDN/-印尼 → 印尼，-THAI/-泰国 → 泰国（大小写不敏感）
    def _country(s):
        if pd.isna(s):
            return ""
        s = str(s)
        su = s.upper()
        if "-IDN" in su or "-印尼" in s:
            return "印尼"
        if "-THAI" in su or "-泰国" in s:
            return "泰国"
        return ""

    df["国家"] = df[drama_col].apply(_country)

    # 剧名（纯）：去掉 -IDN/-印尼、-THAI/-泰国 后缀（大小写不敏感）
    def _pure_name(s):
        if pd.isna(s):
            return ""
        s = str(s).strip()
        s = re.sub(r"[-_]?(IDN|idn|印尼)$", "", s)
        s = re.sub(r"[-_]?(THAI|thai|泰国)$", "", s)
        return s.strip().rstrip("-").rstrip("_")

    df["剧名（纯）"] = df[drama_col].apply(_pure_name)

    # 过滤：组别和国家都有效的行
    before = len(df)
    df = df[(df["组别"] != "") & (df["国家"] != "")].copy()
    dropped = before - len(df)
    if dropped > 0:
        print(f"已剔除 {dropped} 行（组别或国家不匹配）")

    return df


PROFILE_APPLIERS = {
    "周投放数据": _apply_profile_周投放数据,
}


def parse_xlsx(file_path: Path) -> pd.DataFrame:
    """解析 xlsx 文件，返回 DataFrame"""
    df = pd.read_excel(file_path, engine="openpyxl")
    return df


def main():
    parser = argparse.ArgumentParser(description="解析短剧出海后台导出的 xlsx 数据")
    parser.add_argument(
        "xlsx_path",
        nargs="?",
        default=None,
        help="xlsx 文件路径。不指定时，解析 data/ 目录下最新的 xlsx",
    )
    parser.add_argument(
        "-o", "--output",
        default=None,
        help=f"输出 CSV 路径。不指定时，输出到 data/{DEFAULT_CSV_NAME}（固定命名）",
    )
    parser.add_argument(
        "--profile",
        default=DEFAULT_PROFILE,
        choices=list(PROFILE_APPLIERS.keys()),
        help=f"数据类型规则。默认: {DEFAULT_PROFILE}",
    )
    args = parser.parse_args()

    base_dir = Path(__file__).parent.parent
    data_dir = base_dir / "data"
    data_dir.mkdir(exist_ok=True)

    if args.xlsx_path:
        xlsx_path = Path(args.xlsx_path).expanduser().resolve()
        if not xlsx_path.exists():
            print(f"错误：文件不存在 {xlsx_path}", file=sys.stderr)
            sys.exit(1)
    else:
        # 使用 data/ 下最新的 xlsx
        xlsx_files = sorted(data_dir.glob("*.xlsx"), key=lambda p: p.stat().st_mtime, reverse=True)
        if not xlsx_files:
            print("错误：未指定文件，且 data/ 目录下无 xlsx 文件", file=sys.stderr)
            print("用法：python parse_export.py <xlsx 文件路径>", file=sys.stderr)
            sys.exit(1)
        xlsx_path = xlsx_files[0]
        print(f"使用最新文件: {xlsx_path.name}")

    df = parse_xlsx(xlsx_path)
    print(f"解析到 {len(df)} 行 x {len(df.columns)} 列")
    print(f"列名: {list(df.columns)}")

    # 按数据类型规则处理
    applier = PROFILE_APPLIERS.get(args.profile)
    if applier:
        df = applier(df)
        print(f"应用规则「{args.profile}」后: {len(df)} 行")

    if args.output:
        csv_path = Path(args.output).expanduser().resolve()
    else:
        csv_path = data_dir / DEFAULT_CSV_NAME

    df.to_csv(csv_path, index=False, encoding="utf-8-sig")
    print(f"已保存 CSV: {csv_path}")


if __name__ == "__main__":
    main()
