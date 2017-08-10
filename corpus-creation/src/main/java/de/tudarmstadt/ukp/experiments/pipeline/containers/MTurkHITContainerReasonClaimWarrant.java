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

package de.tudarmstadt.ukp.experiments.pipeline.containers;

import org.apache.commons.lang.StringUtils;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Ivan Habernal
 */
public class MTurkHITContainerReasonClaimWarrant
{
    public int numberOfArguments;
    public String mturkURL;

    public List<HITBufferElement> reasonClaimWarrantList = new ArrayList<>();

    static Random random = new Random(1234);

    public static class HITReasonClaimWarrant implements HITBufferElement
    {
        public List<DistractingOrOriginalOrUndecidableReason> reasonOptions = new ArrayList<>();

        public HITReasonClaimWarrant(ReasonClaimWarrantContainer reasonClaimWarrantContainer)
        {
            this.title = reasonClaimWarrantContainer.getDebateMetaData().getTitle();
            this.description = reasonClaimWarrantContainer.getDebateMetaData().getDescription();
            this.reasonId = reasonClaimWarrantContainer.getReasonId();
            this.stanceOpposingToAnnotatedStance = reasonClaimWarrantContainer
                    .getStanceOpposingToAnnotatedStance();
            this.annotatedStance = reasonClaimWarrantContainer.getAnnotatedStance();
            this.alternativeWarrant = reasonClaimWarrantContainer.getAlternativeWarrant();
            this.reasonClaimWarrantId = reasonClaimWarrantContainer.getReasonClaimWarrantId();

            // fill reason options
            DistractingOrOriginalOrUndecidableReason original = new DistractingOrOriginalOrUndecidableReason();
            original.reasonText = reasonClaimWarrantContainer.getReasonGist();
            original.value = 1;

            DistractingOrOriginalOrUndecidableReason distracting = new DistractingOrOriginalOrUndecidableReason();
            distracting.reasonText = reasonClaimWarrantContainer.getDistractingReasonGist();
            distracting.value = 0;

            DistractingOrOriginalOrUndecidableReason undecidable = new DistractingOrOriginalOrUndecidableReason();
            undecidable.reasonText = "It's hard to say, both look rather irrelevant";
            undecidable.value = 2;

            this.reasonOptions.add(original);
            this.reasonOptions.add(distracting);

            // shuffle
            Collections.shuffle(this.reasonOptions, random);

            // and add undecidable
            this.reasonOptions.add(undecidable);

            validateFields(this.title, this.description, this.reasonId,
                    this.stanceOpposingToAnnotatedStance, this.annotatedStance,
                    this.alternativeWarrant, this.reasonClaimWarrantId, original.reasonText,
                    distracting.reasonText);
        }

        private void validateFields(String... fields)
        {
            for (String field : fields) {
                if (StringUtils.isBlank(field)) {
                    throw new IllegalArgumentException("Field is blank: " + field);
                }
            }
        }

        public final String title;
        public final String description;

        public final String reasonId;
        public final String stanceOpposingToAnnotatedStance;
        public final String annotatedStance;
        public final String alternativeWarrant;
        public final String reasonClaimWarrantId;
    }

    public static class DistractingOrOriginalOrUndecidableReason
    {
        // 0 = distracting; 1 = original; 2 = unknown
        public int value;
        public String reasonText;
    }
}
