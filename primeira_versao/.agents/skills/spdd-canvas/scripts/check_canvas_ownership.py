"""
Verifica que cada dimensão do REASONS Canvas tem linha de ownership (_Atualizado por:).
Saída: erros em stderr, exit 1 se inválido.
"""

import re
import sys
from pathlib import Path

DIMENSIONS = ["R", "E", "A", "S", "O", "N"]
OWNERSHIP_RE = re.compile(r'_Atualizado por:')
DIM_HEADING_RE = re.compile(r'^##\s+([REASONS])\s+—')


def main():
    if len(sys.argv) < 3 or sys.argv[1] != "--canvas":
        print("Uso: check_canvas_ownership.py --canvas <arquivo>", file=sys.stderr)
        sys.exit(2)

    canvas_path = Path(sys.argv[2])
    if not canvas_path.exists():
        print(f"ERRO: Canvas '{canvas_path}' não encontrado.", file=sys.stderr)
        sys.exit(2)

    content = canvas_path.read_text(encoding="utf-8")
    lines = content.splitlines()

    errors = []
    current_dim = None
    dim_start = None
    dim_has_ownership = False
    dims_seen = []

    for i, line in enumerate(lines):
        m = DIM_HEADING_RE.match(line)
        if m:
            if current_dim and not dim_has_ownership:
                errors.append(f"ERRO: Dimensão {current_dim} sem linha '_Atualizado por:'")
            current_dim = m.group(1)
            dim_start = i
            dim_has_ownership = False
            dims_seen.append(current_dim)
        elif current_dim and OWNERSHIP_RE.search(line):
            dim_has_ownership = True

    if current_dim and not dim_has_ownership:
        errors.append(f"ERRO: Dimensão {current_dim} sem linha '_Atualizado por:'")

    for msg in errors:
        print(msg, file=sys.stderr)

    sys.exit(0 if not errors else 1)


if __name__ == "__main__":
    main()
