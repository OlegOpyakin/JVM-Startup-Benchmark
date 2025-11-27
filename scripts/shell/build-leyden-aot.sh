#!/bin/bash

set -e

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

