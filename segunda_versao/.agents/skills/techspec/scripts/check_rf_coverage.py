"""
Verifica que todos os RFs do PRD aparecem na Matriz de Rastreabilidade do TechSpec.
Uso: python check_rf_coverage.py --prd <prd.md> [--techspec <techspec.md>]
Exit 0 = cobertura completa | Exit 1 = gaps encontrados
"""

import re
import sys
from pathlib import Path

RF_RE = re.compile(r'\bRF-\d{3}\b')


def extract_rfs(path: Path) -> set[str]:
    if not path.exists():
        return set()
    return set(RF_RE.findall(path.read_text(encoding="utf-8")))


def main():
    args = sys.argv[1:]
    prd_path = None
    techspec_path = None

    i = 0
    while i < len(args):
        if args[i] == "--prd" and i + 1 < len(args):
            prd_path = Path(args[i + 1])
            i += 2
        elif args[i] == "--techspec" and i + 1 < len(args):
            techspec_path = Path(args[i + 1])
            i += 2
        else:
            i += 1

    if not prd_path:
        print("ERRO: --prd é obrigatório", file=sys.stderr)
        sys.exit(2)

    prd_rfs = extract_rfs(prd_path)
    if not prd_rfs:
        sys.exit(0)

    if techspec_path is None:
        # Tentar inferir techspec do mesmo diretório nível acima
        # ex: docs/prd/feat-prd.md → docs/techspec/feat-techspec.md
        stem = prd_path.stem.replace("-prd", "")
        techspec_path = prd_path.parent.parent / "techspec" / f"{stem}-techspec.md"

    techspec_rfs = extract_rfs(techspec_path)

    gaps = prd_rfs - techspec_rfs
    if gaps:
        for rf in sorted(gaps):
            print(f"AVISO: {rf} presente no PRD mas não encontrado na TechSpec", file=sys.stderr)
        sys.exit(1)

    sys.exit(0)


if __name__ == "__main__":
    main()
