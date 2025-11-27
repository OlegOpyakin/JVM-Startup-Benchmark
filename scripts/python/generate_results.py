import os
import sys

from report_generator import generate_summary_report
from parser import read_benchmark_results, get_total_times
from plotter import plot_latency_vs_time, plot_individual_latencies, plot_startup_comparison, plot_peak_comparison, plot_total_execution_time

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
