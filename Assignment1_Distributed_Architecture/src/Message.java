import java.util.UUID;

/**
 * Represents a message passed between nodes in the distributed system.
 */
public class Message {
    private String messageId;
    private String type;
    private String sourceNodeId;
    private String destinationNodeId;
    private Object payload;
    private long timestamp;
    private int retryCount;
    private boolean requiresAck;
    
    public enum MessageType {
        RPC_REQUEST, RPC_RESPONSE, HEARTBEAT, 
        TRANSACTION, ACKNOWLEDGMENT, BROADCAST
    }
    
    /**
     * Constructor for Message
     */
    public Message(String type, String sourceNodeId, String destinationNodeId, 
                  Object payload, boolean requiresAck) {
        this.messageId = UUID.randomUUID().toString();
        this.type = type;
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.retryCount = 0;
        this.requiresAck = requiresAck;
    }
    
    /**
     * Increment retry counter
     */
    public void incrementRetry() {
        retryCount++;
    }
    
    /**
     * Check if max retries exceeded
     */
    public boolean maxRetriesExceeded(int maxRetries) {
        return retryCount >= maxRetries;
    }
    
    /**
     * Get message age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }
    
    // Getters and Setters
    public String getMessageId() { return messageId; }
    public String getType() { return type; }
    public String getSourceNodeId() { return sourceNodeId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public int getRetryCount() { return retryCount; }
    public boolean requiresAck() { return requiresAck; }
    
    @Override
    public String toString() {
        return String.format("Message[%s, type=%s, from=%s, to=%s, retries=%d]",
                           messageId.substring(0, 8), type, sourceNodeId, 
                           destinationNodeId, retryCount);
    }
}