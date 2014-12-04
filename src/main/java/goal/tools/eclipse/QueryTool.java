package goal.tools.eclipse;

import goal.core.agent.Agent;
import krTools.language.Substitution;
import krTools.language.Var;
import goal.core.mentalstate.MentalState;
import languageTools.errors.ValidatorError;
import languageTools.parser.GOAL;
import languageTools.parser.GOALLexer;
import languageTools.program.agent.ActionSpecification;
import languageTools.program.agent.Module;
import languageTools.program.agent.actions.Action;
import languageTools.program.agent.actions.MentalAction;
import languageTools.program.agent.actions.UserSpecAction;
import languageTools.program.agent.actions.UserSpecOrModuleCall;
import languageTools.program.agent.msc.MentalStateCondition;
import goal.tools.IDEGOALInterpreter;
import goal.tools.debugger.SteppingDebugger;
import goal.tools.errorhandling.exceptions.GOALBug;
import goal.tools.errorhandling.exceptions.GOALException;
import goal.tools.errorhandling.exceptions.GOALUserError;
import krTools.errors.exceptions.KRInitFailedException;
import krTools.errors.exceptions.ParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class QueryTool {
	private final Agent<IDEGOALInterpreter> agent;

	public QueryTool(final Agent<IDEGOALInterpreter> agent) {
		this.agent = agent;
	}

	public String doquery(String userEnteredQuery) throws GOALUserError {
		MentalStateCondition mentalStateCondition;
		try {
			mentalStateCondition = parseMSC(userEnteredQuery);
		} catch (Exception e) {
			throw new GOALUserError("Parsing of " + userEnteredQuery
					+ " failed: " + e.getMessage(), e);
		}
		// Perform query: get the agent's mental state and evaluate the query.
		MentalState mentalState = this.agent.getController().getRunState()
				.getMentalState();
		try {
			// use a dummy debugger
			Set<Substitution> substitutions = mentalStateCondition.evaluate(
					mentalState, new SteppingDebugger("query", null));
			String resulttext = "";
			if (substitutions.isEmpty()) {
				resulttext = "No solutions";
			} else {
				for (Substitution s : substitutions) {
					resulttext = resulttext + s + "\n";
				}
			}
			return resulttext;
		} catch (Exception e) {
			throw new GOALUserError("Query entered in query area in "
					+ "introspector of agent " + agent.getId() + " failed: "
					+ e.getMessage(), e);
		}
	}

	public String doaction(String userEnteredAction) throws GOALUserError {
		MentalState mentalState = this.agent.getController().getRunState()
				.getMentalState();
		if (mentalState == null) {
			throw new GOALUserError("Agent has not yet "
					+ "initialized its databases");
		}
		try {
			Action action = parseAction(userEnteredAction);
			ActionValidator val = new ActionValidator(new NameSpace(),
					new HashSet<Var>());
			if (!val.validate(action, false)) {
				throw new GOALUserError("Action " + action + " is not valid:\n"
						+ val.getAllReports());
			}
			if (action.isClosed()) {
				// Perform the action.
				this.agent.getController().doPerformAction(action);
				return "Executed action " + action;
			} else {
				return "Action is not closed and cannot be executed";
			}

		} catch (Exception e) {
			throw new GOALUserError("Action entered in query area in "
					+ "introspector of agent " + agent.getId() + " failed: "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Creates an embedded GOAL parser that can parse the given string.
	 *
	 * @param pString
	 *            is the string to be parsed.
	 * @return a GOALWAlker that can parse text at the GOAL level.
	 */
	private GOALWalker prepareGOALWalker(String pString) {
		try {
			ANTLRInputStream charstream = new ANTLRInputStream(
					new StringReader(pString));
			GOALLexer lexer = new GOALLexer(charstream);
			CommonTokenStream stream = new CommonTokenStream(lexer);
			GOAL parser = new GOAL(stream);
			return new GOALWalker(null, parser, lexer, agent.getController()
					.getProgram().getKRInterface());
		} catch (IOException e) {
			throw new GOALBug("internal error while handling the query", e);
		}
	}

	/**
	 * DOC
	 *
	 * @param mentalStateCondition
	 *            Input string that should represent a mental state condition.
	 * @return The mental state condition that resulted from parsing the input
	 *         string.
	 * @throws GOALException
	 *             When the parser throws a RecognitionException, which should
	 *             have been buffered and ignored.
	 * @throws ParserException
	 *             DOC
	 */
	private MentalStateCondition parseMSC(String mentalStateCondition)
			throws GOALException, ParserException {
		MentalStateCondition msc;

		// Try to parse the MSC.
		GOALWalker walker = this.prepareGOALWalker(mentalStateCondition);
		try {
			msc = walker.visitConditions(walker.getParser().conditions());
		} catch (Exception ex) {
			// It should not throw a RecognitionException.
			throw new GOALBug("Recognition exceptions should be handled by "
					+ "the parser, and not thrown out.", ex);
		}

		checkParserErrors(walker, mentalStateCondition,
				"mental state condition ");

		// TODO? macros are not resolved, not clear how to do that anyways.
		return msc;
	}

	/**
	 * check if any error has occurred. (only print lexer errors when there are
	 * no parser errors) Aggregate the error messages into a string, such that
	 * it can be put into the message of an exception to be thrown. The caller
	 * prints that message to the query console.
	 *
	 * @param walker
	 *            is the GOALWalker used to parse the string
	 * @param query
	 *            is the string that was fed to the GOALParser and that failed.
	 * @pparam desc is a string describing what was attempted to parse. Used for
	 *         error message generation if the parse failed.
	 * @throws GOALUserError
	 *             if error occured..
	 * @throws ParserException
	 */
	private void checkParserErrors(GOALWalker walker, String query, String desc)
			throws GOALUserError, ParserException {
		List<ValidatorError> errors = walker.getErrors();
		String errMessage = "";
		if (!errors.isEmpty()) {
			for (ValidatorError err : errors) {
				errMessage += err.toString() + "\n";
			}
		}

		// if any error has occurred, throw a UserError
		if (!errMessage.isEmpty()) {
			throw new GOALUserError("Term " + query + " failed to parse as "
					+ desc + ";\n" + errMessage);
		}
	}

	/**
	 * Parse string as a mental action.
	 *
	 * @author W.Pasman.
	 * @throws ParserException
	 * @modified N.Kraayenbrink - GOAL parser does not print errors any more.
	 * @modified W.Pasman 8feb2012 now also UserSpecActions can be parsed.
	 * @modified K.Hindriks if UserOrFocusAction action must be UserSpecAction.
	 */
	private Action parseAction(String action) throws GOALException,
	ParserException {
		Action act;

		// try and parse the MSC. It should not throw a RecognitionException.
		GOALWalker walker = this.prepareGOALWalker(action);
		try {
			act = walker.visitAction(walker.getParser().action());
		} catch (Exception ex) {
			throw new GOALBug("Recognition exceptions should be handled by "
					+ "the parser, and not thrown out.", ex);
		}

		checkParserErrors(walker, action, "action");

		// TODO: this code implements a rather naive approach to getting the
		// action specification(s)
		// of the action. In particular, it does not take scope into account. If
		// an action is specified
		// e.g. at top level and again within a module, then this approach is
		// not able to determine
		// which action specification should be associated with the action.
		// If the user intends to execute the action in the module, we should
		// allow for expressions of
		// the form: module.action(parameters) so that the user is able to refer
		// to the right scope.
		if (act instanceof UserSpecOrModuleCall) {
			// Must be user-specified action.
			// TODO: now sets by default that action is EXTERNAL and should be
			// sent to environment.
			act = new UserSpecAction(act.getName(),
					((UserSpecOrModuleCall) act).getParameters(), true, null,null,null);
			// Search for all corresponding action specifications.
			for( Module module : agent.getController().getProgram().getModules()){
				for (ActionSpecification specification : module.getActionSpecifications() ) {
					if (act.getName().equals(specification.getAction().getName())
							&& (((UserSpecAction) act).getParameters().size() == specification
							.getAction().getParameters().size())) {
						try {
							((UserSpecAction) act).addSpecification(specification);
						} catch (KRInitFailedException e) {
							throw new GOALUserError(
									"Failed to associate specification with action: "
											+ e.getMessage(), e);
						}
					}
				}
			}
		}

		// module calls would be dangerous.
		if (!(act instanceof MentalAction)
				&& (!(act instanceof UserSpecAction))) {
			throw new GOALUserError(
					"Action "
							+ action
							+ " must be either a built-in mental action or a user-specified action (found "
							+ act.getClass() + " instead).");
		}

		return act;
	}
}
