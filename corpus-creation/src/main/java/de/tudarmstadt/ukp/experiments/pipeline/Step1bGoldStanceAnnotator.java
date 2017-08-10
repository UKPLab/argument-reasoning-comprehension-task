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

package de.tudarmstadt.ukp.experiments.pipeline;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.Frequency;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static de.tudarmstadt.ukp.experiments.pipeline.AbstractArgumentHITCreator.checkConsistencyOfData;

/**
 * Assigns gold labels to stance annotated comments (Step 1 in the paper). At the ends, prints
 * statistics (annotated, skipped from MACE, sarcastic, etc.):
 * <p>
 * Value             Freq.       Pct.       Cum Pct.
 * 0                 2884        58%        58%
 * 0_sarc             481        10%        67%
 * 3                  285         6%        73%
 * 3_sarc              18         0%        73%
 * No stance         1083        22%        95%
 * Skipped from MACE  249         5%       100%
 * <p>
 * Where
 * <p>
 * 0 = annotated with one of the two stances
 * 0_sarc = the same as 0 but also sarcastic argument
 * 3 = takes both stances but remains neutral
 * 3_sarc = the same as 3 but also sarcastic
 *
 * @author Ivan Habernal
 */
public class Step1bGoldStanceAnnotator
{

    public static GoldEstimationResult annotateWithGoldLabels(File mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter)
            throws Exception
    {
        return annotateWithGoldLabels(mTurkOutputCSVFile, originalXMLArgumentsFile, outputFile,
                maceThreshold, assignmentsFilter, null);
    }

    public static GoldEstimationResult annotateWithGoldLabels(File mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter, File workerStatisticsOutputCSV)
            throws Exception
    {
        MTurkOutputReader outputReader = new MTurkOutputReader(false, mTurkOutputCSVFile);

        // {argument ID: worker assignments}
        // these are sorted by date
        SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> stanceAssignments = new TreeMap<>();
        SortedMap<String, SortedSet<SingleWorkerAssignment<Boolean>>> sarcasmAssignments = new TreeMap<>();

        SortedMap<String, SortedMap<String, String>> allArgsAndFeedback = new TreeMap<>();

        for (Map<String, String> row : outputReader) {
            //            System.out.println(row);

            String workerId = row.get("workerid");
            String acceptTimeString = row.get("assignmentaccepttime");

            if (acceptTimeString == null) {
                System.err.println("Skipping unanswered HIT " + row.get("hitid"));
            }
            else {

                Date time = MACEHelper.DATE_FORMAT.parse(acceptTimeString);

                // feedback
                String feedback = row.get("Answer.feedback");

                SortedMap<String, SortedSet<SingleWorkerAssignment<Boolean>>> currentHITsarcasmAssignments = new TreeMap<>();

                int assignmentsPerHIT = 0;

                // stances
                for (String columnName : row.keySet()) {
                    if (columnName.contains("_stance_group")) {
                        String argId = columnName.replaceAll("Answer.", "")
                                .replaceAll("_stance_group", "");

                        Integer stanceValue = Integer.valueOf(row.get(columnName));

                        /* post-hoc fix for one stance:
                           --
                           Machines are not gaining the upper hand on humans
                           Machines are gaining the upper hand on humans
                           --
                           When the HITs were created, there was a typo in on of the texts.
                           Later on, the typo was fixed in all data (also the original ones)
                           but it resulted into a different alphabetical order of these two
                           stances. However, this alphabetical sorting influences the value
                           assigned as stance by workers. Therefore, for the following IDs,
                           the worker's annotations 0 and 1 must be switched. (Lesson learned:
                           always use unique IDs for *everything* in the annotation pipeline!)
                         */
                        Set<String> idsWithSwitchedStances = new HashSet<>(Arrays.asList(
                                "17840023", "17889471", "17882524", "17848851", "17848547",
                                "17843878",
                                "17840378", "17916180"));
                        if (idsWithSwitchedStances.contains(argId)) {
                            switch (stanceValue) {
                            case 0:
                                stanceValue = 1;
                                break;
                            case 1:
                                stanceValue = 0;
                                break;
                            }
                        }

                        assignmentsPerHIT++;

                        // create and fill a new object
                        SingleWorkerAssignment<Integer> stanceAssignment = new SingleWorkerAssignment<>(
                                workerId, time, stanceValue);

                        // and update the global maps
                        if (!stanceAssignments.containsKey(argId)) {
                            stanceAssignments.put(argId, new TreeSet<>());
                        }

                        // create map with all arguments for sarcastic assignment
                        if (!currentHITsarcasmAssignments.containsKey(argId)) {
                            currentHITsarcasmAssignments.put(argId, new TreeSet<>());
                        }

                        boolean added = stanceAssignments.get(argId).add(stanceAssignment);
                        if (!added) {
                            System.err.println(
                                    "Entry " + stanceAssignment + " not added to "
                                            + stanceAssignments
                                            .get(argId));
                        }
                    }

                }

                // we need 5 stances
                if (assignmentsPerHIT != 5) {
                    System.err.println(
                            "Fewer assignments per HIT (" + assignmentsPerHIT + "), HIT: " + row
                                    .get("hitid") + ", worker: " + row.get("workerid"));
                }

                //            System.out.println(sarcasmAssignments);

                // sarcastic
                for (Map.Entry<String, SortedSet<SingleWorkerAssignment<Boolean>>> entry : currentHITsarcasmAssignments
                        .entrySet()) {
                    // what is the column name?
                    String argId = entry.getKey();
                    String columnName = "Answer." + argId + "_sarcastic";

                    boolean sarcastic = false;
                    if (row.containsKey(columnName)) {
                        sarcastic = "on".equals(row.get(columnName));
                    }

                    entry.getValue().add(new SingleWorkerAssignment<>(workerId, time, sarcastic));
                }

                // add the current map to the global map
                for (Map.Entry<String, SortedSet<SingleWorkerAssignment<Boolean>>> entry : currentHITsarcasmAssignments
                        .entrySet()) {
                    // create map with all arguments for sarcastic assignment
                    if (!sarcasmAssignments.containsKey(entry.getKey())) {
                        sarcasmAssignments.put(entry.getKey(), new TreeSet<>());
                    }
                    sarcasmAssignments.get(entry.getKey()).addAll(entry.getValue());
                }
            }
        }

        // any filtering of assignments?
        if (assignmentsFilter != null) {
            stanceAssignments = assignmentsFilter.filterAssignments(stanceAssignments);
            sarcasmAssignments = assignmentsFilter.filterAssignments(sarcasmAssignments);
        }

        System.out.println("Estimating gold labels for stance");
        MACEHelper.GoldLabelEstimationResultContainer estimatedStancesContainer = MACEHelper
                .estimateGoldLabels(stanceAssignments, maceThreshold);
        SortedMap<String, String> estimatedStanceAssignments = estimatedStancesContainer.goldLabels;

        // save worker statistics
        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV,
                    MACEHelper.saveWorkerStatisticsToCSV(
                            estimatedStancesContainer.workerCompetences, mTurkOutputCSVFile
                    ));
        }

        System.out.println("Estimating gold labels for sarcasm");
        MACEHelper.GoldLabelEstimationResultContainer estimatedSarcasmContainer = MACEHelper
                .estimateGoldLabels(sarcasmAssignments, maceThreshold);
        SortedMap<String, String> estimatedSarcasmAssignments = estimatedSarcasmContainer.goldLabels;

        System.out.println("Estimated sarcasm assignments");
        System.out.println(estimatedSarcasmAssignments);

        // read all arguments from the original corpus
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(originalXMLArgumentsFile);

        // now add the annotations back to the jcas and save
        List<StandaloneArgument> annotatedArguments = new ArrayList<>();

        int skippedArgumentsFromMACE = 0;

        for (StandaloneArgument argument : arguments) {
            // process only annotated ones
            if (stanceAssignments.containsKey(argument.getId())) {

                // first assign the annotated stance
                String annotatedStanceStr = estimatedStanceAssignments.get(argument.getId());
                if (annotatedStanceStr == null) {
                    skippedArgumentsFromMACE++;
                }
                else {
                    int annotatedStanceInt = Integer.valueOf(annotatedStanceStr);
                    argument.setAnnotatedStance(annotatedStanceInt);

                    // now sarcasm
                    String annotatedSarcasm = estimatedSarcasmAssignments.get(argument.getId());
                    argument.setAnnotatedSarcastic(Boolean.valueOf(annotatedSarcasm));

                    annotatedArguments.add(argument);
                }
            }
        }

        computeStatistics(annotatedArguments, skippedArgumentsFromMACE);

        // save annotated arguments to output xml
        outputFile.getParentFile().mkdirs();
        XStreamSerializer.serializeToXml(annotatedArguments, outputFile);

        return new GoldEstimationResult(annotatedArguments.size(), skippedArgumentsFromMACE);
    }

    private static void computeStatistics(List<StandaloneArgument> annotatedArguments,
            int skippedArgumentsFromMACE)
    {
        Frequency frequency = new Frequency();
        for (int i = 0; i < skippedArgumentsFromMACE; i++) {
            frequency.addValue("Skipped from MACE");
        }

        for (StandaloneArgument argument : annotatedArguments) {
            String annotatedStance = argument.getAnnotatedStance();

            if (annotatedStance != null) {
                int intValue = argument.getMappingAllStancesToInt().get(annotatedStance);
                // 0 and 1 treat as one stance (pro/con doesn't matter here)
                if (intValue == 1) {
                    intValue = 0;
                }

                frequency.addValue(String.valueOf(intValue) + (argument.isAnnotatedSarcastic() ?
                        "_sarc" :
                        ""));
            }
            else {
                frequency.addValue("No stance");
            }
        }

        System.out.println(frequency);
    }

    public static void saveOnlyNonSarcasticArgumentsWithStance(File inputFile, File outputFile)
            throws IOException
    {
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);

        checkConsistencyOfData(arguments);

        List<StandaloneArgument> annotatedArguments = new ArrayList<>();

        // filtering
        for (StandaloneArgument argument : arguments) {
            if ((argument.getAnnotatedStanceAsInt() == 0
                    || argument.getAnnotatedStanceAsInt() == 1) &&
                    !argument.isAnnotatedSarcastic()) {
                annotatedArguments.add(argument);
            }
        }

        System.out.println("Saving " + annotatedArguments.size() +
                " non-sarcastic arguments to " + outputFile);

        // save annotated arguments to output xml
        outputFile.getParentFile().mkdirs();
        XStreamSerializer.serializeToXml(annotatedArguments, outputFile);
    }

    public static void main(String[] args)
            throws Exception
    {
        // 22 big batch 1-5000
        annotateWithGoldLabels(new File(
                        "mturk/annotation-task/22-stance-batch-0001-5000-task.output.csv"),
                new File("mturk/annotation-task/data/arguments-with-full-segmentation-rfd.xml.gz"),
                new File("mturk/annotation-task/data/22-stance-batch-0001-5000-all.xml.gz"), 0.95,
                null,
                new File("mturk/annotation-task/22-stance-batch-0001-5000-task.worker-stats.csv"));

        // export only comments that take stance (aka. arguments) and are not sarcastic
        saveOnlyNonSarcasticArgumentsWithStance(
                new File("mturk/annotation-task/data/22-stance-batch-0001-5000-all.xml.gz"),
                new File(
                        "mturk/annotation-task/data/22-stance-batch-0001-5000-only-with-clear-stances.xml.gz"));
    }

}
