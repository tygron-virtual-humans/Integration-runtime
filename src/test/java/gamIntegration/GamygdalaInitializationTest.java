package gamIntegration;

import static org.junit.Assert.assertEquals;
import eis.exceptions.EnvironmentInterfaceException;
import goal.core.agent.AbstractAgentFactory;
import goal.core.agent.AgentFactory;
import goal.core.agent.GOALInterpreter;
import goal.core.gam.Gamygdala;
import goal.core.runtime.MessagingService;
import goal.core.runtime.RemoteRuntimeService;
import goal.core.runtime.RuntimeManager;
import goal.core.runtime.service.agent.AgentService;
import goal.core.runtime.service.environment.EnvironmentService;
import goal.tools.AbstractRun;
import goal.tools.PlatformManager;
import goal.tools.adapt.Learner;
import goal.tools.debugger.NOPDebugger;
import goal.tools.errorhandling.exceptions.GOALLaunchFailureException;
import goal.tools.logging.Loggers;

import java.io.File;
import java.io.FileNotFoundException;

import languageTools.program.mas.MASProgram;
import localmessaging.LocalMessaging;
import nl.tudelft.goal.messaging.exceptions.MessagingException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GamygdalaInitializationTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Loggers.addConsoleLogger();
	}

	@AfterClass
	public static void tearDownAfterClass() {
		Loggers.removeConsoleLogger();
	}
	
	MessagingService messagingService;
	AgentService<NOPDebugger, GOALInterpreter<NOPDebugger>> agentService;
	EnvironmentService environmentService;
	RemoteRuntimeService<NOPDebugger, GOALInterpreter<NOPDebugger>> remoteRuntimeService;
	
	RuntimeManager<NOPDebugger, GOALInterpreter<NOPDebugger>> runtimeManager;
	Gamygdala gamInstance;

	@Before
	public void setUp() throws Exception {
		final PlatformManager platform = PlatformManager.createNew();
		final MASProgram program = platform
				.parseMASFile(new File(
						"src/test/resources/goal/core/runtime/runtimeServices/fibonaci.mas2g"));
		
		messagingService = new MessagingService("localhost", new LocalMessaging());
		AgentFactory<NOPDebugger, GOALInterpreter<NOPDebugger>> factory = new AbstractAgentFactory<NOPDebugger, GOALInterpreter<NOPDebugger>>(
				messagingService) {
			@Override
			protected NOPDebugger provideDebugger() {
				return new NOPDebugger(this.agentId);
			}

			@Override
			protected GOALInterpreter<NOPDebugger> provideController(
					NOPDebugger debugger, Learner learner) {
				return new GOALInterpreter<NOPDebugger>(this.program, debugger,
						learner);
			}
		};

		agentService = new AgentService<NOPDebugger, GOALInterpreter<NOPDebugger>>(
				program, platform.getParsedAgentPrograms(), factory);
		
		environmentService = new EnvironmentService(program, messagingService);
		
		remoteRuntimeService = new RemoteRuntimeService<NOPDebugger, GOALInterpreter<NOPDebugger>>(
				messagingService);
		
		runtimeManager = new RuntimeManager<NOPDebugger, GOALInterpreter<NOPDebugger>>(
				messagingService, agentService, environmentService, remoteRuntimeService);
	}

	@After
	public void tearDown() throws Exception {
		Gamygdala.getInstance().reset();
		runtimeManager.awaitTermination(AbstractRun.TIMEOUT_FIRST_AGENT_SECONDS);
		messagingService.shutDown();
	}

	@Test
	public void testStart() throws MessagingException, EnvironmentInterfaceException,
			InterruptedException, GOALLaunchFailureException, FileNotFoundException {
		Gamygdala.getInstance().reset();
		runtimeManager.start(true);
		
		// fibonaci.mas2g has four agents
		assertEquals(Gamygdala.getInstance().gamydgalaMap.getAgentMap().size(), 4);
	}
}
