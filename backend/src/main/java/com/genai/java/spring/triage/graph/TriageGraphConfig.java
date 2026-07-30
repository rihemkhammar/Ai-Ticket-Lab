package com.genai.java.spring.triage.graph;

import org.springframework.context.annotation.Configuration;

/**
 * Wires the LangGraph4j StateGraph for the triage pipeline:
 *
 *   START -> Classify -> Order -> Dispatch
 *              -> Investigation -> Review -> Rules -> Hitl
 *              -> (conditional: orderedQueue non-empty? loop back to
 *                  Dispatch : END)
 *
 * IMPORTANT: the exact langgraph4j-core API (StateGraph/NodeAction/
 * addConditionalEdges signatures) depends on the library version pinned
 * in this project's pom.xml. The method bodies below show the intended
 * wiring using each node's plain apply(TriageGraphState) method; adapt
 * the exact org.bsc.langgraph4j calls to match your installed version's
 * javadoc/README before compiling (Story M7, Phase 1, Section 3.3:
 * "learn from official documentation, no video for this milestone").
 *
 * Each node stays a plain, independently unit-testable Spring bean
 * (ClassifyTicketsNode, OrderQueueNode, ...); this class only concerns
 * itself with graph wiring, so tests can cover node logic without ever
 * touching langgraph4j itself (Rule 2.9).
 */
@Configuration
public class TriageGraphConfig {

    private final ClassifyTicketsNode classifyTicketsNode;
    private final OrderQueueNode orderQueueNode;
    private final DispatchNextTicketNode dispatchNextTicketNode;
    private final InvestigationNode investigationNode;
    private final ReviewNode reviewNode;
    private final RulesNode rulesNode;
    private final HitlCheckpointNode hitlCheckpointNode;

    public TriageGraphConfig(ClassifyTicketsNode classifyTicketsNode,
                             OrderQueueNode orderQueueNode,
                             DispatchNextTicketNode dispatchNextTicketNode,
                             InvestigationNode investigationNode,
                             ReviewNode reviewNode,
                             RulesNode rulesNode,
                             HitlCheckpointNode hitlCheckpointNode) {
        this.classifyTicketsNode = classifyTicketsNode;
        this.orderQueueNode = orderQueueNode;
        this.dispatchNextTicketNode = dispatchNextTicketNode;
        this.investigationNode = investigationNode;
        this.reviewNode = reviewNode;
        this.rulesNode = rulesNode;
        this.hitlCheckpointNode = hitlCheckpointNode;
    }

    /**
     * TODO (verify against your langgraph4j-core version's real API):
     *
     * StateGraph<TriageGraphState> graph =
     *     new StateGraph<>(TriageGraphState.class, TriageGraphState::new);
     *
     * graph.addNode("classify", node_async(classifyTicketsNode::apply));
     * graph.addNode("order",    node_async(orderQueueNode::apply));
     * graph.addNode("dispatch", node_async(dispatchNextTicketNode::apply));
     * graph.addNode("investigate", node_async(investigationNode::apply));
     * graph.addNode("review",   node_async(reviewNode::apply));
     * graph.addNode("rules",    node_async(rulesNode::apply));
     * graph.addNode("hitl",     node_async(hitlCheckpointNode::apply));
     *
     * graph.addEdge(START, "classify");
     * graph.addEdge("classify", "order");
     * graph.addEdge("order", "dispatch");
     *
     * // If dispatch found nothing left to process, go straight to END;
     * // otherwise walk this ticket through the rest of the pipeline.
     * graph.addConditionalEdges("dispatch",
     *     edge_async(state -> state.getCurrentTicketId() != null ? "process" : "end"),
     *     Map.of("process", "investigate", "end", END));
     *
     * graph.addEdge("investigate", "review");
     * graph.addEdge("review", "rules");
     * graph.addEdge("rules", "hitl");
     *
     * // After hitl, loop back to dispatch while tickets remain.
     * graph.addConditionalEdges("hitl",
     *     edge_async(state -> state.hasRemainingTickets() ? "continue" : "end"),
     *     Map.of("continue", "dispatch", "end", END));
     *
     * this.compiledGraph = graph.compile();
     */
    // NOT annotated with @Bean on purpose: a bean that throws at
    // construction would crash the whole Spring context on startup,
    // blocking every other unrelated endpoint (M1-M6) while this graph
    // wiring is still being finished. Turn this into a real @Bean
    // returning CompiledGraph<TriageGraphState> only once the wiring
    // above compiles against your langgraph4j-core version - then
    // inject it into TriageOrchestratorService to replace the Phase 4
    // TODO left in startBatch().
    public Object buildCompiledGraphOnceWiringIsFinished() {
        throw new UnsupportedOperationException(
                "TriageGraphConfig: wire the real langgraph4j StateGraph here " +
                        "(see class-level TODO) before calling this method.");
    }
}