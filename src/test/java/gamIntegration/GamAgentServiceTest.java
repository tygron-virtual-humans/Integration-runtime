
package gamIntegration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import agent.Agent;
import gamygdala.Engine;
import goal.parser.unittest.AbstractUnitTestTest;

@SuppressWarnings("javadoc")
public class GamAgentServiceTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		Engine gamEngine = Engine.getInstance();
		//gamEngine.setStart(true);
		//gamEngine.useFile(true);
		
		runTest("src/test/resources/goal/parser/unittest/correctMinimal.test2g");

		Iterator<Entry<String, Agent>> iter = gamEngine.getMap().getAgentMap().getIterator();
		
		
		assertTrue(gamEngine.getMap().getAgentMap().containsKey("agentUnderTest"));
		assertTrue(gamEngine.getMap().getAgentMap().containsKey("secondAgentUnderTest"));
		
		//gamEngine.reset();
	}
}