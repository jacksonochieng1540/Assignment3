import java.util.*;
import java.util.concurrent.*;

/**
 * Handles message passing between nodes in the distributed system.
 * Implements synchronous RPC and asynchronous messaging.
 */
public class MessagePassing {
    private Map<String, Node> nodes;
    private Map<String, CompletableFuture<Message>> pendingRPCs;
    private ExecutorService messageExecutor;
    private static final int MAX_RETRIES = 3;
    private static final long RPC_TIMEOUT_MS = 5000;
    private static final long MESSAGE_TIMEOUT_MS = 10000;
    
    /**
     * Constructor
     */
    public MessagePassing(Map<String, Node> nodes) {
        this.nodes = nodes;
        this.pendingRPCs = new ConcurrentHashMap<>();
        this.messageExecutor = Executors.newFixedThreadPool(10);
    }
    
    /**
     * Send synchronous RPC call with timeout and retry logic
     */
    public Message sendRPC(String sourceId, String destinationId, Object payload) {
        Node sourceNode = nodes.get(sourceId);
        Node destinationNode = nodes.get(destinationId);
        
        if (sourceNode == null || destinationNode == null) {
            System.out.println("RPC Error: Source or destination node not found");
            return null;
        }
        
        System.out.println(String.format("\n[RPC] %s → %s", sourceId, destinationId));
        
        Message request = new Message("RPC_REQUEST", sourceId, destinationId, payload, true);
        
        // Create future for response
        CompletableFuture<Message> responseFuture = new CompletableFuture<>();
        pendingRPCs.put(request.getMessageId(), responseFuture);
        
        // Send request with retry logic
        int retries = 0;
        boolean sent = false;
        
        while (retries < MAX_RETRIES && !sent) {
            sent = destinationNode.processMessage(request);
            if (!sent) {
                request.incrementRetry();
                retries++;
                System.out.println("[RPC] Retry " + retries + "/" + MAX_RETRIES);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        if (!sent) {
            System.out.println("[RPC] Failed after " + MAX_RETRIES + " retries");
            pendingRPCs.remove(request.getMessageId());
            return null;
        }
        
        // Simulate response processing
        messageExecutor.submit(() -> {
            try {
                // Simulate processing time
                Thread.sleep(destinationNode.getLatency());
                
                Message response = new Message("RPC_RESPONSE", destinationId, sourceId,
                                              "Response to: " + payload, false);
                responseFuture.complete(response);
                
            } catch (InterruptedException e) {
                responseFuture.completeExceptionally(e);
            }
        });
        
        // Wait for response with timeout
        try {
            Message response = responseFuture.get(RPC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            System.out.println("[RPC] Success: " + response.getMessageId());
            return response;
            
        } catch (TimeoutException e) {
            System.out.println("[RPC] Timeout after " + RPC_TIMEOUT_MS + "ms");
            responseFuture.cancel(true);
            return null;
            
        } catch (Exception e) {
            System.out.println("[RPC] Error: " + e.getMessage());
            return null;
            
        } finally {
            pendingRPCs.remove(request.getMessageId());
        }
    }
    
    /**
     * Send asynchronous message (fire and forget)
     */
    public CompletableFuture<Boolean> sendAsyncMessage(String sourceId, String destinationId, 
                                                       Object payload) {
        Node destinationNode = nodes.get(destinationId);
        
        if (destinationNode == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        Message message = new Message("ASYNC", sourceId, destinationId, payload, false);
        
        System.out.println(String.format("[ASYNC] %s → %s: %s", 
                                        sourceId, destinationId, payload));
        
        return CompletableFuture.supplyAsync(() -> {
            return destinationNode.processMessage(message);
        }, messageExecutor);
    }
    
    /**
     * Broadcast message to all nodes
     */
    public void broadcast(String sourceId, Object payload) {
        System.out.println(String.format("\n[BROADCAST] From %s to all nodes", sourceId));
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (String nodeId : nodes.keySet()) {
            if (!nodeId.equals(sourceId)) {
                Message message = new Message("BROADCAST", sourceId, nodeId, payload, false);
                
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    Node node = nodes.get(nodeId);
                    return node.processMessage(message);
                }, messageExecutor);
                
                futures.add(future);
            }
        }
        
        // Wait for all broadcasts to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenRun(() -> {
                            System.out.println("[BROADCAST] Complete");
                        });
    }
    
    /**
     * Send heartbeat to all nodes
     */
    public void sendHeartbeats(String sourceId) {
        for (Node node : nodes.values()) {
            if (!node.getNodeId().equals(sourceId)) {
                Message heartbeat = new Message("HEARTBEAT", sourceId, node.getNodeId(),
                                              System.currentTimeMillis(), false);
                node.processMessage(heartbeat);
            }
        }
    }
    
    /**
     * Multicast to specific group of nodes
     */
    public void multicast(String sourceId, List<String> targetNodeIds, Object payload) {
        System.out.println(String.format("\n[MULTICAST] From %s to %d nodes", 
                                        sourceId, targetNodeIds.size()));
        
        for (String targetId : targetNodeIds) {
            if (nodes.containsKey(targetId)) {
                sendAsyncMessage(sourceId, targetId, payload);
            }
        }
    }
    
    /**
     * Send message with acknowledgment requirement
     */
    public boolean sendWithAck(String sourceId, String destinationId, Object payload,
                              long timeoutMs) {
        Node destinationNode = nodes.get(destinationId);
        
        if (destinationNode == null) {
            return false;
        }
        
        Message message = new Message("WITH_ACK", sourceId, destinationId, payload, true);
        
        CompletableFuture<Boolean> ackFuture = CompletableFuture.supplyAsync(() -> {
            boolean processed = destinationNode.processMessage(message);
            
            if (processed) {
                // Send acknowledgment back
                Message ack = new Message("ACKNOWLEDGMENT", destinationId, sourceId,
                                         message.getMessageId(), false);
                nodes.get(sourceId).processMessage(ack);
            }
            
            return processed;
        }, messageExecutor);
        
        try {
            return ackFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            System.out.println("[ACK] Timeout or error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get statistics about message passing
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder("\n=== Message Passing Statistics ===\n");
        sb.append(String.format("Pending RPCs: %d\n", pendingRPCs.size()));
        sb.append(String.format("Active threads: %d\n", 
                               ((ThreadPoolExecutor)messageExecutor).getActiveCount()));
        return sb.toString();
    }
    
    /**
     * Shutdown executor
     */
    public void shutdown() {
        messageExecutor.shutdown();
        try {
            if (!messageExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                messageExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            messageExecutor.shutdownNow();
        }
    }
}