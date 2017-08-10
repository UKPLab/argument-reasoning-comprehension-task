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

package de.tudarmstadt.ukp.experiments.pipeline.filters;

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Debate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ivan Habernal
 */
public class BlacklistDebatesFilter
        implements DebateSamplingFilter
{

    final private static Set<String> BANNED_KEYWORDS = new HashSet<>(
            Arrays.asList("god", "pedophile")
    );

    @Override
    public boolean keepDebate(Debate debate)
    {
        String title = debate.getDebateMetaData().getTitle().toLowerCase();
        for (String keyword : BANNED_KEYWORDS) {
            if (title.contains(keyword)) {
                return false;
            }
        }

        return true;
    }
}
