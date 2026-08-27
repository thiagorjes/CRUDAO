"""
Verifica que cada TASK-XX.Y citada no documento consolidado de tasks tem um
arquivo individual correspondente em docs/tasks/[feature]/.
Uso: check_task_files.py --artifact <feature-tasks.md>
Saida: erros em stderr, exit 1 se algum arquivo individual estiver ausente.
"""

import re
import sys
from pathlib import Path

TASK_ID_RE = re.compile(r"TASK-\d{2}\.\d{1,2}")


def main():
    if len(sys.argv) < 3 or sys.argv[1] != "--artifact":
        print("Uso: check_task_files.py --artifact <feature-tasks.md>", file=sys.stderr)
        sys.exit(2)

    tasks_doc = Path(sys.argv[2])
    if not tasks_doc.exists():
        print(f"ERRO: '{tasks_doc}' não encontrado.", file=sys.stderr)
        sys.exit(2)

    content = tasks_doc.read_text(encoding="utf-8")
    task_ids = sorted(set(TASK_ID_RE.findall(content)))

    feature_dir = tasks_doc.parent / tasks_doc.stem.replace("-tasks", "")
    existing = {p.name for p in feature_dir.glob("TASK-*.md")} if feature_dir.exists() else set()

    errors = []
    for task_id in task_ids:
        if not any(name.startswith(f"{task_id}-") or name == f"{task_id}.md" for name in existing):
            errors.append(f"ERRO: arquivo individual ausente para '{task_id}' em '{feature_dir}'.")

    for msg in errors:
        print(msg, file=sys.stderr)

    sys.exit(0 if not errors else 1)


if __name__ == "__main__":
    main()
