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
import java.util.Arrays;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class DirectResponsesFilter
        implements ArgumentSamplingFilter
{
    private int firstNWords = 30;

    public DirectResponsesFilter()
    {
    }

    public DirectResponsesFilter(int firstNWords)
    {
        this.firstNWords = firstNWords;
    }

    @Override
    public boolean keepArgument(Argument argument, Debate parentDebate)
            throws IOException
    {
        List<String> tokens = Arrays.asList(argument.getText().split("\\s+"));
        for (int i = 0; i < firstNWords && i < tokens.size(); i++) {
            String token = tokens.get(i).toLowerCase();

            if ("you".equals(token) || "your".equals(token)) {
                return false;
            }
        }

        return true;
    }
}
