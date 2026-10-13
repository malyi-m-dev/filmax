"""Сводка Macrobenchmark: время кадра и холодный старт по сценариям — таблицей Markdown.

Запуск: py scripts/bench-summary.py <benchmarkData.json> [<benchmarkData.json «до»>]
Со вторым файлом рядом с каждым значением печатается изменение относительно «до».
"""
import json
import sys

METRICS = [
    ("frameDurationCpuMs", "кадр CPU, мс"),
    ("frameOverrunMs", "перерасход кадра, мс"),
    ("timeToInitialDisplayMs", "старт, мс"),
]
PERCENTILES = ["P50", "P90", "P99"]


def load(path):
    with open(path, encoding="utf-8") as source:
        data = json.load(source)
    results = {}
    for benchmark in data["benchmarks"]:
        name = f"{benchmark['name']}"
        for metric, _ in METRICS:
            values = benchmark.get("sampledMetrics", {}).get(metric) or benchmark.get("metrics", {}).get(metric)
            if values:
                results[(name, metric)] = values
    return results


def cell(values, key, before):
    value = values.get(key) if "P50" in key or "P9" in key else values.get("median")
    if value is None:
        return "—"
    if before is None:
        return f"{value:.1f}"
    old = before.get(key) if "P50" in key or "P9" in key else before.get("median")
    if not old:
        return f"{value:.1f}"
    change = (value - old) / old * 100
    return f"{value:.1f} ({change:+.0f}%)"


def main():
    after = load(sys.argv[1])
    before = load(sys.argv[2]) if len(sys.argv) > 2 else {}
    print("| сценарий | метрика | P50 | P90 | P99 |")
    print("|---|---|---|---|---|")
    for (name, metric), values in sorted(after.items()):
        label = dict(METRICS)[metric]
        previous = before.get((name, metric))
        if metric == "timeToInitialDisplayMs":
            row = [cell(values, "median", previous), "", ""]
        else:
            row = [cell(values, percentile, previous) for percentile in PERCENTILES]
        print(f"| {name} | {label} | " + " | ".join(row) + " |")


if __name__ == "__main__":
    main()
