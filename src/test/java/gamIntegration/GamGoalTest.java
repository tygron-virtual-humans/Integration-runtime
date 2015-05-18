package gamIntegration;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import goal.core.gam.Agent;
import goal.core.gam.Gamygdala;
import goal.core.gam.Goal;
import goal.parser.unittest.AbstractUnitTestTest;

@SuppressWarnings("javadoc")
public class GamGoalTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		Gamygdala gamEngine = Gamygdala.getInstance();
		gamEngine.setStart(true);
		gamEngine.useFile(true);
		
		runTest("src/test/resources/goal/parser/unittest/testGamGoals.test2g");
		
		assertEquals(gamEngine.gamydgalaMap.getAgentMap().getAgentByName("agentUnderTest").getGoalByName("aap/1").getName(), "aap/1");
		assertEquals(gamEngine.gamydgalaMap.getAgentMap().getAgentByName("secondAgentUnderTest").getGoalByName("aap/1").getName(), "aap/1");
		
		gamEngine.reset();
	}
}
