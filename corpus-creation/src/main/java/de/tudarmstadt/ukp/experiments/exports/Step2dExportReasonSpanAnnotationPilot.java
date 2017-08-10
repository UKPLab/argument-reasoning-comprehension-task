/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.experiments.exports;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import de.tudarmstadt.ukp.experiments.pipeline.Step2bGoldReasonAnnotator;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.SingleWorkerAssignment;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentsFilter;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentsFilterSubsetByTime;
import de.tudarmstadt.ukp.experiments.pipeline.utils.TableUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;

import java.io.File;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Export crowd worker annotations from the pilot for reason span annotation; each item was
 * labeled by 18 workers. The items correspond to EDU (Elementary Discourse Units) and
 * the labels are BIO tags (Reason-B, Reason-I, O). Each item has thus 18 assignments.
 * <p>
 * The assignments are split to two groups (9+9) given by the submission time, so two files
 * for two groups of workers are created; see the paper for details about this setup.
 * <p>
 * The output format is a simple self-explaining CSV file, in the following format
 * <pre>
 * " ",A10S5LDYYYB496,A1FGKIKJYSL1MI,A1LLT1N2U68K50,A1OYYWJ2B7OQTD, ....
 * 12014246_00,-,O,O,O,-,-,-,O,-,O,-,-,-,O,Premise-B,-,O,-,-,-,-,-,O,-
 * 12014246_01,-,Premise-B,Premise-B,Premise-B,-,-,-,O,-,O,-,-,-,O,Premise-I,-,O,-,-,-,-,-,O,-
 * 12014246_02,-,Premise-I,Premise-I,Premise-I,-,-,-,O,-,O,-,-,-,O,Premise-I,-,O,-,-,-,-,-,O,-
 * 12014246_03,-,Premise-I,Premise-I,Premise-B,-,-,-,O,-,O,-,-,-,O,O,-,O,-,-,-,-,-,O,-
 * ...
 * </pre>
 * <p>
 * where the columns are worker IDs, rows are composed of "argumentId_EDUid" (so they are
 * ordered sequences of EDUs as in the original texts), and the values are "Premise-B", "Premise-I",
 * "O" or "-" if no value is available for this worker (which means that the worker did not
 * work on this task).
 *
 * @author Ivan Habernal
 */
public class Step2dExportReasonSpanAnnotationPilot
{

    public static void main(String[] args)
            throws Exception
    {
        final File csvFile = new File(
                "mturk/annotation-task/31-pilot-reasons-task.output.csv");
        final File argumentsFile = new File(
                "mturk/annotation-task/data/arguments-with-full-segmentation-rfd.xml.gz");

        final File groupExport1 = new File(
                "mturk/annotation-task/data/exported-reason-spans-pilot-group1.csv");
        final File groupExport2 = new File(
                "mturk/annotation-task/data/exported-reason-spans-pilot-group2.csv");

        // read all arguments from the original corpus
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(argumentsFile);

        SortedMap<String, SortedSet<SingleWorkerAssignment<JSONArray>>> globalReasonAssignments = Step2bGoldReasonAnnotator
                .loadGlobalReasonAssignments(
                        csvFile);

        WorkerAssignmentsFilter assignmentsFilterGroup1 = new WorkerAssignmentsFilterSubsetByTime(0,
                9,
                true);
        WorkerAssignmentsFilter assignmentsFilterGroup2 = new WorkerAssignmentsFilterSubsetByTime(10,
                18,
                true);

        SortedMap<String, SortedSet<SingleWorkerAssignment<Step2bGoldReasonAnnotator.SentenceLabel>>> globalSentenceAssignmentsGroup1 = Step2bGoldReasonAnnotator
                .loadGlobalSentenceAssignments(
                        globalReasonAssignments, arguments, assignmentsFilterGroup1);

        SortedMap<String, SortedSet<SingleWorkerAssignment<Step2bGoldReasonAnnotator.SentenceLabel>>> globalSentenceAssignmentsGroup2 = Step2bGoldReasonAnnotator
                .loadGlobalSentenceAssignments(
                        globalReasonAssignments, arguments, assignmentsFilterGroup2);

        FileUtils.write(groupExport1, TableUtils
                .tableToCsv(assignmentsToTable(globalSentenceAssignmentsGroup1), CSVFormat.DEFAULT,
                        "-"));
        FileUtils.write(groupExport2, TableUtils
                .tableToCsv(assignmentsToTable(globalSentenceAssignmentsGroup2), CSVFormat.DEFAULT,
                        "-"));
    }

    public static Table<String, String, String> assignmentsToTable(
            SortedMap<String, SortedSet<SingleWorkerAssignment<Step2bGoldReasonAnnotator.SentenceLabel>>> assignments)
    {
        TreeBasedTable<String, String, String> result = TreeBasedTable.create();

        assignments.forEach((unitID, singleWorkerAssignments) -> {
            singleWorkerAssignments.forEach(sentenceLabelSingleWorkerAssignment -> {
                String workerID = sentenceLabelSingleWorkerAssignment.getWorkerID();
                String label = sentenceLabelSingleWorkerAssignment.getLabel().toString();

                // update the table
                result.put(unitID, workerID, label);
            });
        });

        return result;
    }
}
