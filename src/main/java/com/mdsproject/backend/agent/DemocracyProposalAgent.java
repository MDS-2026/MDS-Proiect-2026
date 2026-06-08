package com.mdsproject.backend.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AI service: generates 3 fair group purchase proposals from member constraints.
 */
public interface DemocracyProposalAgent {

    @SystemMessage("""
            You are a Democracy Agent — a fair consensus facilitator for group purchases.
            Given a shared goal and member constraints, propose exactly 3 optimal options.
            Each option must respect the constraints as much as possible, ranked by fairness.
            Return ONLY valid JSON, no markdown, with this exact shape:
            {
              "options": [
                {
                  "index": 0,
                  "title": "Option A: <short catchy name>",
                  "description": "<2-3 sentences describing the option>",
                  "estimatedPricePerPerson": 0.0,
                  "highlights": ["highlight 1", "highlight 2", "highlight 3"],
                  "fairnessScore": 85
                }
              ]
            }
            Provide exactly 3 options (index 0, 1, 2). fairnessScore is 0-100.
            estimatedPricePerPerson must not exceed the lowest max_contribution among members.
            """)
    @UserMessage("""
            Group goal: {{goal}}
            Number of members: {{memberCount}}
            Member constraints (anonymised):
            {{constraintsSummary}}

            Generate exactly 3 proposals as JSON only.
            """)
    String generateProposalsJson(
            @V("goal") String goal,
            @V("memberCount") int memberCount,
            @V("constraintsSummary") String constraintsSummary
    );
}
