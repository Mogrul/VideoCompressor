from __future__ import annotations

import sqlite3
from pathlib import Path
from typing import Any, Iterable

RESET = "\033[0m"
BOLD  = "\033[1m"

RED   = "\033[31m"
GREEN = "\033[32m"
YEL   = "\033[33m"
BLUE  = "\033[34m"
CYAN  = "\033[36m"

class Job:
    def __init__(
        self,
        row: sqlite3.Row
    ) -> None:
        self.source_path = Path(row["source_path"])
        self.output_path : Path | None = Path(row["output_path"]) if row["output_path"] is not None else None
        self.size : int = row["size"]
        self.mtime_ms : int = row["mtime_ms"]
        self.partial_sha256 = bytes.fromhex(row["partial_sha256"])
        self.status : str = row["status"]
        self.last_error : str | None = row["last_error"]
        self.updated_at : int = row["updated_at"]

def connect(db_path: Path) -> sqlite3.Connection:
    """Establish a connection to the SQLite database."""
    if not db_path.exists():
        raise FileNotFoundError(f"Database file not found: {db_path}")

    url = f"file:{db_path}?mode=ro"
    connection = sqlite3.connect(url, uri=True)
    connection.row_factory = sqlite3.Row
    return connection

def run_query(
    connection: sqlite3.Connection,
    query: str,
    params: Iterable[Any]
) -> list[sqlite3.Row]:
    """Execute a SQL query with the provided parameters."""
    cursor = connection.execute(query, tuple(params))
    rows = cursor.fetchall()
    return rows

def main() -> None:
    connection = connect(Path("compressed.db"))
    query = "SELECT * FROM compress_jobs WHERE status = 'DONE'"

    rows = run_query(connection, query, [])

    compression_ratios : list[float] = []
    mb_saved = 0.0
    for row in rows:
        job = Job(row)

        try:
            output_size = job.output_path.stat().st_size / 1_000_000 # in MB
        except (FileNotFoundError):
            print(f"FILE ({YEL}{job.source_path.name}{RESET}):")
            print(f"        {RED}Not found. Skipping...{RESET}")
            continue

        input_size = job.size / 1_000_000 # in MB

        compression_ratio = output_size / input_size if input_size > 0 else 0
        compression_reduction = (input_size - output_size)

        compression_ratios.append(compression_ratio)
        mb_saved += compression_reduction

        print(f"FILE ({YEL}{job.source_path.name}{RESET}):")
        print(f"        {BOLD}Input Size{RESET}: {CYAN}{input_size:.2f}{RESET} MB")
        print(f"        {BOLD}Output Size{RESET}: {CYAN}{output_size:.2f}{RESET} MB")
        print(f"        {BOLD}Compression Ratio{RESET}: {CYAN}{compression_ratio:.2f}{RESET}")
        print(f"        {BOLD}Size Reduction{RESET}: {CYAN}{compression_reduction:.2f}{RESET} MB")

    avg_ratio = (sum(compression_ratios) / len(compression_ratios)) if compression_ratios else 0.0
    avg_savings_percent = (1 - avg_ratio) * 100


    print(f"\n{BOLD}{GREEN}COMPLETE{RESET}:")
    print(f"    {BOLD}Saved{RESET}: {GREEN}{mb_saved / 1_000:.2f}GB{RESET}")
    print(f"    {BOLD}Average Reduction{RESET}: {GREEN}{avg_savings_percent:.2f}%{RESET}")

if __name__ == "__main__":
    main()