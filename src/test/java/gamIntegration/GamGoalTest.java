package gamIntegration;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import org.junit.Test;

import goal.core.gamygdala.Engine;
import goal.core.gamygdala.Goal;
import goal.parser.unittest.AbstractUnitTestTest;

@SuppressWarnings("javadoc")
public class GamGoalTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		Engine gamEngine = Engine.getInstance();
		//gamEngine.setStart(true);
		//gamEngine.useFile(true);
		
		runTest("src/test/resources/goal/parser/unittest/testGamGoals.test2g");

		Iterator<Goal> testIterator = gamEngine.getMap().getGoalMap().values().iterator();
		while(testIterator.hasNext()){
			System.out.println("GO: ");
			System.out.println(testIterator.next().getName());

		}
		assertTrue(gamEngine.getMap().getAgentMap().containsKey("agentUnderTest"));
		assertTrue(gamEngine.getMap().getAgentMap().containsKey("secondAgentUnderTest"));
		
		//gamEngine.reset();
	}
}
