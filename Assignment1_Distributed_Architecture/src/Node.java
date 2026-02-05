import java.util.*;
import java.util.concurrent.*;

/**
 * Represents a node in the distributed telecom network system.
 * Each node has performance characteristics and can handle various events.
 */
public class Node {
    private String nodeId;
    private int latency;           // milliseconds
    private int throughput;        // Mbps
    private double packetLoss;     // percentage
    private int cpuUsage;          // percentage
    private double memory;         // GB
    private List<String> supportedEvents;
    private NodeStatus status;
    private Queue<Message> messageQueue;
    private long lastHeartbeat;
    
    public enum NodeStatus {
        ACTIVE, DEGRADED, FAILED, RECOVERING
    }
    
    /**
     * Constructor for Node
     */
    public Node(String nodeId, int latency, int throughput, double packetLoss,
                int cpuUsage, double memory, List<String> events) {
        this.nodeId = nodeId;
        this.latency = latency;
        this.throughput = throughput;
        this.packetLoss = packetLoss;
        this.cpuUsage = cpuUsage;
        this.memory = memory;
        this.supportedEvents = new ArrayList<>(events);
        this.status = NodeStatus.ACTIVE;
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.lastHeartbeat = System.currentTimeMillis();
    }
    
    /**
     * Calculate bottleneck score for this node
     * BS = (Latency/Throughput) × CPU × (1 + PacketLoss)
     */
    public double calculateBottleneckScore() {
        double latencyThroughputRatio = (double) latency / throughput;
        double packetLossMultiplier = 1 + (packetLoss / 100.0);
        return latencyThroughputRatio * cpuUsage * packetLossMultiplier;
    }
    
    /**
     * Calculate CPU Pressure Index
     * CPI = CPU_usage / Memory_capacity
     */
    public double calculateCPUPressureIndex() {
        return cpuUsage / memory;
    }
    
    /**
     * Process an incoming message
     */
    public boolean processMessage(Message message) {
        if (status == NodeStatus.FAILED) {
            System.out.println("[" + nodeId + "] Cannot process message - node failed");
            return false;
        }
        
        // Simulate latency
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        // Simulate packet loss
        if (Math.random() * 100 < packetLoss) {
            System.out.println("[" + nodeId + "] Packet lost for message: " + message.getMessageId());
            return false;
        }
        
        messageQueue.offer(message);
        System.out.println("[" + nodeId + "] Processed message: " + message.getMessageId() + 
                          " (Type: " + message.getType() + ")");
        
        // Update CPU usage based on processing
        updateCPUUsage(5); // Increment by 5% per message
        
        return true;
    }
    
    /**
     * Handle specific event types
     */
    public void handleEvent(Event event) {
        if (!supportedEvents.contains(event.getType())) {
            System.out.println("[" + nodeId + "] Event type not supported: " + event.getType());
            return;
        }
        
        System.out.println("[" + nodeId + "] Handling event: " + event.getType());
        
        switch (event.getType()) {
            case "RPCCall":
                handleRPCCall(event);
                break;
            case "TransactionCommit":
                handleTransactionCommit(event);
                break;
            case "NodeFailure":
                handleNodeFailure(event);
                break;
            case "Recovery":
                handleRecovery(event);
                break;
            default:
                System.out.println("[" + nodeId + "] Unknown event type");
        }
    }
    
    private void handleRPCCall(Event event) {
        System.out.println("[" + nodeId + "] Executing RPC: " + event.getData());
        // Simulate RPC execution time
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void handleTransactionCommit(Event event) {
        System.out.println("[" + nodeId + "] Committing transaction: " + event.getData());
        // Simulate transaction commit
        updateCPUUsage(10);
    }
    
    private void handleNodeFailure(Event event) {
        System.out.println("[" + nodeId + "] Detecting node failure: " + event.getData());
        status = NodeStatus.DEGRADED;
    }
    
    private void handleRecovery(Event event) {
        System.out.println("[" + nodeId + "] Initiating recovery: " + event.getData());
        status = NodeStatus.RECOVERING;
        // Simulate recovery process
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                status = NodeStatus.ACTIVE;
                System.out.println("[" + nodeId + "] Recovery complete");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Update CPU usage with bounds checking
     */
    private void updateCPUUsage(int delta) {
        cpuUsage = Math.min(100, Math.max(0, cpuUsage + delta));
    }
    
    /**
     * Send heartbeat signal
     */
    public void sendHeartbeat() {
        lastHeartbeat = System.currentTimeMillis();
    }
    
    /**
     * Check if node is alive based on heartbeat timeout
     */
    public boolean isAlive(long timeout) {
        return (System.currentTimeMillis() - lastHeartbeat) < timeout;
    }
    
    /**
     * Get node performance metrics as string
     */
    public String getPerformanceMetrics() {
        return String.format(
            "Node: %s | Latency: %dms | Throughput: %d Mbps | CPU: %d%% | " +
            "Memory: %.1fGB | Status: %s | BS: %.2f | CPI: %.2f",
            nodeId, latency, throughput, cpuUsage, memory, status,
            calculateBottleneckScore(), calculateCPUPressureIndex()
        );
    }
    
    // Getters and Setters
    public String getNodeId() { return nodeId; }
    public int getLatency() { return latency; }
    public int getThroughput() { return throughput; }
    public double getPacketLoss() { return packetLoss; }
    public int getCpuUsage() { return cpuUsage; }
    public double getMemory() { return memory; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public List<String> getSupportedEvents() { return supportedEvents; }
    public int getQueueSize() { return messageQueue.size(); }
    
    @Override
    public String toString() {
        return String.format("Node[%s, %dms, %dMbps, %.1f%%loss, %d%%CPU, %.1fGB]",
                           nodeId, latency, throughput, packetLoss, cpuUsage, memory);
    }
}