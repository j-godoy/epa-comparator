package main;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import epa.*;
import org.xml.sax.SAXException;

import static main.Main.completeHomeUserPath;
import static main.Options.*;

class EPAComparator
{
	private static StringBuilder string_output = new StringBuilder();

	void run(Properties properties) throws ParserConfigurationException, SAXException, IOException
	{
		int max_id = Integer.parseInt(properties.getProperty(MAX_ID));
		String output_file = completeHomeUserPath(properties.getProperty(OUTPUT));
		String subjects_folder_epa = completeHomeUserPath(properties.getProperty(SUBJECTS_FOLDER_EPA_PATH));
		String metrics_folder = completeHomeUserPath(properties.getProperty(METRICS_FOLDER_PATH));
		String inferred_epa_xml_name = properties.getProperty(INFERRED_XML_EPA_NAME);
		String[] criteria = properties.getProperty(CRITERIA).replaceAll(" ", "").split(",");
		String[] subjects = properties.getProperty(SUBJECTS).replaceAll(" ", "").split(",");
		String[] bug_types = properties.getProperty(BUG_TYPES).replaceAll(" ", "").split(",");
		String[] budgets = properties.getProperty(BUDGETS).replaceAll(" ", "").split(",");
		String[] strategies = properties.getProperty(STRATEGY).replaceAll(" ", "").split(",");

		List<List<String>> data = new ArrayList<>();
		for(String strategy : strategies) {
			for (String bug_type : bug_types) {
				for (String budget : budgets) {
					for (String subject : subjects) {
						for (String criterion : criteria) {
							Set<EPATransition> alreadyCoveredTxs = new HashSet<>();
							Set<EPATransition> newInferredTxs = new HashSet<>();
							Set<EPAState> alreadyCoveredStates = new HashSet<>();

							String golden_epa_path = Paths.get(subjects_folder_epa, subject, "epa", subject + ".xml").toString();
							int repeticion = -1;
							while (repeticion < max_id) {
								repeticion += 1;
								string_output.append(String.format("%n==============================> RUNNING for subject '%s' with id '%s', criterion = '%s', budget = '%s', bug_type = '%s', strategy = '%s'%n", subject, repeticion, criterion, budget, bug_type, strategy));
								String inferred_epa_path = Paths.get(metrics_folder, subject, bug_type, ("maxtime"), budget, strategy, criterion, repeticion + "", inferred_epa_xml_name).toString();

								if (!new File(inferred_epa_path).exists()) {
									string_output.append("!!!!!!inferred epa xml path does not exists: ").append(inferred_epa_path).append("\n\n");
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
								string_output.append("SIZE EPA GOLDEN STATES = ").append(golden_states_size).append("\n");

								// size(transitions)
								int golden_transition_size = golden_epa.getTransitions().size();
								string_output.append("SIZE EPA GOLDEN TRANSITIONS = ").append(golden_transition_size).append("\n");

								// size(normal transitions)
								int golden_normalTransitions_size = golden_epa.getNormalTransitions().size();
								string_output.append("SIZE EPA GOLDEN NORMAL TRANSITIONS = ").append(golden_normalTransitions_size).append("\n");

								// size(exceptional transitions)
								int golden_exceptionalTransitions_size = golden_epa.getExceptionalTransitions().size();
								string_output.append("SIZE EPA GOLDEN EXCEPTIONAL TRANSITIONS = ").append(golden_exceptionalTransitions_size).append("\n");

								//
								// Inferred EPA
								//

								// size(states)
								int inferred_states_size = getStatesToCover(inferred_epa).size();
								string_output.append("SIZE INFERRED GOLDEN STATES = ").append(inferred_states_size).append("\n");

								// size(transitions)
								int inferred_transition_size = inferred_epa.getTransitions().size();
								string_output.append("SIZE EPA INFERRED TRANSITIONS = ").append(inferred_transition_size).append("\n");

								// size(normal transitions)
								int inferred_normalTransitions_size = inferred_epa.getNormalTransitions().size();
								string_output.append("SIZE EPA INFERRED NORMAL TRANSITIONS = ").append(inferred_normalTransitions_size).append("\n");

								// size(exceptional transitions)
								int inferred_exceptionalTransitions_size = inferred_epa.getExceptionalTransitions().size();
								string_output.append("SIZE EPA INFERRED EXCEPTIONAL TRANSITIONS = ").append(inferred_exceptionalTransitions_size).append("\n");


								EPA normalizedInferredEPA = getNormalizedInferredEPA(inferred_epa, golden_epa);
								Set<EPATransition> covered_golden_txs = golden_epa.getNormalTransitions();
								covered_golden_txs.retainAll(normalizedInferredEPA.getNormalTransitions());
								string_output.append("\t COVERED NORMAL TRANSITIONS:(").append(covered_golden_txs.size()).append(")\n");
								appendToNewLine(covered_golden_txs);
								alreadyCoveredTxs.addAll(covered_golden_txs);

								Set<EPATransition> not_covered_golden_txs = golden_epa.getNormalTransitions();
								not_covered_golden_txs.removeAll(normalizedInferredEPA.getNormalTransitions());
								string_output.append("\n\t NOT COVERED NORMAL TRANSITIONS:(").append(not_covered_golden_txs.size()).append(")\n");
								appendToNewLine(not_covered_golden_txs);

								Set<EPATransition> not_covered_inferred_txs = normalizedInferredEPA.getNormalTransitions();
								not_covered_inferred_txs.removeAll(golden_epa.getNormalTransitions());
								newInferredTxs.addAll(not_covered_inferred_txs);
								string_output.append("\n\t NEW COVERED NORMAL TRANSITIONS IN INFERREDEPA:(").append(not_covered_inferred_txs.size()).append(")\n");
								appendToNewLine(not_covered_inferred_txs);

								Set<EPAState> newCoveredGoldenStates = normalizedInferredEPA.getStates();
								newCoveredGoldenStates.removeAll(golden_epa.getStates());
								string_output.append("\nNEW INFERRED STATES = ").append(newCoveredGoldenStates.size()).append(" <-- ").append(newCoveredGoldenStates).append("\n");

								inferred_epa = EPAFactory.buildEPA(inferred_epa_path);
								Set<EPAState> coveredGoldenStates = getCoveredEPAStates(golden_epa, inferred_epa);
								alreadyCoveredStates.addAll(coveredGoldenStates);
								string_output.append("\nTOTAL GOLDEN STATES TO COVER = ").append(getStatesToCover(golden_epa).size()).append("\n");
								string_output.append("COVERED STATES = ").append(coveredGoldenStates.size()).append("  <-- ").append(coveredGoldenStates).append("\n");
								Set<EPAState> notCoveredGoldenStates = golden_epa.getStates();
								notCoveredGoldenStates.removeAll(normalizedInferredEPA.getStates());
								notCoveredGoldenStates.removeIf(s -> isIsolatedState(golden_epa, s));
								string_output.append("NOT COVERED STATES = ").append(notCoveredGoldenStates.size()).append(" <-- ").append(notCoveredGoldenStates).append("\n");

								List<String> current = new ArrayList<>();
								current.add(repeticion + "");//id
								current.add(bug_type); // bug type
								current.add(budget);//Budget
								current.add(subject); //Subject
                                if(!strategy.equalsIgnoreCase("evosuite") && !strategy.equalsIgnoreCase("randoop"))
                                    current.add(strategy.toLowerCase() + "_" + criterion);
                                else
                                    current.add(criterion);
								current.add(golden_states_size + "");
								current.add(coveredGoldenStates.size() + "");
								//never covered states
								if (repeticion < max_id) {
									current.add("-1");
								} else {
									Set<EPAState> neverCovered_states = getStatesToCover(golden_epa);
									neverCovered_states.removeAll(alreadyCoveredStates);
									current.add(neverCovered_states.size() + "");
								}
								current.add(inferred_states_size + "");
								current.add(golden_transition_size + "");
								int covered_golden_txs_size = (golden_transition_size - not_covered_golden_txs.size());
								current.add(covered_golden_txs_size + "");
								//never covered txs
								if (repeticion < max_id) {
									current.add("-1");
								} else {
									Set<EPATransition> never_covered_txs = golden_epa.getTransitions();
									never_covered_txs.removeAll(alreadyCoveredTxs);
									current.add(never_covered_txs.size() + "");
								}
								current.add(inferred_transition_size + "");
								current.add(not_covered_inferred_txs.size() + "");
								//new unique inferred txs
								if (repeticion < max_id) {
									current.add("-1");
								} else {
									current.add(newInferredTxs.size() + "");
								}

								current.add(inferred_normalTransitions_size + "");
								current.add(inferred_exceptionalTransitions_size + "");

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
		}

		writeOutputCSV(output_file, data);
		runRScript(output_file, properties);

		String output_log = "log_output_epacomparator.txt";
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
		return goldenEPA.getStates().stream().filter(epaState-> !(epaState.getName().equals(EPAState.INITIAL_STATE.getName())) && !isIsolatedState(goldenEPA, epaState)).collect(Collectors.toSet());
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
		Set<String> transitions = inferredEPA.getOutgoingTransitions(state).stream().map(EPATransition::getActionName)
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
		if(goldenState.equals(EPAState.INITIAL_STATE) || inferredState.equals(EPAState.INITIAL_STATE)) {
			return goldenState.equals(inferredState);
		}
		Set<EPATransition> golden_normalTransitions = goldenEPA.getNormalTransitions(goldenState);
		Set<String> golden_normalTransitions_ActionNames = golden_normalTransitions.stream()
				.map(EPATransition::getActionName).collect(Collectors.toSet());
		Set<String> inferred_enabledActions = getEnabledActionsInState(inferredState, inferredEPA);

		assert inferred_enabledActions != null;
		return inferred_enabledActions.equals(golden_normalTransitions_ActionNames);
	}

	private static boolean isIsolatedState(EPA epaGolden, EPAState goldenState)
	{
		return epaGolden.getTransitions().stream().noneMatch(t -> t.getOriginState().equals(goldenState)) && epaGolden.getTransitions().stream().noneMatch(t -> t.getDestinationState().equals(goldenState));
	}

	// devuelve la epa inferida pero cambia los nombres de los estados según la golden epa
	private static EPA getNormalizedInferredEPA(final EPA inferredEPA, EPA goldenEPA)
	{
	    Set<EPAState> checkedStates = new HashSet<>();
		for(EPAState goldenState : goldenEPA.getStates()) {
			for(EPAState inferredState : inferredEPA.getStates()) {
				if(!checkedStates.contains(inferredState) && areEquals(goldenState, inferredState, goldenEPA, inferredEPA)) {
                    inferredState.setName(goldenState.getName());
                    checkedStates.add(inferredState);
                    break;
                }
			}
		}
		return inferredEPA;
	}

	private static void writeOutputCSV(String output_filename, List<List<String>> data) throws IOException
	{
		FileWriter writer = new FileWriter(output_filename);
		String HEADERS = "ID,BUG_TYPE,BUD,SUBJ,CRITERION,STATES_GOLDEN,COVERED_GOLDEN_STATES,NEVER_COVERED_GOLDEN_STATES,INFERRED_STATES,GOLDEN_TXS,COVERED_GOLDEN_TXS,NEVER_COVERED_GOLDEN_TXS," +
				"INFERRED_TXS,NOT_IN_GOLDEN_TXS,UNIQUE_NEW_TX,NORMAL_INFERRED_TXS,EXCEP_INFERRED_TXS";
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