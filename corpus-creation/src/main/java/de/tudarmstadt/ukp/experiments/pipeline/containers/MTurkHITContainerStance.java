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

/**
 * Container for storing data passed to Mustache to generate HITs
 * <p>
 * (c) 2016 Ivan Habernal
 */
public class MTurkHITContainerStance
        extends MTurkHITContainer
{
    public static class HITArgumentStance
    {
        public static final int STANCE_OPPOSITE = 0;
        public static final int STANCE_CORRECT = 1;
        public static final int STANCE_UNCLEAR = 2;

        String stanceText;

        /**
         * 0, 1 = first stance, second stance (sorted alphabetically)
         * 2 = not clear
         */
        int stanceValue;

        @Override public String toString()
        {
            return "HITArgumentStance{" +
                    "stanceText='" + stanceText + '\'' +
                    ", stanceValue=" + stanceValue +
                    '}';
        }

        public HITArgumentStance(String stanceText, int stanceValue)
        {
            this.stanceText = stanceText;
            this.stanceValue = stanceValue;
        }
    }

}
