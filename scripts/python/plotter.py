import os
import matplotlib.pyplot as plt
# Configuration for outlier filtering
# We now use a quantile-based filter: keep data up to this latency quantile.
OUTLIER_QUANTILE = 0.999  # keep 99.9% of lowest latencies, drop top 0.1%
USE_OUTLIER_FILTER = True  # Set to False to disable filtering


def filter_outliers(df, quantile=OUTLIER_QUANTILE):
    """Filter out extreme latency outliers based on a quantile threshold."""
    if not USE_OUTLIER_FILTER or df.empty:
        return df
    
    cutoff_ns = df['latency_ns'].quantile(quantile)
    # If we can't compute a cutoff (e.g., single row), skip filtering
    if cutoff_ns is None:
        return df
    
    filtered_df = df[df['latency_ns'] <= cutoff_ns].copy()
    removed = len(df) - len(filtered_df)
    if removed > 0:
        cutoff_us = cutoff_ns / 1000.0
        print(f"  Filtered {removed} outliers (>~{cutoff_us:.1f} μs, above {quantile*100:.2f}th percentile) for clearer visualization")
    
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
        title += f' [top {(1-OUTLIER_QUANTILE)*100:.2f}% outliers filtered]'
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
            title += f' [top {(1-OUTLIER_QUANTILE)*100:.2f}% outliers filtered]'
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
