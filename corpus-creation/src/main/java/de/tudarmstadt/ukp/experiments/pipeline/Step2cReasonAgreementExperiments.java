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

import com.google.common.collect.TreeBasedTable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.dkpro.statistics.agreement.unitizing.KrippendorffAlphaUnitizingAgreement;
import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.GoldEstimationResult;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentFilterRandomized;
import de.tudarmstadt.ukp.experiments.pipeline.uima.AnnotationSpans;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static de.tudarmstadt.ukp.experiments.pipeline.Step1cStanceAgreementExperiments.printTable;

/**
 * Annotation agreement experiments for Reason span annotation (as shown in the Appendix table)
 *
 * @author Ivan Habernal
 */
public class Step2cReasonAgreementExperiments
{
    public static void main(String[] args)
            throws Exception
    {
        final File csvFile = new File(
                "mturk/annotation-task/31-pilot-reasons-task.output.csv");
        final File argumentsFile = new File(
                "mturk/annotation-task/data/arguments-with-full-segmentation-rfd.xml.gz");

        TreeBasedTable<Integer, Double, DescriptiveStatistics> table = TreeBasedTable.create();

        TreeBasedTable<Integer, Double, DescriptiveStatistics> tableAnnotatedArguments = TreeBasedTable
                .create();

        IntStream.range(1, 10).parallel().forEach(crowdSize -> {

            //            Arrays.asList(0.85, 0.9, 0.95, 1.0).parallelStream().forEach(maceThreshold -> {
            Arrays.asList(0.94, 0.96, 0.98, 1.0).parallelStream().forEach(maceThreshold -> {
                // ten random repeats
                for (int i = 0; i < 20; i++) {
                    Random random = new Random(i);

                    try {
                        File crowdExpert1 = File.createTempFile("crowd1", ".xml.gz");
                        File crowdExpert2 = File.createTempFile("crowd2", ".xml.gz");

                        GoldEstimationResult goldEstimationResult1 = Step2bGoldReasonAnnotator
                                .annotateWithGoldLabels(csvFile, argumentsFile, crowdExpert1,
                                        maceThreshold,
                                        new WorkerAssignmentFilterRandomized(18, 1, crowdSize,
                                                random),
                                        null);

                        GoldEstimationResult goldEstimationResult2 = Step2bGoldReasonAnnotator
                                .annotateWithGoldLabels(csvFile, argumentsFile, crowdExpert2,
                                        maceThreshold,
                                        new WorkerAssignmentFilterRandomized(18, 2, crowdSize,
                                                random),
                                        null);

                        synchronized (tableAnnotatedArguments) {
                            if (!tableAnnotatedArguments.contains(crowdSize, maceThreshold)) {
                                tableAnnotatedArguments
                                        .put(crowdSize, maceThreshold,
                                                new DescriptiveStatistics());
                            }
                            tableAnnotatedArguments.get(crowdSize, maceThreshold)
                                    .addValue(goldEstimationResult1.annotatedInstances);
                            tableAnnotatedArguments.get(crowdSize, maceThreshold)
                                    .addValue(goldEstimationResult2.annotatedInstances);
                        }

                        double kappa = computeKappa(crowdExpert1, crowdExpert2);

                        synchronized (table) {
                            if (!table.contains(crowdSize, maceThreshold)) {
                                table.put(crowdSize, maceThreshold,
                                        new DescriptiveStatistics());
                            }
                            table.get(crowdSize, maceThreshold).addValue(kappa);
                        }

                        FileUtils.forceDelete(crowdExpert1);
                        FileUtils.forceDelete(crowdExpert2);

                        synchronized (table) {
                            System.out.println("===================================");
                            printTable(table);
                            System.out.println("===================================");
                        }
                        synchronized (tableAnnotatedArguments) {
                            printTable(tableAnnotatedArguments);
                            System.out.println("===================================");
                        }
                    }
                    catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }

                }
                printTable(table);
            });
        });
    }

    private static double computeKappa(File crowdExpert1, File crowdExpert2)
            throws IOException
    {
        // read all arguments from the original corpus
        List<StandaloneArgument> expert1Arguments = XStreamSerializer
                .deserializeArgumentListFromXML(crowdExpert1);
        List<StandaloneArgument> expert2Arguments = XStreamSerializer
                .deserializeArgumentListFromXML(crowdExpert2);

        Set<String> expert1IDs = new HashSet<>();
        Set<String> expert2IDs = new HashSet<>();
        for (StandaloneArgument argument : expert1Arguments) {
            expert1IDs.add(argument.getId());
        }
        for (StandaloneArgument argument : expert2Arguments) {
            expert2IDs.add(argument.getId());
        }

        // intersection
        SortedSet<String> intersectionIDs = new TreeSet<>();
        intersectionIDs.addAll(expert1IDs);
        intersectionIDs.retainAll(expert2IDs);

        SortedMap<String, AnnotationSpans> expert1spans = new TreeMap<>();
        SortedMap<String, AnnotationSpans> expert2spans = new TreeMap<>();

        for (StandaloneArgument argument : expert1Arguments) {
            if (intersectionIDs.contains(argument.getId())) {
                expert1spans.put(argument.getId(),
                        AnnotationSpans.extractAnnotationSpans(argument.getJCas()));
            }
        }

        for (StandaloneArgument argument : expert2Arguments) {
            if (intersectionIDs.contains(argument.getId())) {
                expert2spans.put(argument.getId(),
                        AnnotationSpans.extractAnnotationSpans(argument.getJCas()));
            }
        }

        // compute the overall length
        int totalLength = 0;
        for (AnnotationSpans spans : expert1spans.values()) {
            totalLength += spans.getDocumentLength();
        }

        UnitizingAnnotationStudy study = new UnitizingAnnotationStudy(2, totalLength);

        int offset = 0;
        for (AnnotationSpans spans : expert1spans.values()) {
            for (AnnotationSpans.SingleAnnotationSpan singleSpan : spans.getAnnotationSpans()) {
                study.addUnit(singleSpan.getRelativeOffset() + offset, singleSpan.getLength(), 0,
                        singleSpan.getType());
            }
            offset += spans.getDocumentLength();
        }

        offset = 0;
        for (AnnotationSpans spans : expert2spans.values()) {
            for (AnnotationSpans.SingleAnnotationSpan singleSpan : spans.getAnnotationSpans()) {
                study.addUnit(singleSpan.getRelativeOffset() + offset, singleSpan.getLength(), 1,
                        singleSpan.getType());
            }
            offset += spans.getDocumentLength();
        }

        KrippendorffAlphaUnitizingAgreement agreement = new KrippendorffAlphaUnitizingAgreement(
                study);

        // overall agreement
        double alphaUnitized = agreement.calculateAgreement();
        System.out.println(alphaUnitized);

        return alphaUnitized;
    }

}
