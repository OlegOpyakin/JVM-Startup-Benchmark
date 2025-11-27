import os

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
