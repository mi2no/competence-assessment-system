# Competence Assessment System

Early experiments with building an ICT competence graph from ESCO.

## Data

`nodes.csv` and `edges.csv` contain a small ICT-related subset of
**ESCO v1.2.1** (English and Polish), extracted with `extract.py`.

> This service uses the ESCO classification of the European Commission.

Source: https://esco.ec.europa.eu. The data has been filtered and modified.
It is not an official ESCO product.

## Reproducing

1. Download ESCO v1.2.1 (classification, CSV, `en` and `pl`) from
   https://esco.ec.europa.eu/en/use-esco/download
2. Extract the CSV files into `esco/`
3. Run `pip install pandas` and then `python extract.py`