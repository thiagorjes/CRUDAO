"""
Valida o campo 'status' do frontmatter de um Decision Record (DR).
Status válidos: accepted, superseded, deprecated.

Uso: python check_dr_status.py <artifact.md>
Exit 0 = status válido | Exit 1 = status inválido ou ausente
"""

import re
import sys
from pathlib import Path

VALID_STATUS = {"accepted", "superseded", "deprecated"}
FRONTMATTER_RE = re.compile(r'^---\n(.*?)\n---', re.DOTALL | re.MULTILINE)


def main():
    if len(sys.argv) < 2:
        print("ERRO: uso: check_dr_status.py <artifact.md>", file=sys.stderr)
        sys.exit(2)

    artifact = Path(sys.argv[1])
    if not artifact.exists():
        print(f"ERRO: artefato '{artifact}' não encontrado.", file=sys.stderr)
        sys.exit(2)

    content = artifact.read_text(encoding="utf-8")
    m = FRONTMATTER_RE.search(content)
    if not m:
        print("ERRO: frontmatter YAML ausente ou malformado", file=sys.stderr)
        sys.exit(1)

    status = None
    for line in m.group(1).splitlines():
        if line.strip().startswith("status:"):
            status = line.split(":", 1)[1].strip()
            break

    if status is None:
        print("ERRO: campo 'status' ausente no frontmatter", file=sys.stderr)
        sys.exit(1)

    if status not in VALID_STATUS:
        print(f"ERRO: status inválido: {status}", file=sys.stderr)
        sys.exit(1)

    sys.exit(0)


if __name__ == "__main__":
    main()
