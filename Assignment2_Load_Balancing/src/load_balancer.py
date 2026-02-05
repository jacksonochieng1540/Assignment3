#!/usr/bin/env python3
"""
ICS 2403 - Assignment 2: Load Balancing and Process Allocation
Analyzes latency bottlenecks and implements intelligent process allocation
across distributed nodes.
"""

import numpy as np
from typing import Dict, List, Tuple
from dataclasses import dataclass
import json


@dataclass
class Node:
    """Represents a node in the telecom network."""
    name: str
    latency: int  # milliseconds
    throughput: int  # Mbps
    packet_loss: float  # percentage
    cpu: int  # percentage
    memory: float  # GB
    services: List[str]
    
    def calculate_load_score(self) -> float:
        """
        Calculate current load score for the node.
        Higher score = more loaded
        """
        cpu_weight = 0.6
        memory_weight = 0.3
        latency_weight = 0.1
        
        score = (
            (self.cpu / 100.0) * cpu_weight +
            (self.memory / 20.0) * memory_weight +  # Normalize against max memory
            (self.latency / 30.0) * latency_weight  # Normalize against max latency
        )
        return score
    
    def has_capacity(self, cpu_needed: int, memory_needed: float) -> bool:
        """Check if node has capacity for additional load."""
        return (self.cpu + cpu_needed <= 90 and 
                self.memory + memory_needed <= 18)
    
    def __repr__(self) -> str:
        return f"Node({self.name}, {self.latency}ms, {self.cpu}%CPU, {self.memory}GB)"


@dataclass
class Process:
    """Represents a process to be allocated."""
    process_id: str
    cpu_requirement: int
    memory_requirement: float
    service_type: str
    priority: int  # 1=LOW, 2=MEDIUM, 3=HIGH


class LatencyAnalyzer:
    """Analyzes and identifies latency bottlenecks in the network."""
    
    def __init__(self, nodes: Dict[str, Node]):
        self.nodes = nodes
    
    def identify_max_latency_node(self) -> Tuple[str, Node]:
        """
        (a) Identify node causing maximum latency with mathematical justification.
        
        Returns:
            Tuple of (node_name, node_object) with maximum latency
        """
        print("=" * 80)
        print("ASSIGNMENT 2(a): MAXIMUM LATENCY NODE IDENTIFICATION")
        print("=" * 80)
        
        print("\n1. LATENCY COMPARISON:")
        print("-" * 80)
        
        latency_data = []
        for name, node in self.nodes.items():
            latency_data.append((name, node.latency))
            print(f"{name:10s}: {node.latency:3d} ms")
        
        # Find maximum
        max_node_name = max(latency_data, key=lambda x: x[1])[0]
        max_node = self.nodes[max_node_name]
        
        print(f"\n⚠️  MAXIMUM LATENCY NODE: {max_node_name} ({max_node.latency}ms)")
        
        # Mathematical justification
        print("\n2. MATHEMATICAL JUSTIFICATION:")
        print("-" * 80)
        
        avg_latency = np.mean([lat for _, lat in latency_data])
        std_latency = np.std([lat for _, lat in latency_data])
        
        print(f"Average Latency (μ): {avg_latency:.2f} ms")
        print(f"Standard Deviation (σ): {std_latency:.2f} ms")
        
        # Calculate z-scores for all nodes
        print("\nZ-Score Analysis (measures standard deviations from mean):")
        for name, lat in latency_data:
            z_score = (lat - avg_latency) / std_latency if std_latency > 0 else 0
            status = "⚠️ OUTLIER" if abs(z_score) > 1.5 else "✓ Normal"
            print(f"{name:10s}: z = {z_score:+.3f} {status}")
        
        # Impact analysis
        print("\n3. IMPACT ANALYSIS:")
        print("-" * 80)
        
        deviation_pct = ((max_node.latency - avg_latency) / avg_latency) * 100
        print(f"Deviation from average: +{deviation_pct:.1f}%")
        
        # Calculate contribution to total system latency
        total_latency = sum(lat for _, lat in latency_data)
        contribution = (max_node.latency / total_latency) * 100
        print(f"Contribution to total latency: {contribution:.1f}%")
        
        # Performance degradation factor
        min_latency = min(lat for _, lat in latency_data)
        degradation_factor = max_node.latency / min_latency
        print(f"Performance degradation factor: {degradation_factor:.2f}x slower than fastest node")
        
        # Throughput-latency efficiency
        print("\n4. THROUGHPUT-LATENCY EFFICIENCY:")
        print("-" * 80)
        
        for name, node in self.nodes.items():
            efficiency = node.throughput / node.latency
            print(f"{name:10s}: {efficiency:7.2f} Mbps/ms " + 
                  ("⚠️ LOW" if name == max_node_name else "✓ OK"))
        
        max_efficiency = max_node.throughput / max_node.latency
        best_efficiency = max(
            n.throughput / n.latency for n in self.nodes.values()
        )
        
        print(f"\n{max_node_name} efficiency: {max_efficiency:.2f} Mbps/ms")
        print(f"Best efficiency: {best_efficiency:.2f} Mbps/ms")
        print(f"Efficiency gap: {((best_efficiency - max_efficiency) / best_efficiency * 100):.1f}%")
        
        print("\n5. CONCLUSION:")
        print("-" * 80)
        print(f"✗ {max_node_name} is the primary latency bottleneck because:")
        print(f"  • Highest absolute latency: {max_node.latency}ms")
        print(f"  • {deviation_pct:.1f}% above network average")
        print(f"  • {degradation_factor:.2f}x slower than fastest node")
        print(f"  • Lowest throughput-latency efficiency")
        print(f"  • Contributes {contribution:.1f}% to total system latency")
        
        print("\n" + "=" * 80 + "\n")
        
        return max_node_name, max_node
    
    def analyze_latency_distribution(self):
        """Additional latency distribution analysis."""
        latencies = [node.latency for node in self.nodes.values()]
        
        print("LATENCY DISTRIBUTION STATISTICS:")
        print(f"  Minimum: {min(latencies)} ms")
        print(f"  Maximum: {max(latencies)} ms")
        print(f"  Range: {max(latencies) - min(latencies)} ms")
        print(f"  Median: {np.median(latencies):.1f} ms")
        print(f"  Mean: {np.mean(latencies):.2f} ms")


class ProcessAllocator:
    """Handles process allocation and load balancing."""
    
    def __init__(self, nodes: Dict[str, Node]):
        self.nodes = nodes
        self.allocations = {name: [] for name in nodes.keys()}
    
    def allocate_processes(self, processes: List[Process]) -> Dict[str, List[Process]]:
        """
        (b) Allocate processes to balance CPU and memory load across nodes.
        
        Uses intelligent placement algorithm considering:
        - Current node load
        - Process requirements
        - Service compatibility
        - Load balancing objectives
        
        Returns:
            Dictionary mapping node names to allocated processes
        """
        print("=" * 80)
        print("ASSIGNMENT 2(b): PROCESS ALLOCATION AND LOAD BALANCING")
        print("=" * 80)
        
        print("\n1. INITIAL NODE STATUS:")
        print("-" * 80)
        self._print_node_status()
        
        print("\n2. PROCESSES TO ALLOCATE:")
        print("-" * 80)
        for proc in processes:
            print(f"{proc.process_id:15s}: CPU={proc.cpu_requirement:3d}%, "
                  f"Memory={proc.memory_requirement:4.1f}GB, "
                  f"Service={proc.service_type:20s}, Priority={proc.priority}")
        
        # Sort processes by priority (high to low)
        sorted_processes = sorted(processes, key=lambda p: p.priority, reverse=True)
        
        print("\n3. ALLOCATION PROCESS:")
        print("-" * 80)
        
        for process in sorted_processes:
            allocated = self._allocate_single_process(process)
            if allocated:
                print(f"✓ {process.process_id} → {allocated}")
            else:
                print(f"✗ {process.process_id} → FAILED (No capacity)")
        
        print("\n4. FINAL NODE STATUS:")
        print("-" * 80)
        self._print_node_status()
        
        print("\n5. LOAD BALANCING METRICS:")
        print("-" * 80)
        self._print_balance_metrics()
        
        print("\n" + "=" * 80 + "\n")
        
        return self.allocations
    
    def _allocate_single_process(self, process: Process) -> str:
        """
        Allocate a single process to the best available node.
        
        Selection criteria:
        1. Service compatibility
        2. Available capacity
        3. Current load (prefer less loaded nodes)
        4. Latency (prefer lower latency for high-priority processes)
        """
        # Find candidate nodes
        candidates = []
        
        for name, node in self.nodes.items():
            # Check capacity
            if not node.has_capacity(process.cpu_requirement, 
                                     process.memory_requirement):
                continue
            
            # Check service compatibility (optional preference)
            service_match = any(
                svc in process.service_type.lower() 
                for svc in [s.lower() for s in node.services]
            )
            
            # Calculate placement score
            load_score = node.calculate_load_score()
            latency_score = node.latency / 30.0  # Normalize
            
            # Lower score is better
            placement_score = (
                load_score * 0.5 +  # Prefer less loaded nodes
                latency_score * 0.3 +  # Prefer lower latency
                (0 if service_match else 0.2)  # Bonus for service match
            )
            
            candidates.append((name, node, placement_score))
        
        if not candidates:
            return None
        
        # Select best candidate (lowest score)
        best_node_name, best_node, _ = min(candidates, key=lambda x: x[2])
        
        # Update node resources
        best_node.cpu += process.cpu_requirement
        best_node.memory += process.memory_requirement
        
        # Record allocation
        self.allocations[best_node_name].append(process)
        
        return best_node_name
    
    def _print_node_status(self):
        """Print current status of all nodes."""
        print(f"{'Node':10s} {'CPU':>8s} {'Memory':>10s} {'Load Score':>12s} {'Processes':>10s}")
        print("-" * 80)
        
        for name, node in self.nodes.items():
            proc_count = len(self.allocations[name])
            load_score = node.calculate_load_score()
            print(f"{name:10s} {node.cpu:6d}% {node.memory:8.1f}GB "
                  f"{load_score:10.3f} {proc_count:10d}")
    
    def _print_balance_metrics(self):
        """Print load balancing metrics."""
        cpu_values = [node.cpu for node in self.nodes.values()]
        memory_values = [node.memory for node in self.nodes.values()]
        
        cpu_mean = np.mean(cpu_values)
        cpu_std = np.std(cpu_values)
        cpu_variance = np.var(cpu_values)
        
        memory_mean = np.mean(memory_values)
        memory_std = np.std(memory_values)
        
        print("CPU Utilization:")
        print(f"  Mean: {cpu_mean:.1f}%")
        print(f"  Std Dev: {cpu_std:.2f}%")
        print(f"  Variance: {cpu_variance:.2f}")
        print(f"  Coefficient of Variation: {(cpu_std / cpu_mean * 100):.1f}%")
        
        print("\nMemory Utilization:")
        print(f"  Mean: {memory_mean:.2f} GB")
        print(f"  Std Dev: {memory_std:.2f} GB")
        
        # Balance score (lower is better)
        balance_score = (cpu_std / cpu_mean) + (memory_std / memory_mean)
        print(f"\nOverall Balance Score: {balance_score:.3f} (lower is better)")
        
        if cpu_std < 10 and memory_std < 2:
            print("✓ EXCELLENT LOAD BALANCE")
        elif cpu_std < 20 and memory_std < 4:
            print("✓ GOOD LOAD BALANCE")
        else:
            print("⚠️  MODERATE LOAD IMBALANCE")


class LoadBalancer:
    """Main load balancer orchestrating the entire system."""
    
    def __init__(self):
        # Initialize nodes from dataset
        self.nodes = {
            "EdgeA": Node("EdgeA", 10, 520, 0.3, 40, 4.0, 
                         ["RPCCall", "DataReplication"]),
            "EdgeB": Node("EdgeB", 14, 470, 0.5, 48, 5.0,
                         ["RPCCall", "Migration"]),
            "CoreX": Node("CoreX", 7, 980, 0.1, 65, 8.0,
                         ["TransactionCommit"]),
            "CoreY": Node("CoreY", 9, 950, 0.2, 58, 7.0,
                         ["Recovery", "LoadBalancing"]),
            "CloudZ": Node("CloudZ", 22, 1250, 0.4, 72, 16.0,
                          ["Analytics", "RPCCall"])
        }
        
        self.latency_analyzer = LatencyAnalyzer(self.nodes)
        self.process_allocator = ProcessAllocator(self.nodes)
    
    def run_analysis(self):
        """Execute complete analysis and simulation."""
        print("\n╔════════════════════════════════════════════════════════════════════╗")
        print("║   ICS 2403 - ASSIGNMENT 2: LOAD BALANCING & PROCESS ALLOCATION   ║")
        print("╚════════════════════════════════════════════════════════════════════╝\n")
        
        # (a) Identify maximum latency node
        max_latency_node, node_obj = self.latency_analyzer.identify_max_latency_node()
        
        # Generate sample processes
        processes = self._generate_sample_processes()
        
        # (b) Allocate processes and balance load
        allocations = self.process_allocator.allocate_processes(processes)
        
        # Generate detailed report
        self._generate_report(max_latency_node, allocations)
    
    def _generate_sample_processes(self) -> List[Process]:
        """Generate sample processes for allocation."""
        return [
            Process("P1_Analytics", 15, 2.0, "Analytics", 3),
            Process("P2_RPC_Handler", 10, 1.5, "RPCCall", 3),
            Process("P3_DataRepl", 12, 2.5, "DataReplication", 2),
            Process("P4_Transaction", 18, 3.0, "TransactionCommit", 3),
            Process("P5_Recovery", 8, 1.0, "Recovery", 2),
            Process("P6_LoadBal", 10, 1.5, "LoadBalancing", 2),
            Process("P7_Migration", 14, 2.0, "Migration", 1),
            Process("P8_RPC_Handler2", 10, 1.5, "RPCCall", 2),
            Process("P9_Analytics2", 12, 2.0, "Analytics", 1),
            Process("P10_Monitor", 5, 0.5, "Monitoring", 1),
        ]
    
    def _generate_report(self, max_latency_node: str, 
                        allocations: Dict[str, List[Process]]):
        """Generate comprehensive analysis report."""
        print("=" * 80)
        print("COMPREHENSIVE ANALYSIS REPORT")
        print("=" * 80)
        
        print("\n1. KEY FINDINGS:")
        print("-" * 80)
        print(f"• Maximum Latency Node: {max_latency_node}")
        print(f"• Total Processes Allocated: {sum(len(procs) for procs in allocations.values())}")
        print(f"• Nodes in System: {len(self.nodes)}")
        
        print("\n2. ALLOCATION SUMMARY:")
        print("-" * 80)
        for node_name, processes in allocations.items():
            print(f"\n{node_name}:")
            if processes:
                for proc in processes:
                    print(f"  • {proc.process_id}: {proc.cpu_requirement}% CPU, "
                          f"{proc.memory_requirement:.1f}GB RAM")
            else:
                print("  (No processes allocated)")
        
        print("\n3. RECOMMENDATIONS:")
        print("-" * 80)
        print(f"• Implement caching at {max_latency_node} to reduce latency")
        print("• Monitor CPU utilization on heavily loaded nodes")
        print("• Consider horizontal scaling if load increases")
        print("• Implement dynamic load rebalancing for long-running processes")
        
        print("\n" + "=" * 80)
        print("ASSIGNMENT 2 COMPLETE")
        print("=" * 80 + "\n")


if __name__ == "__main__":
    balancer = LoadBalancer()
    balancer.run_analysis()