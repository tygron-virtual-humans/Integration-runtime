package goal.core.gamygdala;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
	
	public HashSet<String> getAffectedIndividualGoal(String mainGoalName, String agentId) {
		String key = mainGoalName + agentId;
		if(this.containsKey(key)) {
			return this.get(key);
		} else {
			return new HashSet<String>();
		}
	}
	
	public void addIndividualGoal(String goalName, String agentId, String fullName) { 
		String key = goalName + agentId;
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
	
	public void addCommonGoal(String goalName, String fullName) { 
		String key = goalName;
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
	
	public void removeCommonGoal(String goalName, String fullName) {
		if(this.containsKey(goalName)) {
			HashSet<String> toEdit = this.get(goalName);
			if(toEdit.contains(fullName)) {
				toEdit.remove(fullName);
				this.put(goalName, toEdit);
			}
		}
	}
	
	public void removeIndividualGoal(String goalName, String agentId, String fullName) {
		String key = goalName + agentId;
		if(this.containsKey(key)) {
			HashSet<String> toEdit = this.get(key);
			if(toEdit.contains(fullName)) {
				toEdit.remove(fullName);
				this.put(key, toEdit);
			}
		}
	}
}
