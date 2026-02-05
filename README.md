package messaging;

import models.Node;
import models.Message;
import java.util.*;

/**
 * Distributed System Architecture for Telecom Network
 * Manages node connections, message routing, and system topology
 */
public class DistributedSystemArchitecture {
    private Map<String, Node> nodes;
    private List<String[]> connections;
    
    /**
     * Initialize the distributed system architecture
     */
    public DistributedSystemArchitecture() {
        this.nodes = new HashMap<>();
        this.connections = new ArrayList<>();
    }
    
    /**
     * Add a node to the system
     */
    public void addNode(Node node) {
        nodes.put(node.getNodeId(), node);
        System.out.println("Added node: " + node.getNodeId());
    }
    
    /**
     * Create hierarchical edge-core-cloud architecture
     * Edge nodes connect to Core, Core nodes connect to Cloud
     */
    public void buildHierarchicalTopology() {
        List<Node> edgeNodes = new ArrayList<>();
        List<Node> coreNodes = new ArrayList<>();
        List<Node> cloudNodes = new ArrayList<>();
        
        // Categorize nodes
        for (Node node : nodes.values()) {
            switch (node.getNodeType()) {
                case EDGE:
                    edgeNodes.add(node);
                    break;
                case CORE:
                    coreNodes.add(node);
                    break;
                case CLOUD:
                    cloudNodes.add(node);
                    break;
            }
        }
        
        System.out.println("\n=== Building Hierarchical Topology ===");
        
        // Connect Edge to Core (each edge to all cores for redundancy)
        for (Node edge : edgeNodes) {
            for (Node core : coreNodes) {
                edge.connectTo(core);
                core.connectTo(edge);
                connections.add(new String[]{edge.getNodeId(), core.getNodeId()});
            }
        }
        
        // Connect Core to Cloud (all cores to all clouds)
        for (Node core : coreNodes) {
            for (Node cloud : cloudNodes) {
                core.connectTo(cloud);
                cloud.connectTo(core);
                connections.add(new String[]{core.getNodeId(), cloud.getNodeId()});
            }
        }
        
        // Connect Core nodes to each other for redundancy
        for (int i = 0; i < coreNodes.size(); i++) {
            for (int j = i + 1; j < coreNodes.size(); j++) {
                coreNodes.get(i).connectTo(coreNodes.get(j));
                coreNodes.get(j).connectTo(coreNodes.get(i));
                connections.add(new String[]{
                    coreNodes.get(i).getNodeId(), 
                    coreNodes.get(j).getNodeId()
                });
            }
        }
        
        System.out.println("Topology built with " + connections.size() + " connections");
    }
    
    /**
     * Route message from source to destination using shortest path
     */
    public List<Node> routeMessage(String sourceId, String destinationId) {
        Node source = nodes.get(sourceId);
        Node destination = nodes.get(destinationId);
        
        if (source == null || destination == null) {
            System.out.println("Error: Invalid source or destination");
            return new ArrayList<>();
        }
        
        // BFS to find shortest path
        Queue<List<Node>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        List<Node> initialPath = new ArrayList<>();
        initialPath.add(source);
        queue.add(initialPath);
        visited.add(sourceId);
        
        while (!queue.isEmpty()) {
            List<Node> path = queue.poll();
            Node current = path.get(path.size() - 1);
            
            if (current.getNodeId().equals(destinationId)) {
                return path;
            }
            
            for (Node neighbor : current.getConnectedNodes().values()) {
                if (!visited.contains(neighbor.getNodeId())) {
                    visited.add(neighbor.getNodeId());
                    List<Node> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                }
            }
        }
        
        return new ArrayList<>(); // No path found
    }
    
    /**
     * Send message through the network with routing
     */
    public boolean sendMessageThroughNetwork(String sourceId, String destId, 
                                            Message message) {
        List<Node> path = routeMessage(sourceId, destId);
        
        if (path.isEmpty()) {
            System.out.println("No path found from " + sourceId + " to " + destId);
            return false;
        }
        
        System.out.println("\nRouting path: ");
        for (int i = 0; i < path.size(); i++) {
            System.out.print(path.get(i).getNodeId());
            if (i < path.size() - 1) System.out.print(" -> ");
        }
        System.out.println("\nTotal latency: " + 
                         path.get(0).calculatePathLatency(path) + "ms\n");
        
        // Forward message through path
        for (int i = 0; i < path.size() - 1; i++) {
            Node current = path.get(i);
            Node next = path.get(i + 1);
            if (!current.sendMessage(next.getNodeId(), message)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Display system topology and statistics
     */
    public void displayTopology() {
        System.out.println("\n=== Network Topology ===");
        System.out.println("Total Nodes: " + nodes.size());
        System.out.println("Total Connections: " + connections.size());
        
        System.out.println("\nNode Details:");
        for (Node node : nodes.values()) {
            System.out.println(node);
            System.out.println("  Connected to: " + 
                             node.getConnectedNodes().keySet());
            System.out.println("  Efficiency Score: " + 
                             String.format("%.2f", node.getEfficiencyScore()));
        }
    }
    
    /**
     * Get all nodes
     */
    public Map<String, Node> getNodes() {
        return nodes;
    }
    
    /**
     * Get node by ID
     */
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }
}
