#!/bin/bash

set -e

#
# This file contains configuration and main script 
#

# ============================================
# CONFIGURATION - Set your JVM paths here
# ============================================

# OpenJDK (standard) - using GraalVM as baseline
OPENJDK_PATH="/Users/oleg/Desktop/graalvm-jdk-21.0.9+7.1/Contents/Home/bin/java"

# OpenJDK with Leyden (Mainline build)
OPENJDK_LEYDEN_PATH="/Users/oleg/Desktop/C++/SBER/Mainline_build/jdk/build/macosx-aarch64-server-release/images/jdk/bin/java"

# GraalVM
GRAALVM_PATH="/Users/oleg/Desktop/graalvm-jdk-21.0.9+7.1/Contents/Home/bin/java"

# GraalVM Native Image (compiled binary)
GRAALVM_NATIVE_PATH="/Users/oleg/Desktop/graalvm-jdk-21.0.9+7.1/Contents/Home/bin/native-image"

# ============================================
# END CONFIGURATION
# ============================================

BENCHMARK_CLASS="com.jvmbenchmark.ComplexBenchmark"
BENCHMARK_JAR="target/jvm-benchmark-suite-1.0.0-jar-with-dependencies.jar"
RESULTS_DIR="results"
TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)

# Number of runs (default 1, can be overridden with first argument)
NUM_RUNS=${1:-1}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "======================================"
echo "JVM Benchmark Suite"
echo "======================================"
echo "Timestamp: $TIMESTAMP"
echo "Number of runs: $NUM_RUNS"
echo ""

# Create results directory and source scripts
mkdir -p "$RESULTS_DIR"
source "./scripts/shell/build-benchmark.sh"
source "./scripts/shell/build-leyden-aot.sh"
source "./scripts/shell/run-jvm-benchmark.sh"
source "./scripts/shell/run-leyden-aot-benchmark.sh"
source "./scripts/shell/run-native-image-benchmark.sh"

# Main execution
main() {
    build_benchmark
    
    # Run standard OpenJDK
    run_jvm_benchmark "openjdk" "$OPENJDK_PATH"
    
    # Run OpenJDK Leyden (without AOT)
    run_jvm_benchmark "openjdk_leyden" "$OPENJDK_LEYDEN_PATH"
    
    # Build and run OpenJDK Leyden with AOT
    build_leyden_aot
    run_leyden_aot_benchmark
    
    # Run GraalVM
    run_jvm_benchmark "graalvm" "$GRAALVM_PATH"
    
    # Build and run GraalVM Native Image
    run_native_image_benchmark
    
    echo "======================================"
    echo -e "${GREEN}All benchmarks completed!${NC}"
    echo "Results saved in: $RESULTS_DIR"
    echo "Total runs per JVM: $NUM_RUNS"
    echo ""
    echo "Next steps:"
    echo "  Run: source venv/bin/activate && python3 plot_results.py $RESULTS_DIR"
    echo "  To run more iterations: ./run_benchmarks.sh <num_runs>"
    echo "======================================"
}

main
