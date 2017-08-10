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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class DisputedOrSupportedArgumentFilter
        implements ArgumentSamplingFilter
{
    @Override
    public boolean keepArgument(Argument argument, Debate parentDebate)
            throws IOException
    {
        // now find if there is any reaction to this argument (dispute or support)
        List<Argument> childArguments = new ArrayList<>();
        for (Argument childArgument : parentDebate.getArgumentList()) {
            if (argument.getId().equals(childArgument.getParentId())) {
                childArguments.add(childArgument);
            }
        }

        return childArguments.size() >= 1;
    }
}
