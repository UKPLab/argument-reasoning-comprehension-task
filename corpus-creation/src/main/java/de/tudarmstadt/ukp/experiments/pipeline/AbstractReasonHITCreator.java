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
import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainerSingleReasons;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Ivan Habernal
 */
public abstract class AbstractReasonHITCreator
        extends AbstractHITCreator
{
    List<HITArgumentWithSingleReason> buffer = new ArrayList<>();

    public AbstractReasonHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    @Override
    public void prepareBatchFromTo(File inputFile, File outputDir, int from, int to,
            String mustacheTemplate)
            throws IOException
    {
        this.outputPath = outputDir;
        this.initialize(mustacheTemplate);

        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);

        List<StandaloneArgumentWithSinglePremise> allArguments = new ArrayList<>();

//        System.err.println("Warning: loading only first 200 arguments; need to be fixed later!!!");
//        for (int i = 0; i < 200; i++) {
//            StandaloneArgument standaloneArgument = arguments.get(i);
        for (StandaloneArgument standaloneArgument : arguments) {
            List<StandaloneArgumentWithSinglePremise> premises = applyFilterOnSelectedPremises(
                    StandaloneArgumentWithSinglePremise
                            .extractPremises(standaloneArgument));

            allArguments.addAll(premises);
        }

        // shuffle them
        Random random = new Random(0);
        Collections.shuffle(allArguments, random);
        try {
            allArguments = allArguments.subList(from, to);
        }
        catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Actual size: " + allArguments.size(), ex);
        }

        for (StandaloneArgumentWithSinglePremise argument : allArguments) {
            // process
            this.process(argument);

            if (buffer.size() >= getArgumentsPerHIT()) {
                flushArgumentBufferToHIT();
            }
        }

        // some resting ones?
        if (!buffer.isEmpty()) {
            flushArgumentBufferToHIT();
        }
    }

    /**
     * Can filter some premises (e.g. only those containing "gist" annotations)
     *
     * @param premiseList list
     * @return list
     */
    protected List<StandaloneArgumentWithSinglePremise> applyFilterOnSelectedPremises(
            List<StandaloneArgumentWithSinglePremise> premiseList)
    {
        return premiseList;
    }

    protected abstract void process(StandaloneArgumentWithSinglePremise argument);

    protected void flushArgumentBufferToHIT()
            throws FileNotFoundException
    {
        MTurkHITContainerSingleReasons hitContainer = new MTurkHITContainerSingleReasons();
        hitContainer.arguments.addAll(this.buffer);
        hitContainer.numberOfArguments = hitContainer.arguments.size();

        // make sure you use the proper type
        if (sandbox) {
            hitContainer.mturkURL = MTURK_SANDBOX_URL;
        }
        else {
            hitContainer.mturkURL = MTURK_ACTUAL_URL;
        }

        executeMustacheTemplate(hitContainer);

        // empty the current buffer
        this.buffer.clear();
    }

    public void collectionProcessComplete()
            throws IOException
    {
        // fill the rest of the buffer
        if (!buffer.isEmpty()) {
            flushArgumentBufferToHIT();
        }
    }
}
