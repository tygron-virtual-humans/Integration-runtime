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

		Iterator<Entry<String, Agent>> iter = gamEngine.gamydgalaMap.getAgentMap().getIterator();
		
		Iterator<Goal> testIterator = gamEngine.gamydgalaMap.getGoalMap().values().iterator();
		while(testIterator.hasNext()){
			System.out.println("GO: ");
			System.out.println(testIterator.next().getName());

		}
		
		assertEquals(iter.next().getKey(), "agentUnderTest");
		assertEquals(iter.next().getKey(),"secondAgentUnderTest");
		
		gamEngine.reset();
	}
}
