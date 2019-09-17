package main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class EPAComparator {

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		// if(args.length != 3)
		// {
		// System.err.println("You must enter 3 parameters: path_golden_epa,
		// path_inferred_epa, used_criterion");
		// System.exit(1);
		// }

		// String golden_epa_path = args[0];
		// String inferred_epa_path = args[1];
		// String used_criterion_path = args[2];
		int i = -1;
		int MAX_ID = 3;
		String budget = "60";
		String criterion = "epatransitionmining";
//		String criterion = "epaadjacentedgesmining";
//		String criterion = "line_branch_exception";
//		String criterion = "line_branch_exception_epaadjacentedgesmining";
//		 String subject = "JDBCResultSet"; // muchos!!!!
//		String subject = "ListItr"; // OK!
//		 String subject = "NumberFormatStringTokenizer"; // OK!
//		 String subject = "SftpConnection"; // muchos!!!!
//		 String subject = "Signature"; // OK!
//		 String subject = "SMTPProcessor_h"; //OK!
		 String subject = "SMTPProtocol"; // OK!
		// String subject = "Socket"; // OK!
		// String subject = "StackAr"; //OK!
		// String subject = "StringTokenizer"; //OK!
		// String subject = "ToHTMLStream"; // OK!
//		 String subject = "ZipOutputStream"; // OK!
		 
		String golden_epa_path = "C:\\Users\\JGodoy\\Replication-Package\\epa-benchmark\\subjects\\" + subject + "\\epa\\" + subject + ".xml";
		EPA golden_epa = null;
		EPA inferred_epa = null;

		while (i < MAX_ID) {
			i += 1;
			System.out.println("==============================> RUNNING for subject " + subject.toUpperCase() + " with id = " + i + " with criterion = " + criterion);
			String inferred_epa_path = "C:\\Users\\JGodoy\\Replication-Package\\epa-benchmark\\results\\metrics\\" + subject
					+ "\\all\\maxtime\\" + budget + "\\" + criterion + "\\" + i + "\\inferred_epa.xml";
			// String used_criterion_path = args[2];
			if(!new File(inferred_epa_path).exists()) {
				System.out.println("inferred epa xml path does not exists: " + inferred_epa_path);
				continue;
			}

			golden_epa = EPAFactory.buildEPA(golden_epa_path);
			inferred_epa = EPAFactory.buildEPA(inferred_epa_path);

			//
			// Golden EPA
			//

			// size(states)
			int golden_states_size = golden_epa.getStates().size();

			// size(transitions)
			int golden_transition_size = golden_epa.getTransitions().size();

			// size(normal transitions)
			int golden_normalTransitions_size = getNormalTransitions(golden_epa).size();

			// size(exceptional transitions)
			int golden_exceptionalTransitions_size = getExceptionalTransitions(golden_epa).size();

			//
			// Inferred EPA
			//

			// size(states)
			int inferred_states_size = inferred_epa.getStates().size();

			// size(transitions)
			int inferred_transition_size = inferred_epa.getTransitions().size();

			// size(normal transitions)
			int inferred_normalTransitions_size = getNormalTransitions(inferred_epa).size();

			// size(exceptional transitions)
			int inferred_exceptionalTransitions_size = getExceptionalTransitions(inferred_epa).size();

			Set<Set<String>> enabledActions = getEnabledActions(inferred_epa);

			Set<String> inferred_normalTransitions_ActionNames = getNormalTransitions(inferred_epa).stream()
					.map(t -> t.getActionName()).collect(Collectors.toSet());

			int coveredGoldenStates = getCoveredEPAStates(golden_epa, inferred_epa).size();
			System.out.println("TOTAL GOLDEN STATES = " + golden_epa.getStates().stream().filter(s-> !(s.getName().equals("S0"))).collect(Collectors.toSet()).size());
			System.out.println("COVERED STATES = " + coveredGoldenStates);
			System.out.println(getCoveredEPAStates(golden_epa, inferred_epa));
		}

	}

	private static Set<EPATransition> getNormalTransitions(EPA epa) {
		return epa.getTransitions().stream().filter(t -> t instanceof EPANormalTransition).collect(Collectors.toSet());
	}
	
	private static List<String> getActionNamesFromStateName(String epaStateName) {
		epaStateName = epaStateName.replaceAll("\\[", "").replaceAll("\\]", "");
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

	    final String name = epaStateName.substring(start, epaStateName.length()).trim();
	    if (!name.isEmpty()) {
	      actionNames.add(name);
	    }

	    return actionNames;
	  }

	private static Set<Set<String>> getEnabledActions(EPA inferredEPA) {
		Set<Set<String>> enabledActions = new HashSet<>();
		for (EPAState state : inferredEPA.getStates()) {
			Set<String> enabledList = new HashSet<>();
			List<String> actions = getActionNamesFromStateName(state.getName());
			if (actions.size() == 0)
				continue;
			Set<String> transitions = inferredEPA.getTransitions(state).stream().map(t -> t.getActionName())
					.collect(Collectors.toSet());
			for (String action : actions) {
				if (action.contains("true")) {
					String enabledAction = action.split("=")[0];
					enabledList.add(enabledAction);
				} else {
					String actionError = action.split("=")[0];
					if (transitions.contains(actionError))
						System.err.println(actionError + " - " + "no cumple predicado en el estado " + state);
				}
			}
			enabledActions.add(enabledList);
		}
		return enabledActions;
	}

	private static Set<EPATransition> getExceptionalTransitions(EPA epa) {
		return epa.getTransitions().stream().filter(t -> t instanceof EPAExceptionalTransition)
				.collect(Collectors.toSet());
	}

	private static Set<EPAState> getCoveredEPAStates(EPA goldenEPA, EPA inferredEPA) {
		Set<EPAState> coveredEPAStates = new HashSet<>();
		for (EPAState epaState : goldenEPA.getStates()) {
			Set<EPATransition> golden_normalTransitions = goldenEPA.getNormalTransitions(epaState);
			Set<String> golden_normalTransitions_ActionNames = golden_normalTransitions.stream()
					.map(t -> t.getActionName()).collect(Collectors.toSet());
			// for(EPAState epaStateInferred : inferred.getStates())
			// {
			// Set<EPATransition> inferred_normalTransitions =
			// inferred.getNormalTransitions(epaStateInferred);
			// Set<String> inferred_normalTransitions_ActionNames =
			// inferred_normalTransitions.stream().map(t ->
			// t.getActionName()).collect(Collectors.toSet());
			//// if(inferred_normalTransitions_ActionNames.containsAll(golden_normalTransitions_ActionNames)
			// ||
			// golden_normalTransitions_ActionNames.containsAll(inferred_normalTransitions_ActionNames))
			// if(inferred_normalTransitions_ActionNames.equals(golden_normalTransitions_ActionNames))
			// coveredEPAStates.add(epaState);
			// }
			Set<Set<String>> inferred_enabledActions = getEnabledActions(inferredEPA);
			if (inferred_enabledActions.contains(golden_normalTransitions_ActionNames))
				coveredEPAStates.add(epaState);
		}

		return coveredEPAStates;
	}

}
