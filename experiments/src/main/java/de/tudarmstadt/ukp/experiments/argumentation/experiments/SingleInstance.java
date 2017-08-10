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

package de.tudarmstadt.ukp.experiments.argumentation.experiments;

import org.apache.commons.lang.StringUtils;

import java.util.Objects;

/**
 * IDs are unique and solely used for compare, hash, and equal
 */
public class SingleInstance
        implements Comparable<SingleInstance>
{
    private final String id;
    private final String warrant0;
    private final String warrant1;
    private final Integer correctLabelW0orW1;
    private final String reason;
    private final String claim;
    private final String debateTitle;
    private final String debateInfo;

    public SingleInstance(SingleInstance original)
    {
        this.id = original.id;
        this.warrant0 = original.warrant0;
        this.warrant1 = original.warrant1;
        this.correctLabelW0orW1 = original.correctLabelW0orW1;
        this.reason = original.reason;
        this.claim = original.claim;
        this.debateTitle = original.debateTitle;
        this.debateInfo = original.debateInfo;
    }

    public SingleInstance(String id, String warrant0, String warrant1, Integer correctLabelW0orW1,
            String reason, String claim, String debateTitle, String debateInfo)
    {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Blank parameter: id");
        }
        if (StringUtils.isBlank(warrant0)) {
            throw new IllegalArgumentException("Blank parameter: warrant0");
        }
        if (StringUtils.isBlank(warrant1)) {
            throw new IllegalArgumentException("Blank parameter: warrant1");
        }
        if (correctLabelW0orW1 < 0 || correctLabelW0orW1 > 1) {
            throw new IllegalArgumentException("Wrong parameter: correctLabelW0orW1");
        }
        if (StringUtils.isBlank(reason)) {
            throw new IllegalArgumentException("Blank parameter: reason");
        }
        if (StringUtils.isBlank(claim)) {
            throw new IllegalArgumentException("Blank parameter: claim");
        }
        if (StringUtils.isBlank(debateTitle)) {
            throw new IllegalArgumentException("Blank parameter: debateTitle");
        }
        if (StringUtils.isBlank(debateInfo)) {
            throw new IllegalArgumentException("Blank parameter: debateInfo");
        }

        this.id = id;
        this.warrant0 = warrant0;
        this.warrant1 = warrant1;
        this.correctLabelW0orW1 = correctLabelW0orW1;
        this.reason = reason;
        this.claim = claim;
        this.debateTitle = debateTitle;
        this.debateInfo = debateInfo;
    }

    public SingleInstance(String tsvLine)
    {
        this(tsvLine.split("\t")[0], tsvLine.split("\t")[1], tsvLine.split("\t")[2],
                Integer.valueOf(tsvLine.split("\t")[3]), tsvLine.split("\t")[4],
                tsvLine.split("\t")[5], tsvLine.split("\t")[6], tsvLine.split("\t")[7]);
    }

    public String getId()
    {
        return id;
    }

    public String getWarrant0()
    {
        return warrant0;
    }

    public String getWarrant1()
    {
        return warrant1;
    }

    public Integer getCorrectLabelW0orW1()
    {
        return correctLabelW0orW1;
    }

    public String getReason()
    {
        return reason;
    }

    public String getClaim()
    {
        return claim;
    }

    public String getDebateTitle()
    {
        return debateTitle;
    }

    public String getDebateInfo()
    {
        return debateInfo;
    }

    @Override
    public int compareTo(SingleInstance o)
    {
        return this.id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SingleInstance)) {
            return false;
        }
        SingleInstance that = (SingleInstance) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getId());
    }
}
