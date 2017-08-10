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

package de.tudarmstadt.ukp.experiments.examples;

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Walk-trough example extracted from the annotated data
 *
 * @author Ivan Habernal
 */
public class WalkTroughExampleExtractor
{
    public static void main(String[] args)
            throws IOException
    {
        // these are the final data for the task
        File argumentReasoningTaskFinalCorpus = new File(
                "mturk/annotation-task/data/96-original-warrant-batch-0001-5000-final-1970-good-reason-claim-pairs.xml.gz");

        // load the to a list
        List<ReasonClaimWarrantContainer> allFinalAnnotatedPairs = XStreamSerializer
                .deserializeReasonListFromXML(argumentReasoningTaskFinalCorpus);

        // find instance
        ReasonClaimWarrantContainer reasonClaimWarrantContainer = allFinalAnnotatedPairs.stream()
                .filter(pair -> pair.getAlternativeWarrant().contains(
                        "there is no innovation in 3-d printing since it's unsustainable"))
                .findFirst().get();

        String reasonId = reasonClaimWarrantContainer.getReasonId();

        System.out.println("reasonId: " + reasonId);

        // get the argument id from the reason id; they are just delimited by "_"
        String argumentId = reasonId.split("_")[0];

        // load the annotated argument with the given id
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons.xml.gz"));

        // find the argument
        Optional<StandaloneArgument> first = arguments.stream()
                .filter(standaloneArgument -> argumentId.equals(standaloneArgument.getId()))
                .findFirst();

        if (!first.isPresent()) {
            throw new IllegalStateException("Argument with reasonId " + reasonId + " not found");
        }

        StandaloneArgument argument = first.get();

        // show the whole text
        System.out.println(argument.getDebateMetaData().getUrl());
        System.out.println(argument.getDebateMetaData().getTitle());
        System.out.println(argument.getDebateMetaData().getDescription());
        System.out.println(argument.getText());

        // then the stance
        System.out.println(argument.getAnnotatedStance());

        // then the reasons
        List<StandaloneArgumentWithSinglePremise> argumentWithSinglePremises = StandaloneArgumentWithSinglePremise
                .extractPremises(argument);

        Set<String> argumentReasonIds = new TreeSet<>();

        for (StandaloneArgumentWithSinglePremise standaloneArgumentWithSinglePremise : argumentWithSinglePremises) {
            // stance
            System.out.println(
                    "\\noindent \\textbf{Reason id:} " + standaloneArgumentWithSinglePremise
                            .getPremise().getPremiseId());
            System.out.println("\\noindent \\textbf{Disambiguated stance:} "
                    + standaloneArgumentWithSinglePremise.getPremise().getDisambiguatedStance());
            System.out.println(
                    "\\noindent \\textbf{Original text:} " + standaloneArgumentWithSinglePremise
                            .getPremise().getOriginalText());
            System.out.println(
                    "\\noindent \\textbf{Gist:} " + standaloneArgumentWithSinglePremise.getPremise()
                            .getGist());
            argumentReasonIds.add(standaloneArgumentWithSinglePremise.getPremise().getPremiseId());
        }

        // then the gist
        System.out.println("-----------------------");

        // load all data
        List<ReasonClaimWarrantContainer> list = XStreamSerializer
                .deserializeReasonListFromXML(new File(
                        "mturk/annotation-task/data/92-original-warrant-batch-0001-5000-2447-good-reason-claim-pairs.xml.gz"));
        for (ReasonClaimWarrantContainer container : list) {
            String currentReasonId = container.getReasonId();
            if (argumentReasonIds.contains(currentReasonId)) {
                System.out.println("+++ ReasonID: " + currentReasonId);
                System.out.println("AW: " + container.getAlternativeWarrant());
                System.out.println("W: " + container.getOriginalWarrant());
                System.out.println("Complexity: " + container.getHardScore());
                System.out.println("Logic: " + container.getLogicScore());
                System.out.println("Distracing reason: " + container.getDistractingReasonGist());

            }
        }
    }
}
