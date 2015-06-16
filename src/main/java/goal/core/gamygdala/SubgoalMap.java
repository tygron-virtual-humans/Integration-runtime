package goal.core.gamygdala;

import goal.core.mentalstate.SingleGoal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import languageTools.parser.relationParser.EmotionConfig;
import languageTools.parser.relationParser.GamSubGoal;

/**
 * This map maintains data needed to appraise maingoals of subgoals.
 *
 */
public class SubgoalMap extends HashMap<String, HashSet<String>>{
	public HashSet<String> getAffectedCommonGoals(String mainGoalName) {
		if(this.containsKey(mainGoalName))  {
			return this.get(mainGoalName);
		} else {
			return new HashSet<String>();
		}
	}
	
	/**
	 * Return the set of affectedgoals when given a subgoal and a the agent that is trying to appraise this subgoal
	 * @param subGoal subgoal to get affected goals from
	 * @param agent agents that's trying to appraise these goals
	 * @return set of affectedgoalnames
	 */
	public HashSet<String> getAffectedGoals(GamSubGoal subGoal, String agent) {
		boolean isIndividual = EmotionConfig.getInstance().getGoal(subGoal.getAffectedGoalName(), agent).isIndividualGoal();
		if(isIndividual) { //if the goal is individual then we need to add the name of the agent to each returned goal since we add individual goals to gamygdala that way to make them unique
			HashSet<String> temp = Engine.getInstance().getSubgoalMap().getAffectedIndividualGoal(subGoal.getAffectedGoalName(), agent);
			HashSet<String> res = new HashSet<String>();
			Iterator<String > it = temp.iterator();
			while(it.hasNext()) {
				res.add(it.next() + agent);
			}
			return res;
		} else { //else we can just return the goal name since if it is a common goal it does not need to be unique between agents
			return Engine.getInstance().getSubgoalMap().getAffectedCommonGoals(subGoal.getAffectedGoalName());
		}
	}
	
	/**
	 * Returns the set of affected goals for a goal that is individual
	 * @param mainGoalName the name of the goal we're trying to get (this should be the general name so on a singlegoal goal.getGoal().getSignature())
	 * @param agentId the agent that is trying to get the affected goals
	 * @return
	 */
	public HashSet<String> getAffectedIndividualGoal(String mainGoalName, String agentId) {
		String key = mainGoalName + agentId;
		if(this.containsKey(key)) {
			return this.get(key);
		} else {
			return new HashSet<String>();
		}
	}
	
	/**
	 * Add a individual goal to the subgoalmap
	 * @param goalName the general name of the goal (on a singlegoal goal.getGoal().getSignature())
	 * @param agentId the name of the agent that is adding the goal
	 * @param fullName the full name of the goal including the parameters of the goal
	 */
	public void addIndividualGoal(String goalName, String agentId, String fullName) { 
		String key = goalName + agentId; //it's an individual goal so it will be saved to the subgoalmap under goalName + agentId;
		if(this.containsKey(key)) {
			HashSet<String> toEdit = this.get(key);
			toEdit.add(fullName + agentId);
			this.put(key, toEdit);
			
		} else {
			HashSet<String> toPut = new HashSet<String>();
			toPut.add(fullName);
			this.put(key, toPut);
		}
	}
	
	/**
	 * Adds a common goal to the subgoalmap
	 * @param goalName the general name of the goal (on a singlegoal goal.getGoal().getSignature())
	 * @param fullName the full name of the goal including the parameters of the goal
	 */
	public void addCommonGoal(String goalName, String fullName) { 
		String key = goalName; //it's a common goal so it will just be saved under the goalname
		if(this.containsKey(key)) {
			HashSet<String> toEdit = this.get(key);
			toEdit.add(fullName);
			this.put(key, toEdit);
			
		} else {
			HashSet<String> toPut = new HashSet<String>();
			toPut.add(fullName);
			this.put(key, toPut);
		}
	}
	
	/**
	 * removes a common goal from the subgoalmap
	 * @param goalName the general name of the goal (on a singlegoal goal.getGoal().getSignature())
	 */
	public void removeCommonGoal(String goalName, String fullName) {
		if(this.containsKey(goalName)) {
			HashSet<String> toEdit = this.get(goalName);
			if(toEdit.contains(fullName)) {
				toEdit.remove(fullName);
				this.put(goalName, toEdit);
			}
		}
	}
	
	/**
	 * removes an indivudla goal from the subgoalmap
	 * @param goalName the general name of the goal (on a singlegoal goal.getGoal().getSignature())
	 * @param agentId agent for who the individual goal should be removed
	 * @param fullName the full name of the goal including the parameters of the goal
	 */
	public void removeIndividualGoal(String goalName, String agentId, String fullName) {
		String key = goalName + agentId;
		if(this.containsKey(key)) {
			HashSet<String> toEdit = this.get(key);
			if(toEdit.contains(fullName + agentId)) {
				toEdit.remove(fullName + agentId);
				this.put(key, toEdit);
			}
		}
	}
	
	/**
	 * remove a singleGoal from the subgoalmap, it will check whether the goal is individual or not in the emotionconfig
	 * and based on that correctly remove the goal from the subgoalmap
	 * @param goal the SingleGoal to be removed
	 * @param agentName the name of the agent for who it should be removed
	 */
	public void removeGoal(SingleGoal goal, String agentName) {
		boolean isIndividual = EmotionConfig.getInstance().getGoal(goal.getGoal().getSignature(), agentName).isIndividualGoal();
		if(isIndividual) {
			Engine.getInstance().getSubgoalMap().removeIndividualGoal(goal.getGoal().getSignature(), agentName, goal.getGoal().getAddList().get(0).toString());
		} else {
			Engine.getInstance().getSubgoalMap().removeCommonGoal(goal.getGoal().getSignature(), goal.getGoal().getAddList().get(0).toString());
		}
	}
}
