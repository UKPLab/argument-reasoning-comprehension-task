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

import de.tudarmstadt.ukp.dkpro.argumentation.io.writer.ArgumentDumpWriter;
import de.tudarmstadt.ukp.dkpro.argumentation.types.ArgumentUnitUtils;
import de.tudarmstadt.ukp.dkpro.argumentation.types.Premise;
import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Adds "gist" property to reasons in the arguments as annotated by MTurkers.
 *
 * @author Ivan Habernal
 */
public class Step3bReasonGistAnnotator
{

    public static GoldEstimationResult annotateWithGoldLabels(File mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, File workerStatisticsOutputCSV,
            Integer lastNHoursOnly)
            throws Exception
    {
        MTurkOutputReader outputReader = new MTurkOutputReader(false, mTurkOutputCSVFile);

        // if true, the worker checked "this is no reason"
        SortedMap<String, SortedSet<SingleWorkerAssignment<Boolean>>> noReasonAssignments = new TreeMap<>();

        SortedMap<String, SortedSet<SingleWorkerAssignment<String>>> reasonGistAssignments = new TreeMap<>();

        // for saving HIT ids for all reason ids -- in case additional assignments are required
        //        SortedMap<String, String> reasonIdHITIdMap = new TreeMap<>();

        Date notBefore = null;
        if (lastNHoursOnly != null) {
            notBefore = new Date(
                    System.currentTimeMillis() - TimeUnit.HOURS.toMillis(lastNHoursOnly));
        }

        for (Map<String, String> row : outputReader) {
            String workerId = row.get("workerid");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy",
                    Locale.ENGLISH);
            Date time = dateFormat.parse(row.get("assignmentaccepttime"));

            if ((notBefore != null && time.after(notBefore)) || notBefore == null) {

                for (String columnName : row.keySet()) {
                    if (columnName.contains("_no_reason")) {
                        String reasonId = columnName.replaceAll("^Answer.", "")
                                .replaceAll("_no_reason$", "");

                        SingleWorkerAssignment<Boolean> assignment = new SingleWorkerAssignment<>(
                                workerId, time, true);

                        if (!noReasonAssignments.containsKey(reasonId)) {
                            noReasonAssignments.put(reasonId, new TreeSet<>());
                        }
                        noReasonAssignments.get(reasonId).add(assignment);
                    }

                    if (columnName.contains("_rephrasedReason")) {
                        String reasonId = columnName.replaceAll("^Answer.", "")
                                .replaceAll("_rephrasedReason$", "");

                        SingleWorkerAssignment<Boolean> assignment = new SingleWorkerAssignment<>(
                                workerId, time, false);

                        if (!noReasonAssignments.containsKey(reasonId)) {
                            noReasonAssignments.put(reasonId, new TreeSet<>());
                        }
                        noReasonAssignments.get(reasonId).add(assignment);

                        // and now the reason gist
                        SingleWorkerAssignment<String> reasonGistAssignment = new SingleWorkerAssignment<>(
                                workerId, time, row.get(columnName));
                        if (!reasonGistAssignments.containsKey(reasonId)) {
                            reasonGistAssignments.put(reasonId, new TreeSet<>());
                        }
                        reasonGistAssignments.get(reasonId).add(reasonGistAssignment);
                    }
                }
            }
        }

        //        System.out.println(noReasonAssignments);

        MACEHelper.GoldLabelEstimationResultContainer maceResults = MACEHelper
                .estimateGoldLabels(noReasonAssignments, 1.0);
        SortedMap<String, String> noReasonGoldLabels = maceResults.goldLabels;

        //        System.out.println(reasonGistAssignments);
        //        System.out.println(noReasonGoldLabels);

        // remove these from the reasonGistAssignments
        for (Map.Entry<String, String> entry : noReasonGoldLabels.entrySet()) {
            // if true, we remove it
            if (Boolean.valueOf(entry.getValue())) {
                System.out.println("Removing " + entry.getKey() + " because it's 'not-a-reason'");
                reasonGistAssignments.remove(entry.getKey());
            }
        }

        // select only one final gist
        ReasonGistSelector gistSelector = new ReasonGistSelectorSingleWorkerOnly();

        SortedMap<String, String> finalReasonGistMap = new TreeMap<>();
        for (Map.Entry<String, SortedSet<SingleWorkerAssignment<String>>> entry : reasonGistAssignments
                .entrySet()) {
            finalReasonGistMap
                    .put(entry.getKey(), gistSelector.selectFinalReasonGist(entry.getValue()));
        }

        // also extract all argument ids from the reason ids for a faster look-up
        Set<String> argumentIDs = new TreeSet<>();
        for (String reasonId : finalReasonGistMap.keySet()) {
            argumentIDs.add(reasonId.split("_")[0]);
        }

        // and push it back to the data
        // read all arguments from the original corpus
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(originalXMLArgumentsFile);

        // now add the annotations back to the jcas and save
        List<StandaloneArgument> annotatedArguments = new ArrayList<>();

        for (StandaloneArgument standaloneArgument : arguments) {
            // only if any reason from the argument was annotated
            if (argumentIDs.contains(standaloneArgument.getId())) {

                // and now annotate
                StandaloneArgument argumentCopy = new StandaloneArgument(standaloneArgument);
                JCas jCas = argumentCopy.getJCas();
                Collection<Premise> premises = new ArrayList<>(
                        JCasUtil.select(jCas, Premise.class));
                if (premises.isEmpty()) {
                    throw new IllegalStateException("No premises found");
                }

                for (Premise premise : premises) {
                    // get an id
                    String premiseId = StandaloneArgumentWithSinglePremise
                            .createUniquePremiseId(argumentCopy.getId(), premise);

                    boolean hasAnnotatedGist = false;
                    boolean isNotAReason = false;

                    // look-up its annotated gist
                    String annotatedGist = finalReasonGistMap.get(premiseId);
                    if (annotatedGist != null) {
                        ArgumentUnitUtils.setProperty(premise, "gist", annotatedGist);

                        hasAnnotatedGist = true;
                    }

                    if (noReasonGoldLabels.containsKey(premiseId) && Boolean
                            .valueOf(noReasonGoldLabels.get(premiseId))) {
                        ArgumentUnitUtils.setProperty(premise, "not-a-reason", "true");

                        isNotAReason = true;
                    }

                    if (hasAnnotatedGist && isNotAReason) {
                        throw new IllegalStateException("Premise " + premiseId
                                + " has annotated gist but marked as not-a-reason");
                    }
                }

                argumentCopy.setJCas(jCas);

                annotatedArguments.add(argumentCopy);

                System.out.println(ArgumentDumpWriter.dumpArguments(argumentCopy.getJCas()));
            }
        }

        // save worker statistics
        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV, MACEHelper
                    .saveWorkerStatisticsToCSV(maceResults.workerCompetences, mTurkOutputCSVFile));
        }

        // save annotated arguments to output xml
//        System.out.println(noReasonGoldLabels.size() + " reasons marked as not-a-reason");
        XStreamSerializer.serializeToXml(annotatedArguments, outputFile);

        // re-load the data and compute again the statistics
        List<StandaloneArgument> deserialized = XStreamSerializer
                .deserializeArgumentListFromXML(outputFile);
        System.out.println(arguments.size() + " arguments were originally to be annotated");
        System.out.println("Original reason statistics:");
        System.out.println(arguments.parallelStream()
                .mapToInt(value -> JCasUtil.select(value.getJCas(), Premise.class).size())
                .summaryStatistics());

        System.out.println(deserialized.size() + " arguments are in the output dataset");
        System.out.println("Reason with gist statistics:");
        System.out.println(deserialized.parallelStream()
                .mapToLong(value -> JCasUtil.select(value.getJCas(), Premise.class).stream()
                        .filter(premise -> ArgumentUnitUtils
                                .getProperty(premise, "gist") != null).count()
                ).summaryStatistics());

        /*
        SortedSet<String> hitsForFurtherAssignments = new TreeSet<>();
        // get all HIT ids for which "no-reason" was selected
        for (String reasonId : noReasonGoldLabels.keySet()) {
            if (Boolean.valueOf(noReasonGoldLabels.get(reasonId))) {
                hitsForFurtherAssignments.add(reasonIdHITIdMap.get(reasonId));
            }
        }
        */

        return null;
    }

    public static void main(String[] args)
            throws Exception
    {
        annotateWithGoldLabels(new File(
                        "mturk/annotation-task/42-reasons-gist-batch-0001-5000-5119reasons-task.output.csv"),
                new File(
                        "mturk/annotation-task/data/32-reasons-batch-0001-5000-2026args-gold.xml.gz"),
                new File(
                        "mturk/annotation-task/data/42-reasons-gist-batch-0001-5000-4294reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/42-reasons-gist-batch-0001-5000-5119reasons-task.worker-stats.csv"),
                null);
    }
}
