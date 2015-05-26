package gamIntegration;

import org.junit.Test;

import goal.parser.unittest.AbstractUnitTestTest;
import goal.tools.unittest.result.UnitTestResult;

@SuppressWarnings("javadoc")
public class EmotionBeliefTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		//Engine gamEngine = Engine.getInstance();
		//gamEngine.setStart(true);
		//gamEngine.useFile(true);
		
		UnitTestResult results = runTest("src/test/resources/goal/parser/unittest/CountsTo100Test2.test2g");
		assertPassedAndPrint(results);
		//testGamGoals.test2g");
		}		
		//gamEngine.reset();
	}

