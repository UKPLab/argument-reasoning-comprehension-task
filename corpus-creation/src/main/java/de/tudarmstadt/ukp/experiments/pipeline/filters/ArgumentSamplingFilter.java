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

/**
 * @author Ivan Habernal
 */
public interface ArgumentSamplingFilter
{
    /**
     * True if the argument should be kept for further processing, false otherwise
     *
     * @param argument     argument
     * @param parentDebate debate to which the argument belongs to
     * @return boolean
     * @throws IOException any exception
     */
    boolean keepArgument(Argument argument, Debate parentDebate)
            throws IOException;
}
