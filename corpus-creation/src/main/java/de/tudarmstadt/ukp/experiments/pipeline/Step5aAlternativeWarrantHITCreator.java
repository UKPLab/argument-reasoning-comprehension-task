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

import de.tudarmstadt.ukp.experiments.pipeline.containers.HITArgumentWithSingleReasonForWarrants;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.DisambiguatedStance;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class Step5aAlternativeWarrantHITCreator
        extends AbstractReasonHITCreator
{

    public Step5aAlternativeWarrantHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    @Override
    protected List<StandaloneArgumentWithSinglePremise> applyFilterOnSelectedPremises(
            List<StandaloneArgumentWithSinglePremise> premiseList)
    {
        List<StandaloneArgumentWithSinglePremise> result = new ArrayList<>();

        for (StandaloneArgumentWithSinglePremise singlePremise : premiseList) {
            String gist = singlePremise.getPremise().getGist();

            DisambiguatedStance disambiguatedStance = null;
            String disambiguatedStanceString = singlePremise.getPremise().getDisambiguatedStance();
            if (disambiguatedStanceString != null) {
                disambiguatedStance = DisambiguatedStance.valueOf(disambiguatedStanceString);
            }

            // we want only clear stance-taking reason
            if (gist != null && DisambiguatedStance.ORIGINAL.equals(disambiguatedStance)) {
                result.add(singlePremise);
            }
        }

        return result;
    }

    @Override
    protected int getArgumentsPerHIT()
    {
        return 3;
    }

    protected void process(StandaloneArgumentWithSinglePremise argument)
    {
        // fill the container
        HITArgumentWithSingleReasonForWarrants hitArgument = new HITArgumentWithSingleReasonForWarrants();
        hitArgument.argumentId = argument.getId();
        hitArgument.description = argument.getDebateMetaData().getDescription();
        hitArgument.title = argument.getDebateMetaData().getTitle();

        hitArgument.opposingStance = argument.getStanceOpposingToAnnotatedStance();

        hitArgument.reasonId = argument.getPremise().getPremiseId();
        hitArgument.reasonText = argument.getPremise().getGist();

        this.buffer.add(hitArgument);
    }

    public static void main(String[] args)
            throws IOException
    {
        // 71 batch 0-600; max size is 1955
        new Step5aAlternativeWarrantHITCreator(false).prepareBatchFromTo(new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/html/71-alternative-warrants-batch-0001-5000-4235reasons-001-600"),
                0, 600,
                "mturk-template-warrant3.mustache");

        // 72 batch 600-1955; max size is 1955
        new Step5aAlternativeWarrantHITCreator(false).prepareBatchFromTo(
                new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/html/72-alternative-warrants-batch-0001-5000-4235reasons-600-1955"),
                600, 1955, "mturk-template-warrant3.mustache");
    }
}
