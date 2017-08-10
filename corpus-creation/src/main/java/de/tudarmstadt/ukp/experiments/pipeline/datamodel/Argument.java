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

import java.util.Date;

/**
 * @author Ivan Habernal
 */
public class Argument
{
    /**
     * Argument author's id
     */
    private String author;

    /**
     * Number of points this argument got "vote-up" the debate portal user
     */
    private Integer voteUpCount;

    /**
     * Number of points this argument got "vote-down" the debate portal user
     */
    private int voteDownCount;

    /**
     * Stance taken by the author (always only two options per debate)
     */
    private String stance;

    /**
     * Text of the full argument
     */
    private String text;

    /**
     * parent argument (not-null if the argumetn is an answer, null otherwise)
     */
    private String parentId;

    /**
     * Id of the element as present on the html page
     */
    private String id;

    /**
     * For storing original HTML if any re-parsing required
     */
    private String originalHTML;

    private Date timestamp;

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public void setVoteUpCount(Integer argPoints)
    {
        this.voteUpCount = argPoints;
    }

    public void setStance(String stance)
    {
        this.stance = stance;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setParentId(String parentId)
    {
        this.parentId = parentId;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public Integer getVoteUpCount()
    {
        return this.voteUpCount;
    }

    public String getStance()
    {
        return this.stance;
    }

    public String getText()
    {
        return this.text;

    }

    public String getParentId()
    {
        return this.parentId;

    }

    public String getId()
    {
        return this.id;
    }

    public String getOriginalHTML()
    {
        return originalHTML;
    }

    public void setOriginalHTML(String originalHTML)
    {
        this.originalHTML = originalHTML;
    }

    public void setVoteDownCount(int voteDownCount)
    {
        this.voteDownCount = voteDownCount;
    }

    public int getVoteDownCount()
    {
        return voteDownCount;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    @Override
    public String toString()
    {
        return "Argument{" +
                "author='" + author + '\'' +
                ", voteUpCount=" + voteUpCount +
                ", voteDownCount=" + voteDownCount +
                ", stance='" + stance + '\'' +
                ", text='" + text + '\'' +
                ", parentId='" + parentId + '\'' +
                ", id='" + id + '\'' +
                ", originalHTML='" + originalHTML + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
