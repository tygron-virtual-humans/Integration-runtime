/**
 * GOAL interpreter that facilitates developing and executing GOAL multi-agent
 * programs. Copyright (C) 2011 K.V. Hindriks, W. Pasman
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package goal.core.executors;

import goal.core.mentalstate.BASETYPE;
import goal.core.mentalstate.MentalState;
import goal.core.runtime.service.agent.Result;
import goal.core.runtime.service.agent.RunState;
import goal.tools.debugger.Debugger;
import goal.tools.errorhandling.exceptions.GOALActionFailedException;

import java.util.Set;

import krTools.errors.exceptions.KRInitFailedException;
import krTools.language.Substitution;
import languageTools.program.agent.AgentId;
import languageTools.program.agent.actions.Action;
import languageTools.program.agent.actions.SendAction;
import languageTools.program.agent.msg.Message;

public class SendActionExecutor extends ActionExecutor {

	private SendAction action;

	public SendActionExecutor(SendAction act) {
		action = act;
	}

	@Override
	protected Result executeAction(RunState<?> runState, Debugger debugger) {
		MentalState mentalState = runState.getMentalState();

		Set<AgentId> receivers = determineReceivers(mentalState, debugger);
		Message message = action.getMessage();

		message.setReceivers(receivers);
		message.setSender(mentalState.getAgentId());

		runState.postMessage(message);

		mentalState.getOwnBase(BASETYPE.MAILBOX).insert(message, false,
				debugger);
		
		// TODO: implement functionality below but then efficiently!!
		// Identifier eisname = new Identifier(receiver.getName());
		// try{
		// // Get the KR language used for representing the content.
		// language = this.message.getContent().getLanguage();
		// } catch (KRInitFailedException e) {
		// throw new GOALActionFailedException("Failed to get receiver name "
		// + " due to KR language failure: " + e.getMessage(), e);
		// }
		// AgentId name = new
		// AgentId(language.ConvertEISParameterToTerm(eisname).toString());
		//
		// // Create 'sent' formula to be inserted into mail box.
		// try{
		// sent = language.parseDBFormula(this.message.toString(true, name));
		// } catch (GOALParseException e) {
		// throw new GOALActionFailedException("Failed to create record of"
		// + "message to be sent for: " + this.message.toString(true, name) +
		// ".", e);
		// }

		mentalState.updateGoalState(debugger); // TRAC #749

		report(debugger);

		return new Result(action);
	}
	
	/**
	 * Returns the list of agent names that should receive the message.
	 *
	 * @param mentalState
	 *            The {@link MentalState} in which the action is executed.
	 * @param debugger
	 *            The current debugger
	 * @return A list of agents that should receive the message.
	 */
	private Set<AgentId> determineReceivers(MentalState mentalState,
			Debugger debugger) {
		try {
			return action.getSelector().resolve(mentalState);
		} catch (KRInitFailedException e) {
			throw new GOALActionFailedException(
					"Could not determine receivers: " + e.getMessage(), e);
		}
	}
	
	@Override
	protected ActionExecutor applySubst(Substitution subst) {
		return new SendActionExecutor(action.applySubst(subst));
	}
	
	@Override
	public Action<?> getAction() {
		return action;
	}
}
