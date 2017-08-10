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

/**
 * All final fields must be not-null; the disambiguated stance is assumed to be ORIGINAL
 *
 * @author Ivan Habernal
 */
public class ReasonClaimWarrantContainer
{
    /**
     * A combination of reasonId and workerId (reasonId_workerId, e.g., 20232_12_AX7894455)
     */
    private final String reasonClaimWarrantId;
    private final String reasonId;
    private final DebateMetaData debateMetaData;
    private final String stanceOpposingToAnnotatedStance;
    private final String annotatedStance;
    private final String reasonGist;
    private final String workerId;
    private final Integer workerFamiliarity;

    private String alternativeWarrant;
    private String distractingReasonGist;
    private String distractingReasonId;
    private double hardScore;
    private double logicScore;
    private String originalWarrant;

    public ReasonClaimWarrantContainer(String reasonId,
            DebateMetaData debateMetaData, String annotatedStance,
            String stanceOpposingToAnnotatedStance, String reasonGist,
            String workerId, Integer workerFamiliarity, String alternativeWarrant)
    {
        if (reasonId.isEmpty()) {
            throw new IllegalArgumentException("reasonId is empty");
        }

        if (debateMetaData == null) {
            throw new IllegalArgumentException("debateMetaData is null");
        }

        if (stanceOpposingToAnnotatedStance.isEmpty()) {
            throw new IllegalArgumentException("stanceOpposingToAnnotatedStance is empty");
        }

        if (annotatedStance.isEmpty()) {
            throw new IllegalArgumentException("annotatedStance is empty");
        }

        if (reasonGist.isEmpty()) {
            throw new IllegalArgumentException("reasonGist is empty");
        }

        if (workerId.isEmpty()) {
            throw new IllegalArgumentException("workerId is empty");
        }

        if (workerFamiliarity < -1 || workerFamiliarity > 1) {
            throw new IllegalArgumentException("Worker familiarity must be between -1 and 1");
        }

        if (alternativeWarrant.isEmpty()) {
            throw new IllegalArgumentException("alternativeWarrant is empty");
        }

        this.reasonId = reasonId;
        this.debateMetaData = debateMetaData;
        this.stanceOpposingToAnnotatedStance = stanceOpposingToAnnotatedStance;
        this.annotatedStance = annotatedStance;
        this.reasonGist = reasonGist;
        this.workerId = workerId;
        this.workerFamiliarity = workerFamiliarity;
        this.alternativeWarrant = alternativeWarrant;

        // create id
        this.reasonClaimWarrantId = this.reasonId + "_" + workerId;
    }

    public String getReasonId()
    {
        return reasonId;
    }

    public DebateMetaData getDebateMetaData()
    {
        return debateMetaData;
    }

    public String getStanceOpposingToAnnotatedStance()
    {
        return stanceOpposingToAnnotatedStance;
    }

    public String getAnnotatedStance()
    {
        return annotatedStance;
    }

    public String getReasonGist()
    {
        return reasonGist;
    }

    public String getWorkerId()
    {
        return workerId;
    }

    public String getAlternativeWarrant()
    {
        return alternativeWarrant;
    }

    /**
     * -1 = no; 0 = somewhat; 1 = yes;
     *
     * @return int or null if unknown
     */
    public Integer getWorkerFamiliarity()
    {
        return workerFamiliarity;
    }

    public void setDistractingReasonGist(String distractingReasonGist)
    {
        if (distractingReasonGist.isEmpty()) {
            throw new IllegalArgumentException("distractingReasonGist is empty");
        }

        this.distractingReasonGist = distractingReasonGist;
    }

    public String getDistractingReasonGist()
    {
        return distractingReasonGist;
    }

    public String getReasonClaimWarrantId()
    {
        return reasonClaimWarrantId;
    }

    public String getDistractingReasonId()
    {
        return distractingReasonId;
    }

    public void setDistractingReasonId(String distractingReasonId)
    {
        this.distractingReasonId = distractingReasonId;
    }

    public void setHardScore(double hardScore)
    {
        this.hardScore = hardScore;
    }

    public double getHardScore()
    {
        return hardScore;
    }

    public void setLogicScore(double logicScore)
    {
        this.logicScore = logicScore;
    }

    public double getLogicScore()
    {
        return logicScore;
    }

    public void setOriginalWarrant(String originalWarrant)
    {
        if (originalWarrant.isEmpty()) {
            throw new IllegalArgumentException("originalWarrant param is empty");
        }

        this.originalWarrant = originalWarrant;
    }

    public String getOriginalWarrant()
    {
        return originalWarrant;
    }

    public void setAlternativeWarrant(String alternativeWarrant)
    {
        if (alternativeWarrant.isEmpty()) {
            throw new IllegalArgumentException("alternativeWarrant is empty");
        }

        this.alternativeWarrant = alternativeWarrant;
    }
}
