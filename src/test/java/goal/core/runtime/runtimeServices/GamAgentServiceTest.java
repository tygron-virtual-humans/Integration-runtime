package goal.core.runtime.runtimeServices;

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

		assert(iter.next().getKey()=="agentUnderTest" && iter.next().getKey()=="secondAgentUnderTest");
	}
}
