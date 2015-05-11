package gamIntegration;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import goal.core.gam.Agent;
import goal.core.gam.Gamygdala;
import goal.parser.unittest.AbstractUnitTestTest;

@SuppressWarnings("javadoc")
public class GamAgentServiceTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		runTest("src/test/resources/goal/parser/unittest/correctMinimal.test2g");

		Gamygdala gamEngine = Gamygdala.getInstance();
		Iterator<Entry<String, Agent>> iter = gamEngine.gamydgalaMap.getAgentMap().getIterator();

		assertEquals(iter.next().getKey(), "agentUnderTest");
		assertEquals(iter.next().getKey(),"secondAgentUnderTest");
	}
}
