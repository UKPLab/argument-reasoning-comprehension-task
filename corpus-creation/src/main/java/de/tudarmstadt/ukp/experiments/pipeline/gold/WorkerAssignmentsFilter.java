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

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Filters assignments per HIT, i.e. first n assignments by time
 *
 * @author Ivan Habernal
 */
public abstract class WorkerAssignmentsFilter
{
    /**
     * Filters assignments per HIT, i.e. first n assignments by time
     *
     * @param assignments existing assignments
     * @param <T>         type of assignments (integer, boolean, etc.)
     * @return new sorted map with filtered assignments
     */
    public <T> SortedMap<String, SortedSet<SingleWorkerAssignment<T>>> filterAssignments(
            SortedMap<String, SortedSet<SingleWorkerAssignment<T>>> assignments)
    {
        SortedMap<String, SortedSet<SingleWorkerAssignment<T>>> result = new TreeMap<>();

        for (Map.Entry<String, SortedSet<SingleWorkerAssignment<T>>> entry : assignments
                .entrySet()) {
            SortedSet<SingleWorkerAssignment<T>> filtered = filter(entry.getValue());

            if (!filtered.isEmpty()) {
                result.put(entry.getKey(), filtered);
            }
        }

        return result;
    }

    /**
     * Filter assignments for a single HIT
     *
     * @param value assignments
     * @param <T>   type
     * @return a new collection, can be empty
     */
    protected abstract <T> SortedSet<SingleWorkerAssignment<T>> filter(
            SortedSet<SingleWorkerAssignment<T>> value);
}
