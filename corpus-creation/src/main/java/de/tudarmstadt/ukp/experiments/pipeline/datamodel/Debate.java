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

package de.tudarmstadt.ukp.experiments.pipeline.datamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Ivan Habernal
 */
public class Debate
{
    private DebateMetaData debateMetaData;

    private List<Argument> argumentList = new ArrayList<>();

    public List<Argument> getArgumentList()
    {
        return argumentList;
    }

    public void setArgumentList(List<Argument> argumentList)
    {
        this.argumentList = argumentList;
    }

    public DebateMetaData getDebateMetaData()
    {
        return debateMetaData;
    }

    public void setDebateMetaData(
            DebateMetaData debateMetaData)
    {
        this.debateMetaData = debateMetaData;
    }

    @Override public String toString()
    {
        return "Debate{" +
                "debateMetaData=" + debateMetaData +
                ", argumentList=" + argumentList +
                '}';
    }

    /**
     * Returns the debate stances (usually two; some malformed data have also three or more...)
     *
     * @return set of stances
     */
    public SortedSet<String> getStances()
    {
        SortedSet<String> result = new TreeSet<>();

        for (Argument argument : this.argumentList) {
            result.add(argument.getStance());
        }

        return result;
    }

    /**
     * Returns a list of arguments with the given stance; doesn't affect the original list of arguments
     *
     * @param stance stance
     * @return list of arguments
     * @throws IllegalArgumentException if stance is null or empty
     */
    public List<Argument> getArgumentsForStance(String stance)
            throws IllegalArgumentException
    {
        if (stance == null || stance.isEmpty()) {
            throw new IllegalArgumentException("Parameter stance is null or empty");
        }

        List<Argument> result = new ArrayList<>();

        for (Argument argument : this.argumentList) {
            if (stance.equals(argument.getStance())) {
                result.add(argument);
            }
        }

        return result;
    }
}
