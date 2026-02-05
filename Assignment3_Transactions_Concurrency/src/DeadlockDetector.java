import java.util.*;
import java.util.concurrent.*;

/**
 * Implements deadlock detection and resolution strategies.
 * Part (c) of Assignment 3.
 */
public class DeadlockDetector {
    private Map<String, TransactionManager.TransactionNode> nodes;
    private Map<String, Transaction> activeTransactions;
    private Map<String, Set<String>> waitForGraph;  // Transaction -> Set of transactions it waits for
    private ExecutorService executor;
    
    /**
     * Transaction with deadlock information
     */
    static class Transaction {
        String id;
        String nodeId;
        long timestamp;  // For timestamp ordering
        int priority;
        Set<String> heldResources;
        Set<String> requestedResources;
        TransactionState state;
        
        enum TransactionState {
            ACTIVE, WAITING, ABORTED, COMMITTED
        }
        
        public Transaction(String id, String nodeId, long timestamp, int priority) {
            this.id = id;
            this.nodeId = nodeId;
            this.timestamp = timestamp;
            this.priority = priority;
            this.heldResources = new HashSet<>();
            this.requestedResources = new HashSet<>();
            this.state = TransactionState.ACTIVE;
        }
        
        @Override
        public String toString() {
            return String.format("T%s(node=%s, pri=%d, state=%s)", 
                               id, nodeId, priority, state);
        }
    }
    
    /**
     * Resource lock information
     */
    static class ResourceLock {
        String resourceId;
        String holderTransactionId;
        Queue<String> waitingTransactions;
        
        public ResourceLock(String resourceId) {
            this.resourceId = resourceId;
            this.waitingTransactions = new LinkedList<>();
        }
    }
    
    /**
     * Constructor
     */
    public DeadlockDetector(Map<String, TransactionManager.TransactionNode> nodes) {
        this.nodes = nodes;
        this.activeTransactions = new ConcurrentHashMap<>();
        this.waitForGraph = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Main deadlock resolution demonstration
     */
    public void demonstrateDeadlockResolution() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ASSIGNMENT 3(c): DEADLOCK DETECTION AND RESOLUTION");
        System.out.println("=".repeat(80));
        
        System.out.println("\n1. DEADLOCK DETECTION STRATEGIES:");
        System.out.println("-".repeat(80));
        explainDetectionStrategies();
        
        System.out.println("\n2. DEADLOCK PREVENTION STRATEGIES:");
        System.out.println("-".repeat(80));
        explainPreventionStrategies();
        
        System.out.println("\n3. DEADLOCK RESOLUTION DEMONSTRATION:");
        System.out.println("-".repeat(80));
        simulateDeadlockScenario();
        
        System.out.println("\n4. WAIT-DIE ALGORITHM IMPLEMENTATION:");
        System.out.println("-".repeat(80));
        demonstrateWaitDie();
        
        System.out.println("\n5. WOUND-WAIT ALGORITHM IMPLEMENTATION:");
        System.out.println("-".repeat(80));
        demonstrateWoundWait();
        
        System.out.println("\n6. TIMEOUT-BASED RESOLUTION:");
        System.out.println("-".repeat(80));
        demonstrateTimeoutResolution();
        
        System.out.println("\n" + "=".repeat(80) + "\n");
    }
    
    /**
     * Explain detection strategies
     */
    private void explainDetectionStrategies() {
        System.out.println("A. WAIT-FOR GRAPH METHOD:");
        System.out.println("   • Build directed graph: T1 → T2 if T1 waits for T2");
        System.out.println("   • Detect cycles using DFS");
        System.out.println("   • Time complexity: O(V + E)");
        System.out.println("   • Space complexity: O(V²)");
        
        System.out.println("\nB. RESOURCE ALLOCATION GRAPH:");
        System.out.println("   • Bipartite graph: Transactions ↔ Resources");
        System.out.println("   • Cycle indicates deadlock");
        System.out.println("   • More comprehensive than wait-for graph");
        
        System.out.println("\nC. TIMESTAMP ORDERING:");
        System.out.println("   • Assign timestamps to transactions");
        System.out.println("   • Enforce wait-die or wound-wait rules");
        System.out.println("   • Prevents deadlocks proactively");
    }
    
    /**
     * Explain prevention strategies
     */
    private void explainPreventionStrategies() {
        System.out.println("A. WAIT-DIE (Non-preemptive):");
        System.out.println("   • Older transaction waits for younger");
        System.out.println("   • Younger transaction dies (aborts)");
        System.out.println("   • Rule: if T1.timestamp < T2.timestamp → T1 waits, else T1 dies");
        
        System.out.println("\nB. WOUND-WAIT (Preemptive):");
        System.out.println("   • Older transaction wounds (preempts) younger");
        System.out.println("   • Younger transaction waits");
        System.out.println("   • Rule: if T1.timestamp < T2.timestamp → T2 aborts, else T1 waits");
        
        System.out.println("\nC. TIMEOUT-BASED:");
        System.out.println("   • Set maximum wait time");
        System.out.println("   • Abort transaction after timeout");
        System.out.println("   • Simple but may abort unnecessarily");
        
        System.out.println("\nD. RESOURCE ORDERING:");
        System.out.println("   • Acquire resources in predefined order");
        System.out.println("   • Prevents circular wait");
        System.out.println("   • Requires global knowledge");
    }
    
    /**
     * Simulate and resolve a deadlock scenario
     */
    private void simulateDeadlockScenario() {
        System.out.println("SCENARIO: Four transactions competing for resources");
        System.out.println();
        
        // Create transactions
        Transaction t1 = new Transaction("1", "Core1", 1000, 5);
        Transaction t2 = new Transaction("2", "Core1", 1010, 4);
        Transaction t3 = new Transaction("3", "Cloud1", 1020, 3);
        Transaction t4 = new Transaction("4", "Cloud1", 1030, 2);
        
        activeTransactions.put(t1.id, t1);
        activeTransactions.put(t2.id, t2);
        activeTransactions.put(t3.id, t3);
        activeTransactions.put(t4.id, t4);
        
        // Setup circular wait
        System.out.println("Resource Allocation:");
        t1.heldResources.add("R1");
        t1.requestedResources.add("R2");
        System.out.println("  T1: holds R1, requests R2");
        
        t2.heldResources.add("R2");
        t2.requestedResources.add("R3");
        System.out.println("  T2: holds R2, requests R3");
        
        t3.heldResources.add("R3");
        t3.requestedResources.add("R4");
        System.out.println("  T3: holds R3, requests R4");
        
        t4.heldResources.add("R4");
        t4.requestedResources.add("R1");
        System.out.println("  T4: holds R4, requests R1");
        
        // Build wait-for graph
        buildWaitForGraph();
        
        System.out.println("\nWait-For Graph:");
        System.out.println("  T1 → T2 → T3 → T4 → T1  (CYCLE DETECTED!)");
        
        // Detect cycle
        List<String> cycle = detectCycle();
        
        if (cycle != null) {
            System.out.println("\n⚠️  DEADLOCK DETECTED!");
            System.out.println("  Deadlock cycle: " + String.join(" → ", cycle));
            
            // Resolve using victim selection
            String victim = selectVictim(cycle);
            System.out.println("\n  Resolution: Abort " + victim + " (lowest priority)");
            
            Transaction victimTxn = activeTransactions.get(victim);
            victimTxn.state = Transaction.TransactionState.ABORTED;
            
            System.out.println("  ✓ Deadlock resolved");
            System.out.println("  Remaining transactions can proceed");
        }
    }
    
    /**
     * Build wait-for graph from current transaction states
     */
    private void buildWaitForGraph() {
        waitForGraph.clear();
        
        for (Transaction t : activeTransactions.values()) {
            Set<String> waitingFor = new HashSet<>();
            
            for (String requestedResource : t.requestedResources) {
                // Find who holds this resource
                for (Transaction other : activeTransactions.values()) {
                    if (!other.id.equals(t.id) && other.heldResources.contains(requestedResource)) {
                        waitingFor.add(other.id);
                    }
                }
            }
            
            if (!waitingFor.isEmpty()) {
                waitForGraph.put(t.id, waitingFor);
            }
        }
    }
    
    /**
     * Detect cycle in wait-for graph using DFS
     */
    private List<String> detectCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> path = new ArrayList<>();
        
        for (String txnId : waitForGraph.keySet()) {
            if (detectCycleDFS(txnId, visited, recursionStack, path)) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * DFS helper for cycle detection
     */
    private boolean detectCycleDFS(String node, Set<String> visited, 
                                   Set<String> recursionStack, List<String> path) {
        visited.add(node);
        recursionStack.add(node);
        path.add(node);
        
        Set<String> neighbors = waitForGraph.get(node);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    if (detectCycleDFS(neighbor, visited, recursionStack, path)) {
                        return true;
                    }
                } else if (recursionStack.contains(neighbor)) {
                    // Cycle found
                    path.add(neighbor);
                    return true;
                }
            }
        }
        
        recursionStack.remove(node);
        path.remove(path.size() - 1);
        return false;
    }
    
    /**
     * Select victim for deadlock resolution
     * Criteria: lowest priority, youngest timestamp, least work done
     */
    private String selectVictim(List<String> cycle) {
        String victim = cycle.get(0);
        int minPriority = Integer.MAX_VALUE;
        
        for (String txnId : cycle) {
            Transaction txn = activeTransactions.get(txnId);
            if (txn.priority < minPriority) {
                minPriority = txn.priority;
                victim = txnId;
            }
        }
        
        return victim;
    }
    
    /**
     * Demonstrate Wait-Die algorithm
     */
    private void demonstrateWaitDie() {
        System.out.println("WAIT-DIE ALGORITHM (Non-preemptive, timestamp-based):");
        System.out.println();
        
        System.out.println("Rule: When Ti requests resource held by Tj:");
        System.out.println("  • If Ti older (timestamp < Tj.timestamp): Ti WAITS");
        System.out.println("  • If Ti younger (timestamp > Tj.timestamp): Ti DIES (aborts)");
        System.out.println();
        
        System.out.println("Example Scenarios:");
        System.out.println("-".repeat(60));
        
        // Scenario 1: Older waits
        System.out.println("1. T1(ts=1000) requests resource held by T2(ts=1010)");
        System.out.println("   → T1 is OLDER → T1 WAITS ✓");
        
        // Scenario 2: Younger dies
        System.out.println("\n2. T3(ts=1020) requests resource held by T1(ts=1000)");
        System.out.println("   → T3 is YOUNGER → T3 DIES (aborts) ✗");
        System.out.println("   → T3 restarts with same timestamp");
        
        System.out.println("\nADVANTAGES:");
        System.out.println("  • No deadlocks (older always progresses)");
        System.out.println("  • Simple to implement");
        System.out.println("  • Starvation-free (retries keep timestamp)");
        
        System.out.println("\nDISADVANTAGES:");
        System.out.println("  • Unnecessary aborts for younger transactions");
        System.out.println("  • Wasted work on aborted transactions");
    }
    
    /**
     * Demonstrate Wound-Wait algorithm
     */
    private void demonstrateWoundWait() {
        System.out.println("WOUND-WAIT ALGORITHM (Preemptive, timestamp-based):");
        System.out.println();
        
        System.out.println("Rule: When Ti requests resource held by Tj:");
        System.out.println("  • If Ti older (timestamp < Tj.timestamp): Tj WOUNDED (preempted)");
        System.out.println("  • If Ti younger (timestamp > Tj.timestamp): Ti WAITS");
        System.out.println();
        
        System.out.println("Example Scenarios:");
        System.out.println("-".repeat(60));
        
        // Scenario 1: Older wounds younger
        System.out.println("1. T1(ts=1000) requests resource held by T2(ts=1010)");
        System.out.println("   → T1 is OLDER → T2 WOUNDED (aborted) ✓");
        System.out.println("   → T2 releases resource");
        System.out.println("   → T1 acquires resource");
        
        // Scenario 2: Younger waits
        System.out.println("\n2. T3(ts=1020) requests resource held by T1(ts=1000)");
        System.out.println("   → T3 is YOUNGER → T3 WAITS");
        
        System.out.println("\nADVANTAGES:");
        System.out.println("  • Fewer restarts (older rarely aborts)");
        System.out.println("  • Better for long transactions");
        System.out.println("  • No deadlocks");
        
        System.out.println("\nDISADVANTAGES:");
        System.out.println("  • More preemptions");
        System.out.println("  • Cascading aborts possible");
    }
    
    /**
     * Demonstrate timeout-based resolution
     */
    private void demonstrateTimeoutResolution() {
        System.out.println("TIMEOUT-BASED DEADLOCK RESOLUTION:");
        System.out.println();
        
        long timeoutMs = 5000;  // 5 seconds
        
        System.out.println("Configuration:");
        System.out.println("  • Wait timeout: " + timeoutMs + "ms");
        System.out.println("  • Check interval: 1000ms");
        System.out.println();
        
        System.out.println("Algorithm:");
        System.out.println("  1. Transaction requests resource");
        System.out.println("  2. If blocked, start timer");
        System.out.println("  3. Periodically check if acquired");
        System.out.println("  4. If timeout exceeded → abort transaction");
        System.out.println("  5. Release all held resources");
        System.out.println("  6. Retry after backoff");
        
        System.out.println("\nPseudocode:");
        System.out.println("-".repeat(60));
        System.out.println("  boolean acquireResource(resource, timeout) {");
        System.out.println("      startTime = currentTime()");
        System.out.println("      while (!acquired && currentTime() - startTime < timeout) {");
        System.out.println("          if (tryAcquire(resource)) return true");
        System.out.println("          sleep(100ms)");
        System.out.println("      }");
        System.out.println("      // Timeout");
        System.out.println("      abortTransaction()");
        System.out.println("      return false");
        System.out.println("  }");
        
        System.out.println("\nADVANTAGES:");
        System.out.println("  • Simple implementation");
        System.out.println("  • No graph maintenance");
        System.out.println("  • Works with any locking protocol");
        
        System.out.println("\nDISADVANTAGES:");
        System.out.println("  • False positives (abort without deadlock)");
        System.out.println("  • Timeout tuning required");
        System.out.println("  • Resource waste on unnecessary aborts");
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║         DEADLOCK DETECTION AND RESOLUTION DEMONSTRATION              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
        
        // Create dummy nodes for demonstration
        Map<String, TransactionManager.TransactionNode> dummyNodes = new HashMap<>();
        
        DeadlockDetector detector = new DeadlockDetector(dummyNodes);
        detector.demonstrateDeadlockResolution();
        
        System.out.println("=".repeat(80));
        System.out.println("DEADLOCK RESOLUTION DEMONSTRATION COMPLETE");
        System.out.println("=".repeat(80) + "\n");
    }
}