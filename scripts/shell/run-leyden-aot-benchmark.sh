#!/bin/bash

set -e

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