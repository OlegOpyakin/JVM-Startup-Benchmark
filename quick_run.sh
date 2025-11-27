#!/bin/bash

# Quick script to run benchmarks and generate plots
# Usage: ./quick_run.sh [num_runs]
# Example: ./quick_run.sh 3  (runs benchmark 3 times per JVM)

NUM_RUNS=${1:-1}

echo "Running JVM Benchmarks ($NUM_RUNS runs per JVM)..."
./run_benchmarks.sh $NUM_RUNS

echo ""
echo "Generating plots..."
source venv/bin/activate
python3 plot_results.py ../../results/
deactivate

echo ""
echo "Done! Check results/plots/ for visualizations"
echo "Opening plots directory..."
open results/plots/
