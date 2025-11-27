#!/bin/bash

set -e

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