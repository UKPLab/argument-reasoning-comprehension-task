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

import de.tudarmstadt.ukp.experiments.pipeline.containers.HITArgumentWithSingleReasonForDisambiguation;
import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainerStance;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates HITs for the "Reason disambiguation" step (4)
 *
 * @author Ivan Habernal
 */
public class Step4aReasonDisambiguationHITCreator
        extends AbstractReasonHITCreator
{

    public Step4aReasonDisambiguationHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    @Override
    protected List<StandaloneArgumentWithSinglePremise> applyFilterOnSelectedPremises(
            List<StandaloneArgumentWithSinglePremise> premiseList)
    {
        List<StandaloneArgumentWithSinglePremise> result = new ArrayList<>();

        for (StandaloneArgumentWithSinglePremise singlePremise : premiseList) {
            if (singlePremise.getPremise().getGist() != null) {
                result.add(singlePremise);
            }
        }

        return result;
    }

    @Override
    protected int getArgumentsPerHIT()
    {
        return 6;
    }

    protected void process(StandaloneArgumentWithSinglePremise argument)
    {
        // fill the container
        HITArgumentWithSingleReasonForDisambiguation hitArgument = new HITArgumentWithSingleReasonForDisambiguation();
        hitArgument.argumentId = argument.getId();
        hitArgument.description = argument.getDebateMetaData().getDescription();
        hitArgument.title = argument.getDebateMetaData().getTitle();

        // correct one
        hitArgument.stances
                .add(new MTurkHITContainerStance.HITArgumentStance(argument.getAnnotatedStance(),
                        1));
        hitArgument.stances.add(new MTurkHITContainerStance.HITArgumentStance(
                argument.getStanceOpposingToAnnotatedStance(), 0));
        // shuffle
        Collections.shuffle(hitArgument.stances, this.random);

        // add the third and fourth option
        hitArgument.stances.add(new MTurkHITContainerStance.HITArgumentStance(
                "Both options above are equally possible", 2));

        hitArgument.stances.add(new MTurkHITContainerStance.HITArgumentStance(
                "The reason only rephrases or directly says '" + argument.getAnnotatedStance()
                        + "', no reasoning is required", 3));

        hitArgument.reasonId = argument.getPremise().getPremiseId();
        hitArgument.reasonText = argument.getPremise().getGist();

        this.buffer.add(hitArgument);
    }

    public static void main(String[] args)
            throws IOException
    {
        // 60 pilot; these HITs were also used in the gold standard annotation
        //        new Step12aReasonDisambiguationHITCreator(false).prepareBatchFromTo(new File(
        //                            "mturk/annotation-task/data/42-reasons-gist-batch-0001-5000-4294reasons.xml.gz"),
        //                new File("mturk/annotation-task/html/60-pilot-reason-disambiguation"), 0, 60,
        //                "mturk-template-reason-disambiguation.mustache");

        // 61 batch full
        new Step4aReasonDisambiguationHITCreator(false).prepareBatchFromTo(new File(
                        "mturk/annotation-task/data/42-reasons-gist-batch-0001-5000-4294reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/html/61-reason-disambiguation-batch-0001-5000-4294reasons-0060-4294"),
                // we start from 60, as the first 60 were used for a pilot
                60, 4294,
                "mturk-template-reason-disambiguation.mustache");
    }
}
