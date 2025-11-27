#!/bin/bash

set -e

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