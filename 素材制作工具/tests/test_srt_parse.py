"""简易自检：python tests/test_srt_parse.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from srt_parse import cues_to_blocks, parse_srt_file  # noqa: E402


def main() -> None:
    sample = ROOT / "tests" / "sample.id.srt"
    cues = parse_srt_file(sample)
    assert len(cues) >= 2, cues
    blocks = cues_to_blocks(cues, merge_gap_sec=2.0)
    assert blocks, blocks
    print("srt_parse ok:", len(cues), "cues,", len(blocks), "blocks")


if __name__ == "__main__":
    main()
