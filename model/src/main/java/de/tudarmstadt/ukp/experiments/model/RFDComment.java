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

package de.tudarmstadt.ukp.experiments.model;

import java.util.Date;

/**
 * POJO container for a single comment
 *
 * @author Ivan Habernal
 */
public class RFDComment
{

    private String id;

    private Date timestamp;

    private String parentId;

    private String previousPostId;

    private String author;

    private boolean authorTrusted;

    private String authorLocation;

    private int voteUpCount;

    private String text;

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getParentId()
    {
        return parentId;
    }

    public void setParentId(String parentId)
    {
        this.parentId = parentId;
    }

    public String getPreviousPostId()
    {
        return previousPostId;
    }

    public void setPreviousPostId(String previousPostId)
    {
        this.previousPostId = previousPostId;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public boolean isAuthorTrusted()
    {
        return authorTrusted;
    }

    public void setAuthorTrusted(boolean authorTrusted)
    {
        this.authorTrusted = authorTrusted;
    }

    public String getAuthorLocation()
    {
        return authorLocation;
    }

    public void setAuthorLocation(String authorLocation)
    {
        this.authorLocation = authorLocation;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public int getVoteUpCount()
    {
        return voteUpCount;
    }

    public void setVoteUpCount(int voteUpCount)
    {
        this.voteUpCount = voteUpCount;
    }

    @Override public String toString()
    {
        return "Comment{" +
                "text='" + text + '\'' +
                ", id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", previousPostId='" + previousPostId + '\'' +
                ", commenterName='" + author + '\'' +
                ", commenterTrusted=" + authorTrusted +
                ", commenterLocation='" + authorLocation + '\'' +
                ", timestamp=" + timestamp +
                ", recommendCount=" + voteUpCount +
                '}';
    }
}
