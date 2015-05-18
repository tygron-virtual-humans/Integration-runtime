package goal.core.gam;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * A Belief contains information about actions of a particular Agent and about
 * how much those actions affect goals.
 */
public class Belief {

    private double likelihood;
    private Agent causalAgentObject;
    private HashMap<Goal, Double> goalCongruenceMap;
    private boolean isIncremental;

    /**
     * This class is a data structure to store one Belief for an agent. A belief
     * is created and fed into a Gamygdala instance (method
     * Gamygdala.appraise()) for evaluation
     *
     * @param likelihood The likelihood of this belief to be true.
     * @param agent The Agent object of the causal agent of this belief.
     * @param affectedGoals An array of affected goals.
     * @param goalCongruences An array of the affected goals' congruences (i.e.,
     *            the extend to which this event is good or bad for a goal
     *            [-1,1]).
     * @param isIncremental Incremental evidence enforces gamygdala to see this
     *            event as incremental evidence for (or against) the list of
     *            goals provided, i.e, it will add or subtract this belief's
     *            likelihood*congruence from the goal likelihood instead of
     *            using the belief as "state" defining the absolute likelihood
     */
    public Belief(double likelihood, Agent agent, ArrayList<Goal> affectedGoals, ArrayList<Double> goalCongruences, boolean isIncremental) {
        if (isIncremental) {
            this.isIncremental = isIncremental;
        } else {
            this.isIncremental = false;
        }

        this.likelihood = Math.min(1, Math.max(-1, likelihood));
        this.causalAgentObject = agent;

        this.goalCongruenceMap = new HashMap<Goal, Double>();

        if (affectedGoals.size() != goalCongruences.size()) {
            Gamygdala.debug("Error: the congruence list is not of the same size " + "as the affected goal list.");
            return;
        }

        // Add goals and congruences to Map.
        for (int i = 0; i < affectedGoals.size(); i++) {
            double congruence = Math.min(1, Math.max(-1, goalCongruences.get(i)));
            this.goalCongruenceMap.put(affectedGoals.get(i), congruence);
        }
    }

    /**
     * Get the likelihood of this belief.
     *
     * @return the likelihood
     */
    public double getLikelihood() {
        return likelihood;
    }

    // /**
    // * Set the likelihood of this belief.
    // *
    // * @param likelihood the likelihood to set
    // */
    // public void setLikelihood(double likelihood) {
    // this.likelihood = likelihood;
    // }

    /**
     * Get the name of the causal Agent.
     *
     * @return the causalAgentName
     */
    public Agent getCausalAgent() {
        return causalAgentObject;
    }

    // /**
    // * Set the name of the causal Agent.
    // *
    // * @param causalAgentName the causalAgentName to set
    // */
    // public void setCausalAgentName(String causalAgentName) {
    // this.causalAgentName = causalAgentName;
    // }

    /**
     * Get the names of the goals affected and their congruences.
     *
     * @return The names of the goals affected and their congruences.
     */
    public HashMap<Goal, Double> getGoalCongruenceMap() {
        return goalCongruenceMap;
    }

    /**
     * Return whether or not belief is incremental.
     *
     * @return the isIncremental
     */
    public boolean isIncremental() {
        return isIncremental;
    }

    // /**
    // * Set whether or not belief is incremental.
    // *
    // * @param isIncremental the isIncremental to set
    // */
    // public void setIncremental(boolean isIncremental) {
    // this.isIncremental = isIncremental;
    // }

    /**
     * Return string representation of Belief.
     */
    public String toString() {
        String str = "<Belief[CausalAgent = " + causalAgentObject + ", likelihood = " + likelihood + ", incremental = " + isIncremental + "]>";
        return str;
    }

}
