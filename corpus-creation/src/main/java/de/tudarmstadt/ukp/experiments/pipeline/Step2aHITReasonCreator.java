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

import de.tudarmstadt.ukp.dkpro.argumentation.types.Claim;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import de.tudarmstadt.ukp.experiments.pipeline.containers.HITArgumentReason;
import de.tudarmstadt.ukp.experiments.pipeline.containers.HITSentence;
import de.tudarmstadt.ukp.experiments.pipeline.containers.HITSentenceReason;
import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainer;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Creates HITs for Step 2 in the pipeline (Reason span annotation)
 *
 * @author Ivan Habernal
 */
public class Step2aHITReasonCreator
        extends AbstractArgumentHITCreator
{

    /**
     * How many arguments are shown in one HIT
     */
    private static final int ARGUMENTS_PER_HIT = 3;

    public Step2aHITReasonCreator(boolean sandbox)
    {
        super(sandbox);
    }

    @Override
    public void process(StandaloneArgument argument)
            throws IOException
    {
        HITArgumentReason hitArgument = new HITArgumentReason();
        hitArgument.description = argument.getDebateMetaData().getDescription();
        hitArgument.title = argument.getDebateMetaData().getTitle();
        hitArgument.stance = argument.getAnnotatedStance();

        if (hitArgument.stance == null) {
            throw new IllegalStateException("Argument has no annotated stance");
        }

        hitArgument.argumentId = argument.getId();

        // now get the sentences
        hitArgument.argumentSentences.addAll(extractSentencesForReasons(argument));

        argumentBuffer.add(hitArgument);
    }

    private Collection<? extends HITSentence> extractSentencesForReasons(
            StandaloneArgument argument)
            throws IOException
    {
        // extract sentences
        List<HITSentenceReason> result = new ArrayList<>();

        ArrayList<Sentence> sentences = new ArrayList<>(
                JCasUtil.select(argument.getJCas(), Sentence.class));
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);

            HITSentenceReason s = new HITSentenceReason();
            // position
            s.position = i;
            // create unique id by combining argument id and sentence position
            s.sentenceId = StandaloneArgument.getSentenceID(argument, s.position);
            s.text = sentence.getCoveredText();

            // find out whether this sentence is already covered by a claim
            List<Claim> coveringClaims = JCasUtil.selectCovering(Claim.class, sentence);

            s.disabled = !coveringClaims.isEmpty();

            // there can't be any claims at the moment!
            if (s.disabled) {
                throw new IllegalStateException("No claim annotations are allowed at this point");
            }

            result.add(s);
        }

        return result;
    }

    @Override
    protected MTurkHITContainer createHITContainer()
            throws IOException
    {
        return new MTurkHITContainer();
    }

    @Override protected int getArgumentsPerHIT()
    {
        return ARGUMENTS_PER_HIT;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args)
            throws IOException
    {
        // from gold data from 22-stance-batch-0001-5000.xml.gz
        new Step2aHITReasonCreator(false).prepareBatchFromTo(
                new File(
                        "mturk/annotation-task/data/22-stance-batch-0001-5000-only-with-clear-stances.xml.gz"),
                new File(
                        "mturk/annotation-task/html/32-reasons-batch-0001-5000-2883args"),
                0, 2883 - 1, "mturk-template-reasons.mustache");

        // there should have been 2884 arguments, the number 2882 is probably a typo
    }

}
