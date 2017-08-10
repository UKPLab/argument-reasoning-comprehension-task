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

import de.tudarmstadt.ukp.experiments.pipeline.containers.HITArgumentWithSingleReason;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;

import java.io.File;
import java.io.IOException;

/**
 * Creates HITs for writing Reason gist (Step 3 in the paper)
 *
 * @author Ivan Habernal
 */
public class Step3aReasonGistHITCreator
        extends AbstractReasonHITCreator
{

    public Step3aReasonGistHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    protected void process(StandaloneArgumentWithSinglePremise argument)
    {
        // fill the container
        HITArgumentWithSingleReason hitArgument = new HITArgumentWithSingleReason();
        hitArgument.argumentId = argument.getId();
        hitArgument.description = argument.getDebateMetaData().getDescription();
        hitArgument.title = argument.getDebateMetaData().getTitle();
        hitArgument.stance = argument.getAnnotatedStance();
        hitArgument.opposingStance = argument.getStanceOpposingToAnnotatedStance();

        hitArgument.reasonId = argument.getPremise().getPremiseId();
        hitArgument.reasonText = argument.getPremise().getOriginalText();

        this.buffer.add(hitArgument);
    }

    @Override
    protected int getArgumentsPerHIT()
    {
        return 5;
    }

    public static void main(String[] args)
            throws IOException
    {
        // 42 full batch from 32-reasons-batch-0001-5000-2026args
        new Step3aReasonGistHITCreator(false).prepareBatchFromTo(
                new File(
                        "mturk/annotation-task/data/32-reasons-batch-0001-5000-2026args-gold.xml.gz"),
                new File("mturk/annotation-task/html/42-reasons-gist-batch-0001-5000-5119reasons"),
                0, 5119, "mturk-template-reason-gist.mustache"
        );

    }
}
