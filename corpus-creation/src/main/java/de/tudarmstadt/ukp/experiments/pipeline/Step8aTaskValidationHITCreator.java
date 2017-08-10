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

import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainerReasonClaimWarrantOriginal;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * (c) 2017 Ivan Habernal
 */
public class Step8aTaskValidationHITCreator
        extends Step6aAlternativeWarrantValidationHITCreator
{
    static Random random = new Random(1234);

    public Step8aTaskValidationHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    @Override
    protected void process(ReasonClaimWarrantContainer reasonClaimWarrantContainer)
    {
        MTurkHITContainerReasonClaimWarrantOriginal.HITReasonClaimWarrantOriginalValidation reasonClaimWarrant
                = new MTurkHITContainerReasonClaimWarrantOriginal.HITReasonClaimWarrantOriginalValidation(
                reasonClaimWarrantContainer.getDebateMetaData().getTitle(),
                reasonClaimWarrantContainer.getDebateMetaData().getDescription(),
                reasonClaimWarrantContainer.getReasonClaimWarrantId(),
                reasonClaimWarrantContainer.getAnnotatedStance(),
                reasonClaimWarrantContainer.getAlternativeWarrant(),
                reasonClaimWarrantContainer.getReasonGist());

        List<MTurkHITContainerReasonClaimWarrantOriginal.ValidationWarrant> list = new ArrayList<>();

        list.add(new MTurkHITContainerReasonClaimWarrantOriginal.ValidationWarrant(0,
                reasonClaimWarrantContainer.getAlternativeWarrant()));
        list.add(new MTurkHITContainerReasonClaimWarrantOriginal.ValidationWarrant(1,
                reasonClaimWarrantContainer.getOriginalWarrant()));
        Collections.shuffle(list, random);

        list.add(new MTurkHITContainerReasonClaimWarrantOriginal.ValidationWarrant(2,
                "neither of them makes sense"));

        reasonClaimWarrant.validationWarrants.addAll(list);

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
        // 95 pilot
//        new Step16aTaskValidationHITCreator(false).prepareBatchFromTo(new File(
//                        "mturk/annotation-task/data/92-original-warrant-batch-0001-5000-2447-good-reason-claim-pairs.xml.gz"),
//                new File("mturk/annotation-task/html/95-validation-task-pilot"), 0, 50,
//                "task-validation.mustache");

        // 96-validation-task-batch-0001-5000-full-batch-2447
        new Step8aTaskValidationHITCreator(false).prepareBatchFromTo(new File(
                        "mturk/annotation-task/data/92-original-warrant-batch-0001-5000-2447-good-reason-claim-pairs.xml.gz"),
                new File("mturk/annotation-task/html/96-validation-task-batch-0001-5000-full-batch-2447"), 0, 2447,
                "task-validation.mustache");


    }
}
