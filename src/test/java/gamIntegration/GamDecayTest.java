package gamIntegration;

import static org.junit.Assert.assertSame;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class GamDecayTest extends SimpleEmotionTest {
	@Test
	public void testDecay() throws Exception {
		assertSame(RunResult.OK,
		           runAgent("src/test/resources/goal/gam/decayTest/decayTest.goal",
		        		    "src/test/resources/goal/gam/decayTest/decayTest.emo2g"));
	}
}
