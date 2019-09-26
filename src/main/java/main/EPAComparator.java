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
	final private static String MAX_ID = "MAX_ID";
	final private static String OUTPUT = "OUTPUT_FILE";
	final private static String SUBJECTS_FOLDER_EPA_PATH = "SUBJECTS_FOLDER_EPA_PATH";
	final private static String METRICS_FOLDER_PATH = "METRICS_FOLDER_PATH";
	final private static String INFERRED_XML_EPA_NAME = "INFERRED_XML_EPA_NAME";
	final private static String CRITERIA = "CRITERIA";
	final private static String SUBJECTS = "SUBJECTS";
	final private static String BUG_TYPES = "BUG_TYPES";
	final private static String BUDGETS = "BUDGETS";
	final private static String R_SCRIPT = "R_SCRIPT";
	final private static String R_PATH = "R_PATH";
	final private static String R_OUTPUT_FILE = "R_OUTPUT_FILE";

	static StringBuilder string_output = new StringBuilder("");

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException
	{
		 if(args.length != 1)
		 {
		 	System.err.println("You must enter 1 parameter: properties file path");
		 	System.exit(1);
		 }

		String propertyFile = args[0];
		Properties properties = loadProperty(propertyFile);

		int max_id = Integer.parseInt(properties.getProperty(MAX_ID));
		String output_file = completeHomeUserPath(properties.getProperty(OUTPUT));
		String subjects_folder_epa = completeHomeUserPath(properties.getProperty(SUBJECTS_FOLDER_EPA_PATH));
		String metrics_folder = completeHomeUserPath(properties.getProperty(METRICS_FOLDER_PATH));
		String inferred_epa_xml_name = properties.getProperty(INFERRED_XML_EPA_NAME);
		String[] criteria = properties.getProperty(CRITERIA).replaceAll(" ", "").split(",");
		String[] subjects = properties.getProperty(SUBJECTS).replaceAll(" ", "").split(",");
		String[] bug_types = properties.getProperty(BUG_TYPES).replaceAll(" ", "").split(",");
		String[] budgets = properties.getProperty(BUDGETS).replaceAll(" ", "").split(",");

		List<List<String>> data = new ArrayList<>();
		for(String bug_type : bug_types) {
			for(String budget : budgets) {
				for (String subject : subjects) {
					for (String criterion : criteria) {
						String golden_epa_path = Paths.get(subjects_folder_epa, subject, "epa", subject + ".xml").toString();
						int repeticion = -1;
						while (repeticion < max_id) {
							repeticion += 1;
							string_output.append(String.format("%n==============================> RUNNING for subject '%s' with id '%s', criterion = '%s', budget = '%s', bug_type = '%s'%n", subject, repeticion, criterion, budget, bug_type));
							String inferred_epa_path = Paths.get(metrics_folder, subject, bug_type, ("maxtime"), budget, criterion, repeticion+"", inferred_epa_xml_name).toString();

							if(!new File(inferred_epa_path).exists()) {
								string_output.append("!!!!!!inferred epa xml path does not exists: " + inferred_epa_path + "\n\n");
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
							string_output.append("SIZE EPA GOLDEN STATES = " + golden_states_size + "\n");

							// size(transitions)
							int golden_transition_size = golden_epa.getTransitions().size();
							string_output.append("SIZE EPA GOLDEN TRANSITIONS = " + golden_transition_size + "\n");

							// size(normal transitions)
							int golden_normalTransitions_size = golden_epa.getNormalTransitions().size();
							string_output.append("SIZE EPA GOLDEN NORMAL TRANSITIONS = " + golden_normalTransitions_size + "\n");

							// size(exceptional transitions)
							int golden_exceptionalTransitions_size = golden_epa.getExceptionalTransitions().size();
							string_output.append("SIZE EPA GOLDEN EXCEPTIONAL TRANSITIONS = " + golden_exceptionalTransitions_size + "\n");

							//
							// Inferred EPA
							//

							// size(states)
							int inferred_states_size = getStatesToCover(inferred_epa).size();
							string_output.append("SIZE INFERRED GOLDEN STATES = " + inferred_states_size + "\n");

							// size(transitions)
							int inferred_transition_size = inferred_epa.getTransitions().size();
							string_output.append("SIZE EPA INFERRED TRANSITIONS = " + inferred_transition_size + "\n");

							// size(normal transitions)
							int inferred_normalTransitions_size = inferred_epa.getNormalTransitions().size();
							string_output.append("SIZE EPA INFERRED NORMAL TRANSITIONS = " + inferred_normalTransitions_size + "\n");

							// size(exceptional transitions)
							int inferred_exceptionalTransitions_size = inferred_epa.getExceptionalTransitions().size();
							string_output.append("SIZE EPA INFERRED EXCEPTIONAL TRANSITIONS = " + inferred_exceptionalTransitions_size + "\n");


							EPA normalizedInferredEPA = getNormalizedInferredEPA(inferred_epa, golden_epa);
							Set<EPATransition> covered_golden_txs = golden_epa.getNormalTransitions();
							covered_golden_txs.retainAll(normalizedInferredEPA.getNormalTransitions());
							string_output.append("\t COVERED NORMAL TRANSITIONS:(" + covered_golden_txs.size() + ")\n");
							appendToNewLine(covered_golden_txs);

							Set<EPATransition> not_covered_golden_txs = golden_epa.getNormalTransitions();
							not_covered_golden_txs.removeAll(normalizedInferredEPA.getNormalTransitions());
							string_output.append("\n\t NOT COVERED NORMAL TRANSITIONS:(" + not_covered_golden_txs.size() + ")\n");
							appendToNewLine(not_covered_golden_txs);

							Set<EPATransition> not_covered_inferred_txs = normalizedInferredEPA.getNormalTransitions();
							not_covered_inferred_txs.removeAll(golden_epa.getNormalTransitions());
							string_output.append("\n\t NEW COVERED NORMAL TRANSITIONS IN INFERREDEPA:(" + not_covered_inferred_txs.size() + ")\n");
							appendToNewLine(not_covered_inferred_txs);

							Set<EPAState> newCoveredGoldenStates = normalizedInferredEPA.getStates();
							newCoveredGoldenStates.removeAll(golden_epa.getStates());
							string_output.append("\nNEW INFERRED STATES = " + newCoveredGoldenStates.size() + " <-- " + newCoveredGoldenStates + "\n");

							inferred_epa = EPAFactory.buildEPA(inferred_epa_path);
							Set<EPAState> coveredGoldenStates = getCoveredEPAStates(golden_epa, inferred_epa);
							string_output.append("\nTOTAL GOLDEN STATES TO COVER = " + getStatesToCover(golden_epa).size() + "\n");
							string_output.append("COVERED STATES = " + coveredGoldenStates.size() + "  <-- " + coveredGoldenStates + "\n");
							Set<EPAState> notCoveredGoldenStates = golden_epa.getStates();
							notCoveredGoldenStates.removeAll(normalizedInferredEPA.getStates());
							string_output.append("NOT COVERED STATES = " + notCoveredGoldenStates.size() + " <-- " + notCoveredGoldenStates + "\n");

							List<String> current = new ArrayList<>();
							current.add(repeticion+"");//id
							current.add(bug_type); // bug type
							current.add(budget);//Budget
							current.add(subject); //Subject
							current.add(criterion);
							current.add(golden_states_size+"");
							current.add(coveredGoldenStates.size()+"");
							current.add(inferred_states_size+"");
							current.add(golden_transition_size+"");
							int covered_golden_txs_size = (golden_transition_size-not_covered_golden_txs.size());
							current.add(covered_golden_txs_size+"");
							current.add(inferred_transition_size+"");
							current.add(not_covered_inferred_txs.size()+"");
							current.add(inferred_normalTransitions_size+"");
							current.add(inferred_exceptionalTransitions_size+"");

							if (inferred_normalTransitions_size + inferred_exceptionalTransitions_size != inferred_transition_size) {
								System.err.printf("ERROR in subject '%s' with id '%s', criterion = '%s', budget = '%s', bug_type = '%s'%n", subject, repeticion, criterion, budget, bug_type);
								System.err.printf("Error al obtener transiciones de la epa inferida: %s=Normal, %s=Exceptional, %s=total%n%n", inferred_normalTransitions_size, inferred_exceptionalTransitions_size, inferred_transition_size);
							}

							data.add(current);
						}
					}

				}
			}
		}

		writeOutputCSV(output_file, data);
		runRScript(output_file, properties);

		String output_log = "log_output.txt";
		FileWriter writer = new FileWriter(output_log);
		writer.write(string_output.append("\n").toString());
		writer.close();
		System.out.printf("%nLog ouput saved to %s", new File(output_log).getAbsoluteFile());
	}

	private static void appendToNewLine(Set<?> collection)
	{
		collection.forEach(t-> string_output.append(String.format("\t- %s%n", t)));
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
				if (transitions.contains(actionError)) {
					String out = "\n" + actionError + " - " + "no cumple predicado en el estado " + state + "\n";
					string_output.append(out);
					System.err.println(out);
				}
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

	// devuelve true ssi el estado (que pertenece a la golden epa) es cubierto por la epa inferida
	private static boolean isCovered(EPAState epaGoldenState, EPA goldenEPA, EPA inferredEPA)
	{
		if(isIsolatedState(goldenEPA, epaGoldenState))
			return false;
		Set<EPATransition> golden_normalTransitions = goldenEPA.getNormalTransitions(epaGoldenState);
		Set<String> golden_normalTransitions_ActionNames = golden_normalTransitions.stream()
				.map(EPATransition::getActionName).collect(Collectors.toSet());
		Set<Set<String>> inferred_enabledActions = getEnabledActions(inferredEPA);
		return inferred_enabledActions.stream().anyMatch(enabledActions -> enabledActions.equals(golden_normalTransitions_ActionNames));
	}

	private static boolean areEquals(EPAState goldenState, EPAState inferredState, EPA goldenEPA, EPA inferredEPA)
	{
		if(isIsolatedState(goldenEPA, goldenState))
			return false;
		Set<EPATransition> golden_normalTransitions = goldenEPA.getNormalTransitions(goldenState);
		Set<String> golden_normalTransitions_ActionNames = golden_normalTransitions.stream()
				.map(EPATransition::getActionName).collect(Collectors.toSet());
		Set<String> inferred_enabledActions = getEnabledActionsInState(inferredState, inferredEPA);

		assert inferred_enabledActions != null;
		return inferred_enabledActions.equals(golden_normalTransitions_ActionNames);
	}

	private static boolean isIsolatedState(EPA epaGolden, EPAState goldenState)
	{
		return epaGolden.getTransitions().stream().noneMatch(t -> t.getOriginState().equals(goldenState));
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
				case MAX_ID:
				case BUDGETS:
				case R_OUTPUT_FILE:
				case INFERRED_XML_EPA_NAME:
				case CRITERIA:
				case SUBJECTS:
				case R_PATH:
					break;
				case SUBJECTS_FOLDER_EPA_PATH:
				case METRICS_FOLDER_PATH:
					if (!checkFolder(property, properties))
						System.exit(1);
					break;
				case BUG_TYPES:
					if (!Arrays.stream(properties.getProperty(property).split(",")).allMatch(b ->
							b.toUpperCase().equals("ALL") || b.toUpperCase().equals("ERRPROT"))) {
						System.err.println("Bug type does not exists: " + properties.getProperty(property));
						System.exit(1);
					}
					break;
				case R_SCRIPT:
					if (!checkFolder(property, properties))
						System.err.println("R script will not be executed!");
					break;
				default:
					System.err.println("Unknown defined property: " + property);
					break;
			}
		}

		//obligatorios
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(OUTPUT)))) {
			System.err.printf("Input value '%s' not defined in properties file", OUTPUT);
			System.exit(1);
		}
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(MAX_ID)))) {
			System.err.printf("Input value '%s' not defined in properties file", MAX_ID);
			System.exit(1);
		}
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(SUBJECTS_FOLDER_EPA_PATH)))) {
			System.err.printf("Input value '%s' not defined in properties file", SUBJECTS_FOLDER_EPA_PATH);
			System.exit(1);
		}
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(METRICS_FOLDER_PATH)))) {
			System.err.printf("Input value '%s' not defined in properties file", METRICS_FOLDER_PATH);
			System.exit(1);
		}
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(INFERRED_XML_EPA_NAME)))) {
			System.err.printf("Input value '%s' not defined in properties file", INFERRED_XML_EPA_NAME);
			System.exit(1);
		}
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(CRITERIA)))) {
			System.err.printf("Input value '%s' not defined in properties file", CRITERIA);
			System.exit(1);
		}
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(SUBJECTS)))) {
			System.err.printf("Input value '%s' not defined in properties file", SUBJECTS);
			System.exit(1);
		}
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(BUG_TYPES)))) {
			System.err.printf("Input value '%s' not defined in properties file", BUG_TYPES);
			System.exit(1);
		}
		if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(BUDGETS)))) {
			System.err.printf("Input value '%s' not defined in properties file", BUDGETS);
			System.exit(1);
		}

		return properties;
	}

	private static boolean checkFolder(String property, Properties properties)
	{
		String path = completeHomeUserPath(properties.getProperty(property));
		if(!new File(path).exists()) {
			System.err.println("(PROPERTY ERROR - " + property + "). File does not exists: " + properties.getProperty(property));
			return false;
		}
		return true;
	}

	private static String completeHomeUserPath(String path)
	{
		if(new File(path).exists() || path.startsWith("/") || path.startsWith("C:"))
			return path;
		String home_dir = System.getProperty("user.home");
		return Paths.get(home_dir, path).toString();
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

	private static void runRScript(String file_path_input, Properties properties)
	{
		final Runtime r = Runtime.getRuntime();
		String r_path = properties.getProperty(R_PATH);
		String r_script = properties.getProperty(R_SCRIPT);
		String r_output_file = properties.getProperty(R_OUTPUT_FILE);
//		if(r_path == null || !(new File(r_path).exists())
//				|| r_script == null || !(new File(r_script).exists()))
//			return;
		String command = String.format("%s %s %s %s", r_path, r_script, file_path_input, r_output_file);
		string_output.append(String.format("%n%nRunning command '%s'%n", command));
		Process p = null;
		try {
			p = r.exec(command);
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		if(p != null && p.exitValue() == 0)
			string_output.append("RScript executed successfully!");
	}

}