package main;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import epa.*;
import org.xml.sax.SAXException;

public class EPAComparator
{

	final private static String OUTPUT = "OUTPUT_FILE";
	final private static String SUBJECTS_FOLDER_EPA_PATH = "SUBJECTS_FOLDER_EPA_PATH";
	final private static String METRICS_FOLDER_PATH = "METRICS_FOLDER_PATH";
	final private static String INFERRED_XML_EPA_NAME = "INFERRED_XML_EPA_NAME";
	final private static String CRITERIA = "CRITERIA";
	final private static String SUBJECTS = "SUBJECTS";
	final private static String BUG_TYPES = "BUG_TYPES";
	final private static String BUDGETS = "BUDGETS";

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException
	{
		 if(args.length != 1)
		 {
		 	System.err.println("You must enter 1 parameter: properties file path");
		 	System.exit(1);
		 }

		String propertyFile = args[0];
		Properties appProps = loadProperty(propertyFile);

		final int MAX_ID = 1;
		String output_filename = appProps.getProperty(OUTPUT);
		String subjects_folder_epa = appProps.getProperty(SUBJECTS_FOLDER_EPA_PATH);
		String metrics_folder = appProps.getProperty(METRICS_FOLDER_PATH);
		String inferred_epa_xml_name = appProps.getProperty(INFERRED_XML_EPA_NAME);
		String[] criteria = appProps.getProperty(CRITERIA).replaceAll(" ", "").split(",");
		String[] subjects = appProps.getProperty(SUBJECTS).replaceAll(" ", "").split(",");
		String[] bug_types = appProps.getProperty(BUG_TYPES).replaceAll(" ", "").split(",");
		String[] budgets = appProps.getProperty(BUDGETS).replaceAll(" ", "").split(",");

		List<List<String>> data = new ArrayList<>();
		for(String bug_type : bug_types) {
			for(String budget : budgets) {
				for (String subject : subjects) {
					for (String criterion : criteria) {
						String golden_epa_path = Paths.get(subjects_folder_epa, subject, "epa", subject + ".xml").toString();
						int repeticion = -1;
						while (repeticion < MAX_ID) {
							repeticion += 1;
							System.out.printf("==============================> RUNNING for subject '%s' with id '%s', criterion = '%s', budget = '%s', bug_type = '%s'%n", subject, repeticion, criterion, budget, bug_type);
							String inferred_epa_path = Paths.get(metrics_folder, subject, bug_type, ("maxtime"), budget, criterion, repeticion+"", inferred_epa_xml_name).toString();

							if(!new File(inferred_epa_path).exists()) {
								System.err.println("inferred epa xml path does not exists: " + inferred_epa_path);
								continue;
							}

							EPA golden_epa = EPAFactory.buildEPA(golden_epa_path);
							golden_epa.getInitialState().setName(EPAState.INITIAL_STATE.getName());
							EPA inferred_epa = EPAFactory.buildEPA(inferred_epa_path);

							//
							// Golden EPA
							//

							// size(states)
							int golden_states_size = getStatesToCover(golden_epa).size();
							System.out.println("SIZE EPA GOLDEN STATES = " + golden_states_size);

							// size(transitions)
							int golden_transition_size = golden_epa.getTransitions().size();
							System.out.println("SIZE EPA GOLDEN TRANSITIONS = " + golden_transition_size);

							// size(normal transitions)
							int golden_normalTransitions_size = golden_epa.getNormalTransitions().size();
							System.out.println("SIZE EPA GOLDEN NORMAL TRANSITIONS = " + golden_normalTransitions_size);

							// size(exceptional transitions)
							int golden_exceptionalTransitions_size = golden_epa.getExceptionalTransitions().size();
							System.out.println("SIZE EPA GOLDEN EXCEPTIONAL TRANSITIONS = " + golden_exceptionalTransitions_size);

							//
							// Inferred EPA
							//

							// size(states)
							int inferred_states_size = getStatesToCover(inferred_epa).size();
							System.out.println("SIZE INFERRED GOLDEN STATES = " + inferred_states_size);

							// size(transitions)
							int inferred_transition_size = inferred_epa.getTransitions().size();
							System.out.println("SIZE EPA INFERRED TRANSITIONS = " + inferred_transition_size);

							// size(normal transitions)
							int inferred_normalTransitions_size = inferred_epa.getNormalTransitions().size();
							System.out.println("SIZE EPA INFERRED NORMAL TRANSITIONS = " + inferred_normalTransitions_size);
							EPA normalizedInferredEPA = getNormalizedInferredEPA(inferred_epa, golden_epa);
							Set<EPATransition> not_covered_golden_txs = golden_epa.getNormalTransitions();
							not_covered_golden_txs.removeAll(normalizedInferredEPA.getNormalTransitions());
							System.out.println("\t not covered normal transitions" + not_covered_golden_txs);
							Set<EPATransition> not_covered_inferred_txs = normalizedInferredEPA.getNormalTransitions();
							not_covered_inferred_txs.removeAll(golden_epa.getNormalTransitions());
							System.out.println("\t NEW covered normal transitions in inferredEPA" + not_covered_inferred_txs);


							// size(exceptional transitions)
							int inferred_exceptionalTransitions_size = inferred_epa.getExceptionalTransitions().size();
							System.out.println("SIZE EPA INFERRED EXCEPTIONAL TRANSITIONS = " + inferred_exceptionalTransitions_size);

							inferred_epa = EPAFactory.buildEPA(inferred_epa_path);
							int coveredGoldenStates = getCoveredEPAStates(golden_epa, inferred_epa).size();
							System.out.println("\nTOTAL GOLDEN STATES TO COVER = " + getStatesToCover(golden_epa).size());
							System.out.println("COVERED STATES = " + coveredGoldenStates + "  <-- " + getCoveredEPAStates(golden_epa, inferred_epa));
							Set<EPAState> notCoveredGoldenStates = golden_epa.getStates();
							notCoveredGoldenStates.removeAll(normalizedInferredEPA.getStates());
							System.out.println("NOT COVERED STATES = " + notCoveredGoldenStates.size() + " <-- " + notCoveredGoldenStates);

							List<String> current = new ArrayList<>();
							current.add(repeticion+"");//id
							current.add(bug_type); // bug type
							current.add(budget);//Budget
							current.add(subject); //Subject
							current.add(criterion);
							current.add(golden_states_size+"");
							current.add(coveredGoldenStates+"");
							current.add(inferred_states_size+"");
							current.add(golden_transition_size+"");
							int covered_golden_txs = (golden_transition_size-not_covered_golden_txs.size());
							current.add(covered_golden_txs+"");
							current.add(inferred_transition_size+"");
							current.add(not_covered_inferred_txs.size()+"");
							current.add(inferred_normalTransitions_size+"");
							current.add(inferred_exceptionalTransitions_size+"");

							data.add(current);
						}
					}

				}
			}
		}

		writeOutputCSV(output_filename, data);
	}

	private static Set<EPAState> getStatesToCover(EPA goldenEPA)
	{
		return goldenEPA.getStates().stream().filter(epaState-> !(epaState.getName().equals(EPAState.INITIAL_STATE.getName()))).collect(Collectors.toSet());
	}

	private static Set<Set<String>> getEnabledActions(EPA inferredEPA)
	{
		Set<Set<String>> enabledActions = new HashSet<>();
		for (EPAState state : inferredEPA.getStates()) {
			Set<String> enabledList = getEnabledActionsInState(state, inferredEPA);
			enabledActions.add(enabledList);
		}
		return enabledActions;
	}

	private static Set<String> getEnabledActionsInState(EPAState state, EPA inferredEPA)
	{
		Set<String> enabledList = new HashSet<>();
		List<String> actions = inferredEPA.getActionNamesFromStateName(state);
		if (actions.size() == 0)
			return null; // TODO
		Set<String> transitions = inferredEPA.getTransitions(state).stream().map(EPATransition::getActionName)
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
		return enabledList;
	}

	// Devuelve los estados cubiertos de la epa golden
	private static Set<EPAState> getCoveredEPAStates(EPA goldenEPA, EPA inferredEPA)
	{
		Set<EPAState> coveredEPAStates = new HashSet<>();
		Set<EPAState> toCoverStates = getStatesToCover(goldenEPA);
		for (EPAState epaState : toCoverStates) {
			if(isCovered(epaState, goldenEPA, inferredEPA))
				coveredEPAStates.add(epaState);
		}
		return coveredEPAStates;
	}

	// devuelve true ssi el estado (pertenece a la golden epa) es cubierto por la epa inferida
	private static boolean isCovered(EPAState epaGoldenState, EPA goldenEPA, EPA inferredEPA)
	{
		Set<EPATransition> golden_normalTransitions = goldenEPA.getNormalTransitions(epaGoldenState);
		Set<String> golden_normalTransitions_ActionNames = golden_normalTransitions.stream()
				.map(EPATransition::getActionName).collect(Collectors.toSet());
		Set<Set<String>> inferred_enabledActions = getEnabledActions(inferredEPA);

		return inferred_enabledActions.stream().anyMatch(enabledActions -> enabledActions.equals(golden_normalTransitions_ActionNames));
	}

	private static boolean areEquals(EPAState goldenState, EPAState inferredState, EPA goldenEPA, EPA inferredEPA)
	{
		Set<EPATransition> golden_normalTransitions = goldenEPA.getNormalTransitions(goldenState);
		Set<String> golden_normalTransitions_ActionNames = golden_normalTransitions.stream()
				.map(EPATransition::getActionName).collect(Collectors.toSet());
		Set<String> inferred_enabledActions = getEnabledActionsInState(inferredState, inferredEPA);

		assert inferred_enabledActions != null;
		return inferred_enabledActions.equals(golden_normalTransitions_ActionNames);
	}

	// devuelve la epa inferida pero cambia los nombres de los estados según la golden epa
	private static EPA getNormalizedInferredEPA(final EPA inferredEPA, EPA goldenEPA)
	{
		for(EPAState goldenState : goldenEPA.getStates()) {
			for(EPAState inferredState : inferredEPA.getStates()) {
				if(areEquals(goldenState, inferredState, goldenEPA, inferredEPA))
					inferredState.setName(goldenState.getName());
			}
		}
		return inferredEPA;
	}

	private static Properties loadProperty(String arg)
	{
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(arg));
		} catch (IOException e) {
			System.err.println("File does not exists: " + arg + ". Error: " + e.getMessage());
		}

		for(String property : properties.stringPropertyNames()) {
			switch (property) {
				case OUTPUT:
					break;
				case SUBJECTS_FOLDER_EPA_PATH:
				case METRICS_FOLDER_PATH:
					if(!checkFolder(property, properties))
						System.exit(1);
					break;
				case INFERRED_XML_EPA_NAME:
					break;
				case CRITERIA:
					break;
				case SUBJECTS:
					break;
				case BUG_TYPES:
					if(!Arrays.stream(properties.getProperty(property).split(",")).allMatch(b ->
							b.toUpperCase().equals("ALL") || b.toUpperCase().equals("ERRPROT"))) {
						System.err.println("Bug type does not exists: " + properties.getProperty(property));
						System.exit(1);
					}
					break;
				case BUDGETS:
					break;
				default:
					System.out.println("Unknown defined property: " + property);
					break;
			}

		}
		if (!properties.stringPropertyNames().stream().allMatch(p -> (p.equals(OUTPUT) || p.equals(SUBJECTS_FOLDER_EPA_PATH) ||
				p.equals(METRICS_FOLDER_PATH) || p.equals(INFERRED_XML_EPA_NAME) || p.equals(CRITERIA) || p.equals(SUBJECTS) ||
				p.equals(BUG_TYPES) || p.equals(BUDGETS))))
			System.err.println("Some input value not defined");
		return properties;
	}

	private static boolean checkFolder(String property, Properties properties)
	{
		String path = properties.getProperty(property);
		if(!new File(path).exists()) {
			System.err.println("(PROPERTY ERROR - " + property + "). File does not exists: " + properties.getProperty(property));
			return false;
		}
		return true;
	}

	private static void writeOutputCSV(String output_filename, List<List<String>> data) throws IOException
	{
		FileWriter writer = new FileWriter(output_filename);
		String HEADERS = "ID,BUG_TYPE,BUD,SUBJ,CRITERION,STATES_GOLDEN,COVERED_GOLDEN_STATES,INFERRED_STATES,GOLDEN_TXS,COVERED_GOLDEN_TXS,INFERRED_TXS,NOT_IN_GOLDEN_TXS,NORMAL_INFERRED_TXS,EXCEP_INFERRED_TXS";
		writer.write(HEADERS);
		writer.append("\n");
		for(List<String> lines : data) {
			boolean f = true;
			for(String column : lines) {
				if(f) {
					f = false;
					writer.append(column);
				}
				else {
					writer.append(",");
					writer.append(column);
				}
			}
			writer.append("\n");
		}
		writer.close();
	}

}