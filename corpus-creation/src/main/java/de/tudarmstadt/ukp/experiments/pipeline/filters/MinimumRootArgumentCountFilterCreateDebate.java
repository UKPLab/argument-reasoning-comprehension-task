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

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Argument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Debate;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Ivan Habernal
 */
public class MinimumRootArgumentCountFilterCreateDebate
        implements DebateSamplingFilter
{
    // parameters
    public static final int MINIMUM_NUMBER_OF_FIRST_LEVEL_ARGUMENTS_PER_SIDE = 2;

    @Override
    public boolean keepDebate(Debate debate)
            throws IOException
    {
        // collect counts for each stance
        SortedMap<String, Integer> counts = new TreeMap<>();
        for (Argument argument : debate.getArgumentList()) {
            String stance = argument.getStance();

            if (argument.getParentId() == null) {

                if (!counts.containsKey(stance)) {
                    counts.put(stance, 0);
                }
                counts.put(stance, counts.get(stance) + 1);
            }
        }

        // make sure we have two stances
        if (counts.size() < 2) {
            return false;
        }

        // check constraints for each stance
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() <= MINIMUM_NUMBER_OF_FIRST_LEVEL_ARGUMENTS_PER_SIDE) {
                return false;
            }
        }

        return true;
    }
}
