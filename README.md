# Competence Assessment System

Work-in-progress system for assessing ICT competences based on submitted
task solutions. Competences come from an ESCO-based graph; uploaded
solutions (artifacts) are analysed by a local LLM.

## Project structure

| Path                       | Description                                                       |
|----------------------------|-------------------------------------------------------------------|
| `ESCO/`                    | ESCO subset extraction scripts and the resulting CSV files        |
| `backend/getting-started/` | WildFly (Jakarta EE) backend – tasks, artifact upload & analysis  |
| `backend/test-client/`     | Simple test client for the backend                                |
| `frontend/`                | React + TypeScript + Vite frontend                                |
| `db.sql`                   | Database schema                                                   |

## Tech stack

- **Backend:** WildFly, Jakarta EE, Hibernate, LangChain4j
- **LLM:** [Ollama](https://ollama.com) running locally (`qwen3:4b` by default,
  configured in `microprofile-config.properties`)
- **Frontend:** React, React Router, Vite, [Shiki](https://shiki.style) for
  code highlighting

## Data

`ESCO/nodes.csv` and `ESCO/edges.csv` contain a small ICT-related subset of
**ESCO v1.2.1** (English and Polish), extracted with `ESCO/extract.py`.

> This service uses the ESCO classification of the European Commission.

Source: https://esco.ec.europa.eu. The data has been filtered and modified.
It is not an official ESCO product.

### Reproducing

1. Download ESCO v1.2.1 (classification, CSV, `en` and `pl`) from
   https://esco.ec.europa.eu/en/use-esco/download
2. Extract the CSV files into `ESCO/`
3. Run `pip install pandas` and then `python extract.py` inside `ESCO/`

## Third-party assets

- **File icons** in `frontend/src/assets/icons/` come from
  [Catppuccin Icons for VSCode](https://github.com/catppuccin/vscode-icons)
  (MIT License, © Catppuccin, © thang-nm). The license is included in
  `frontend/src/assets/icons/LICENSE.txt`.
- **Code highlighting themes** are the
  [Catppuccin](https://github.com/catppuccin/catppuccin) themes bundled with Shiki.
