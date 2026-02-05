import java.util.*;

/**
 * Main class orchestrating the distributed telecom network system.
 * Implements edge-core-cloud architecture with message passing.
 */
public class DistributedSystem {
    private Map<String, Node> nodes;
    private MessagePassing messagePassing;
    private PerformanceAnalyzer analyzer;
    
    /**
     * Constructor - Initialize the distributed system
     */
    public DistributedSystem() {
        this.nodes = new LinkedHashMap<>();
        initializeNodes();
        this.messagePassing = new MessagePassing(nodes);
        this.analyzer = new PerformanceAnalyzer(nodes);
    }
    
    /**
     * Initialize nodes based on the dataset
     */
    private void initializeNodes() {
        // Edge1: 12ms, 500 Mbps, 0.2% loss, 40% CPU, 4GB
        nodes.put("Edge1", new Node("Edge1", 12, 500, 0.2, 40, 4.0,
                  Arrays.asList("RPCCall", "TransactionCommit")));
        
        // Edge2: 15ms, 480 Mbps, 0.5% loss, 45% CPU, 4.5GB
        nodes.put("Edge2", new Node("Edge2", 15, 480, 0.5, 45, 4.5,
                  Arrays.asList("RPCCall", "NodeFailure")));
        
        // Core1: 8ms, 1000 Mbps, 0.1% loss, 60% CPU, 8GB
        nodes.put("Core1", new Node("Core1", 8, 1000, 0.1, 60, 8.0,
                  Arrays.asList("TransactionCommit")));
        
        // Core2: 10ms, 950 Mbps, 0.2% loss, 55% CPU, 7.5GB
        nodes.put("Core2", new Node("Core2", 10, 950, 0.2, 55, 7.5,
                  Arrays.asList("NodeFailure", "Recovery")));
        
        // Cloud1: 20ms, 1200 Mbps, 0.3% loss, 70% CPU, 16GB
        nodes.put("Cloud1", new Node("Cloud1", 20, 1200, 0.3, 70, 16.0,
                  Arrays.asList("RPCCall", "NodeFailure")));
    }
    
    /**
     * Demonstrate the distributed system architecture
     */
    public void demonstrateArchitecture() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("    DISTRIBUTED TELECOM NETWORK SYSTEM - ARCHITECTURE DEMO");
        System.out.println("=".repeat(70));
        
        printTopology();
        
        System.out.println("\n--- Node Initialization ---");
        for (Node node : nodes.values()) {
            System.out.println(node);
        }
    }
    
    /**
     * Print network topology
     */
    private void printTopology() {
        System.out.println("\nNetwork Topology (Edge-Core-Cloud):");
        System.out.println();
        System.out.println("                ┌─────────────┐");
        System.out.println("                │   Cloud1    │");
        System.out.println("                │  (20ms, 70%)│");
        System.out.println("                └──────┬──────┘");
        System.out.println("                       │");
        System.out.println("          ┌────────────┴────────────┐");
        System.out.println("          │                         │");
        System.out.println("     ┌────▼─────┐            ┌─────▼────┐");
        System.out.println("     │  Core1   │◄──────────►│  Core2   │");
        System.out.println("     │(8ms, 60%)│            │(10ms, 55%)│");
        System.out.println("     └────┬─────┘            └─────┬────┘");
        System.out.println("          │                        │");
        System.out.println("    ┌─────┴──────┐          ┌─────┴──────┐");
        System.out.println("    │            │          │            │");
        System.out.println("┌───▼───┐    ┌───▼───┐  ┌───▼───┐    ┌───▼───┐");
        System.out.println("│Edge1  │    │Edge2  │  │Edge1  │    │Edge2  │");
        System.out.println("│(12ms) │    │(15ms) │  │(12ms) │    │(15ms) │");
        System.out.println("└───────┘    └───────┘  └───────┘    └───────┘");
    }
    
    /**
     * Demonstrate message passing mechanisms
     */
    public void demonstrateMessagePassing() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                MESSAGE PASSING DEMONSTRATION");
        System.out.println("=".repeat(70));
        
        // 1. Synchronous RPC Call
        System.out.println("\n--- 1. SYNCHRONOUS RPC CALL ---");
        Message response = messagePassing.sendRPC("Edge1", "Core1", 
                          "Transaction: COMMIT_USER_DATA");
        if (response != null) {
            System.out.println("✓ RPC Completed: " + response.getPayload());
        }
        
        // 2. Asynchronous Message
        System.out.println("\n--- 2. ASYNCHRONOUS MESSAGE ---");
        messagePassing.sendAsyncMessage("Core1", "Cloud1", 
                       "Analytics: Process user metrics");
        
        // Wait for async completion
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 3. Broadcast
        System.out.println("\n--- 3. BROADCAST MESSAGE ---");
        messagePassing.broadcast("Core2", "SYSTEM ALERT: High load detected");
        
        // Wait for broadcast completion
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 4. Multicast to specific nodes
        System.out.println("\n--- 4. MULTICAST TO EDGE NODES ---");
        messagePassing.multicast("Cloud1", Arrays.asList("Edge1", "Edge2"),
                                "CONFIG UPDATE: New routing policy");
        
        // Wait for multicast completion
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 5. Message with acknowledgment
        System.out.println("\n--- 5. MESSAGE WITH ACKNOWLEDGMENT ---");
        boolean acked = messagePassing.sendWithAck("Edge2", "Core2",
                        "CRITICAL: Node failure detected", 3000);
        System.out.println(acked ? "✓ Acknowledgment received" : "✗ No acknowledgment");
    }
    
    /**
     * Demonstrate event handling
     */
    public void demonstrateEventHandling() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                 EVENT HANDLING DEMONSTRATION");
        System.out.println("=".repeat(70));
        
        // Create various events
        Event rpcEvent = new Event("RPCCall", "Edge1", "Core1", 
                                   "getUserProfile(userId=123)");
        Event commitEvent = new Event("TransactionCommit", "Core1", "Cloud1",
                                     "txn_id=TX_9876");
        Event failureEvent = new Event("NodeFailure", "Edge2", "Core2",
                                      "Connection timeout");
        Event recoveryEvent = new Event("Recovery", "Core2", "Edge2",
                                       "Initiating recovery sequence");
        
        // Handle events
        System.out.println("\n--- Handling RPC Event ---");
        nodes.get("Edge1").handleEvent(rpcEvent);
        
        System.out.println("\n--- Handling Transaction Commit Event ---");
        nodes.get("Core1").handleEvent(commitEvent);
        
        System.out.println("\n--- Handling Node Failure Event ---");
        nodes.get("Edge2").handleEvent(failureEvent);
        
        System.out.println("\n--- Handling Recovery Event ---");
        nodes.get("Core2").handleEvent(recoveryEvent);
        
        // Wait for recovery to complete
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Run performance analysis
     */
    public void runPerformanceAnalysis() {
        analyzer.analyzeBottlenecks();
        analyzer.printSummary();
    }
    
    /**
     * Simulate system operation
     */
    public void simulateOperation() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("              SYSTEM OPERATION SIMULATION");
        System.out.println("=".repeat(70));
        
        System.out.println("\nSimulating 10 transactions across the network...\n");
        
        for (int i = 1; i <= 10; i++) {
            System.out.println("--- Transaction " + i + " ---");
            
            // Edge to Core
            messagePassing.sendRPC("Edge1", "Core1", "Transaction_" + i);
            
            // Core to Cloud
            messagePassing.sendAsyncMessage("Core1", "Cloud1", 
                           "Store Transaction_" + i);
            
            // Small delay between transactions
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("\n✓ Simulation complete");
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        System.out.println("\n--- Shutting down system ---");
        messagePassing.shutdown();
        System.out.println("✓ System shutdown complete");
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║      ICS 2403 - ASSIGNMENT 1: DISTRIBUTED SYSTEM ARCHITECTURE     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
        
        DistributedSystem system = new DistributedSystem();
        
        try {
            // (a) Demonstrate architecture
            system.demonstrateArchitecture();
            
            // (b) Demonstrate message passing and event handling
            system.demonstrateMessagePassing();
            system.demonstrateEventHandling();
            
            // Simulate system operation
            system.simulateOperation();
            
            // (c) Performance bottleneck analysis
            system.runPerformanceAnalysis();
            
        } finally {
            system.shutdown();
        }
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                    ASSIGNMENT 1 COMPLETE");
        System.out.println("=".repeat(70) + "\n");
    }
}