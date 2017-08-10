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

import de.tudarmstadt.ukp.experiments.pipeline.containers.HITArgumentClaim;
import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainer;
import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainerStance;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Generates HITs for Stance annotation (Step 1 in the paper)
 *
 * @author Ivan Habernal
 */
public class Step1aStanceAnnotationHITCreator
        extends AbstractArgumentHITCreator
{
    /**
     * How many arguments are shown in one HIT
     */
    private static final int ARGUMENTS_PER_HIT = 5;

    /**
     * Where the mustache template is stored (can also be classpath:xxx if packed in resources folder)
     */
    private final static String SOURCE_MUSTACHE_TEMPLATE = "mturk-template-stance.mustache";

    protected Step1aStanceAnnotationHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    @Override public void process(StandaloneArgument argument)
            throws IOException
    {
        HITArgumentClaim hitArgumentClaim = new HITArgumentClaim();
        hitArgumentClaim.description = argument.getDebateMetaData().getDescription();
        hitArgumentClaim.title = argument.getDebateMetaData().getTitle();

        // add stances and their keys
        for (Map.Entry<String, Integer> entry : argument.getMappingAllStancesToInt().entrySet()) {
            hitArgumentClaim.stances.add(
                    new MTurkHITContainerStance.HITArgumentStance(entry.getKey(),
                            entry.getValue()));
        }

        hitArgumentClaim.argumentId = argument.getId();

        // now get the sentences
        hitArgumentClaim.argumentSentences.addAll(extractSentences(argument));

        argumentBuffer.add(hitArgumentClaim);

    }

    @Override protected MTurkHITContainer createHITContainer()
            throws IOException
    {
        return new MTurkHITContainerStance();
    }

    @Override protected int getArgumentsPerHIT()
    {
        return ARGUMENTS_PER_HIT;
    }

    public static void main(String[] args)
            throws IOException
    {
        // First big batch -- 5000 out of 11820 comments
        new Step1aStanceAnnotationHITCreator(false).prepareBatchFromTo(new File(
                        "mturk/annotation-task/data/arguments-with-full-segmentation-rfd.xml.gz"),
                new File("mturk/annotation-task/html/22-stance-batch-0001-5000"), 0, 5000,
                SOURCE_MUSTACHE_TEMPLATE);
    }
}
