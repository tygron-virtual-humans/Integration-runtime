/**
 * GOAL interpreter that facilitates developing and executing GOAL multi-agent
 * programs. Copyright (C) 2011 K.V. Hindriks, W. Pasman
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package goal.core.runtime.service.agent;

import eis.exceptions.EnvironmentInterfaceException;
import eis.iilang.Percept;
import goal.core.agent.Agent;
import goal.core.agent.AgentId;
import goal.core.agent.EnvironmentCapabilities;
import goal.core.agent.LoggingCapabilities;
import goal.core.agent.MessagingCapabilities;
import goal.core.kr.KRlanguage;
import goal.core.kr.language.DatabaseFormula;
import goal.core.kr.language.Update;
import goal.core.mentalstate.BASETYPE;
import goal.core.mentalstate.MentalState;
import goal.core.mentalstate.SingleGoal;
import goal.core.program.GOALProgram;
import goal.core.program.Message;
import goal.core.program.Module;
import goal.core.program.Module.TYPE;
import goal.core.program.actions.LogAction;
import goal.core.program.actions.ModuleCallAction;
import goal.core.program.actions.UserSpecAction;
import goal.core.program.rules.RuleSet;
import goal.core.program.rules.RuleSet.RuleEvaluationOrder;
import goal.core.runtime.service.environmentport.EnvironmentPort;
import goal.preferences.PMPreferences;
import goal.tools.adapt.FileLearner;
import goal.tools.adapt.Learner;
import goal.tools.debugger.Channel;
import goal.tools.debugger.Debugger;
import goal.tools.debugger.DebuggerKilledException;
import goal.tools.debugger.SteppingDebugger;
import goal.tools.debugger.SteppingDebugger.RunMode;
import goal.tools.errorhandling.Resources;
import goal.tools.errorhandling.Warning;
import goal.tools.errorhandling.WarningStrings;
import goal.tools.errorhandling.exceptions.GOALBug;
import goal.tools.errorhandling.exceptions.GOALLaunchFailureException;
import goal.tools.errorhandling.exceptions.KRInitFailedException;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import nl.tudelft.goal.messaging.exceptions.MessagingException;
import nl.tudelft.goal.messaging.messagebox.MessageBox;

/**
 * The run state of an {@link Agent}.
 * <p>
 * Note that this has nothing to do with the {@link SteppingDebugger}'s
 * {@link RunMode} .A
 * </p>
 *
 * @param <D>
 *            The debugger type
 *
 * @author W.Pasman
 * @modified K.Hindriks
 */
public class RunState<D extends Debugger> {
	/**
	 * The agent's name. The name of an agent is derived from its
	 * {@link MessageBox}, but stored here in case the agent gets killed and its
	 * message box has been removed.
	 */
	private final AgentId agentName;
	/**
	 * The GOALProgram associated with this RunState.
	 */
	private final GOALProgram program;
	/**
	 * Counter for the number of times the agent's reasoning cycle has been
	 * performed.
	 * <p>
	 * Corresponds with the number of times that {@link Agent#processPercepts}
	 * is called.
	 * </p>
	 */
	private int roundCounter = 0;
	/**
	 * Trigger for the event module. The event module is only executed if this
	 * flag is {@code true}.
	 */
	private boolean event = false;
	/**
	 * Measure time this agent started running.
	 */
	private long start;
	/**
	 * The {@link MentalState} of the {@link AgentMesg}.
	 */
	private MentalState mentalState = null;

	private boolean usesMentalModels = false;
	/**
	 * The port to the environment.
	 */
	private final EnvironmentCapabilities environment;

	private final MessagingCapabilities messaging;

	private final LoggingCapabilities logActionsLogger;

	/**
	 * Previous input states recording inputs from previous round. Used to check
	 * whether agent can go to sleep.
	 */
	private Set<Percept> previousPercepts = new LinkedHashSet<>();
	private Set<Message> previousMessages = new LinkedHashSet<>();
	/**
	 * The goal that is focused on is stored temporarily in the run state for
	 * later reference when a {@link ModuleCallAction} is executed. The goal
	 * needs to be adopted as a side effect of evaluating the condition of a
	 * rule. It is stored here because only the {@link ModuleCallAction} uses
	 * the goal.
	 */
	private SingleGoal focusgoal = null;
	/**
	 * The {@link RunState} needs to provide the event module that will be
	 * executed at the start of a new reasoning cycle of the {@link Agent}.
	 */
	private Module initModule = null;
	private Module eventModule = null;
	private Module mainModule = null;
	/**
	 * Stack of (non-anonymous) modules that have been entered and not yet
	 * exited; last element on the list has been entered last.
	 */
	private final LinkedList<Module> activeStackOfModules = new LinkedList<>();
	/**
	 * Top level context in which we are running now; Each of three main
	 * built-in modules is considered a run context. We're assuming by default
	 * that we're in the main context.
	 */
	private TYPE topLevelRunContext = TYPE.MAIN;
	/**
	 * The debugger used for reporting debug events and for controlling and
	 * monitoring agent execution.
	 */
	private final D debugger;
	/**
	 * Learner that allows agent to learn from repeated trials.
	 */
	private final Learner learner;

	/**
	 * Keep track whether sleep condition held previous cycle.
	 */
	private boolean sleepConditionsHoldingPreviousCycle;

	/**
	 * Creates a new {@link RunState}.
	 *
	 * @param agentName
	 * @param environment
	 * @param messaging
	 * @param logger
	 *            the logger for {@link LogAction}s.
	 * @param program
	 * @param debugger
	 * @param learner
	 * @throws KRInitFailedException
	 */
	public RunState(AgentId agentName, EnvironmentCapabilities environment,
			MessagingCapabilities messaging, LoggingCapabilities logger,
			GOALProgram program, D debugger, Learner learner)
			throws KRInitFailedException {

		this.environment = environment;
		this.messaging = messaging;
		this.logActionsLogger = logger;

		// Get the built-in modules from the agent's program, if available.
		this.initModule = program.getModule().getModuleOfType(TYPE.INIT);
		this.eventModule = program.getModule().getModuleOfType(TYPE.EVENT);
		this.mainModule = program.getModule().getModuleOfType(TYPE.MAIN);

		// Check there is a main module; create a "dummy" one if main is absent.
		if (this.mainModule == null) {
			// program did not specify a main module; insert a fake one to make
			// sure event module is continually run.
			this.mainModule = new Module("main", null, TYPE.MAIN, null, //$NON-NLS-1$
					program.getKRLanguage());
			// Add an empty set of rules to the module.
			this.mainModule.setRuleSet(new RuleSet(RuleEvaluationOrder.LINEAR,
					null));
		}

		// Store reference to program for possible reset.
		this.program = program;

		this.agentName = agentName;

		this.debugger = debugger;
		// TODO: notify all GUI components that want to (should) subscribe to
		// debugger here? Would fix issue in {@link Agent#run}? Maybe create
		// an observable LaunchManager that informs about construction steps
		// before agent is actually started so we do not get racing conditions.

		// Create a new mental state for the agent.
		this.mentalState = new MentalState(this.getId(), program, debugger);
		this.usesMentalModels = program.usesMentalModels();

		// Configure learner.
		this.learner = learner;
	}

	/**
	 * Returns the name of the agent.
	 *
	 * @return String representing agent's name.
	 */
	public AgentId getId() {
		return this.agentName;
	}

	/**
	 * Returns the number of rounds that have been executed so far.
	 *
	 * @return The number of rounds.
	 */
	public int getRoundCounter() {
		return roundCounter;
	}

	/**
	 * Increase the round counter by one.
	 */
	public void incrementRoundCounter() {
		roundCounter++;
	}

	public long getStartTime() {
		return start;
	}

	public void setStartTime(long start) {
		this.start = start;
	}

	/**
	 * Returns the {@link MentalState} of the agent's {@link RunState}.
	 *
	 * @return The mental state of the agent.
	 */
	public MentalState getMentalState() {
		return this.mentalState;
	}

	/**
	 * Resetting is same as soft kill and replacing mental state with new
	 * initial mental state.
	 *
	 * TODO: merge kill and reset functionality, basically we have: - reset
	 * which now replaces mental state with initial one (TODO: possibly in the
	 * middle of an agent run(!); things are not so simple here, if we kill the
	 * agent's thread, environment entities, if any, are also freed up again,
	 * and a new agent is MAY be (re-)launched immediately instead of using this
	 * agent... (but only if the launch rules would still allow for it, which is
	 * not what we want). - soft kill that only kills agent thread - hard kill
	 * which kills agent thread, cleans up mental state, and kills connection
	 * with messaging service and TODO: environment.
	 *
	 * @throws GOALLaunchFailureException
	 *             DOC
	 * @throws KRInitFailedException
	 */
	public void reset() throws KRInitFailedException {
		roundCounter = 0;
		// Clean up old and create new initial mental state.
		this.mentalState.cleanUp();
		this.mentalState = new MentalState(this.getId(), program, debugger);
		//
		previousPercepts.clear();
		previousMessages.clear();
		messaging.reset();
		focusgoal = null;
		activeStackOfModules.clear();
		sleepConditionsHoldingPreviousCycle = false;
		topLevelRunContext = TYPE.MAIN;
	}

	/**
	 * Terminates all the runtime resources used by the run state, specifically
	 * agent's mental state.
	 */
	synchronized public void dispose() {
		// Check whether we need to cleanup mental state.
		if (mentalState != null) {
			mentalState.cleanUp();
		}
	}

	/**
	 * Returns the {@link KRlanguage}.
	 *
	 * @return The KR language.
	 */
	private KRlanguage getKRLanguage() {
		return this.getMentalState().getKRLanguage();
	}

	/**
	 * Adds a {@link Percept} to the percept buffer.
	 *
	 * @param percept
	 *            The percept added to the percept buffer.
	 * @deprecated percepts should be communicated to the agent through
	 *             {@link Capabilities#getPercepts()}.
	 */
	@Deprecated
	public void addPercept(Percept percept) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Processes EIS percepts received from the agent's environment. Converts
	 * EIS {@link Percept}s to {@link DatabaseFormula}s and inserts new and
	 * removes old percepts from the percept base.
	 *
	 * @param newPercepts
	 *            The percepts to be processed.
	 * @param previousPercepts
	 *            The percepts processed last round.
	 */
	public void processPercepts(Set<Percept> newPercepts,
			Set<Percept> previousPercepts) {
		// Compute which percepts need to be deleted and which percepts need to
		// be added
		// to the percept base using the list of percepts from the previous
		// round. The
		// set of percepts to be deleted/added are called lists for historical
		// reasons.
		Set<Percept> deleteList = new HashSet<>(previousPercepts);
		deleteList.removeAll(newPercepts);
		Set<Percept> addList = new HashSet<>(newPercepts);
		addList.removeAll(previousPercepts);

		// nothing to do if both add and delete lists are empty.
		getMentalState().getOwnBase(BASETYPE.PERCEPTBASE).updatePercepts(
				addList, deleteList, debugger);
	}

	/**
	 * Processes all given messages. Processing involves updating the mental
	 * model of the sending agent in a way that depends on the messages mood,
	 * which is indicated by the ACL's performative.
	 */
	private void processMessages(Set<Message> messages) {
		if (!messages.isEmpty()) {
			getDebugger().breakpoint(Channel.MAILS, null, "Processing mails."); //$NON-NLS-1$
			for (Message message : messages) {
				processMessageMentalModel(message);
				processMessageToMessagebox(message);
			}

			// Check if goals have been achieved and, if so, update goal base.
			getMentalState().updateGoalState(getDebugger());
			// breakpoint AFTER change, to trigger introspector refresh #2853
			getDebugger().breakpoint(Channel.MAILS, null, "Processed mails."); //$NON-NLS-1$
		}
	}

	/**
	 * Process one message that we received by updating the mental models. This
	 * does not update the message box, see also
	 * {@link #processMessageToMessagebox(Message)}
	 *
	 * @param message
	 *            the new message
	 */
	private void processMessageMentalModel(Message message) {
		DatabaseFormula content = message.getContent();
		AgentId sender = message.getSender();

		if (this.usesMentalModels) {
			if (!this.getMentalState().getKnownAgents().contains(sender)) {
				try {
					this.getMentalState().addAgentModel(sender, debugger);
				} catch (Exception e) {
					new Warning(debugger, String.format(
							Resources.get(WarningStrings.FAILED_ADD_MODEL),
							sender.getName()));
				}
			}

			try {
				Update update = this.getMentalState().getKRLanguage()
						.parseUpdate(content.toString());

				switch (message.getMood()) {
				case INDICATIVE:
					this.getMentalState().insert(update, BASETYPE.BELIEFBASE,
							debugger, sender);
					break;
				case IMPERATIVE:
					this.getMentalState().adopt(update, true, debugger, sender);
					this.getMentalState().delete(update, BASETYPE.BELIEFBASE,
							debugger, sender);
					break;
				case INTERROGATIVE:
					this.getMentalState().delete(update, BASETYPE.BELIEFBASE,
							debugger, sender);
					break;
				default:
					throw new GOALBug(
							"Received a message with unexpected mood: " //$NON-NLS-1$
									+ message.getMood());
				}
				this.getMentalState().updateGoalState(debugger, sender);
			} catch (Exception e) {
				throw new GOALBug("Processing of message with content: " //$NON-NLS-1$
						+ content + " failed due to exception " + e.toString(), //$NON-NLS-1$
						e);
			}
		}

	}

	/**
	 * Process the message: put it in the message box.
	 *
	 * @param message
	 *            the message that was received.
	 */
	private void processMessageToMessagebox(Message message) {
		/*
		 * Put the received message in the mailbox as a "received" fact. The
		 * message content is annotated with the mood, unless it is an
		 * indicative.
		 */
		getMentalState().getOwnBase(BASETYPE.MAILBOX).insert(message, true,
				debugger);
		// TODO: do this in a proper but also EFFICIENT way!!
		// TRAC #1125, #1128, #738. This is getting ugly, see #....
		// Identifier eisname = new Identifier(message.getSender().getName());
		// AgentId langSpecificSenderName = new AgentId(this.getKRLanguage()
		// .ConvertEISParameterToTerm(eisname).toString());
		//
		// DatabaseFormula formula = this.getKRLanguage().parseDBFormula(
		// message.toString(false, langSpecificSenderName) + ".");
		// this.getMentalState().insert(formula, BASETYPE.MAILBOX, debugger);
	}

	/**
	 * Returns the goal to be focused on.
	 *
	 * @return The goal to focus on, if available; {@code null} otherwise.
	 */
	public SingleGoal getFocusGoal() {
		return focusgoal;
	}

	/**
	 * Sets the goal to be focused on.
	 *
	 * @param goal
	 *            The goal to focus on.
	 */
	public void setFocusGoal(SingleGoal goal) {
		this.focusgoal = goal;
	}

	/**
	 * Gets the debugger.
	 *
	 * @return the debugger
	 */
	public D getDebugger() {
		return this.debugger;
	}

	/**
	 * DOC
	 * 
	 * @param isActionPerformed
	 */
	public void startCycle(boolean isActionPerformed) {
		startCycle(isActionPerformed, new HashSet<Percept>());
	}

	private Set<Percept> getPercepts() throws DebuggerKilledException {
		try {
			return environment.getPercepts();
		} catch (MessagingException e) {
			// typically, when system is taken down.
			throw new DebuggerKilledException(
					"Fatal error: messaging is failing.", e);
		} catch (EnvironmentInterfaceException e) {
			new Warning(Resources.get(WarningStrings.FAILED_GET_PERCEPT), e);
			return new HashSet<>(0);
		}
	}

	/**
	 * Perform preparations for starting a new cycle:
	 * <ul>
	 * <li>send outgoing mails
	 * <li>Increment the round counter (if not asleep).</li>
	 * <li>Display round separator via debugger (if not asleep).</li>
	 * <li>Collect and process percepts.</li>
	 * <li>Collect and process messages.</li>
	 * <li>Execute the init and event module, if present.</li>
	 * </ul>
	 *
	 * This function may go to sleep until there are new percepts or messages.
	 *
	 * @param isActionPerformed
	 *            is true if there has been performed an action between this
	 *            call and the previous call to {@link #startCycle(boolean)}. We
	 *            only consider going to sleep if this is false.
	 * @param initial
	 *            the initial set of percepts to use
	 */
	// TODO: Does not yet support measuring time used in Thread.
	public void startCycle(boolean isActionPerformed, Set<Percept> initial) {
		Set<Message> newMessages = messaging.getAllMessages();
		Set<Percept> newPercepts = initial;
		if (initial.isEmpty()) {
			newPercepts = getPercepts();
		}

		event = !newMessages.isEmpty() || !newPercepts.isEmpty()
				|| isActionPerformed;

		boolean sameMessages = newMessages.equals(previousMessages);
		boolean samePercepts = newPercepts.equals(previousPercepts);
		boolean sleepConditionsHoldingNow = samePercepts && sameMessages
				&& !isActionPerformed;

		/**
		 * if sleep condition held previously and now, we go to sleep mode. In
		 * sleep mode we wait till new messages or percepts come in.
		 */
		if (PMPreferences.getSleepRepeatingAgent()) {
			if (sleepConditionsHoldingPreviousCycle
					&& sleepConditionsHoldingNow) {
				// sleep condition holds also NOW. Go sleep.
				debugger.breakpoint(Channel.SLEEP, null, "Going to sleep mode."); //$NON-NLS-1$

				while (sleepConditionsHoldingPreviousCycle) {
					// TODO would be nice to be event triggered here,
					// and wake up on new message or percept, e.g., by
					// using a blocking queue. But we are using a pull model for
					// percepts... Maybe we can hand over responsibility for
					// checking
					// our percepts to the environment port that is running in
					// its own
					// thread and have that port notify us when something has
					// changed!?

					// Check if thread has been interrupted, we're in a
					// busy-waiting loop!
					// If so, we should kill the thread we're using; the
					// debugger will
					// take care of this.
					debugger.breakpoint(Channel.RUNMODE, null, "sleeping"); //$NON-NLS-1$
					Thread.yield();

					newMessages = messaging.getAllMessages();
					newPercepts = getPercepts();
					sameMessages = newMessages.equals(previousMessages);
					samePercepts = newPercepts.equals(previousPercepts);
					sleepConditionsHoldingPreviousCycle = samePercepts
							&& sameMessages;
				}
				debugger.breakpoint(Channel.SLEEP, null,
						"Woke up from sleep mode."); //$NON-NLS-1$
			}
		}

		// Increment round counter and display round separator via debugger.
		this.incrementRoundCounter();
		this.debugger.breakpoint(Channel.REASONING_CYCLE_SEPARATOR,
				getRoundCounter(), " +++++++ Cycle " + getRoundCounter() //$NON-NLS-1$
						+ " +++++++ "); //$NON-NLS-1$

		// Get and process percepts.
		this.processPercepts(newPercepts, previousPercepts);
		// Get messages and update message box.
		this.processMessages(newMessages);

		// If there is an init module, run it in the first round.
		if (this.initModule != null && this.getRoundCounter() == 1) {
			this.initModule.executeFully(this, this.getKRLanguage()
					.getEmptySubstitution());
		}

		// If there is an event module, run it at the start of a cycle (but not
		// in the first round).
		if (this.eventModule != null && event) {
			this.eventModule.executeFully(this, this.getKRLanguage()
					.getEmptySubstitution());
		}

		event = false;
		previousMessages = newMessages;
		previousPercepts = newPercepts;
		sleepConditionsHoldingPreviousCycle = sleepConditionsHoldingNow;
	}

	/**
	 * Returns the main module from the {@link #program}. If the program does
	 * not have a main module, a "dummy" instance of a main module is returned;
	 * see {@link #RunState(EnvironmentPort, MessageBox, GOALProgram)}.
	 *
	 * @return The main module of the program, or a "dummy" instance if the
	 *         program does not have a main module.
	 */
	public Module getMainModule() {
		return this.mainModule;
	}

	/**
	 * Returns the module that was entered most recently.
	 *
	 * {@link FileLearner} is only user of this method, see
	 * {@link RuleSet#run(RunState, goal.core.kr.language.Substitution)}.
	 *
	 * @return The (non-anonymous) module that was entered last.
	 */
	public Module getActiveModule() {
		return this.activeStackOfModules.peek();
	}

	/**
	 * Pushes (non-anonymous) module that was just entered onto stack and
	 * changes top level context if one of init, event, or main module has been
	 * entered.
	 *
	 * @param module
	 *            A (non-anonymous) module.
	 */
	public void enteredModule(Module module) {
		if (module.isAnonymous()) {
			return;
		}

		this.activeStackOfModules.push(module);
		switch (module.getType()) {
		case MAIN:
		case EVENT:
		case INIT:
			this.topLevelRunContext = module.getType();
			break;
		default:
			// top level context does not change for other
			// kinds of modules.
		}
	}

	/**
	 * Removes the last entered (non-anonymous) module from the stack of active
	 * modules. Should be called when exiting *any* module. The
	 * {@link RunState#topLevelRunContext} is updated here as well.
	 *
	 * @param module
	 *            The module that is exited.
	 * @return {@code true} if another module is re-entered from a non-
	 *         anonymous module.
	 */
	public boolean exitModule(Module module) {
		if (module.isAnonymous()) {
			return false;
		}

		switch (module.getType()) {
		case EVENT:
		case INIT:
			// We're leaving the init or event module and returning
			// to main top level context.
			this.topLevelRunContext = TYPE.MAIN;
			break;
		default:
			// top level context does not change for other
			// kinds of modules. If we're leaving the main module,
			// main module should be only element on stack; in that
			// case we're leaving the agent, no need to reset context.
			break;
		}
		activeStackOfModules.pop();
		// Report module re-entry on module's debug channel.
		return (activeStackOfModules.peek() != null);
	}

	/**
	 * Check if main module is context in which we run now.
	 *
	 * @return {@code true} if main module is context in which we run now.
	 */
	public boolean isMainModuleRunning() {
		return this.topLevelRunContext.equals(TYPE.MAIN);
	}

	/**
	 * Get the action selector to be used in ADAPTIVE mode.
	 *
	 * @return the Learner
	 */
	public Learner getLearner() {
		return learner;
	}

	public boolean setMainModule(String id) {
		if (id == null) {
			return true;
		} else if (program.hasModule(id)) {
			mainModule = program.getModule(id);
			return true;
		}
		return false;
	}

	public Double getReward() {
		try {
			return environment.getReward();
		} catch (Exception e) {
			new Warning(Resources.get(WarningStrings.FAILED_ENV_GET_REWARD), e);
			return null;
		}
	}

	public void postMessage(Message message) {
		messaging.postMessage(message);
	}

	public void doPerformAction(UserSpecAction action) {
		try {
			environment.performAction(action);
		} catch (EnvironmentInterfaceException e) {
			new Warning(String.format(
					Resources.get(WarningStrings.FAILED_ACTION_EXECUTE),
					action.toString()), e);
		} catch (MessagingException e) {
			new Warning(String.format(
					Resources.get(WarningStrings.FAILED_ACTION_SEND),
					action.toString()), e);
		}
	}

	/**
	 * log the message of a {@link LogAction}.
	 *
	 * @param message
	 *            The message.
	 */
	public void doLog(String message) {
		logActionsLogger.log(message);
	}
}
