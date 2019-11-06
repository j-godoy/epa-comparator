#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)

# test if there is at least one argument: if not, return an error
if (length(args)!=2) {
  stop("Input csv file argument must be supplied (epa-comparator.csv) and also output file path", call.=FALSE)
}

csv_filename = args[1]
output_file = args[2]


# run the script
stats = read.csv(csv_filename, header=TRUE, sep=",")

subjects = unique(stats$SUBJ)
criteria = unique(stats$CRITERION)
bug_type = unique(stats$BUG_TYPE)
budgets = unique(stats$BUD)
strategies = unique(stats$STRATEGY)

printHeader <- function()
{
																															
	cat("STRATEGY","BUG_TYPE", "BUD", "SUBJ", "CRITERION", "#REP", "STATES_GOLDEN", "COVERED_GOLDEN_STATES", "NEVER_COVERED_GOLDEN_STATES", "INFERRED_STATES", "GOLDEN_TXS", "COVERED_GOLDEN_TXS", "NEVER_COVERED_GOLDEN_TXS", "INFERRED_TXS", "NOT_IN_GOLDEN_TXS", "UNIQUE_NEW_TX", "NORMAL_INFERRED_TXS", "EXCEP_INFERRED_TXS", sep=", ")
	cat("\n")
}


printAvgEpaComparator <- function() {
	for (strategy in strategies) {
		for (b_type in bug_type) {
			for (budget in budgets) {
				for(subj in subjects) {
					error_type = b_type
					default_rows = {}
					for (criterion in criteria) {
						default_rows  = subset(stats,SUBJ==subj & CRITERION==criterion & BUG_TYPE==b_type & BUD==budget)
						states_golden_avg = round(mean(default_rows$STATES_GOLDEN), digits=2)
						covered_golden_states_avg = paste(round(round(mean(default_rows$COVERED_GOLDEN_STATES), digits=2)*100/states_golden_avg, digits=2), "%(", round(mean(default_rows$COVERED_GOLDEN_STATES), digits=2), "/", states_golden_avg, ")", sep="")
						never_covered_golden_states = max(default_rows$NEVER_COVERED_GOLDEN_STATES)
						inferred_states = round(mean(default_rows$INFERRED_STATES), digits=2)
						golden_txs_avg = round(mean(default_rows$GOLDEN_TXS), digits=2)
						covered_golden_txs_avg = paste(round(round(mean(default_rows$COVERED_GOLDEN_TXS), digits=2)*100/golden_txs_avg, digits=2), "%(", round(mean(default_rows$COVERED_GOLDEN_TXS), digits=2), "/", golden_txs_avg, ")", sep="")
						never_covered_golden_txs = max(default_rows$NEVER_COVERED_GOLDEN_TXS)
						inferred_txs_avg = round(mean(default_rows$INFERRED_TXS), digits=2)
						not_in_golden_txs_avg = round(mean(default_rows$NOT_IN_GOLDEN_TXS), digits=2)
						unique_new_tx = max(default_rows$UNIQUE_NEW_TX)
						normal_inferred_txs_avg = round(mean(default_rows$NORMAL_INFERRED_TXS), digits=2)
						excep_inferred_txs_avg = round(mean(default_rows$EXCEP_INFERRED_TXS), digits=2)
						repeticiones = length(default_rows$STATES_GOLDEN)

						cat(strategy, error_type, budget, subj, criterion, repeticiones, states_golden_avg, covered_golden_states_avg, never_covered_golden_states, inferred_states, golden_txs_avg, covered_golden_txs_avg, never_covered_golden_txs,
						inferred_txs_avg, not_in_golden_txs_avg, unique_new_tx, normal_inferred_txs_avg, excep_inferred_txs_avg, sep=", ")
						cat("\n")
					}
				}
			}
		}
	}
}

sink(output_file)
printHeader()
printAvgEpaComparator()
sink()
