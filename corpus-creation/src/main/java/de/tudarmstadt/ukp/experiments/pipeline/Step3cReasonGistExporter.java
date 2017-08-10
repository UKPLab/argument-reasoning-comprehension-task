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

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.DisambiguatedStance;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exporting all gist to a CSV file to be later used for "distracting reason" annotations in
 * "Alternative warrant validation" (Step 5)
 *
 * @author Ivan Habernal
 */
public class Step3cReasonGistExporter
{
    public static void exportReasonGist(File inputFile, File outputCSVFile)
            throws Exception
    {
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);

        // result: stance; reasonid; gist

        Map<String, Map<String, String>> reasonIdGistMap = new TreeMap<>();

        for (StandaloneArgument standaloneArgument : arguments) {
            List<StandaloneArgumentWithSinglePremise> argumentWithSinglePremises = StandaloneArgumentWithSinglePremise
                    .extractPremises(standaloneArgument);

            for (StandaloneArgumentWithSinglePremise standaloneArgumentWithSinglePremise : argumentWithSinglePremises) {
                // stance
                String stance = standaloneArgumentWithSinglePremise.getAnnotatedStance();

                String gist = standaloneArgumentWithSinglePremise.getPremise().getGist();
                String reasonId = standaloneArgumentWithSinglePremise.getPremise().getPremiseId();

                DisambiguatedStance disambiguatedStance = null;
                String disambiguatedStanceString = standaloneArgumentWithSinglePremise.getPremise()
                        .getDisambiguatedStance();
                if (disambiguatedStanceString != null) {
                    disambiguatedStance = DisambiguatedStance.valueOf(disambiguatedStanceString);
                }

                // we want only clear stance-taking reason
                if (gist != null && DisambiguatedStance.ORIGINAL.equals(disambiguatedStance)) {
                    reasonIdGistMap.putIfAbsent(stance, new TreeMap<>());
                    reasonIdGistMap.get(stance).put(reasonId, gist);
                }
            }
        }

        // save to a tab-separated file
        PrintWriter pw = new PrintWriter(outputCSVFile);
        reasonIdGistMap.forEach((stance, reasonIdsGist) -> reasonIdsGist.forEach(
                (reasonId, gist) -> pw.println(stance + "\t" + reasonId + "\t" + gist))
        );
        IOUtils.closeQuietly(pw);
    }

    public static void main(String[] args)
            throws Exception
    {
        exportReasonGist(new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons-exported-gist.csv"));
    }

}
