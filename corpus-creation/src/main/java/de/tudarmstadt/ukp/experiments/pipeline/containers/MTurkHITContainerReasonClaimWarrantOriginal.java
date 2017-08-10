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

import java.util.ArrayList;
import java.util.List;

/**
 * (c) 2017 Ivan Habernal
 */
public class MTurkHITContainerReasonClaimWarrantOriginal
{
    public int numberOfArguments;
    public String mturkURL;

    public final List<HITBufferElement> reasonClaimWarrantList = new ArrayList<>();

    public static class HITReasonClaimWarrantOriginal
            implements HITBufferElement
    {
        public HITReasonClaimWarrantOriginal(String title, String description,
                String reasonClaimWarrantId, String annotatedStance, String alternativeWarrant,
                String reasonGist)
        {
            this.title = title;
            this.description = description;
            this.reasonClaimWarrantId = reasonClaimWarrantId;
            this.annotatedStance = annotatedStance;
            this.alternativeWarrant = alternativeWarrant;
            this.reasonGist = reasonGist;

            if (StringUtils.isBlank(title) || StringUtils.isBlank(description) || StringUtils
                    .isBlank(reasonClaimWarrantId) || StringUtils.isBlank(annotatedStance)
                    || StringUtils.isBlank(alternativeWarrant) || StringUtils.isBlank(reasonGist)) {
                throw new IllegalArgumentException("All arguments must be non-empty");
            }
        }

        public final String title;
        public final String description;
        public final String reasonClaimWarrantId;
        public final String annotatedStance;
        public final String alternativeWarrant;
        public final String reasonGist;
    }

    /**
     * User for validation
     */
    public static class HITReasonClaimWarrantOriginalValidation extends HITReasonClaimWarrantOriginal {

        public final List<ValidationWarrant> validationWarrants = new ArrayList<>();

        public HITReasonClaimWarrantOriginalValidation(String title, String description,
                String reasonClaimWarrantId, String annotatedStance, String alternativeWarrant,
                String reasonGist)
        {
            super(title, description, reasonClaimWarrantId, annotatedStance, alternativeWarrant,
                    reasonGist);
        }
    }

    public static class ValidationWarrant {
        public final int identifier;
        public final String warrantText;

        public ValidationWarrant(int identifier, String warrantText)
        {
            if (StringUtils.isBlank(warrantText)) {
                throw new IllegalArgumentException();
            }

            if (identifier < 0 || identifier > 2) {
                throw new IllegalArgumentException();
            }

            this.identifier = identifier;
            this.warrantText = warrantText;
        }
    }
}
