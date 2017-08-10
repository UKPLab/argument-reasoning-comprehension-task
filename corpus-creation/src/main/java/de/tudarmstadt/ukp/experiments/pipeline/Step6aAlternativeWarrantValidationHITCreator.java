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

import de.tudarmstadt.ukp.experiments.pipeline.containers.HITBufferElement;
import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainerReasonClaimWarrant;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;
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
public class Step6aAlternativeWarrantValidationHITCreator
        extends AbstractHITCreator
{
    protected List<HITBufferElement> buffer = new ArrayList<>();

    public Step6aAlternativeWarrantValidationHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    @Override
    public void prepareBatchFromTo(File inputFile, File outputDir, int from, int to,
            String mustacheTemplate)
            throws IOException
    {
        // init
        this.outputPath = outputDir;
        this.initialize(mustacheTemplate);

        // load all data first
        List<ReasonClaimWarrantContainer> list = XStreamSerializer
                .deserializeReasonListFromXML(inputFile);

        System.out.println("Pre-filtered total size: " + list.size());
        list = preFilterList(list);
        System.out.println("Post-filtered total size: " + list.size());

        // shuffle
        Collections.shuffle(list, this.random);

        // and sublist
        try {
            list = list.subList(from, to);
        }
        catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Actual size: " + list.size(), ex);
        }

        for (ReasonClaimWarrantContainer reasonClaimWarrantContainer : list) {
            // process
            this.process(reasonClaimWarrantContainer);

            if (buffer.size() >= getArgumentsPerHIT()) {
                flushArgumentBufferToHIT();
            }
        }

        // some resting ones?
        if (!buffer.isEmpty()) {
            flushArgumentBufferToHIT();
        }
    }

    protected List<ReasonClaimWarrantContainer> preFilterList(
            List<ReasonClaimWarrantContainer> list)
    {
        return list;
    }

    private void flushArgumentBufferToHIT()
            throws FileNotFoundException
    {
        MTurkHITContainerReasonClaimWarrant hitContainer = new MTurkHITContainerReasonClaimWarrant();
        hitContainer.reasonClaimWarrantList.addAll(this.buffer);
        hitContainer.numberOfArguments = hitContainer.reasonClaimWarrantList.size();

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

    protected void process(ReasonClaimWarrantContainer reasonClaimWarrantContainer)
    {
        // create a new container instance and add to the buffer
        MTurkHITContainerReasonClaimWarrant.HITReasonClaimWarrant reasonClaimWarrant = new MTurkHITContainerReasonClaimWarrant.HITReasonClaimWarrant(
                reasonClaimWarrantContainer);

        this.buffer.add(reasonClaimWarrant);
    }

    @Override
    protected int getArgumentsPerHIT()
    {
        return 5;
    }

    public static void main(String[] args)
            throws Exception
    {
        // 80 pilot - 18 annotators
        //        new Step14aAlternativeWarrantValidationHITCreator(false).prepareBatchFromTo(
        //                new File(
        //                        "mturk/annotation-task/data/71-alternative-warrants-batch-0001-5000-001-600aw-batch-2390reason-claim-pairs-with-distracting-reasons.xml.gz"),
        //                new File("mturk/annotation-task/html/80-aw-validation-pilot"),
        //                0, 50, "alternative-warrant-validation.mustache"
        //        );

        // 81 batch
        //        new Step14aAlternativeWarrantValidationHITCreator(false).prepareBatchFromTo(
        //                new File(
        //                        "mturk/annotation-task/data/71-alternative-warrants-batch-0001-5000-001-600aw-batch-2390reason-claim-pairs-with-distracting-reasons.xml.gz"),
        //                new File("mturk/annotation-task/html/81-001-600aw-validation-batch-0050-2390-reason-claim-pairs"),
        //                50, 2390, "alternative-warrant-validation.mustache"
        //        );

        // 82 batch
        new Step6aAlternativeWarrantValidationHITCreator(false).prepareBatchFromTo(new File(
                        "mturk/annotation-task/data/72-alternative-warrants-batch-0001-5000-600-1955w-batch-5342reason-claim-pairs-with-distracting-reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/html/82-600-1955aw-validation-batch-5342-reason-claim-pairs"),
                0, 5342, "alternative-warrant-validation.mustache");
    }
}
