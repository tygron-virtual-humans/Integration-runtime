package gamIntegration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import goal.core.gam.Gamygdala;
import goal.parser.unittest.AbstractUnitTestTest;

@SuppressWarnings("javadoc")
public class GamAgentServiceTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		Gamygdala gamEngine = Gamygdala.getInstance();
		//gamEngine.reset();
		gamEngine.setStart(true);
		//gamEngine.useFile(true);
		
		runTest("src/test/resources/goal/parser/unittest/correctMinimal.test2g");

		assertEquals(gamEngine.gamydgalaMap.getAgentMap().keySet().contains("agentUnderTest"), true);
		assertEquals(gamEngine.gamydgalaMap.getAgentMap().keySet().contains("secondAgentUnderTest"), true);

		gamEngine.reset();
	}
}
