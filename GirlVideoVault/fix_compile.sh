#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"
python - <<'PY'
from pathlib import Path
p = Path('app/src/main/java/com/linnan/girlvideos/MainActivity.kt')
s = p.read_text()
s = s.replace('singleLine = true', 'setSingleLine(true)')
p.write_text(s)
print('MainActivity.kt 修正完了')
PY
