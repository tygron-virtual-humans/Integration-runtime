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

package goal.tools.mc.property.ltl;

import goal.tools.mc.core.lmhashset.LMHashSet;

import java.util.Iterator;

/**
 * Represents an LTL conjunction whose conjuncts are unordered (i.e.
 * commutative).
 * 
 * @author sungshik
 * 
 */
public final class Conjunction extends Formula {

	/**
	 * The arguments of this conjunction, i.e. its conjuncts.
	 */
	private final LMHashSet<Formula> args = new LMHashSet<Formula>();

	//
	// Constructors
	//

	/**
	 * Constructs an empty conjunction.
	 */
	public Conjunction() {
	}

	/**
	 * Constructs a conjunction whose conjuncts are specified by the specified
	 * set.
	 * 
	 * @param args
	 *            - The conjuncts.
	 */
	public Conjunction(LMHashSet<Formula> args) {
		this.args.addAll(args);
	}

	//
	// Public methods
	//

	/**
	 * Adds a conjunct to this conjunction.
	 * 
	 * @param f
	 *            - The conjunct to be added.
	 */
	public void addConjunct(Formula f) {
		args.add(f);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Conjunction) {
			Conjunction object = (Conjunction) o;
			boolean equals = args.size() == object.args.size();
			Iterator<Formula> iterator = args.iterator();
			while (iterator.hasNext() && equals) {
				equals = equals && object.args.contains(iterator.next());
			}
			return equals;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return 4;
	}

	@Override
	public LMHashSet<Formula> getArgs() {
		return args;
	}

	/**
	 * Checks if this conjunction is empty.
	 * 
	 * @return <code>true</code> if empty; <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return args.isEmpty();
	}

	@Override
	public String toString() {
		String string = "";
		Iterator<Formula> iterator = args.iterator();
		while (iterator.hasNext()) {
			Formula argument = iterator.next();
			if (argument instanceof Disjunction || argument instanceof Until
					|| argument instanceof Release) {
				string += "{ " + argument.toString() + " }";
			} else {
				string += argument.toString();
			}
			if (iterator.hasNext()) {
				string += " && ";
			}
		}
		return string;
	}
}
