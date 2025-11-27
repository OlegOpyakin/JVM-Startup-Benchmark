#!/usr/bin/env python3

import os
import sys
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from pathlib import Path

# Configuration for outlier filtering
MAX_LATENCY_US = 100  # Filter out latencies above this (in microseconds)
USE_OUTLIER_FILTER = True  # Set to False to disable filtering

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

def filter_outliers(df, max_latency_us=MAX_LATENCY_US):
    """Filter out extreme latency outliers for better visualization."""
    if not USE_OUTLIER_FILTER:
        return df
    
    # Convert to microseconds and filter
    max_latency_ns = max_latency_us * 1000
    filtered_df = df[df['latency_ns'] <= max_latency_ns].copy()
    
    removed = len(df) - len(filtered_df)
    if removed > 0:
        print(f"  Filtered {removed} outliers (>{max_latency_us} μs) for clearer visualization")
    
    return filtered_df

def plot_latency_vs_time(results, output_dir):
    """Plot latency vs time for all JVMs on the same graph with error bands."""
    plt.figure(figsize=(14, 8))
    
    colors = {
        'openjdk': '#1f77b4',
        'openjdk_leyden': '#ff7f0e',
        'openjdk_leyden_aot': '#2ca02c',
        'graalvm': '#d62728',
        'graalvm_native': '#9467bd'
    }
    
    for jvm_name, df in results.items():
        color = colors.get(jvm_name, None)
        
        # Filter outliers for better visualization
        df_filtered = filter_outliers(df)
        
        # Group by iteration to calculate mean and std across runs
        if 'run' in df_filtered.columns:
            grouped = df_filtered.groupby('iteration')['latency_ns'].agg(['mean', 'std', 'count'])
            latency_us_mean = grouped['mean'] / 1000
            latency_us_std = grouped['std'] / 1000
            
            # Get average timestamp for each iteration
            time_s = df_filtered.groupby('iteration')['timestamp_ms'].mean() / 1000
            
            plt.plot(time_s, latency_us_mean, label=jvm_name.replace('_', ' ').title(), 
                     linewidth=2, alpha=0.8, color=color)
            
            # Add error band if we have multiple runs
            if grouped['count'].iloc[0] > 1:
                plt.fill_between(time_s, 
                                latency_us_mean - latency_us_std, 
                                latency_us_mean + latency_us_std,
                                alpha=0.2, color=color)
        else:
            # Old format, single run
            latency_us = df_filtered['latency_ns'] / 1000
            time_s = df_filtered['timestamp_ms'] / 1000
            plt.plot(time_s, latency_us, label=jvm_name.replace('_', ' ').title(), 
                     linewidth=2, alpha=0.7, color=color)
    
    plt.xlabel('Time (seconds)', fontsize=12)
    plt.ylabel('Latency (μs)', fontsize=12)
    title = 'JVM Latency Over Time Comparison (mean ± std)'
    if USE_OUTLIER_FILTER:
        title += f' [outliers >{MAX_LATENCY_US}μs filtered]'
    plt.title(title, fontsize=14, fontweight='bold')
    plt.legend(loc='best', fontsize=10)
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    
    output_file = os.path.join(output_dir, 'latency_vs_time_all.png')
    plt.savefig(output_file, dpi=300)
    print(f"Saved: {output_file}")
    plt.close()

def plot_individual_latencies(results, output_dir):
    """Plot individual latency graphs for each JVM."""
    for jvm_name, df in results.items():
        plt.figure(figsize=(12, 6))
        
        # Filter outliers
        df_filtered = filter_outliers(df)
        
        latency_us = df_filtered['latency_ns'] / 1000
        time_s = df_filtered['timestamp_ms'] / 1000
        
        # Distinguish warmup and peak phases
        warmup = df_filtered[df_filtered['phase'] == 'warmup']
        peak = df_filtered[df_filtered['phase'] == 'peak']
        
        if not warmup.empty:
            plt.plot(warmup['timestamp_ms'] / 1000, warmup['latency_ns'] / 1000, 
                    'o-', label='Warmup Phase', alpha=0.6, markersize=3)
        
        if not peak.empty:
            plt.plot(peak['timestamp_ms'] / 1000, peak['latency_ns'] / 1000, 
                    'o-', label='Peak Phase', alpha=0.6, markersize=3)
        
        plt.xlabel('Time (seconds)', fontsize=12)
        plt.ylabel('Latency (μs)', fontsize=12)
        title = f'{jvm_name.replace("_", " ").title()} - Latency Over Time'
        if USE_OUTLIER_FILTER:
            title += f' [outliers >{MAX_LATENCY_US}μs filtered]'
        plt.title(title, fontsize=14, fontweight='bold')
        plt.legend(loc='best')
        plt.grid(True, alpha=0.3)
        plt.tight_layout()
        
        output_file = os.path.join(output_dir, f'latency_{jvm_name}.png')
        plt.savefig(output_file, dpi=300)
        print(f"Saved: {output_file}")
        plt.close()

def plot_startup_comparison(results, output_dir):
    """Plot bar chart comparing average latency during startup phase with error bars."""
    jvm_names = []
    startup_latencies = []
    startup_stds = []
    
    for jvm_name, df in results.items():
        warmup = df[df['phase'] == 'warmup']
        if not warmup.empty:
            if 'run' in df.columns:
                # Calculate mean across all measurements in all runs
                avg_latency = warmup['latency_ns'].mean() / 1000
                std_latency = warmup['latency_ns'].std() / 1000
            else:
                avg_latency = warmup['latency_ns'].mean() / 1000
                std_latency = 0
            
            jvm_names.append(jvm_name.replace('_', ' ').title())
            startup_latencies.append(avg_latency)
            startup_stds.append(std_latency)
    
    if not jvm_names:
        return
    
    plt.figure(figsize=(12, 6))
    bars = plt.bar(jvm_names, startup_latencies, yerr=startup_stds, 
                   color='skyblue', edgecolor='navy', alpha=0.7,
                   capsize=5, error_kw={'linewidth': 2})
    
    # Add value labels on bars
    for i, bar in enumerate(bars):
        height = bar.get_height()
        if startup_stds[i] > 0:
            label = f'{height:.2f} ± {startup_stds[i]:.2f} μs'
        else:
            label = f'{height:.2f} μs'
        plt.text(bar.get_x() + bar.get_width()/2., height + startup_stds[i],
                label, ha='center', va='bottom', fontsize=9)
    
    plt.xlabel('JVM Implementation', fontsize=12)
    plt.ylabel('Average Latency (μs)', fontsize=12)
    plt.title('Startup Phase - Average Latency Comparison (mean ± std)', fontsize=14, fontweight='bold')
    plt.xticks(rotation=45, ha='right')
    plt.grid(True, axis='y', alpha=0.3)
    plt.tight_layout()
    
    output_file = os.path.join(output_dir, 'startup_comparison.png')
    plt.savefig(output_file, dpi=300)
    print(f"Saved: {output_file}")
    plt.close()

def plot_peak_comparison(results, output_dir):
    """Plot bar chart comparing average latency during peak phase with error bars."""
    jvm_names = []
    peak_latencies = []
    peak_stds = []
    
    for jvm_name, df in results.items():
        peak = df[df['phase'] == 'peak']
        if not peak.empty:
            if 'run' in df.columns:
                avg_latency = peak['latency_ns'].mean() / 1000
                std_latency = peak['latency_ns'].std() / 1000
            else:
                avg_latency = peak['latency_ns'].mean() / 1000
                std_latency = 0
            
            jvm_names.append(jvm_name.replace('_', ' ').title())
            peak_latencies.append(avg_latency)
            peak_stds.append(std_latency)
    
    if not jvm_names:
        return
    
    plt.figure(figsize=(12, 6))
    bars = plt.bar(jvm_names, peak_latencies, yerr=peak_stds,
                   color='lightcoral', edgecolor='darkred', alpha=0.7,
                   capsize=5, error_kw={'linewidth': 2})
    
    # Add value labels on bars
    for i, bar in enumerate(bars):
        height = bar.get_height()
        if peak_stds[i] > 0:
            label = f'{height:.2f} ± {peak_stds[i]:.2f} μs'
        else:
            label = f'{height:.2f} μs'
        plt.text(bar.get_x() + bar.get_width()/2., height + peak_stds[i],
                label, ha='center', va='bottom', fontsize=9)
    
    plt.xlabel('JVM Implementation', fontsize=12)
    plt.ylabel('Average Latency (μs)', fontsize=12)
    plt.title('Peak Performance Phase - Average Latency Comparison (mean ± std)', fontsize=14, fontweight='bold')
    plt.xticks(rotation=45, ha='right')
    plt.grid(True, axis='y', alpha=0.3)
    plt.tight_layout()
    
    output_file = os.path.join(output_dir, 'peak_comparison.png')
    plt.savefig(output_file, dpi=300)
    print(f"Saved: {output_file}")
    plt.close()

def plot_total_execution_time(times, output_dir):
    """Plot total execution time comparison with error bars."""
    if not times:
        return
    
    jvm_names = [name.replace('_', ' ').title() for name in times.keys()]
    exec_times = [times[name]['mean'] / 1000 for name in times.keys()]
    exec_stds = [times[name]['std'] / 1000 for name in times.keys()]
    
    plt.figure(figsize=(12, 6))
    bars = plt.bar(jvm_names, exec_times, yerr=exec_stds,
                   color='lightgreen', edgecolor='darkgreen', alpha=0.7,
                   capsize=5, error_kw={'linewidth': 2})
    
    # Add value labels on bars
    for i, bar in enumerate(bars):
        height = bar.get_height()
        if exec_stds[i] > 0:
            label = f'{height:.3f} ± {exec_stds[i]:.3f}s'
        else:
            label = f'{height:.3f}s'
        plt.text(bar.get_x() + bar.get_width()/2., height + exec_stds[i],
                label, ha='center', va='bottom', fontsize=9)
    
    plt.xlabel('JVM Implementation', fontsize=12)
    plt.ylabel('Total Execution Time (seconds)', fontsize=12)
    plt.title('Total Execution Time Comparison (mean ± std)', fontsize=14, fontweight='bold')
    plt.xticks(rotation=45, ha='right')
    plt.grid(True, axis='y', alpha=0.3)
    plt.tight_layout()
    
    output_file = os.path.join(output_dir, 'total_execution_time.png')
    plt.savefig(output_file, dpi=300)
    print(f"Saved: {output_file}")
    plt.close()

def generate_summary_report(results, times, output_dir):
    """Generate a text summary report with statistics."""
    report_file = os.path.join(output_dir, 'benchmark_summary.txt')
    
    with open(report_file, 'w') as f:
        f.write("=" * 60 + "\n")
        f.write("JVM BENCHMARK SUMMARY REPORT\n")
        f.write("=" * 60 + "\n\n")
        
        for jvm_name, df in results.items():
            f.write(f"\n{jvm_name.upper()}\n")
            f.write("-" * 60 + "\n")
            
            # Check if multiple runs
            if 'run' in df.columns:
                num_runs = df['run'].nunique()
                f.write(f"Number of runs: {num_runs}\n\n")
            
            warmup = df[df['phase'] == 'warmup']
            peak = df[df['phase'] == 'peak']
            
            if not warmup.empty:
                f.write(f"Warmup Phase:\n")
                avg = warmup['latency_ns'].mean() / 1000
                std = warmup['latency_ns'].std() / 1000
                f.write(f"  Average Latency: {avg:.2f} ± {std:.2f} μs\n")
                f.write(f"  Median Latency:  {warmup['latency_ns'].median() / 1000:.2f} μs\n")
                f.write(f"  P99 Latency:     {warmup['latency_ns'].quantile(0.99) / 1000:.2f} μs\n")
            
            if not peak.empty:
                f.write(f"\nPeak Performance Phase:\n")
                avg = peak['latency_ns'].mean() / 1000
                std = peak['latency_ns'].std() / 1000
                f.write(f"  Average Latency: {avg:.2f} ± {std:.2f} μs\n")
                f.write(f"  Median Latency:  {peak['latency_ns'].median() / 1000:.2f} μs\n")
                f.write(f"  P99 Latency:     {peak['latency_ns'].quantile(0.99) / 1000:.2f} μs\n")
            
            if jvm_name in times:
                f.write(f"\nTotal Execution Time: {times[jvm_name]['mean'] / 1000:.3f} ± {times[jvm_name]['std'] / 1000:.3f}s\n")
        
        f.write("\n" + "=" * 60 + "\n")
    
    print(f"Saved: {report_file}")

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 plot_results.py <results_directory>")
        sys.exit(1)
    
    results_dir = sys.argv[1]
    
    if not os.path.isdir(results_dir):
        print(f"Error: {results_dir} is not a valid directory")
        sys.exit(1)
    
    print(f"Reading benchmark results from: {results_dir}")
    print("=" * 60)
    
    # Read data
    results = read_benchmark_results(results_dir)
    times = get_total_times(results_dir)
    
    if not results:
        print("No valid benchmark results found!")
        sys.exit(1)
    
    # Create plots directory
    plots_dir = os.path.join(results_dir, "plots")
    os.makedirs(plots_dir, exist_ok=True)
    
    print("\nGenerating plots...")
    print("=" * 60)
    
    # Generate all plots
    plot_latency_vs_time(results, plots_dir)
    plot_individual_latencies(results, plots_dir)
    plot_startup_comparison(results, plots_dir)
    plot_peak_comparison(results, plots_dir)
    plot_total_execution_time(times, plots_dir)
    
    # Generate summary report
    generate_summary_report(results, times, plots_dir)
    
    print("\n" + "=" * 60)
    print(f"All plots saved to: {plots_dir}")
    print("=" * 60)

if __name__ == "__main__":
    main()
