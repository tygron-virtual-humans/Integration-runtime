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
		Gamygdala gamEngine = Gamygdala.getInstance();
		//gamEngine.reset();
		gamEngine.setStart(true);
		//gamEngine.useFile(true);
		
		runTest("src/test/resources/goal/parser/unittest/correctMinimal.test2g");

		Iterator<Entry<String, Agent>> iter = gamEngine.gamydgalaMap.getAgentMap().getIterator();
		/*
		while(iter.hasNext()){
			System.out.println("yo : "  + iter.next());
		}
		*/
		System.out.println(" hi" );
		System.out.println(" hi " + iter.hasNext());
		assertEquals(gamEngine.gamydgalaMap.getAgentMap().keySet().contains("agentUnderTest"), true);
		assertEquals(gamEngine.gamydgalaMap.getAgentMap().keySet().contains("secondAgentUnderTest"), true);
		assert(1 == 5);
		gamEngine.reset();
	}
}
