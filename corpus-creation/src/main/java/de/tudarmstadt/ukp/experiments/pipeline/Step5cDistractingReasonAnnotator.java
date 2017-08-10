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

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Ivan Habernal
 */
public class Step5cDistractingReasonAnnotator
{
    public static void addDistractingReasonGist(File exportedGistCSVFile,
            File reasonClaimWarrantFile, File outputFile,
            File distractingReasonIDs)
            throws Exception
    {
        // load all data first
        List<ReasonClaimWarrantContainer> list = XStreamSerializer
                .deserializeReasonListFromXML(reasonClaimWarrantFile);

        // load all exported gist texts
        Map<String, String> reasonIdGist = new TreeMap<>();
        for (String line : FileUtils.readLines(exportedGistCSVFile)) {
            String[] split = line.split("\t");
            reasonIdGist.put(split[1].trim(), split[2].trim());
        }

        // now load all pairs reasonId: reasonId
        Map<String, String> reasonIdToReasonIdMap = new TreeMap<>();
        for (String line : FileUtils.readLines(distractingReasonIDs)) {
            String[] split = line.split("\t");
            if (reasonIdToReasonIdMap.put(split[0].trim(), split[1].trim()) != null) {
                throw new IllegalStateException("Duplicate key in input file: " + split[0].trim());
            }
        }

        if (reasonIdGist.size() != reasonIdToReasonIdMap.size()) {
            throw new IllegalStateException(
                    "Different sizes: " + reasonIdGist.size() + "!=" + reasonIdToReasonIdMap);
        }

        System.out.println(reasonIdToReasonIdMap.size() + " distracting reason pairs loaded from "
                + distractingReasonIDs);

        List<ReasonClaimWarrantContainer> result = new ArrayList<>();

        // and set the distracting gist
        for (ReasonClaimWarrantContainer container : list) {
            String distractingId = reasonIdToReasonIdMap.get(container.getReasonId());

            String distractingGist = reasonIdGist.get(distractingId);
            container.setDistractingReasonGist(distractingGist);
            container.setDistractingReasonId(distractingId);
            result.add(container);
        }

        XStreamSerializer.serializeReasonsToXml(result, outputFile);
    }

    public static void main(String[] args)
            throws Exception
    {
        addDistractingReasonGist(
                new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons-exported-gist.csv"),
                new File(
                        "mturk/annotation-task/data/72-alternative-warrants-batch-0001-5000-600-1955w-batch-5342reason-claim-pairs.xml.gz"),
                new File(
                        "mturk/annotation-task/data/72-alternative-warrants-batch-0001-5000-600-1955w-batch-5342reason-claim-pairs-with-distracting-reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons-dissimilar-reasons-skip-thought.csv")
        );
    }
}
