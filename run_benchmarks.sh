#!/bin/bash

set -e

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

# Create results directory
mkdir -p "$RESULTS_DIR"

# Build with Maven
build_benchmark() {
    echo -e "${YELLOW}Building benchmark with Maven...${NC}"
    mvn clean package -q
    
    if [ -f "$BENCHMARK_JAR" ]; then
        echo -e "${GREEN}Build successful: $BENCHMARK_JAR${NC}"
    else
        echo -e "${RED}Build failed: JAR not found${NC}"
        exit 1
    fi
    echo ""
}

# Run benchmark with a specific JVM
run_jvm_benchmark() {
    local jvm_name=$1
    local jvm_path=$2
    local output_file="$RESULTS_DIR/${jvm_name}.csv"
    local time_file="$RESULTS_DIR/${jvm_name}_times.txt"
    
    if [ ! -f "$jvm_path" ]; then
        echo -e "${RED}Skipping $jvm_name: JVM not found at $jvm_path${NC}"
        echo -e "${YELLOW}Please update the path in the configuration section${NC}"
        echo ""
        return
    fi
    
    echo -e "${GREEN}Running benchmark: $jvm_name (${NUM_RUNS} runs)${NC}"
    echo "JVM Path: $jvm_path"
    echo "Output: $output_file"
    
    for run in $(seq 1 $NUM_RUNS); do
        echo -e "${YELLOW}  Run $run/$NUM_RUNS...${NC}"
        
        # Measure total execution time
        local start_time=$(python3 -c "import time; print(int(time.time() * 1000))")
        
        "$jvm_path" -jar "$BENCHMARK_JAR" "$output_file" "$run" 2>&1 | grep -E "(Benchmark Results|Average Latency|P99 Latency|Total Execution|Improvement)"
        
        local end_time=$(python3 -c "import time; print(int(time.time() * 1000))")
        local total_time=$((end_time - start_time))
        
        echo "$total_time" >> "$time_file"
        echo -e "${GREEN}  Completed run $run in ${total_time}ms${NC}"
    done
    
    echo ""
}

# Build and run GraalVM Native Image
run_native_image_benchmark() {
    local output_file="$RESULTS_DIR/graalvm_native.csv"
    local time_file="$RESULTS_DIR/graalvm_native_times.txt"
    local native_binary="target/jvm-benchmark-native"
    
    if [ ! -f "$GRAALVM_NATIVE_PATH" ]; then
        echo -e "${RED}Skipping GraalVM Native Image: native-image tool not found at $GRAALVM_NATIVE_PATH${NC}"
        echo -e "${YELLOW}Please update the path in the configuration section${NC}"
        echo ""
        return
    fi
    
    echo -e "${GREEN}Building GraalVM Native Image with Maven...${NC}"

    # GraalVM Native Image compilation
    mvn package -Pnative -q 2>&1 | grep -E "(Finished|seconds)" || true
    
    if [ ! -f "$native_binary" ]; then
        echo -e "${RED}Native image build failed or not found at $native_binary${NC}"
        return
    fi
    
    echo -e "${GREEN}Running GraalVM Native Image benchmark (${NUM_RUNS} runs)${NC}"
    echo "Binary: $native_binary"
    echo "Output: $output_file"
    
    for run in $(seq 1 $NUM_RUNS); do
        echo -e "${YELLOW}  Run $run/$NUM_RUNS...${NC}"
        
        # Measure total execution time
        local start_time=$(python3 -c "import time; print(int(time.time() * 1000))")
        
        "$native_binary" "$output_file" "$run" 2>&1 | grep -E "(Benchmark Results|Average Latency|P99 Latency|Total Execution|Improvement)"
        
        local end_time=$(python3 -c "import time; print(int(time.time() * 1000))")
        local total_time=$((end_time - start_time))
        
        echo "$total_time" >> "$time_file"
        echo -e "${GREEN}  Completed run $run in ${total_time}ms${NC}"
    done
    
    echo ""
}

# Build AOT cache for Leyden
build_leyden_aot() {
    local cache_file="app-cds.jsa"
    
    if [ ! -f "$OPENJDK_LEYDEN_PATH" ]; then
        echo -e "${RED}Skipping Leyden AOT build: JVM not found${NC}"
        return
    fi
    
    echo -e "${YELLOW}Building Leyden AOT cache...${NC}"
    # Create training run to generate AOT cache
    "$OPENJDK_LEYDEN_PATH" -XX:AOTMode=record -XX:AOTConfiguration=app -XX:AOTCacheOutput="$cache_file" \
        -jar "$BENCHMARK_JAR" "$RESULTS_DIR/leyden_training.csv" "1" 2>&1 | grep -v "^$"
    
    if [ -f "$cache_file" ]; then
        echo -e "${GREEN}AOT cache built: $cache_file${NC}"
    else
        echo -e "${YELLOW}AOT cache not created (feature may not be available in this build)${NC}"
    fi
    echo ""
}

# Run OpenJDK Leyden with AOT
run_leyden_aot_benchmark() {
    local cache_file="app-cds.jsa"
    local output_file="$RESULTS_DIR/openjdk_leyden_aot.csv"
    local time_file="$RESULTS_DIR/openjdk_leyden_aot_times.txt"
    
    if [ ! -f "$OPENJDK_LEYDEN_PATH" ]; then
        echo -e "${RED}Skipping Leyden AOT: JVM not found${NC}"
        return
    fi
    
    if [ ! -f "$cache_file" ]; then
        echo -e "${YELLOW}Skipping Leyden AOT: cache file not found${NC}"
        return
    fi
    
    echo -e "${GREEN}Running OpenJDK Leyden with AOT (${NUM_RUNS} runs)${NC}"
    echo "Output: $output_file"
    
    for run in $(seq 1 $NUM_RUNS); do
        echo -e "${YELLOW}  Run $run/$NUM_RUNS...${NC}"
        
        local start_time=$(python3 -c "import time; print(int(time.time() * 1000))")
        
        "$OPENJDK_LEYDEN_PATH" -XX:AOTCache="$cache_file" \
            -jar "$BENCHMARK_JAR" "$output_file" "$run" 2>&1 | grep -E "(Benchmark Results|Average Latency|P99 Latency|Total Execution|Improvement)"
        
        local end_time=$(python3 -c "import time; print(int(time.time() * 1000))")
        local total_time=$((end_time - start_time))
        
        echo "$total_time" >> "$time_file"
        echo -e "${GREEN}  Completed run $run in ${total_time}ms${NC}"
    done
    
    echo ""
}

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
