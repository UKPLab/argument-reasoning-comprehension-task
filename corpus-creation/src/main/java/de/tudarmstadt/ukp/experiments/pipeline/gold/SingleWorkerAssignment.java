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

import java.util.Comparator;
import java.util.Date;

/**
 * @author Ivan Habernal
 */
public class SingleWorkerAssignment<T>
        implements Comparable<SingleWorkerAssignment>
{
    private final String workerID;
    private final Date assignmentDate;
    private final T label;

    public String getWorkerID()
    {
        return workerID;
    }

    public Date getAssignmentDate()
    {
        return assignmentDate;
    }

    public T getLabel()
    {
        return label;
    }

    public SingleWorkerAssignment(String workerID, Date assignmentDate, T label)
    {
        if (workerID == null || assignmentDate == null || label == null) {
            throw new IllegalArgumentException("All params must be non-null");
        }

        this.workerID = workerID;
        this.assignmentDate = assignmentDate;
        this.label = label;
    }

    @Override public int compareTo(SingleWorkerAssignment o)
    {
        return this.assignmentDate.compareTo(o.assignmentDate);
    }

    @Override public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SingleWorkerAssignment<?> that = (SingleWorkerAssignment<?>) o;

        if (!workerID.equals(that.workerID)) {
            return false;
        }
        if (!assignmentDate.equals(that.assignmentDate)) {
            return false;
        }
        return label.equals(that.label);
    }

    @Override public int hashCode()
    {
        int result = workerID.hashCode();
        result = 31 * result + assignmentDate.hashCode();
        result = 31 * result + label.hashCode();
        return result;
    }

    @Override public String toString()
    {
        return "SingleWorkerAssignment{" +
                "workerID='" + workerID + '\'' +
                ", assignmentDate=" + assignmentDate +
                ", label=" + label +
                '}';
    }

    /**
     * Returns a comparator instance by worker ID
     *
     * @return comparator instance
     */
    public static Comparator<SingleWorkerAssignment<String>> getComparatorByWorkerID()
    {
        return (o1, o2) -> o1.getWorkerID().compareTo(o2.getWorkerID());
    }
}
