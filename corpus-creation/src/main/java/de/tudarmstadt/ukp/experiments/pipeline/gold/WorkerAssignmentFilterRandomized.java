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
public class WorkerAssignmentFilterRandomized
        extends WorkerAssignmentsFilter
{
    private final int requiredAllAssignmentSize;
    private final int crowdSubsetNumber;
    private final int numberOfSelectedAssignments;
    private final Random random;

    public WorkerAssignmentFilterRandomized(int requiredAllAssignmentSize, int crowdSubsetNumber,
            int numberOfSelectedAssignments, Random random)
    {
        if (crowdSubsetNumber < 1 || crowdSubsetNumber > 2) {
            throw new IllegalArgumentException("crowdSubsetNumber must be either 1 or 2");
        }

        if (requiredAllAssignmentSize % 2 == 1) {
            throw new IllegalArgumentException(
                    "Only even requiredAssignmentSize allowed, got " + requiredAllAssignmentSize);
        }

        if (numberOfSelectedAssignments > requiredAllAssignmentSize / 2) {
            throw new IllegalArgumentException("numberOfSelectedAssignments > requiredAssignmentSize / 2");
        }

        this.requiredAllAssignmentSize = requiredAllAssignmentSize;
        this.numberOfSelectedAssignments = numberOfSelectedAssignments;
        this.crowdSubsetNumber = crowdSubsetNumber;
        this.random = random;
    }

    @Override
    protected <T> SortedSet<SingleWorkerAssignment<T>> filter(
            SortedSet<SingleWorkerAssignment<T>> value)
    {
        List<SingleWorkerAssignment<T>> list = new ArrayList<>(value);

        if (list.size() < requiredAllAssignmentSize) {
            System.err.println("Skipping. Required " + requiredAllAssignmentSize + " assignments but got " + list.size());
            return new TreeSet<>();
        }

        int bucketSize = requiredAllAssignmentSize / 2;

        // first half per default
        List<SingleWorkerAssignment<T>> subSampledList = list.subList(0, bucketSize);

        // or second half
        if (crowdSubsetNumber == 2) {
            subSampledList = list.subList(bucketSize, bucketSize + bucketSize);
        }

        // now sample randomly crowdSubsetNumber samples
        Collections.shuffle(subSampledList, random);
        subSampledList = subSampledList.subList(0, numberOfSelectedAssignments);

        return new TreeSet<>(subSampledList);
    }
}
