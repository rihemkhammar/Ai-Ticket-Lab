package com.genai.java.spring.triage.graph;

import com.genai.java.spring.triage.TicketCriticality;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Wires the LangGraph4j StateGraph for the triage pipeline:
 *
 *   START -> Classify -> Order -> Dispatch
 *              -> (conditional: is the dispatched ticket CRITICAL/HIGH?)
 *                   -> Investigation -> Review
 *                   -> Review directly (skip Investigation)
 *              -> Rules -> Hitl
 *              -> (conditional: orderedQueue non-empty? loop back to
 *                  Dispatch : END)
 *
 * This reproduces EXACTLY the manual chaining that used to live in
 * TriagePipelineService.startAndRun() (including the "skip Agent 2
 * unless CRITICAL/HIGH" branch), so behavior is unchanged - only the
 * orchestration mechanism moves from a hand-written while-loop to a
 * compiled graph.
 *
 * Each node stays a plain, independently unit-testable Spring bean
 * (ClassifyTicketsNode, OrderQueueNode, ...); this class only concerns
 * itself with graph wiring (Rule 2.9).
 *
 * IMPORTANT (version-sensitive API): this file targets
 * langgraph4j-core 1.8.20 as pinned in pom.xml. StateGraph is
 * parameterized by an AgentState (Map<String,Object> + Channel
 * schema), not by TriageGraphState directly - see TriageAgentState.
 * The exact invoke(...) call shape used by TriagePipelineService also
 * changed across releases (Map<String,Object> input + Optional<S>
 * result vs typed initial state + CompletableFuture<S>) - re-check
 * against this version's javadoc/README before relying on it blindly.
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

    @Bean
    public CompiledGraph<TriageAgentState> triageGraph() throws GraphStateException {
        StateGraph<TriageAgentState> graph =
                new StateGraph<>(TriageAgentState.SCHEMA, TriageAgentState::new);

        graph.addNode("classify", asNode(classifyTicketsNode::apply));
        graph.addNode("order", asNode(orderQueueNode::apply));
        graph.addNode("dispatch", asNode(dispatchNextTicketNode::apply));
        graph.addNode("investigate", asNode(investigationNode::apply));
        graph.addNode("review", asNode(reviewNode::apply));
        graph.addNode("rules", asNode(rulesNode::apply));
        graph.addNode("hitl", asNode(hitlCheckpointNode::apply));

        graph.addEdge(START, "classify");
        graph.addEdge("classify", "order");
        graph.addEdge("order", "dispatch");

        // Same branch as TriagePipelineService's old
        // isCriticalEnoughForInvestigation(state) check.
        graph.addConditionalEdges("dispatch",
                edge_async(this::routeAfterDispatch),
                Map.of("investigate", "investigate",
                        "review", "review",
                        "end", END));

        graph.addEdge("investigate", "review");
        graph.addEdge("review", "rules");
        graph.addEdge("rules", "hitl");

        // Same loop condition as the old while (state.getCurrentTicketId() != null).
        graph.addConditionalEdges("hitl",
                edge_async(state -> state.triageState().hasRemainingTickets() ? "continue" : "end"),
                Map.of("continue", "dispatch", "end", END));

        return graph.compile();
    }

    /**
     * Wraps a plain TriageGraphState -> TriageGraphState node (the
     * existing, already-tested apply(...) methods) into the
     * AsyncNodeAction<TriageAgentState> shape langgraph4j needs,
     * without touching any of the 7 node classes themselves.
     */
    private org.bsc.langgraph4j.action.AsyncNodeAction<TriageAgentState> asNode(
            UnaryOperator<TriageGraphState> nodeLogic) {
        return node_async(agentState -> {
            TriageGraphState updated = nodeLogic.apply(agentState.triageState());
            return Map.of(TriageAgentState.STATE_KEY, updated);
        });
    }

    private String routeAfterDispatch(TriageAgentState agentState) {
        TriageGraphState state = agentState.triageState();
        if (state.getCurrentTicketId() == null) {
            return "end";
        }
        return isCriticalEnoughForInvestigation(state) ? "investigate" : "review";
    }

    private boolean isCriticalEnoughForInvestigation(TriageGraphState state) {
        TriageClassification classification = state.getClassifications().get(state.getCurrentTicketId());
        if (classification == null || classification.getCriticality() == null) {
            return false;
        }
        return classification.getCriticality() == TicketCriticality.CRITICAL
                || classification.getCriticality() == TicketCriticality.HIGH;
    }
}
