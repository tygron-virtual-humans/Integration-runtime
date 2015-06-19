package tempEmo;

import goal.tools.SingleRun;
import goal.tools.errorhandling.exceptions.GOALRunFailedException;

import java.io.File;
import java.io.FileNotFoundException;

import krTools.errors.exceptions.ParserException;
import languageTools.exceptions.relationParser.InvalidEmotionConfigFile;


public class BW4TDemo {

	public static void main(String[] args) throws ParserException, GOALRunFailedException, FileNotFoundException, InvalidEmotionConfigFile {
		SingleRun run = new SingleRun(new File("src/demo/EmotionBW4TTest/kreynen_spanoghe.mas2g"), 100000); 
	    run.run();
	}

}
