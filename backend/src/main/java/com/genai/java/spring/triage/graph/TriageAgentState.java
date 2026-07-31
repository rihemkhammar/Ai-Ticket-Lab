package com.genai.java.spring.triage.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

/**
 * Adapts our existing plain {@link TriageGraphState} POJO (7 nodes,
 * getters/setters, already unit-tested independently of langgraph4j -
 * Rule 2.9) to the AgentState/Map<String,Object> model that
 * org.bsc.langgraph4j.StateGraph actually requires.
 *
 * We do NOT rewrite TriageGraphState as a Map of individual channels:
 * we store the whole object under one key and let each node mutate it
 * in place, same as TriagePipelineService did manually before. The
 * schema below uses Channels.base(...), i.e. plain "last write wins"
 * semantics - no merging/appending needed since there's a single
 * writer per step.
 */
public class TriageAgentState extends AgentState {

    /** Key under which the whole TriageGraphState instance lives. */
    public static final String STATE_KEY = "triageState";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            STATE_KEY, Channels.base(() -> new TriageGraphState())
    );

    public TriageAgentState(Map<String, Object> initData) {
        super(initData);
    }

    /**
     * Convenience accessor used by every node wrapper in
     * TriageGraphConfig instead of repeating value(STATE_KEY) casts.
     */
    public TriageGraphState triageState() {
        return this.<TriageGraphState>value(STATE_KEY)
                .orElseThrow(() -> new IllegalStateException(
                        "TriageAgentState: '" + STATE_KEY + "' missing - " +
                                "graph must always be invoked with an initial TriageGraphState."));
    }
}
