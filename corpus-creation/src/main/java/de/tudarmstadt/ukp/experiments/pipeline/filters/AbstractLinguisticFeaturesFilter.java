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

import org.apache.uima.jcas.JCas;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Argument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Debate;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;

import java.io.IOException;

/**
 * (c) 2016 Ivan Habernal
 */
public abstract class AbstractLinguisticFeaturesFilter
        implements ArgumentSamplingFilter
{
    @Override
    public boolean keepArgument(Argument argument, Debate parentDebate)
            throws IOException
    {
        if (!(argument instanceof StandaloneArgument)) {
            throw new IllegalStateException(
                    StandaloneArgument.class.getName() + " expected but was " + argument.getClass()
                            .getName());
        }

        JCas jCas = ((StandaloneArgument) argument).getJCas();

        if (jCas == null) {
            throw new IllegalStateException("Empty jCas for " + argument.getId());
        }

        return keepArgument(jCas);
    }

    abstract boolean keepArgument(JCas jCas);
}
