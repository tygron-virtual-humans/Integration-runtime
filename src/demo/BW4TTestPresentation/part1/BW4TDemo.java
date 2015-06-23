package BW4TTestPresentation.part1;

import goal.tools.SingleRun;
import goal.tools.errorhandling.exceptions.GOALRunFailedException;

import java.io.File;
import java.io.FileNotFoundException;

import krTools.errors.exceptions.ParserException;
import languageTools.exceptions.relationParser.InvalidEmotionConfigFile;
import languageTools.parser.relationParser.EmotionConfig;


public class BW4TDemo {

	public static void main(String[] args) throws ParserException, GOALRunFailedException, FileNotFoundException, InvalidEmotionConfigFile {
		SingleRun run = new SingleRun(new File("src/demo/BW4TTestPresentation/part1/kreynen_spanoghe.mas2g"), 100000); 
	    run.run();
	}

}

