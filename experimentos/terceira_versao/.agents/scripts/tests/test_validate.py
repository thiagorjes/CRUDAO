"""
Suite de testes para .agents/scripts/validate.py (TASK-10.1).
Executar: pytest .agents/scripts/tests/ -v
"""

import importlib.util
import json
import subprocess
import sys
import time
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent.parent
SKILLS_DIR = SCRIPT_DIR.parent / "skills"
VALIDATE_PY = SCRIPT_DIR / "validate.py"

spec = importlib.util.spec_from_file_location("validate", VALIDATE_PY)
validate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validate)


# ---------------------------------------------------------------------------
# Unit: parse_registry / stale detection
# ---------------------------------------------------------------------------


def test_parse_registry_extrai_artefatos_e_status(tmp_path):
    state_md = tmp_path / "state.md"
    state_md.write_text(
        "## Artifact Registry\n\n"
        "| Artefato | v | Status |\n"
        "|---|---|---|\n"
        "| docs/prd/x-prd.md | 1.0 | ok |\n"
        "| docs/techspec/x-techspec.md | 1.0 | stale:prd@1.1 |\n\n"
        "## Outra secao\n"
        "| docs/tasks/x-tasks.md | 1.0 | ok |\n",
        encoding="utf-8",
    )
    registry = validate.parse_registry(state_md)
    assert registry["docs/prd/x-prd.md"] == "ok"
    assert registry["docs/techspec/x-techspec.md"] == "stale:prd@1.1"
    # linha fora da secao Artifact Registry nao deve ser capturada
    assert "docs/tasks/x-tasks.md" not in registry


def test_parse_registry_arquivo_ausente_retorna_vazio(tmp_path):
    assert validate.parse_registry(tmp_path / "nao-existe.md") == {}


def test_mode_input_detecta_stale(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)
    memory = tmp_path / "memory"
    memory.mkdir()
    (memory / "state.md").write_text(
        "## Artifact Registry\n\n"
        "| Artefato | v | Status |\n"
        "|---|---|---|\n"
        "| docs/prd/x-prd.md | 1.0 | stale:prd@1.1 |\n",
        encoding="utf-8",
    )
    docs = tmp_path / "docs" / "prd"
    docs.mkdir(parents=True)
    prd = docs / "x-prd.md"
    prd.write_text("# PRD", encoding="utf-8")

    rules = {
        "modes": {
            "input": {
                "check_registry": True,
                "required_artifacts": ["docs/prd/x-prd.md"],
            }
        }
    }
    errors = validate.mode_input(rules, prd)
    assert any("stale" in e for e in errors)


# ---------------------------------------------------------------------------
# Unit: id_patterns
# ---------------------------------------------------------------------------


def test_check_id_patterns_aceita_id_valido():
    content = "Ver RF-001 e RF-002 no documento."
    errors = validate.check_id_patterns(content, {"RF": r"RF-\d{3}"})
    assert errors == []


def test_check_id_patterns_rejeita_id_invalido():
    content = "Ver RF-1 (formato errado)."
    errors = validate.check_id_patterns(content, {"RF": r"RF-\d{3}"})
    assert len(errors) == 1
    assert "RF-1" in errors[0]


def test_check_id_patterns_aceita_id_com_ponto():
    content = "TASK-01.1 e TASK-12.3 sao validas."
    errors = validate.check_id_patterns(content, {"TASK": r"TASK-\d{2}\.\d"})
    assert errors == []


# ---------------------------------------------------------------------------
# Unit: gherkin detection
# ---------------------------------------------------------------------------


def test_check_gherkin_detecta_ausencia():
    content = "RF-001 — Login\nSem criterio de aceite aqui."
    errors = validate.check_gherkin(content, ["RF"], {"RF": r"RF-\d{3}"})
    assert len(errors) == 1
    assert "RF-001" in errors[0]


def test_check_gherkin_aceita_presenca_pt_br():
    content = (
        "RF-001 — Login\n**Dado que** o usuario esta na tela\n**Quando** ele clica"
    )
    errors = validate.check_gherkin(content, ["RF"], {"RF": r"RF-\d{3}"})
    assert errors == []


def test_check_gherkin_aceita_presenca_en_us():
    content = "RF-002 — Logout\nGiven the user is logged in\nWhen they click logout"
    errors = validate.check_gherkin(content, ["RF"], {"RF": r"RF-\d{3}"})
    assert errors == []


# ---------------------------------------------------------------------------
# Unit: placeholder detection
# ---------------------------------------------------------------------------


def test_check_placeholders_detecta_nao_substituido():
    content = "Projeto: {{PROJECT_NAME}} criado em {{DATE}}."
    errors = validate.check_placeholders(content)
    assert len(errors) == 2


def test_check_placeholders_conteudo_limpo():
    content = "Projeto: SSPDD criado em 2026-08-22."
    assert validate.check_placeholders(content) == []


# ---------------------------------------------------------------------------
# Unit: custom_steps (subprocess mockado)
# ---------------------------------------------------------------------------


class _FakeCompletedProcess:
    def __init__(self, returncode, stderr=""):
        self.returncode = returncode
        self.stderr = stderr


def test_run_custom_steps_sucesso(monkeypatch):
    def fake_run(*args, **kwargs):
        return _FakeCompletedProcess(0)

    monkeypatch.setattr(subprocess, "run", fake_run)
    monkeypatch.setattr(validate, "subprocess", subprocess)
    steps = [{"name": "check", "script": "check.py", "args": []}]
    messages = validate.run_custom_steps(steps, "artifact.md", Path("."))
    assert messages == []


def test_run_custom_steps_falha_gera_erro(monkeypatch):
    def fake_run(*args, **kwargs):
        return _FakeCompletedProcess(1, stderr="RF-999 sem task\n")

    monkeypatch.setattr(validate, "subprocess", subprocess)
    monkeypatch.setattr(subprocess, "run", fake_run)
    steps = [
        {
            "name": "check_rf_coverage",
            "script": "check.py",
            "args": [],
            "on_failure": "error",
        }
    ]
    messages = validate.run_custom_steps(steps, "artifact.md", Path("."))
    assert any("ERRO" in m and "RF-999" in m for m in messages)


def test_run_custom_steps_on_failure_warning(monkeypatch):
    def fake_run(*args, **kwargs):
        return _FakeCompletedProcess(1, stderr="aviso qualquer\n")

    monkeypatch.setattr(validate, "subprocess", subprocess)
    monkeypatch.setattr(subprocess, "run", fake_run)
    steps = [
        {"name": "step", "script": "check.py", "args": [], "on_failure": "warning"}
    ]
    messages = validate.run_custom_steps(steps, "artifact.md", Path("."))
    assert all(m.startswith("AVISO") for m in messages)


# ---------------------------------------------------------------------------
# Integracao: fixtures valid/invalid de cada skill via CLI
# ---------------------------------------------------------------------------

SKILLS_COM_FIXTURE = [
    "prd",
    "techspec",
    "tasks",
    "discovery",
    "spdd-canvas",
    "spdd-sync",
    "guidelines",
    "code-review",
]


def _run_cli(rules: Path, artifact: Path, mode: str = "output"):
    return subprocess.run(
        [
            sys.executable,
            str(VALIDATE_PY),
            "--mode",
            mode,
            "--rules",
            str(rules),
            "--artifact",
            str(artifact),
        ],
        capture_output=True,
        text=True,
        cwd=str(SCRIPT_DIR.parent.parent),
    )


@pytest.mark.parametrize("skill", SKILLS_COM_FIXTURE)
def test_fixture_valida_passa(skill):
    fixtures_dir = SKILLS_DIR / skill / "scripts" / "tests" / "fixtures"
    rules = SKILLS_DIR / skill / "validate-rules.json"
    valid_files = list(fixtures_dir.glob("valid_*.md"))
    assert valid_files, f"nenhuma fixture valid_*.md para {skill}"
    for f in valid_files:
        result = _run_cli(rules, f)
        assert (
            result.returncode == 0
        ), f"{skill}/{f.name} deveria passar: {result.stderr}"


@pytest.mark.parametrize("skill", SKILLS_COM_FIXTURE)
def test_fixture_invalida_falha(skill):
    fixtures_dir = SKILLS_DIR / skill / "scripts" / "tests" / "fixtures"
    rules = SKILLS_DIR / skill / "validate-rules.json"
    invalid_files = list(fixtures_dir.glob("invalid_*.md"))
    assert invalid_files, f"nenhuma fixture invalid_*.md para {skill}"
    for f in invalid_files:
        result = _run_cli(rules, f)
        assert (
            result.returncode == 1
        ), f"{skill}/{f.name} deveria falhar: exit={result.returncode}"
        assert "ERRO" in result.stderr


# ---------------------------------------------------------------------------
# Benchmark: p95 < 5s para arquivo de 500 linhas
# ---------------------------------------------------------------------------


def test_benchmark_arquivo_500_linhas(tmp_path):
    lines = ["# Documento de Teste", ""]
    for i in range(1, 501):
        lines.append(f"Linha {i} de conteudo generico para o benchmark de performance.")
    artifact = tmp_path / "big.md"
    artifact.write_text("\n".join(lines), encoding="utf-8")

    rules_path = tmp_path / "rules.json"
    rules_path.write_text(
        json.dumps(
            {
                "modes": {
                    "output": {
                        "required_sections": ["# Documento de Teste"],
                        "no_empty_placeholders": True,
                    }
                }
            }
        ),
        encoding="utf-8",
    )

    durations = []
    for _ in range(5):
        start = time.perf_counter()
        result = _run_cli(rules_path, artifact)
        durations.append(time.perf_counter() - start)
        assert result.returncode == 0

    durations.sort()
    p95 = durations[-1]
    assert p95 < 5.0, f"p95={p95:.2f}s excede o limite de 5s"
