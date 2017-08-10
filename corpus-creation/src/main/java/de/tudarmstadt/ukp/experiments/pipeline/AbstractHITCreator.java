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

package de.tudarmstadt.ukp.experiments.pipeline;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Locale;
import java.util.Random;

/**
 * @author Ivan Habernal
 */
public abstract class AbstractHITCreator
{
    protected static final String MTURK_SANDBOX_URL = "https://workersandbox.mturk.com/mturk/externalSubmit";
    protected static final String MTURK_ACTUAL_URL = "https://www.mturk.com/mturk/externalSubmit";
    /**
     * Parameter for output HIT files
     */
    protected final static String fileNamePattern = "mthit-%05d.html";
    /**
     * Use sandbox or real MTurk?
     */
    protected final boolean sandbox;
    protected final Random random = new Random(0);
    protected Mustache mustache;
    protected File outputPath;
    protected int outputFileCounter = 0;

    public AbstractHITCreator(
            boolean sandbox)
    {
        this.sandbox = sandbox;
    }

    public void initialize(String mustacheTemplate)
            throws IOException
    {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(mustacheTemplate);
        if (stream == null) {
            throw new FileNotFoundException("Resource not found: " + mustacheTemplate);
        }

        // compile template
        MustacheFactory mf = new DefaultMustacheFactory();
        Reader reader = new InputStreamReader(stream, "utf-8");
        mustache = mf.compile(reader, "template");

        // output path
        if (!outputPath.exists()) {
            outputPath.mkdirs();
        }
    }

    public abstract void prepareBatchFromTo(File inputFile, File outputDir, int from, int to,
            String mustacheTemplate)
            throws IOException;

    protected abstract int getArgumentsPerHIT();

    protected void executeMustacheTemplate(Object hitContainer)
            throws FileNotFoundException
    {
        // get the correct output file
        File outputHITFile = new File(outputPath,
                String.format(Locale.ENGLISH, fileNamePattern, this.outputFileCounter));

        System.out.println("Generating " + outputHITFile);

        PrintWriter pw = new PrintWriter(outputHITFile);
        this.mustache.execute(pw, hitContainer);
        IOUtils.closeQuietly(pw);

        // increase counter
        this.outputFileCounter++;
    }
}
