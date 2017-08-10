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

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import de.tudarmstadt.ukp.experiments.pipeline.containers.HITArgument;
import de.tudarmstadt.ukp.experiments.pipeline.containers.HITSentence;
import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainer;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public abstract class AbstractArgumentHITCreator
        extends AbstractHITCreator
{

    protected List<HITArgument> argumentBuffer = new ArrayList<>();

    protected AbstractArgumentHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    public static List<HITSentence> extractSentences(StandaloneArgument argument)
            throws IOException
    {

        // extract sentences
        List<HITSentence> result = new ArrayList<>();

        ArrayList<Sentence> sentences = new ArrayList<>(
                JCasUtil.select(argument.getJCas(), Sentence.class));
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);

            HITSentence s = new HITSentence();
            // position
            s.position = i;
            // create unique id by combining argument id and sentence position
            s.sentenceId = StandaloneArgument.getSentenceID(argument, s.position);
            s.text = sentence.getCoveredText();

            result.add(s);
        }

        return result;

    }

    public static void checkConsistencyOfData(List<StandaloneArgument> arguments)
            throws IOException
    {
        for (StandaloneArgument argument : arguments) {
            if (argument.getMappingAllStancesToInt().size() != 4) {
                throw new IOException("Argument missing stances: " + argument.getId());
            }
        }

    }

    public abstract void process(StandaloneArgument argument)
            throws IOException;

    protected abstract MTurkHITContainer createHITContainer()
            throws IOException;

    public void collectionProcessComplete()
            throws IOException
    {
        // fill the rest of the buffer
        if (!argumentBuffer.isEmpty()) {
            flushArgumentBufferToHIT(createHITContainer());
        }

    }

    /**
     * Prepares an annotation batch pseudo-randomly sampled from the given debates
     *
     * @param inputFile        with debates XMLs previously pre-filtered
     * @param outputDir        output dir for HITs
     * @param from             current batch sampling range - from (incl.)
     * @param to               current batch sampling range - to (excl.)
     * @param mustacheTemplate template
     * @throws IOException exception
     */
    @Override
    public void prepareBatchFromTo(File inputFile, File outputDir, int from, int to,
            String mustacheTemplate)
            throws IOException
    {
        this.outputPath = outputDir;
        this.initialize(mustacheTemplate);

        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);

        checkConsistencyOfData(arguments);

        // shuffle them
        Collections.shuffle(arguments, random);

        if (to >= arguments.size()) {
            throw new IllegalStateException(
                    "Requested from,to = " + from + "," + to + " but there are only " + arguments
                            .size() + " items");
        }

        arguments = arguments.subList(from, to);

        for (StandaloneArgument reasonUnit : arguments) {
            this.process(reasonUnit);
            if (argumentBuffer.size() >= getArgumentsPerHIT()) {
                MTurkHITContainer hitContainer = createHITContainer();
                flushArgumentBufferToHIT(hitContainer);
            }
        }

        this.collectionProcessComplete();
    }

    protected void flushArgumentBufferToHIT(MTurkHITContainer hitContainer)
            throws FileNotFoundException
    {
        hitContainer.arguments.addAll(this.argumentBuffer);
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
        this.argumentBuffer.clear();
    }
}
