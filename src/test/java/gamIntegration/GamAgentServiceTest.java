
package gamIntegration;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import goal.core.gamygdala.Engine;
import goal.parser.unittest.AbstractUnitTestTest;

@SuppressWarnings("javadoc")
public class GamAgentServiceTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		Engine gamEngine = Engine.getInstance();
		//gamEngine.setStart(true);
		//gamEngine.useFile(true);
		
		runTest("src/test/resources/goal/parser/unittest/correctMinimal.test2g");

		assertTrue(gamEngine.getMap().getAgentMap().containsKey("agentUnderTest"));
		assertTrue(gamEngine.getMap().getAgentMap().containsKey("secondAgentUnderTest"));
		
		//gamEngine.reset();
	}
}