package goal.core.gamygdala;

import goal.core.mentalstate.SingleGoal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import languageTools.exceptions.relationParser.InvalidGamSubGoalException;
import languageTools.parser.relationParser.EmotionConfig;
import languageTools.parser.relationParser.GamGoal;
import languageTools.parser.relationParser.GamRelation;
import languageTools.parser.relationParser.GamSubGoal;

/**
 * Gaming Engine adapter for Gamygdala.
 */
public class Engine {

    /**
     * Singleton Engine object.
     */
    private static Engine engineInstance;

    /**
     * Debug flag.
     */
    public static final boolean DEBUG = false;

    /**
     * Gamygdala instance.
     */
    private Gamygdala gamygdala = new Gamygdala();

    /**
     * Timestamp of last emotion calculation.
     */
    private long lastMillis = System.currentTimeMillis();

    /**
     * Empty constructor to prevent instantiating.
     * Use Engine.getInstance() instead.
     */
    private Engine() {
    }

    /**
     * Get the Engine object. If no Engine has been instantiated,
     * create a new Engine with a fresh Gamygdala instance.
     */
    public static synchronized Engine getInstance() {

        if (engineInstance == null) {
            engineInstance = new Engine();
        }

        return engineInstance;
    }

    public static Engine reset() {
        if (engineInstance != null) {
            synchronized (Engine.class) {
                if (engineInstance != null) {
                    engineInstance = new Engine();
                }
            }
        }

        return engineInstance;
    }

    /**
     * Create and add an Agent to the Engine.
     *
     * @param name The name of the Agent.
     */
    public Agent createAgent(String name) {
        Agent agent = new Agent(name);
        gamygdala.getGamygdalaMap().registerAgent(agent);
        return agent;
    }

    /**
     * Add a Goal to an Agent and register with the Engine.
     * 
     * @param agent The Agent to register the Goal with.
     * @param goalName The name of the new Goal.
     * @param goalUtility The utility of the new Goal.
     * @param isMaintenanceGoal Whether or not this Goal is a maintenance goal.
     * @return The newly created Goal.
     */
    public Goal createGoalForAgent(Agent agent, String goalName, double goalUtility,
                    boolean isMaintenanceGoal) {
        Goal goal = new Goal(goalName, goalUtility, isMaintenanceGoal);

        // Add Goal to Agent
        agent.addGoal(goal);

        // Register Goal with Engine
        gamygdala.getGamygdalaMap().registerGoal(goal);

        return goal;

    }
    
    /**
     * When given a SingeGoal and an agent as input then this function will create a
     *  correct goal for this agent (or a common goal if it is defined as such) in 
     *  gamygdala based on the values present in the emotionconfig
     * @param goal goal for which a gamygdala goal needs to be created
     * @param gamAgent agent for whom it is being created
     * @return the created goal
     */
    public Goal createGoalForAgent(SingleGoal goal, Agent gamAgent) {
    	GamGoal gamGoal = EmotionConfig.getInstance().getGoal(goal.getGoal().getSignature(), gamAgent.name);
		if(gamGoal.isIndividualGoal()) {
		 Engine.getInstance().getSubgoalMap().addIndividualGoal(goal.getGoal().getSignature(), gamAgent.name, goal.getGoal().getAddList().get(0).toString());
		 return Engine.getInstance().createGoalForAgent(gamAgent,goal.getGoal().getAddList().get(0).toString() + gamAgent.name,gamGoal.getValue(),false);
		} else {
		 Engine.getInstance().getSubgoalMap().addCommonGoal(goal.getGoal().getSignature(), goal.getGoal().getAddList().get(0).toString());
		 return Engine.getInstance().createGoalForAgent(gamAgent,goal.getGoal().getAddList().get(0).toString(),gamGoal.getValue(),false);
		}	
    }

    /**
     * Create a relation with between two Agents.
     * 
     * @param source The source Agent.
     * @param target The target Agent.
     * @param relation The intensity of the relation.
     */
    public void createRelation(Agent source, Agent target, double relation) {
        source.updateRelation(target, relation);
    }

    /**
     * This method decays for all registered agents the emotional state and
     * relations. It performs the decay according to the time passed, so longer
     * intervals between consecutive calls result in bigger clunky steps.
     * Typically this is called automatically when you use startDecay(), but you
     * can use it yourself if you want to manage the timing. This function is
     * keeping track of the millis passed since the last call, and will (try to)
     * keep the decay close to the desired decay factor, regardless the time
     * passed So you can call this any time you want (or, e.g., have the game
     * loop call it, or have e.g., Phaser call it in the plugin update, which is
     * default now). Further, if you want to tweak the emotional intensity decay
     * of individual agents, you should tweak the decayFactor per agent not the
     * "frame rate" of the decay (as this doesn't change the rate).
     */
    public void decayAll() {

        // Record current time
        long now = System.currentTimeMillis();

        // Decay all emotions.
        gamygdala.decayAll(this.lastMillis, now);

        // Store time of last decay.
        this.lastMillis = now;

    }

    /**
     * The main emotional interpretation logic entry point. Performs the
     * complete appraisal of a single event (belief) for all agents.
     *
     * @param belief The current event to be appraised.
     */
    public void appraise(Belief belief) {
        gamygdala.appraise(belief, null);
    }

    /**
     * The main emotional interpretation logic entry point. Performs the
     * complete appraisal of a single event (belief) for only one agent. The
     * complete appraisal logic is executed including the effect on relations
     * (possibly influencing the emotional state of other agents), but only if
     * the affected agent (the one owning the goal) == affectedAgent. This is
     * sometimes needed for efficiency. If you as a game developer know that
     * particular agents can never appraise an event, then you can force
     * Gamygdala to only look at a subset of agents. Gamygdala assumes that the
     * affectedAgent is indeed the only goal owner affected and will not perform
     * any checks, nor use Gamygdala's list of known goals to find other agents
     * that share this goal.
     *
     * @param belief The current event to be appraised.
     * @param agent The reference to the agent who appraises the event.
     */
    public void appraise(Belief belief, Agent agent) {
        gamygdala.appraise(belief, agent);
    }

    /**
     * Set the gain for the whole set of agents known to Gamygdala. For more
     * realistic, complex games, you would typically set the gain for each agent
     * type separately, to finetune the intensity of the response.
     *
     * @param gain The gain value [0 and 20].
     */
    public boolean setGain(double gain) {

        if (gain <= 0 || gain > 20) {
            Engine.debug("[Engine.setGain] Error: "
                            + "gain factor for appraisal integration must be between 0 and 20.");
            return false;
        }

        Iterator<Entry<String, Agent>> it = gamygdala.getGamygdalaMap().getAgentIterator();
        Agent temp;
        while (it.hasNext()) {
            Map.Entry<String, Agent> pair = it.next();

            temp = pair.getValue();
            if (temp != null) {
                temp.setGain(gain);
            } else {
                it.remove();
            }

        }

        return true;
    }

    /**
     * Sets the decay factor and type for emotional decay, so that an emotion
     * will slowly get lower in intensity. Whenever decayAll is called, all
     * emotions for all agents are decayed according to the factor and function
     * set here.
     *
     * @param decayFactor The decay factor. A factor of 1 means no decay.
     * @param decayFunction The decay function to be used.
     */
    public void setDecay(double decayFactor, DecayFunction decayFunction) {

        gamygdala.setDecayFactor(decayFactor);

        if (decayFunction != null) {
            gamygdala.setDecayFunction(decayFunction);
        } else {
            Engine.debug("[Engine.setDecay] DecayFunction is null.");
        }

    }

    /**
     * Get the Gamygdala instance for this Engine.
     * 
     * @return The Gamygdala instance.
     */
    public Gamygdala getGamygdala() {
        return gamygdala;
    }

    /**
     * Set a new Gamygdala instance for this Engine.
     * 
     * @param gamygdala The Gamygdala instance.
     */
    public void setGamygdala(Gamygdala gamygdala) {
        this.gamygdala = gamygdala;
    }
    
    /**
     * Returns an agent wit the input string as a name if it is present in gamygdala
     * @param name name of the agent to retrieve
     * @return agent with the input name
     */
    public Agent getAgentByName(String name){
    	return gamygdala.getMap().getAgentMap().getAgentByName(name);
    }
    
    /**
     * Returns a goal with the input as a name if it is present in gamygdala
     * @param name name of the goal to retrieve
     * @return goal with the input name
     */
    public Goal getGoalByName(String name){
    	return gamygdala.getMap().getGoalMap().getGoalByName(name);
    }
    
    /**
     * Retrieves the goal corresponding to the equivalent goal of the SingleGoal owned by an agent with the name agentName
     * (unless it is defined as a common goal in the emotionconfig the the common goal will be returned)
     * @param goal goal for which the equivalent gamygdala goal should be retrieved
     * @param agentName the name of the agent to whom this goal should belong
     * @return the goal
     */
    public Goal getGoal(SingleGoal goal, String agentName){
    	boolean isIndividual = EmotionConfig.getInstance().getGoal(goal.getGoal().getSignature(), agentName).isIndividualGoal();
		if(isIndividual) {
		 return  this.getInstance().getGoalByName(goal.getGoal().getAddList().get(0).toString() + agentName);
		}
		else {
		 return this.getInstance().getGoalByName(goal.getGoal().getAddList().get(0).toString());
		}
    }
    
    /**
     * Returns the internal map of gamygdala
     * @return
     */
    public GamygdalaMap getMap(){
    	return gamygdala.getMap();
    }
    
    /**
     * Inserts relations into gamygdala according to how they're defined in the emotionconfiguration
     */
    public static void insertRelations(){
		// getting both instances
		EmotionConfig emo = EmotionConfig.getInstance();
		Engine engine = Engine.getInstance();
		
		//for all relations
		ArrayList<GamRelation> map = emo.getRelations();
		for(int i=0; i<map.size();i++){
			
			//create agents via their names
			GamRelation relation = map.get(i);
			Agent agent1 = Engine.getInstance().getAgentByName(relation.getAgent1());
			if(agent1 == null) {
			 agent1 = Engine.getInstance().createAgent(relation.getAgent1());
			}
			Agent agent2 = Engine.getInstance().getAgentByName(relation.getAgent2());
			if(agent2 == null) {
			 agent2 = Engine.getInstance().createAgent(relation.getAgent2());
			}
			//add relation to gamygdala instance
 			engine.createRelation(agent1, agent2, relation.getValue());
		}
	}
    
    /**
     * Facilitator method to print all emotional states to the console.
     *
     * @param gain Whether you want to print the gained (true) emotional states
     *            or non-gained (false).
     */
    public void printAllEmotions(boolean gain) {

        Iterator<Entry<String, Agent>> it = gamygdala.getGamygdalaMap().getAgentIterator();
        Agent agent;
        while (it.hasNext()) {
            Map.Entry<String, Agent> pair = it.next();
            agent = pair.getValue();

            agent.printEmotionalState(gain);
            agent.printRelations(null);
        }
    }

    /**
     * Print debug information to console if the debug flag is set to true.
     *
     * @param what Object to print to console.
     */
    public static void debug(Object what) {
        if (Engine.DEBUG) {
            System.out.println(what);
        }
    }
    
    /**
     * @return returns the subgoalmap associated with this gamygdala engine
     */
    public SubgoalMap getSubgoalMap() {
    	return gamygdala.getSubgoalMap();
    }
    
    /**
	 * appraise a goal SingleGoal in gamygdala according to the configurations in emotionConfig
	 * @param agent
	 * @param goal
	 */
	public void appraiseGoal(Agent agent, SingleGoal goal) {
		Engine gam = Engine.getInstance();
		EmotionConfig config = EmotionConfig.getInstance();
		Goal gamGoal = gam.getGoal(goal, agent.name);
		if(gamGoal != null) { //if it is null then the agent did not have this goal
			ArrayList<Goal> affectedGoals = new ArrayList<Goal>();
			affectedGoals.add(gamGoal); //we're appraising only this goal so the affected goals are the goal itself
			ArrayList<Double> congruences = new ArrayList<Double>();
			congruences.add(config.getDefaultPositiveCongruence());
			try {
				Belief bel = new Belief(config.getDefaultBelLikelihood(), agent, affectedGoals, congruences, config.isDefaultIsIncremental());
				gam.appraise(bel);
			} catch (GoalCongruenceMapException e) {
				e.printStackTrace();
			}
			agent.removeGoal(gamGoal); //remove the goals from gamygdala
			gam.getMap().getGoalMap().remove(gamGoal.getName());
			gam.getSubgoalMap().removeGoal(goal, agent.name);
		}
	}
	
	/**
	 * apparaises a goals as a subgoal according to the configuration given in the emotionconfig
	 * @param agent
	 * @param goal
	 */
	public void appraiseGoalAsSubgoal(Agent agent, SingleGoal goal) {
		if(EmotionConfig.getInstance().getSubGoals().containsKey(goal.getGoal().getSignature())) {	
		 Engine gam = Engine.getInstance();
		 EmotionConfig config = EmotionConfig.getInstance();
		 ArrayList<GamSubGoal> subGoals;
		 try {
			 subGoals = config.getSubGoal(goal.getGoal().getSignature());
			for(int i = 0; i<subGoals.size(); i++) { //we should loop through all subgoals and appraise them
				GamSubGoal currSub = subGoals.get(i);
				HashSet<String> affectedNames = new HashSet<String>();
				affectedNames = gam.getSubgoalMap().getAffectedGoals(currSub, agent.name);
				Iterator<String> it = affectedNames.iterator();
				while(it.hasNext()) { //appraise the affected goals one by one
					String affectedName = it.next();
					if(gam.getMap().getGoalMap().containsKey(affectedName)) {
						Goal affectedGoal = gam.getGoalByName(affectedName);
						ArrayList<Goal> affectedGoals = new ArrayList<Goal>();
						affectedGoals.add(affectedGoal);
						ArrayList<Double> congruences = new ArrayList<Double>();
						congruences.add(currSub.getCongruence());
						Belief bel = new Belief(currSub.getLikelihood(), agent, affectedGoals, congruences, currSub.isIncremental());
						gam.appraise(bel);
					}
				}
			}
		} catch (InvalidGamSubGoalException e) {
		 e.printStackTrace();
	    } catch (GoalCongruenceMapException e) {
			e.printStackTrace();
		}	
		
	}
				
}

}
