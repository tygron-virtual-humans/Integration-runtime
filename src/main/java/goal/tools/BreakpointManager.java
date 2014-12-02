package goal.tools;

import goal.core.program.GOALProgram;
import goal.core.program.Module;
import goal.core.program.actions.Action;
import goal.core.program.actions.ActionCombo;
import goal.core.program.rules.Rule;
import goal.parser.IParsedObject;
import goal.parser.ParsedObject;
import goal.tools.debugger.BreakPoint;
import goal.tools.debugger.BreakPoint.Type;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages all breakpoints set by a user in a file.
 *
 * @author K.Hindriks
 */
public class BreakpointManager {
	/**
	 * Map that keeps track for each file (not necessarily an agent file!) for
	 * which {@link IParsedObject}s present in that file a breakpoint has been
	 * set.
	 */
	private final Map<File, Set<IParsedObject>> breakpoints;
	private final PlatformManager platform;

	public BreakpointManager(PlatformManager platform) {
		this.platform = platform;
		this.breakpoints = new HashMap<>();
	}

	/**
	 * Determines the set of objects in this program on which a breakpoint may
	 * be set.
	 *
	 * @param program
	 *            The program.
	 *
	 * @return The set of objects in the program on which a breakpoint may be
	 *         set. They are sorted. The ID of the objects will be the index in
	 *         the returned list.
	 */
	public static List<ParsedObject> getBreakpointObjects(GOALProgram program) {
		// Collect all objects for which a breakpoint can be set.
		List<ParsedObject> objects = new LinkedList<>();
		// All action specifications can be breakpoints.
		objects.addAll(program.getAllActionSpecs());

		// #2117: All condition and action parts of rules can be breakpoints.
		for (Module module : program.getAllModules()) {
			int ruleCount = module.getRuleSet().getRuleCount();
			for (int i = 0; i < ruleCount; i++) {
				Rule rule = module.getRuleSet().getRule(i);
				objects.add(rule.getCondition());
				objects.add(rule.getAction());
			}
		}

		// All non-anonymous modules may also be breakpoints.
		for (Module module : program.getAllModules()) {
			if (module.isAnonymous()) {
				continue;
			}
			objects.add(module);
		}

		Collections.sort(objects);

		// See to it that all objects have a unique identifier.
		for (int i = 0; i < objects.size(); i++) {
			objects.get(i).setID(i);
		}

		return objects;
	}

	/**
	 * Updates all breakpoints used by agent program (and all child files).
	 * Called usually when launching a MAS.
	 *
	 * @param agentFile
	 *            the file to update.
	 */
	public void updateBreakpoints(File agentFile) {
		// make sure to also update the breakpoints for all child files.
		List<File> affectedFiles = new LinkedList<>();
		affectedFiles.add(agentFile);
		affectedFiles.addAll(platform.getGOALProgram(agentFile).getImports());

		for (File file : affectedFiles) {
			// remove and re-add all breakpoints of all affected files
			Set<IParsedObject> originalBPs = breakpoints.remove(file);
			if (originalBPs == null) {
				continue;
			}
			for (IParsedObject bp : originalBPs) {
				// HACK. we should respect breakpoint types.
				addBreakpoint(bp.getSource().getSourceFile(), new BreakPoint(
						agentFile, bp.getSource().getLineNumber() - 1,
						bp instanceof Action ? BreakPoint.Type.CONDITIONAL
								: BreakPoint.Type.ALWAYS));
			}
		}

		// Update the set of breakpoint objects.
		// FIXME: where and when do we need to do this??
		for (ParsedObject bpObj : getBreakpointObjects(platform
				.getGOALProgram(agentFile))) {
			breakpoints.get(agentFile).add(bpObj);
		}
	}

	/**
	 * Attempts to add a breakpoint.
	 *
	 * @param sourceFile
	 *            The file to add a breakpoint to.
	 * @param bpt
	 *            The breakpoint suggestion that should be added. Line number of
	 *            the breakpoint must be &ge; 0. (0-based)
	 *
	 * @return <ul>
	 *         <li>-2 if there is no reference to the given source file in this
	 *         registry.</li>
	 *         <li>-1 if there is a reference, but there is no breakpoint object
	 *         after or on the indicated line</li>
	 *         <li>A number &ge; lineNumber indicating the line on which a
	 *         breakpoint was set.</li>
	 *         </ul>
	 */
	private int addBreakpoint(File sourceFile, BreakPoint bpt) {
		// first check if the given file is used by any of the agents files
		List<File> referencingAgentFiles = getReferencingAgentFiles(sourceFile);
		if (referencingAgentFiles.isEmpty()) {
			return -2;
		}
		int line = -1;

		// since the breakpoints are not stored on an agent-basis but a
		// file-basis, we only need to check the breakpoints of one of the
		// agents that reference the given file.
		GOALProgram program = platform.getGOALProgram(referencingAgentFiles
				.get(0));
		for (IParsedObject bp : getBreakpointObjects(program)) {
			// breakpointLocations.get(referencingAgentFiles.get(0))) {
			/*
			 * since the breakpoint-objects are ordered, the first match is the
			 * first object after the given line in the given file. If the
			 * breakpoint is conditional, search for the first action
			 */
			if (bp.getSource().definedAfter(sourceFile, bpt.getLine())
					&& (bpt.getType() == Type.ALWAYS || (bp instanceof ActionCombo))) {
				line = bp.getSource().getLineNumber();
				addBreakpoint(bp);
				break;
			}
		}
		return line;
	}

	/**
	 * Adds a breakpoint to the set of breakpoints of a certain file.
	 *
	 * @param breakpos
	 *            The location to put a breakpoint. Contains a reference to the
	 *            file it is located in.
	 */
	private void addBreakpoint(IParsedObject breakpos) {
		assert breakpos != null;
		if (!breakpoints.containsKey(breakpos.getSource().getSourceFile())) {
			breakpoints.put(breakpos.getSource().getSourceFile(),
					new HashSet<IParsedObject>());
		}
		Set<IParsedObject> bps = breakpoints.get(breakpos.getSource()
				.getSourceFile());
		bps.add(breakpos);
	}

	/**
	 * Sets the breakpoints for the specified file to the specified set of
	 * breakpoints. The given line numbers may not be the actual location of the
	 * breakpoints after this call; they are only indications.
	 *
	 * @param file
	 *            The file to set the breakpoints for.
	 * @param bpts
	 *            The breakpoints to set.
	 */
	public void setBreakpoints(File file, Set<BreakPoint> bpts) {
		List<File> referencingAgentFiles = getReferencingAgentFiles(file);
		// don't bother settings breakpoints for files that are not referenced.
		if (referencingAgentFiles.isEmpty()) {
			return;
		}
		// remove all old breakpoints for the file
		breakpoints.remove(file);
		// add all the given lines as new breakpoints
		for (BreakPoint bpt : bpts) {
			addBreakpoint(file, bpt);
		}
	}

	/**
	 * The set of breakpoints for the given file. May return null when that file
	 * is not referenced by any of the parsed agent files in this registry, or
	 * if the agent file is not valid at the moment.
	 *
	 * @param file
	 *            The file to get the breakpoints for
	 * @return A set of IParsedObjects corresponding to breakpoints.
	 */
	public Set<IParsedObject> getBreakpoints(File file) {
		// return null when the agent file is not valid. Indicates that the
		// breakpoints should not be updated.
		List<File> referencingAgentFiles = getReferencingAgentFiles(file);
		if (referencingAgentFiles.isEmpty()) {
			return null;
		}
		// return the breakpoints iff one of the referencing agent files is
		// correctly parsed.
		for (File agentFile : referencingAgentFiles) {
			if (platform.getGOALProgram(agentFile) != null) {
				return breakpoints.get(file);
			}
		}
		return null;
	}

	/**
	 * Get a list of agent goal files that reference the given file. The goal
	 * files that we check are the {@link PlatformManager#getAllGOALFiles()}.
	 * The given file may be an agent file, in which case that file is included
	 * as well (assuming it is part of this registry).
	 *
	 * @param file
	 *            a {@link File} that agents might refer to.
	 * @return list of all goal files that use given file.
	 */
	private List<File> getReferencingAgentFiles(File file) {
		List<File> referencingAgentFiles = new LinkedList<>();
		for (File agentFile : platform.getAllGOALFiles()) {
			for (File importedFile : platform.getGOALProgram(agentFile)
					.getImports()) {
				if (importedFile.equals(file)) {
					referencingAgentFiles.add(agentFile);
					break;
				}
			}
			if (agentFile.getName().equals(file.getName())) {
				referencingAgentFiles.add(agentFile);
			}
		}
		return referencingAgentFiles;
	}

	/**
	 * Get ALL the breakpoints, from the goal file and from the imports of the
	 * goal file.
	 *
	 * @param goalProgramFile
	 *            a file containing a {@link GOALProgram}.
	 * @return list of all breakpoints in the program, including imported files.
	 *         List can be empty if no breakpoints have been set for this goal
	 *         program.
	 */
	public Set<IParsedObject> getAllBreakpoints(File goalProgramFile) {
		HashSet1<IParsedObject> breakpts = new HashSet1<>();
		breakpts.addAll(getBreakpoints(goalProgramFile));
		for (File importedFile : platform.getGOALProgram(goalProgramFile)
				.getImports()) {
			breakpts.addAll(getBreakpoints(importedFile));
		}
		return breakpts;
	}

	/**
	 * set that is robust to adding null.
	 *
	 * @author W.Pasman 16sep14
	 *
	 * @param <T>
	 */
	class HashSet1<T> extends HashSet<T> {
		/**
		 *
		 */
		private static final long serialVersionUID = -8565625930726693792L;

		public void addAll(Set<T> elements) {
			if (elements != null) {
				super.addAll(elements);
			}
		}
	}
}
