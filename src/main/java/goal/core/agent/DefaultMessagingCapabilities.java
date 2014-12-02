package goal.core.agent;

import goal.core.program.Message;
import goal.core.runtime.MessagingService;
import goal.tools.errorhandling.Resources;
import goal.tools.errorhandling.Warning;
import goal.tools.errorhandling.WarningStrings;
import goal.tools.errorhandling.exceptions.GOALMessagingException;
import goal.tools.errorhandling.exceptions.GOALRuntimeErrorException;

import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import nl.tudelft.goal.messaging.exceptions.MessagingException;
import nl.tudelft.goal.messaging.messagebox.MessageBox;
import nl.tudelft.goal.messaging.messagebox.MessageBoxListener;

/**
 * Provides the agents {@link MessagingCapabilities} by using a
 * {@link MessageBox}. The complexity of using the message box is hidden from
 * the agent.
 *
 * @author mpkorstanje
 *
 */

public class DefaultMessagingCapabilities implements MessagingCapabilities {

	private final MessageBox messageBox;

	/**
	 * The incoming message queue of the {@link AgentMesg}. Thread safe.
	 */
	private final Queue<Message> messageInQueue = new ConcurrentLinkedQueue<>();

	private final MessagingService messaging;

	/**
	 * Constructs the default messaging capabilities.
	 *
	 * @param messaging
	 *            system used to communicate
	 * @param messageBox
	 *            connected to the messaging system
	 */
	public DefaultMessagingCapabilities(MessagingService messaging,
			MessageBox messageBox) {
		this.messaging = messaging;
		this.messageBox = messageBox;

		this.messageBox.addListener(listener);
	}

	private final MessageBoxListener listener = new MessageBoxListener() {

		@Override
		public boolean newMessage(nl.tudelft.goal.messaging.Message message) {
			switch (message.getSender().getType()) {
			case GOALAGENT:
				// another agent has sent us a message.
				messageInQueue.add((Message) message.getContent());
				return true;
			default:
				// If we get here, we don't know how to handle the
				// message.
				throw new GOALMessagingException("Agent " + getId()
						+ " does not know how to handle received message "
						+ message + ".");
			}
		}
	};

	/*
	 * (non-Javadoc)
	 *
	 * @see goal.core.agent.Capabilities#clear()
	 */
	@Override
	public void reset() {
		messageInQueue.clear();

	}

	private String getId() {
		return messageBox.getId().getName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see goal.core.agent.Capabilities#getAllMessages()
	 */
	@Override
	public Set<Message> getAllMessages() {
		LinkedHashSet<Message> messages = new LinkedHashSet<>();
		while (!messageInQueue.isEmpty()) {
			messages.add(messageInQueue.remove());
		}
		return messages;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see goal.core.agent.Capabilities#postMessage(goal.core.agent.Message)
	 */
	@Override
	public void postMessage(Message message) {
		// Send mails to each of the receivers of the message.
		for (AgentId receiver : message.getReceivers()) {
			try {
				messageBox.send(messageBox.createMessage(receiver, message,
						null));
			} catch (MessagingException e) {
				throw new GOALMessagingException(e.getMessage(), e);
			}
		}
	}

	@Override
	public void dispose() {
		try {
			messageBox.removeListener(listener);
		} catch (MessagingException e) {
			// FIXME: This should never throw an exception.
		}

		try {
			messaging.deleteMessageBox(messageBox);
		} catch (GOALRuntimeErrorException e) {
			// FIXME: Messaging should provide better information here.
			// This can fail because the server was shut down, in which case we
			// can ignore it. Or because of another reason, in which case we
			// might not.
			new Warning(Resources.get(WarningStrings.FAILED_DELETE_MSGBOX), e);
		}
	}
}
