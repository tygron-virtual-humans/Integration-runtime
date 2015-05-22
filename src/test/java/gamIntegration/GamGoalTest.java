package gamIntegration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import goal.core.gamygdala.Engine;
import goal.parser.unittest.AbstractUnitTestTest;

@SuppressWarnings("javadoc")
public class GamGoalTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		Engine gamEngine = Engine.getInstance();
		//gamEngine.setStart(true);
		//gamEngine.useFile(true);
		
		runTest("src/test/resources/goal/parser/unittest/testGamGoals.test2g");

		assertTrue(gamEngine.getMap().getGoalMap().containsKey("aap/1"));
		assertEquals(gamEngine.getMap().getAgentMap().getAgentByName("agentUnderTest").getGoalByName("aap/1").getName(), "aap/1");
		assertEquals(gamEngine.getMap().getAgentMap().getAgentByName("secondAgentUnderTest").getGoalByName("aap/1").getName(), "aap/1");

		//gamEngine.reset();
	}
}
