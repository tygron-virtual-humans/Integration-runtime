package gamIntegration;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import org.junit.Test;

import goal.core.gamygdala.Engine;
import goal.core.gamygdala.Goal;
import goal.parser.unittest.AbstractUnitTestTest;

@SuppressWarnings("javadoc")
public class EmotionBeliefTest extends AbstractUnitTestTest {

	@Test
	public void testCorrectMinimal() throws Exception {
		Engine gamEngine = Engine.getInstance();
		//gamEngine.setStart(true);
		//gamEngine.useFile(true);
		
		runTest("src/test/resources/goal/parser/unittest/CountsTo100Test2.test2g");
		//testGamGoals.test2g");
		}		
		//gamEngine.reset();
	}

