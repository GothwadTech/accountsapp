"""Check local Markdown file links without third-party dependencies."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
errors = []
for folder in ("core", "ui", "app", "sample", "docs"):
    if not (root / folder).is_dir():
        errors.append(f"Missing scaffold directory: {folder}")
for file in [root / "README.md", *root.glob("docs/*.md"), *root.glob("*/README.md")]:
    for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", file.read_text()):
        if "://" in target or target.startswith("#"):
            continue
        if not (file.parent / target.split("#", 1)[0]).exists():
            errors.append(f"{file.relative_to(root)}: broken link {target}")
if errors:
    raise SystemExit("\n".join(errors))
print("Planning scaffold and local documentation links OK (not an Android build).")
