#!/bin/bash

# Quick script to run benchmarks and generate plots
# Usage: ./quick_run.sh [num_runs]
# Example: ./quick_run.sh 3  (runs benchmark 3 times per JVM)

NUM_RUNS=${1:-1}

echo "Running JVM Benchmarks ($NUM_RUNS runs per JVM)..."
./scripts/shell/run-benchmarks.sh $NUM_RUNS

echo ""
echo "Generating plots..."
python3 -m venv my_env
pip install matplotlib pandas numpy
source my_env/bin/activate
python3 scripts/python/generate_results.py results/
deactivate

echo ""
echo "Done! Check results/plots/ for visualizations"
echo "Opening plots directory..."
open results/plots/
