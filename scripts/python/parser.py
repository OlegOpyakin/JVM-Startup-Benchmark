import os
import sys
import glob
import pandas as pd
import numpy as np

def read_benchmark_results(results_dir):
    """Read all CSV files from the results directory."""
    csv_files = glob.glob(os.path.join(results_dir, "*.csv"))
    
    if not csv_files:
        print(f"No CSV files found in {results_dir}")
        sys.exit(1)
    
    results = {}
    for csv_file in csv_files:
        filename = os.path.basename(csv_file)
        # Extract JVM name from filename
        jvm_name = filename.replace('.csv', '')
        
        # Skip training files
        if 'training' in jvm_name.lower():
            continue
        
        try:
            df = pd.read_csv(csv_file)
            if not df.empty:
                # Sort by timestamp only to ensure proper time ordering
                df = df.sort_values(by='timestamp_ms').reset_index(drop=True)
                
                # Check if 'run' column exists (new format)
                if 'run' in df.columns:
                    num_runs = df['run'].nunique()
                    print(f"Loaded {len(df)} measurements from {filename} ({num_runs} runs)")
                else:
                    print(f"Loaded {len(df)} measurements from {filename} (1 run)")
                results[jvm_name] = df
        except Exception as e:
            print(f"Error reading {csv_file}: {e}")
    
    return results


def get_total_times(results_dir):
    """Read total execution times from time files."""
    time_files = glob.glob(os.path.join(results_dir, "*_times.txt"))
    
    times = {}
    for time_file in time_files:
        filename = os.path.basename(time_file)
        jvm_name = filename.replace("_times.txt", "")
        
        try:
            with open(time_file, 'r') as f:
                time_values = [int(line.strip()) for line in f if line.strip()]
                if time_values:
                    # Store mean and std
                    times[jvm_name] = {
                        'mean': np.mean(time_values),
                        'std': np.std(time_values),
                        'values': time_values
                    }
        except Exception as e:
            print(f"Error reading {time_file}: {e}")
    
    return times