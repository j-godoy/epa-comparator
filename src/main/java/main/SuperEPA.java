package main;

import epa.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static main.Main.completeHomeUserPath;
import static main.Options.*;

class SuperEPA
{
	private static StringBuilder string_output = new StringBuilder();
	private static List<List<String>> data = new ArrayList<>();

	void run(Properties properties) throws Exception
	{

		int max_id = Integer.parseInt(properties.getProperty(MAX_ID));
		String metrics_folder = completeHomeUserPath(properties.getProperty(METRICS_FOLDER_PATH));
		String inferred_epa_xml_name = properties.getProperty(INFERRED_XML_EPA_NAME);
		String[] criteria = properties.getProperty(CRITERIA).replaceAll(" ", "").split(",");
		String[] subjects = properties.getProperty(SUBJECTS).replaceAll(" ", "").split(",");
		String[] budgets = properties.getProperty(BUDGETS).replaceAll(" ", "").split(",");
		String[] strategies = properties.getProperty(STRATEGY).replaceAll(" ", "").split(",");

		String bug_type = "all";
		// <subject, <criteria, epa>>
		Map<String, Map<String, EPA>> super_epa_by_subject = new HashMap<>();

		for (String subject : subjects) {
			Map<String, EPA> super_epa_criterion_map = new HashMap<>();
			for (String criterion : criteria) {
				for (String strategy : strategies) {
					for (String budget : budgets) {

						// Tomo como Super EPA a la generada por la última repetición. A esta EPA voy agregando los
						//estados y transiciones que figuren en otras repeticiones/criterios
						String super_epa_path = Paths.get(metrics_folder, subject, bug_type, ("maxtime"), budget, strategy, criterion, (max_id-1) + "", inferred_epa_xml_name).toString();
						if (!new File(super_epa_path).exists()) {
							string_output.append("!!!!!!inferred epa xml path does not exists: ").append(super_epa_path).append("\n\n");
							continue;
						}
						EPA super_epa = EPAFactory.buildEPA(super_epa_path);

						int repeticion = -1;
						while (repeticion < max_id) {
							repeticion += 1;
							string_output.append(String.format("%n==============================> RUNNING for subject '%s' with id '%s', criterion = '%s', budget = '%s', bug_type = '%s', strategy = '%s'%n", subject, repeticion, criterion, budget, bug_type, strategy));
							String inferred_epa_path = Paths.get(metrics_folder, subject, bug_type, ("maxtime"), budget, strategy, criterion, repeticion + "", inferred_epa_xml_name).toString();

							if (!new File(inferred_epa_path).exists()) {
								string_output.append("!!!!!!inferred epa xml path does not exists: ").append(inferred_epa_path).append("\n\n");
								continue;
							}

							EPA inferred_epa = EPAFactory.buildEPA(inferred_epa_path);

							if(super_epa.contains(inferred_epa))
								continue;
							if(inferred_epa.contains(super_epa)) {
								super_epa = inferred_epa;
								continue;
							}

							mergeSuperEpa(super_epa, inferred_epa);
						}
						String key = String.format("%s,%s,%s,%s,%s",subject, criterion, strategy, budget, bug_type);
						super_epa_criterion_map.put(key, super_epa);
						super_epa_by_subject.put(subject, super_epa_criterion_map);
					}
				}
			}
		}

		Map<String, EPA> final_super_epas = new HashMap<>();
		//merge Epas from different criteria
		for(String key : super_epa_by_subject.keySet()) {
			Map<String, EPA> super_epas_by_criteria = super_epa_by_subject.get(key);
			saveSuperInfo(super_epas_by_criteria);
			Iterator<EPA> iterator = super_epas_by_criteria.values().iterator();
			EPA super_epa = iterator.next();
			while(iterator.hasNext()) {
				mergeSuperEpa(super_epa, iterator.next());
			}
			// <subject, EPA>
			final_super_epas.put(key, super_epa);
		}

		// save SuperEpas
		saveSuperInfo(final_super_epas);

		writeOutputCSV(data);

		String output_log = "log_output_superEpa.txt";
		FileWriter writer = new FileWriter(output_log);
		writer.write(string_output.append("\n").toString());
		writer.close();
		System.out.printf("%nLog ouput saved to %s", new File(output_log).getAbsoluteFile());
	}
	
	private static void saveSuperInfo(Map<String, EPA> super_epa_map) throws Exception {
		for(String keyName : super_epa_map.keySet()) {
			EPA epa = super_epa_map.get(keyName);
			String name = keyName.replaceAll(",","_")+"_inferredEPA.xml";
			String subject = name.split("_")[0];
			saveInferredEPA(epa, Paths.get("superEpas", subject, name).toString());
			saveAdjacentEdgesPairsInfo(keyName, epa);
		}
	}

	private static void saveAdjacentEdgesPairsInfo(String keyName, EPA epa) {
		//Check adjacentEdgesPairs
		int adjacentPairs = 0;
		int states = epa.getStates().size();
		int txs = epa.getTransitions().size();
		int normal_txs = epa.getNormalTransitions().size();
		int excep_txs = epa.getExceptionalTransitions().size();
		for(EPAState epaState : epa.getStates()) {
			int incomingTransition = epa.getIncomingTransitions(epaState).size();
			int outgoingTransition = epa.getOutgoingTransitions(epaState).size();
			adjacentPairs += incomingTransition * outgoingTransition;
		}
		data.add(Arrays.asList(keyName, states+"", txs+"", normal_txs+"", excep_txs+"", adjacentPairs+""));
	}

	private static void mergeSuperEpa(EPA super_epa, EPA inferred_epa)
	{
		//Agrego nuevos estados y transiciones encontrados A la super epa
		for(EPAState epaState : inferred_epa.getStates()) {
			for(EPATransition epaTransition : inferred_epa.getIncomingTransitions(epaState)) {
				if (!super_epa.getTransitions().contains(epaTransition))
					super_epa.addTransition(epaState, epaTransition);
			}
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static void saveInferredEPA(EPA epa, String pathToSaveEPA) throws Exception {
		try {
			EPAXMLPrinter xmlPrinter = new EPAXMLPrinter();
			String epa_xml_str = xmlPrinter.toXML(epa);
			new File(new File(pathToSaveEPA).getParent()).mkdirs();
			FileWriter writer = new FileWriter(pathToSaveEPA);
			writer.write(epa_xml_str);
			writer.close();
			EPADotPrinter printer = new EPADotPrinter();
			String dot_str = printer.toDot(epa);
			String dotFileName = pathToSaveEPA.replace(".xml", ".dot");
			writer = new FileWriter(dotFileName);
			writer.write(dot_str);
			writer.close();
		} catch (IOException e) {
			throw new Exception(e);
		}

	}

	private static void writeOutputCSV(List<List<String>> data) throws IOException
	{
		FileWriter writer = new FileWriter("adjacentEdgesInfo.csv");
		String HEADERS = "SUBJECT,CRITERION,STRATEGY,BUDGET,BUGTYPE,STATES,TXS,NORMAL_TXS,EXCEP_TXS,ADJ_PAIRS";
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