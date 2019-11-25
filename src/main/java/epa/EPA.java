package epa;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is a Enabledness Preservation Automata (EPA).
 * 
 * @author galeotti
 *
 */
public class EPA implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7496754070047070624L;

	final private Map<EPAState, Set<EPATransition>> map;

	private final String name;

	private final EPAState initialState;

	public EPA(String name, Map<EPAState, Set<EPATransition>> map, EPAState initialState) {
		this.name = name;
		this.map = map;
		this.initialState = initialState;
	}

	public EPAState getInitialState() {
		return initialState;
	}

	public EPAState getStateByName(String stateName) {
		final Optional<EPAState> epaStateOptional = map.keySet().stream()
				.filter(state -> state.getName().equals(stateName)).findFirst();
		return epaStateOptional.orElse(null);
	}

	public String getName() {
		return name;
	}

	public EPAState temp_anyPossibleDestinationState(EPAState originState, String actionName) {
		return map.get(originState).stream().filter(epaTransition -> epaTransition.getActionName().equals(actionName))
				.map(EPATransition::getDestinationState).findFirst().orElse(null);
	}

	public boolean containsAction(String actionName) {
		return map.values().stream().flatMap(Collection::stream)
				.filter(epaTransition -> epaTransition.getActionName().equals(actionName)).findAny().isPresent();
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}

	public Set<String> getActions() {
		final Set<String> actions = map.values().stream().flatMap(Set::stream).map(t -> t.getActionName())
				.collect(Collectors.toSet());
		return actions;
	}

	public Set<EPAState> getStates() {
		final Set<EPAState> states = new HashSet<EPAState>();
		states.add(initialState);
		states.addAll(map.keySet());
		final Set<EPAState> destination_states = map.values().stream().flatMap(Set::stream)
				.map(t -> t.getDestinationState()).collect(Collectors.toSet());
		states.addAll(destination_states);
		return states;
	}

	public Set<EPATransition> getTransitions() {
		final Set<EPATransition> epaTransitions = map.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toSet());
		return epaTransitions;
	}

	public Set<EPATransition> getOutgoingTransitions(EPAState originState) {
		return getTransitions().stream().filter(t -> t.getOriginState().equals(originState))
				.collect(Collectors.toSet());
	}

	public Set<EPATransition> getIncomingTransitions(EPAState destinationState) {
		return getTransitions().stream().filter(t -> t.getDestinationState().equals(destinationState))
				.collect(Collectors.toSet());
	}
	
	public Set<EPATransition> getNormalTransitions(EPAState originState) {
		return getTransitions().stream().filter(t -> t.getOriginState().equals(originState) && (t instanceof EPANormalTransition))
				.collect(Collectors.toSet());
	}

	public Set<EPATransition> getNormalTransitions() {
		return this.getTransitions().stream().filter(t -> t instanceof EPANormalTransition).collect(Collectors.toSet());
	}

	public Set<EPATransition> getExceptionalTransitions() {
		return this.getTransitions().stream().filter(t -> t instanceof EPAExceptionalTransition).collect(Collectors.toSet());
	}

	public List<String> getActionNamesFromStateName(EPAState state) {
		String epaStateName = state.getName();
		if(epaStateName.startsWith("["))
			epaStateName = epaStateName.substring(1);
		if(epaStateName.endsWith("]"))
			epaStateName = epaStateName.substring(0, epaStateName.length()-1);

		List<String> actionNames = new ArrayList<>();
		boolean insideParens = false;
		int start = 0;
		for (int i = 0; i < epaStateName.length(); i++) {

			if (epaStateName.charAt(i) == '(') {
				insideParens = true;
			}
			if (epaStateName.charAt(i) == ')') {
				insideParens = false;
			}
			if (epaStateName.charAt(i) == ',' && !insideParens) {
				final String name = epaStateName.substring(start, i).trim();
				start = i + 1;

				if (!name.isEmpty()) {
					actionNames.add(name);
				}
			}
		}

		final String name = epaStateName.substring(start).trim();
		if (!name.isEmpty()) {
			actionNames.add(name);
		}

		return actionNames;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((initialState == null) ? 0 : initialState.hashCode());
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EPA other = (EPA) obj;
		if (initialState == null) {
			if (other.initialState != null)
				return false;
		} else if (!initialState.equals(other.initialState))
			return false;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

    public boolean contains(EPA inferred_epa) {
	    if(this.getStates().size() < inferred_epa.getStates().size())
	        return false;
	    if(this.getTransitions().size() < inferred_epa.getTransitions().size())
	        return false;
	    for(EPAState epaState : inferred_epa.getStates()) {
            if (!this.getStates().contains(epaState)) {
                return false;
            }
        }
	    for(EPATransition epaTransition : inferred_epa.getTransitions()) {
	        if(!this.getTransitions().contains(epaTransition))
	            return false;
        }

	    return true;
    }

    public void addState(EPAState epaState) {
	    this.map.put(epaState, new HashSet<>());
    }

    public void addTransition(EPAState epaState, EPATransition epaTransition) {
	    if(!this.map.containsKey(epaState))
	        this.addState(epaState);
	    this.map.get(epaState).add(epaTransition);
    }
}
