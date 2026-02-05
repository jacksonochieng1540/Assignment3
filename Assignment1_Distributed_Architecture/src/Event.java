import java.util.UUID;

/**
 * Represents an event in the distributed system.
 * Events trigger specific actions on nodes.
 */
public class Event {
    private String eventId;
    private String type;
    private String sourceNodeId;
    private String targetNodeId;
    private String data;
    private long timestamp;
    private EventPriority priority;
    
    public enum EventPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Constructor for Event
     */
    public Event(String type, String sourceNodeId, String targetNodeId, String data) {
        this.eventId = UUID.randomUUID().toString();
        this.type = type;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.priority = determinePriority(type);
    }
    
    /**
     * Determine event priority based on type
     */
    private EventPriority determinePriority(String type) {
        switch (type) {
            case "NodeFailure":
                return EventPriority.CRITICAL;
            case "TransactionCommit":
                return EventPriority.HIGH;
            case "Recovery":
                return EventPriority.HIGH;
            case "RPCCall":
                return EventPriority.MEDIUM;
            default:
                return EventPriority.LOW;
        }
    }
    
    /**
     * Check if event has expired based on TTL
     */
    public boolean isExpired(long ttlMillis) {
        return (System.currentTimeMillis() - timestamp) > ttlMillis;
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public String getType() { return type; }
    public String getSourceNodeId() { return sourceNodeId; }
    public String getTargetNodeId() { return targetNodeId; }
    public String getData() { return data; }
    public long getTimestamp() { return timestamp; }
    public EventPriority getPriority() { return priority; }
    
    @Override
    public String toString() {
        return String.format("Event[%s, type=%s, from=%s, to=%s, priority=%s]",
                           eventId.substring(0, 8), type, sourceNodeId, targetNodeId, priority);
    }
}