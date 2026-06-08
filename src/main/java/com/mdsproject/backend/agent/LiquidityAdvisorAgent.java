package com.mdsproject.backend.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AI service: the Predictive Liquidity (Treasury) Agent.
 * Given a cash forecast and a list of non-cash assets that could be converted to cash,
 * it produces a human-friendly shortfall warning and ranks which internal ledger swaps
 * to perform first so the group avoids failed transactions.
 */
public interface LiquidityAdvisorAgent {

    @SystemMessage("""
            You are the Predictive Liquidity Agent (Treasury Agent) for a shared group wallet app.
            Liquid cash is the only thing that can settle an outgoing transaction. Non-cash assets
            (gift cards / vouchers / airline miles) cannot pay merchants directly, but they can be
            swapped INTERNALLY for cash on the group ledger (e.g. a member trades a gift card to the
            group treasury in exchange for cash).

            You are given:
              - available cash, upcoming obligations and the projected balance (negative = shortfall)
              - a numbered list of swap candidates (non-cash assets), each with an estimated EUR value
                and days until expiry.

            Decide the smallest set of swaps whose combined value covers the shortfall, then rank them.
            Prefer assets that expire soonest, then the smallest value that still covers the gap, to
            keep the group's flexibility. Be concise, factual and slightly reassuring.

            Return ONLY valid JSON, no markdown, with this exact shape:
            {
              "severity": "OK" | "WATCH" | "CRITICAL",
              "message": "<1-2 sentence warning the group will read in chat>",
              "recommendedSwapIndexes": [0, 2],
              "rationale": "<1 sentence on why these swaps, in plain language>"
            }
            severity is CRITICAL when there is a shortfall, WATCH when cash is tight (projected
            balance below 20% of obligations) and OK otherwise. recommendedSwapIndexes references the
            candidate list by index and must be empty when severity is OK.
            """)
    @UserMessage("""
            Group: {{groupName}}
            Available cash (EUR): {{availableCash}}
            Upcoming obligations (EUR): {{upcomingObligations}}
            Projected balance after obligations (EUR): {{projectedBalance}}
            Shortfall (EUR, 0 if none): {{shortfall}}

            Swap candidates (non-cash assets that can be converted to cash):
            {{swapCandidates}}

            Produce the liquidity assessment as JSON only.
            """)
    String assessLiquidityJson(
            @V("groupName") String groupName,
            @V("availableCash") double availableCash,
            @V("upcomingObligations") double upcomingObligations,
            @V("projectedBalance") double projectedBalance,
            @V("shortfall") double shortfall,
            @V("swapCandidates") String swapCandidates
    );
}
