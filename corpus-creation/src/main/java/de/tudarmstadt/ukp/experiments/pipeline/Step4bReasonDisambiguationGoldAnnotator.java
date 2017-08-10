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
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.DisambiguatedStance;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MACEHelper;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MTurkOutputReader;
import de.tudarmstadt.ukp.experiments.pipeline.gold.SingleWorkerAssignment;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentsFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.Frequency;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.util.*;

/**
 * Annotates types of reasons back to the premises; there are four types:
 * ORIGINAL, OPPOSITE, BOTH_POSSIBLE, REPHRASED_ORIGINAL
 * <p>
 * Note that the output file will change after each run as the properties in the DKPro-Argumentation
 * save timestamp (see https://github.com/dkpro/dkpro-argumentation/issues/24 )
 *
 * @author Ivan Habernal
 */
public class Step4bReasonDisambiguationGoldAnnotator
{
    public static SortedMap<String, String> annotateWithGoldLabels(File mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter)
            throws Exception
    {
        return annotateWithGoldLabels(Collections.singletonList(mTurkOutputCSVFile),
                originalXMLArgumentsFile, outputFile, maceThreshold, assignmentsFilter, null);
    }

    public static SortedMap<String, String> annotateWithGoldLabels(List<File> mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter)
            throws Exception
    {
        return annotateWithGoldLabels(mTurkOutputCSVFile, originalXMLArgumentsFile, outputFile,
                maceThreshold, assignmentsFilter, null);
    }

    public static SortedMap<String, String> annotateWithGoldLabels(List<File> mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter, File workerStatisticsOutputCSV)
            throws Exception
    {
        MTurkOutputReader outputReader = new MTurkOutputReader(true,
                mTurkOutputCSVFile.toArray(new File[mTurkOutputCSVFile.size()]));

        // {argument ID: worker assignments}
        // these are sorted by date
        SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> stanceAssignments = new TreeMap<>();

        for (Map<String, String> row : outputReader) {

            String workerId = row.get("workerid");
            String acceptTimeString = row.get("assignmentaccepttime");

            Date time = MACEHelper.DATE_FORMAT.parse(acceptTimeString);

            int assignmentsPerHIT = 0;

            // stances
            for (String columnName : row.keySet()) {
                if (columnName.contains("_stance_group")) {
                    String reasonId = columnName.replaceAll("Answer.", "")
                            .replaceAll("_stance_group", "");

                    Integer stanceValue = Integer.valueOf(row.get(columnName));

                    assignmentsPerHIT++;

                    // create and fill a new object
                    SingleWorkerAssignment<Integer> stanceAssignment = new SingleWorkerAssignment<>(
                            workerId, time, stanceValue);

                    stanceAssignments.putIfAbsent(reasonId, new TreeSet<>());
                    stanceAssignments.get(reasonId).add(stanceAssignment);
                }

            }
        }

        // any filtering of assignments?
        if (assignmentsFilter != null) {
            stanceAssignments = assignmentsFilter.filterAssignments(stanceAssignments);
        }

        MACEHelper.GoldLabelEstimationResultContainer estimatedStancesContainer = MACEHelper
                .estimateGoldLabels(stanceAssignments, maceThreshold);
        SortedMap<String, String> estimatedStanceAssignmentsWithNulls = estimatedStancesContainer.goldLabels;

        SortedMap<String, String> estimatedStanceAssignments = new TreeMap<>();
        for (String key : estimatedStanceAssignmentsWithNulls.keySet()) {
            if (estimatedStanceAssignmentsWithNulls.get(key) != null) {
                estimatedStanceAssignments.put(key, estimatedStanceAssignmentsWithNulls.get(key));
            }
        }

        // there is one reason that had a missing stance in the data, annotate it manually
        estimatedStanceAssignments.put("17848547_16", "1");

        // save worker statistics
        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV, MACEHelper
                    .saveWorkerStatisticsToCSV(estimatedStancesContainer.workerCompetences,
                            mTurkOutputCSVFile.get(0)));
        }


        // also extract all argument ids from the reason ids for a faster look-up
        Set<String> argumentIDs = new TreeSet<>();
        for (String reasonId : estimatedStanceAssignments.keySet()) {
            argumentIDs.add(reasonId.split("_")[0]);
        }

        System.out.println("Total estimated: " + estimatedStanceAssignmentsWithNulls.size());
        System.out.println("Estimated non-null: " + estimatedStanceAssignments.size());

        // and push it back to the data
        // read all arguments from the original corpus
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(originalXMLArgumentsFile);

        // now add the annotations back to the jcas and save
        List<StandaloneArgument> annotatedArguments = new ArrayList<>();

        Frequency frequency = new Frequency();

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

                boolean somePremisesAnnotated = false;

                for (Premise premise : premises) {
                    // get an id
                    String premiseId = StandaloneArgumentWithSinglePremise
                            .createUniquePremiseId(argumentCopy.getId(), premise);

                    if (estimatedStanceAssignments.get(premiseId) != null) {
                        // look-up its annotated gist
                        int disambiguatedStanceInt = Integer
                                .valueOf(estimatedStanceAssignments.get(premiseId));
                        DisambiguatedStance disambiguatedStance = DisambiguatedStance
                                .fromInt(disambiguatedStanceInt);

                        // filtering out premise gist that contains "because" as rephrased
                        String gist = ArgumentUnitUtils.getProperty(premise, "gist");
                        if (gist.contains(" because ")) {
                            disambiguatedStance = DisambiguatedStance.REPHRASED_ORIGINAL;
                        }

                        ArgumentUnitUtils.setProperty(premise, "disambiguatedStance",
                                disambiguatedStance.toString());
                        argumentCopy.setJCas(jCas);

                        // set flag
                        somePremisesAnnotated = true;

                        frequency.addValue(disambiguatedStance.toString());
                    }
                }

                if (somePremisesAnnotated) {
                    annotatedArguments.add(argumentCopy);

                    System.out.println(
                            "***************************************************************");
                    System.out.println(argumentCopy.getAnnotatedStance());
                    System.out.println(argumentCopy.getText());
                    System.out.println(
                            ArgumentDumpWriter.dumpArguments(argumentCopy.getJCas(), true, false));
                }
            }
        }

        // save annotated arguments to output xml
        System.out.println("Saving " + annotatedArguments.size() + " arguments with "
                + estimatedStanceAssignments.size() + " disambiguated stances");

        System.out.println(frequency);
        XStreamSerializer.serializeToXml(annotatedArguments, outputFile);

        return estimatedStanceAssignments;
    }

    public static void main(String[] args)
            throws Exception
    {
        // 22 big batch 1-5000
        annotateWithGoldLabels(
                Arrays.asList(
                        // combine both the pilot data and the full batch
                        new File(
                                "mturk/annotation-task/61-reason-disambiguation-batch-0001-5000-4294reasons-0060-4294-task.output.csv"),
                        new File(
                                "mturk/annotation-task/60-pilot-reason-disambiguation-task.output.csv")
                ),
                new File(
                        "mturk/annotation-task/data/42-reasons-gist-batch-0001-5000-4294reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons.xml.gz"),
                1.00, null, new File(
                        "61-reason-disambiguation-batch-0001-5000-4294reasons-task.worker-stats.csv"));
    }
}
