#!/bin/bash

set -e

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
