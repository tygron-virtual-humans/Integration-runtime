package BW4TTest;

import goal.tools.SingleRun;
import goal.tools.errorhandling.exceptions.GOALRunFailedException;

import java.io.File;

import krTools.errors.exceptions.ParserException;

import org.junit.Test;

public class BW4TDemo {

	public static void main(String[] args) throws ParserException, GOALRunFailedException {

		SingleRun run = new SingleRun(new File("demo/BW4TTest/kreynen_spanoghe.mas2g"), 100000); 
	    run.run();

	}

}
