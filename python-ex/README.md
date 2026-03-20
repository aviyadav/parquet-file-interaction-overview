# Parquet File Overview

Small Python scripts to:

- generate sample CSV data,
- convert CSV to Parquet,
- inspect Parquet metadata.

This repository is useful for learning basic Parquet file structure and quick local experiments.

## Project Structure

- `createCsvFile.py`: creates a small CSV file named `my_data.csv` with header and sample rows.
- `createCsvFileLarge.py`: appends many random rows to `my_data-2.csv` (large synthetic dataset).
- `convertCsvToParquet.py`: reads `my_data.csv` and writes `my_data.parquet`.
- `fetchParquetMetadata.py`: prints file-level and row-group-level metadata for `my_data.parquet`.
- `main.py`: placeholder entry script.

## Requirements

- Python 3.14+
- Dependencies from `pyproject.toml`:
	- pandas
	- pyarrow
	- polars

## Setup

### Option 1: with `uv` (recommended)

```bash
uv sync
```

### Option 2: with `pip`

```bash
python -m venv .venv
# Windows PowerShell
.\.venv\Scripts\Activate.ps1

python -m pip install --upgrade pip
pip install pandas pyarrow polars
```

## Quick Start

From the project root, run scripts in this order:

```bash
python createCsvFile.py
python convertCsvToParquet.py
python fetchParquetMetadata.py
```

Expected files after running:

- `my_data.csv`
- `my_data.parquet`

## Generate a Larger CSV (Optional)

```bash
python createCsvFileLarge.py
```

This script appends 100,000 random rows to `my_data-2.csv`.

## Notes

- `fetchParquetMetadata.py` expects `my_data.parquet` to exist.
- `createCsvFileLarge.py` opens `my_data-2.csv` in append mode, so running it multiple times increases file size.
