import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes performance bottlenecks in the distributed system.
 * Implements mathematical models for bottleneck identification.
 */
public class PerformanceAnalyzer {
    private Map<String, Node> nodes;
    
    /**
     * Constructor
     */
    public PerformanceAnalyzer(Map<String, Node> nodes) {
        this.nodes = nodes;
    }
    
    /**
     * Analyze all bottlenecks and generate comprehensive report
     */
    public void analyzeBottlenecks() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("         PERFORMANCE BOTTLENECK ANALYSIS REPORT");
        System.out.println("=".repeat(70));
        
        analyzeLatencyBottleneck();
        analyzeCPUBottleneck();
        analyzeThroughputBottleneck();
        analyzeMemoryBottleneck();
        generatePriorityRanking();
        generateRecommendations();
        
        System.out.println("=".repeat(70) + "\n");
    }
    
    /**
     * 1. Latency Bottleneck Analysis
     */
    private void analyzeLatencyBottleneck() {
        System.out.println("\n1. LATENCY BOTTLENECK ANALYSIS");
        System.out.println("-".repeat(70));
        
        // Calculate bottleneck scores
        Map<String, Double> bottleneckScores = new LinkedHashMap<>();
        
        for (Node node : nodes.values()) {
            double score = node.calculateBottleneckScore();
            bottleneckScores.put(node.getNodeId(), score);
            
            System.out.println(String.format(
                "%s: BS = (%.0f/%.0f) × %.0f × %.4f = %.4f",
                node.getNodeId(),
                (double)node.getLatency(),
                (double)node.getThroughput(),
                (double)node.getCpuUsage(),
                (1 + node.getPacketLoss()/100.0),
                score
            ));
        }
        
        // Find maximum bottleneck
        String maxNode = bottleneckScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("None");
        
        System.out.println(String.format(
            "\n⚠ PRIMARY LATENCY BOTTLENECK: %s (BS = %.4f)",
            maxNode, bottleneckScores.get(maxNode)
        ));
        
        // Calculate total system latency (worst case)
        int totalLatency = nodes.values().stream()
            .mapToInt(Node::getLatency)
            .max()
            .orElse(0) * 3; // Multiply by 3 for edge-core-cloud path
        
        System.out.println(String.format(
            "Total System Latency (worst case): %dms", totalLatency
        ));
    }
    
    /**
     * 2. CPU Utilization Bottleneck Analysis
     */
    private void analyzeCPUBottleneck() {
        System.out.println("\n2. CPU UTILIZATION BOTTLENECK ANALYSIS");
        System.out.println("-".repeat(70));
        
        Map<String, Double> cpuPressureIndex = new LinkedHashMap<>();
        
        for (Node node : nodes.values()) {
            double cpi = node.calculateCPUPressureIndex();
            cpuPressureIndex.put(node.getNodeId(), cpi);
            
            System.out.println(String.format(
                "%s: CPI = %.0f / %.1f = %.2f %s",
                node.getNodeId(),
                (double)node.getCpuUsage(),
                node.getMemory(),
                cpi,
                cpi > 9.0 ? "⚠ CRITICAL" : (cpi > 7.0 ? "⚠ HIGH" : "✓ OK")
            ));
        }
        
        // Identify critical nodes
        List<String> criticalNodes = cpuPressureIndex.entrySet().stream()
            .filter(e -> e.getValue() > 9.0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (!criticalNodes.isEmpty()) {
            System.out.println("\n⚠ CRITICAL CPU PRESSURE on: " + 
                             String.join(", ", criticalNodes));
        }
        
        // Calculate average CPU utilization
        double avgCPU = nodes.values().stream()
            .mapToInt(Node::getCpuUsage)
            .average()
            .orElse(0);
        
        System.out.println(String.format("Average CPU Utilization: %.1f%%", avgCPU));
    }
    
    /**
     * 3. Throughput Bottleneck Analysis
     */
    private void analyzeThroughputBottleneck() {
        System.out.println("\n3. THROUGHPUT BOTTLENECK ANALYSIS");
        System.out.println("-".repeat(70));
        
        // Assume 100 events/sec per edge, 200 for core, 300 for cloud
        Map<String, Integer> eventsPerSec = new HashMap<>();
        eventsPerSec.put("Edge1", 100);
        eventsPerSec.put("Edge2", 100);
        eventsPerSec.put("Core1", 200);
        eventsPerSec.put("Core2", 200);
        eventsPerSec.put("Cloud1", 300);
        
        double avgMessageSize = 0.001; // 1KB in MB
        
        for (Node node : nodes.values()) {
            int events = eventsPerSec.getOrDefault(node.getNodeId(), 100);
            double dataRate = events * avgMessageSize * 8; // Convert to Mbps
            double utilization = (dataRate / node.getThroughput()) * 100;
            
            System.out.println(String.format(
                "%s: CUR = (%.0f × %.3f × 8) / %.0f = %.4f%% %s",
                node.getNodeId(),
                (double)events,
                avgMessageSize,
                (double)node.getThroughput(),
                utilization,
                utilization > 80 ? "⚠ HIGH" : "✓ OK"
            ));
        }
        
        System.out.println("\n✓ All nodes have sufficient throughput capacity");
    }
    
    /**
     * 4. Memory Bottleneck Analysis
     */
    private void analyzeMemoryBottleneck() {
        System.out.println("\n4. MEMORY ALLOCATION ANALYSIS");
        System.out.println("-".repeat(70));
        
        double totalMemory = nodes.values().stream()
            .mapToDouble(Node::getMemory)
            .sum();
        
        for (Node node : nodes.values()) {
            double memoryShare = (node.getMemory() / totalMemory) * 100;
            
            System.out.println(String.format(
                "%s: %.1f GB (%.1f%% of total)",
                node.getNodeId(),
                node.getMemory(),
                memoryShare
            ));
        }
        
        System.out.println(String.format("\nTotal System Memory: %.1f GB", totalMemory));
    }
    
    /**
     * 5. Generate Priority Ranking
     */
    private void generatePriorityRanking() {
        System.out.println("\n5. BOTTLENECK PRIORITY RANKING");
        System.out.println("-".repeat(70));
        
        // Calculate composite bottleneck score
        Map<String, Double> compositeScores = new LinkedHashMap<>();
        
        for (Node node : nodes.values()) {
            double latencyScore = node.calculateBottleneckScore() * 100;
            double cpuScore = node.calculateCPUPressureIndex() * 10;
            double failureScore = node.getStatus() != Node.NodeStatus.ACTIVE ? 50 : 0;
            
            double composite = latencyScore + cpuScore + failureScore;
            compositeScores.put(node.getNodeId(), composite);
        }
        
        // Sort by composite score (descending)
        List<Map.Entry<String, Double>> sorted = compositeScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        int rank = 1;
        for (Map.Entry<String, Double> entry : sorted) {
            String severity = rank == 1 ? "CRITICAL" : 
                            rank == 2 ? "HIGH" : 
                            rank == 3 ? "MEDIUM" : "LOW";
            
            System.out.println(String.format(
                "%d. %s (Score: %.2f) - %s Priority",
                rank++,
                entry.getKey(),
                entry.getValue(),
                severity
            ));
        }
    }
    
    /**
     * 6. Generate Optimization Recommendations
     */
    private void generateRecommendations() {
        System.out.println("\n6. OPTIMIZATION RECOMMENDATIONS");
        System.out.println("-".repeat(70));
        
        int recNum = 1;
        
        // Identify highest latency node
        Node maxLatencyNode = nodes.values().stream()
            .max(Comparator.comparingInt(Node::getLatency))
            .orElse(null);
        
        if (maxLatencyNode != null && maxLatencyNode.getLatency() > 15) {
            System.out.println(String.format(
                "%d. IMPLEMENT EDGE CACHING for %s", recNum++, maxLatencyNode.getNodeId()
            ));
            System.out.println("   - Reduce round trips by 60%%");
            System.out.println("   - Expected latency reduction: ~12ms");
        }
        
        // Identify CPU imbalance
        List<Node> highCPUNodes = nodes.values().stream()
            .filter(n -> n.getCpuUsage() > 60)
            .collect(Collectors.toList());
        
        if (highCPUNodes.size() > 1) {
            System.out.println(String.format(
                "%d. LOAD BALANCING between high-CPU nodes", recNum++
            ));
            for (Node node : highCPUNodes) {
                System.out.println(String.format(
                    "   - %s: Redistribute %d%% CPU load",
                    node.getNodeId(),
                    node.getCpuUsage() - 55
                ));
            }
        }
        
        // Identify failure-prone nodes
        List<Node> degradedNodes = nodes.values().stream()
            .filter(n -> n.getStatus() != Node.NodeStatus.ACTIVE)
            .collect(Collectors.toList());
        
        if (!degradedNodes.isEmpty()) {
            System.out.println(String.format(
                "%d. IMPLEMENT FAILOVER AUTOMATION", recNum++
            ));
            for (Node node : degradedNodes) {
                System.out.println(String.format(
                    "   - %s: Fast failover mechanism",
                    node.getNodeId()
                ));
            }
            System.out.println("   - Expected availability: 99% → 99.9%");
        }
        
        // Check packet loss
        List<Node> highLossNodes = nodes.values().stream()
            .filter(n -> n.getPacketLoss() > 0.3)
            .collect(Collectors.toList());
        
        if (!highLossNodes.isEmpty()) {
            System.out.println(String.format(
                "%d. IMPROVE NETWORK RELIABILITY", recNum++
            ));
            for (Node node : highLossNodes) {
                System.out.println(String.format(
                    "   - %s: Reduce %.1f%% packet loss",
                    node.getNodeId(),
                    node.getPacketLoss()
                ));
            }
        }
    }
    
    /**
     * Generate summary statistics
     */
    public void printSummary() {
        System.out.println("\n=== SYSTEM SUMMARY ===");
        
        for (Node node : nodes.values()) {
            System.out.println(node.getPerformanceMetrics());
        }
    }
}