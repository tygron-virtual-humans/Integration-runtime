package gamIntegration;

import static org.junit.Assert.assertTrue;
import goal.core.agent.AbstractAgentFactory;
import goal.core.agent.Agent;
import goal.core.agent.GOALInterpreter;
import goal.core.agent.MessagingCapabilities;
import goal.core.agent.NoMessagingCapabilities;
import goal.core.executors.MentalStateConditionExecutor;
import goal.core.gamygdala.Engine;
import goal.core.mentalstate.MentalState;
import goal.tools.PlatformManager;
import goal.tools.adapt.Learner;
import goal.tools.debugger.Debugger;
import goal.tools.debugger.NOPDebugger;
import goal.tools.eclipse.QueryTool;
import goal.tools.errorhandling.exceptions.GOALDatabaseException;
import goal.tools.errorhandling.exceptions.GOALLaunchFailureException;
import goal.tools.logging.Loggers;
import goalhub.krTools.KRFactory;

import java.io.File;
import java.util.Set;

import krTools.KRInterface;
import krTools.errors.exceptions.KRInitFailedException;
import krTools.language.Substitution;
import krTools.language.Term;
import krTools.language.Var;
import languageTools.parser.relationParser.EmotionConfig;
import languageTools.program.agent.AgentProgram;
import languageTools.program.agent.msc.MentalStateCondition;
import nl.tudelft.goal.messaging.exceptions.MessagingException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base for simple tests that don't need an environment or messaging.
 *
 * @author mpkorstanje
 */
public class SimpleEmotionTest {

	/**
	 * Abstract base for build agents without messaging or an environment.
	 * Subclasses can provide different Messaging- and EnvironmentCapabilities,
	 * Debuggers, Learners and Controllers. This can be done by overriding or
	 * implementing the proper methods.
	 *
	 * During the construction the class fields will be initialized to assist
	 * the creation of different classes.
	 *
	 * @author mpkorstanje
	 *
	 * @param <D>
	 *            class of the Debugger to provide.
	 * @param <C>
	 *            class of the GOALInterpreter to provide.
	 */
	private abstract class SimpleAgentFactory<D extends Debugger, C extends GOALInterpreter<D>>
	extends AbstractAgentFactory<D, C> {
		/**
		 * Constructs a factory for agents withouth messaging.
		 */
		public SimpleAgentFactory() {
			super();
		}

		@Override
		protected MessagingCapabilities provideMessagingCapabilities() {
			return new NoMessagingCapabilities();
		}
	}

	protected Agent<GOALInterpreter<Debugger>> buildAgent(String id,
			AgentProgram program) throws GOALLaunchFailureException,
			MessagingException, KRInitFailedException {
		SimpleAgentFactory<Debugger, GOALInterpreter<Debugger>> factory = new SimpleAgentFactory<Debugger, GOALInterpreter<Debugger>>() {
			@Override
			protected Debugger provideDebugger() {
				return new NOPDebugger(this.agentId);
			}

			@Override
			protected GOALInterpreter<Debugger> provideController(
					Debugger debugger, Learner learner) {
				return new GOALInterpreter<Debugger>(this.program, debugger,
						learner);
			}
		};
		return factory.build(program, id, null);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public enum RunResult {
		OK, FAILURE, UNKNOWN;

		public static RunResult get(String value) {
			try {
				return RunResult.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {
				return RunResult.UNKNOWN;
			}
		}
	}

	private static KRInterface language;

	@BeforeClass
	public static void setupBeforeClass() throws KRInitFailedException {
		Loggers.addConsoleLogger();
		language = KRFactory.getDefaultInterface();

	}

	@AfterClass
	public static void tearDownAfterClass() {
		Loggers.removeConsoleLogger();
	}

	@After
	public void cleanUp() throws KRInitFailedException {
		// language.reset();
	}

	/**
	 * Do a single run of given GOAL file. Checks after the run why the agent
	 * terminated. The agent should hold the belief "ok" or "failure", and that
	 * is the reason to return. If neither is holding, we return
	 * {@link RunResult#UNKNOWN}.
	 *
	 * @param goalFile
	 * @throws Exception
	 */
	protected RunResult runAgent(String goalFile, String emo2gFile) throws Exception {
		EmotionConfig.getInstance();
		Engine.getInstance();
		
		String id = "TestAgent";
		AgentProgram program = PlatformManager.createNew().parseGOALFile(
				new File(goalFile), language);
		EmotionConfig.parse(emo2gFile);
		assertTrue(program.isValid());

		Agent<GOALInterpreter<Debugger>> agent = buildAgent(id, program);
		agent.start();
		agent.awaitTermination(0); // TODO: timeout?!

		return inspectResult(agent);
	}

	protected RunResult inspectResult(Agent<GOALInterpreter<Debugger>> agent) {
		QueryTool buildQuery = new QueryTool(agent);
		MentalStateCondition mentalStateCondition;
		try {
			mentalStateCondition = buildQuery.parseMSC("bel(result(X))");
		} catch (Exception e) {
			throw new IllegalStateException(
					"Unexpected exception whilst building MSC", e);
		}

		MentalState mentalState = agent.getController().getRunState()
				.getMentalState();
		Set<Substitution> res;
		try {
			res = new MentalStateConditionExecutor(
					mentalStateCondition).evaluate(mentalState, new NOPDebugger(
					agent.getId()));
		} catch (GOALDatabaseException e) {
			throw new IllegalStateException("evaluation of mental state "+mentalState+" failed",e);
		}

		// there should be exactly 1 substi.
		if (res.size() < 1) {
			throw new IllegalStateException(
					"Program failed: it did not set a result");
		}
		if (res.size() > 1) {
			throw new IllegalStateException(
					"Program failed: it set multiple results (ony 1 allowed)");
		}
		// and it should hold exactly 1 variable, our variable "X". Get its
		// value
		Substitution substitution = ((Substitution) res.toArray()[0]);
		Set<Var> variables = substitution.getVariables();
		if (variables.size() < 1) {
			throw new IllegalStateException(
					"Query failed: it did not set a result");
		}
		if (variables.size() > 1) {
			throw new IllegalStateException(
					"Query failed: it set multiple results (ony 1 allowed)");
		}
		Var var = (Var) variables.toArray()[0];
		Term value = substitution.get(var);

		return RunResult.get(value.toString());
	}
}
