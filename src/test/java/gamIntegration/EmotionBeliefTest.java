package gamIntegration;

import languageTools.parser.relationParser.EmotionConfig;

import org.junit.Test;

import goal.core.gamygdala.Engine;
import goal.parser.unittest.AbstractUnitTestTest;
import goal.tools.unittest.result.UnitTestResult;

@SuppressWarnings("javadoc")
public class EmotionBeliefTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		Engine.reset();
		EmotionConfig.reset();
		UnitTestResult results = runTest("src/test/resources/goal/parser/unittest/CountsTo100Test2.test2g");
		assertPassedAndPrint(results);
	}
}
