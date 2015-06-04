package gamIntegration;

import static org.junit.Assert.*;

import java.io.File;

import languageTools.parser.relationParser.EmotionConfig;

import org.junit.Test;

import goal.core.gamygdala.Agent;
import goal.core.gamygdala.AgentRelations;
import goal.core.gamygdala.Engine;
import goal.core.gamygdala.Relation;
import goal.parser.unittest.AbstractUnitTestTest;
import goal.tools.SingleRun;
import goal.tools.unittest.result.UnitTestResult;

@SuppressWarnings("javadoc")
public class GamRelationTest extends AbstractUnitTestTest {

	@Test
	public void RelationTest() throws Exception {
		Engine.reset();
		Engine gamEngine = Engine.getInstance();
		SingleRun run = new SingleRun(new File("src/test/resources/goal/parser/unittest/correctMasUnderTest2.mas2g"), 100000); 
		
		Agent agent = new Agent("agent2");
		Relation rel = new Relation(agent, -1.0);
		AgentRelations relations = new AgentRelations();
		relations.add(rel);

		assertEquals(relations.toString(), gamEngine.getAgentByName("agent1").getCurrentRelations().toString());
	}
}