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
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentFilterRandomized;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.CohenKappaAgreement;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static de.tudarmstadt.ukp.experiments.pipeline.Step1cStanceAgreementExperiments.printTable;

/**
 * Agreement experiments for Step 4 (reason disambiguation)
 *
 * @author Ivan Habernal
 */
public class Step4cReasonDisambiguationAgreementExperiments
{
    public static void main(String[] args)
            throws Exception
    {
        final File csvFile = new File(
                "mturk/annotation-task/60-pilot-reason-disambiguation-task.output.csv");

        TreeBasedTable<Integer, Double, DescriptiveStatistics> table = TreeBasedTable.create();

        for (int crowdSize = 5; crowdSize <= 5; crowdSize++) {
            for (Double maceThreshold : Arrays.asList(0.85, 0.9, 0.95, 1.0)) {
                // ten random repeats
                for (int i = 0; i < 20; i++) {
                    Random random = new Random(i);

                    SortedMap<String, String> gold1 = Step4bReasonDisambiguationGoldAnnotator
                            .annotateWithGoldLabels(csvFile, null, null,
                                    maceThreshold,
                                    //                            new WorkerAssignmentsFilterSubsetByTime(0, crowdSize, true));
                                    new WorkerAssignmentFilterRandomized(18, 1, crowdSize, random));

                    SortedMap<String, String> gold2 = Step4bReasonDisambiguationGoldAnnotator
                            .annotateWithGoldLabels(csvFile, null, null,
                                    maceThreshold,
                                    //                            new WorkerAssignmentsFilterSubsetByTime(crowdSize, crowdSize * 2,
                                    //                                    false));
                                    new WorkerAssignmentFilterRandomized(18, 2, crowdSize, random));

                    gold1 = filterOutNullValueEntries(gold1);
                    gold2 = filterOutNullValueEntries(gold2);

                    double kappa = computeKappa(gold1, gold2);

                    if (!table.contains(crowdSize, maceThreshold)) {
                        table.put(crowdSize, maceThreshold, new DescriptiveStatistics());
                    }
                    table.get(crowdSize, maceThreshold).addValue(kappa);
                }
            }
        }
        printTable(table);

    }

    private static SortedMap<String, String> filterOutNullValueEntries(
            SortedMap<String, String> map)
    {
        SortedMap<String, String> result = new TreeMap<>();

        for (String key : map.keySet()) {
            if (map.get(key) != null) {
                result.put(key, map.get(key));
            }
        }

        return result;
    }

    private static double computeKappa(SortedMap<String, String> expert1Labels,
            SortedMap<String, String> expert2Labels)
            throws IOException
    {
        // intersection
        SortedSet<String> intersectionIDs = new TreeSet<>();
        intersectionIDs.addAll(expert1Labels.keySet());
        intersectionIDs.retainAll(expert2Labels.keySet());

        CodingAnnotationStudy study = new CodingAnnotationStudy(2);

        for (String id : intersectionIDs) {
            String expert1 = expert1Labels.get(id);
            String expert2 = expert2Labels.get(id);
            study.addItem(expert1, expert2);
        }

        CohenKappaAgreement kappa = new CohenKappaAgreement(study);

        return kappa.calculateCategoryAgreement("3");
    }
}
