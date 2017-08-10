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

package de.tudarmstadt.ukp.experiments.pipeline.gold;

import java.util.*;

/**
 * @author Ivan Habernal
 */
public class WorkerAssignmentsFilterSubsetByTime
        extends WorkerAssignmentsFilter
{
    private final int from;
    private final int to;
    private final boolean ascendingSamples;

    public WorkerAssignmentsFilterSubsetByTime(int from, int to, boolean ascendingSamples)
    {
        this.from = from;
        this.to = to;
        this.ascendingSamples = ascendingSamples;
    }

    @Override
    protected <T> SortedSet<SingleWorkerAssignment<T>> filter(
            SortedSet<SingleWorkerAssignment<T>> value)
    {
        List<SingleWorkerAssignment<T>> list = new ArrayList<>(value);

        if (!ascendingSamples) {
            Collections.reverse(list);
        }

        List<SingleWorkerAssignment<T>> subList = list.subList(from, to);

        return new TreeSet<>(subList);
    }
}
