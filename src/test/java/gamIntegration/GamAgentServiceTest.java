
package gamIntegration;

import static org.junit.Assert.assertEquals;

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
		
		
		assertEquals(iter.next().getKey(), "agentUnderTest");
		assertEquals(iter.next().getKey(),"secondAgentUnderTest");
		
		//gamEngine.reset();
	}
}