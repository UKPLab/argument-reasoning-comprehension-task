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

package de.tudarmstadt.ukp.experiments.pipeline.attic;

import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Counterpart to {@linkplain CompressedXmiWriter} which reads XMI entries from a single
 * {@code .tar.gz} file.
 *
 * @author Ivan Habernal
 */
@Deprecated
public class CompressedXmiReader
        extends CasCollectionReader_ImplBase
{
    /**
     * Location (tar.gz file) from which the input is read.
     */
    public static final String PARAM_SOURCE_LOCATION = ComponentParameters.PARAM_SOURCE_LOCATION;
    @ConfigurationParameter(name = PARAM_SOURCE_LOCATION, mandatory = false)
    private String sourceLocation;

    /**
     * In lenient mode, unknown types are ignored and do not cause an exception to be thrown.
     */
    public static final String PARAM_LENIENT = "lenient";
    @ConfigurationParameter(name = PARAM_LENIENT, defaultValue = "false")
    private boolean lenient;

    /**
     * The next entry (can be null if end of tar stream reached)
     */
    private TarArchiveEntry nextTarEntry;

    /**
     * Input stream for reading from tar.gz
     */
    private TarArchiveInputStream tarArchiveInputStream;

    @Override
    public void initialize(UimaContext context)
            throws ResourceInitializationException
    {
        super.initialize(context);

        try {
            tarArchiveInputStream = new TarArchiveInputStream(
                    new GZIPInputStream(new FileInputStream(sourceLocation)));

            fastForwardToNextValidEntry();
        }
        catch (IOException ex) {
            throw new ResourceInitializationException(ex);
        }
    }

    /**
     * As the tar.gz file can contain other stuff (such as "typesystem.xml"), we need to accept
     * only ".xmi" entries for loading
     *
     * @throws IOException IO exception
     */
    private void fastForwardToNextValidEntry()
            throws IOException
    {
        nextTarEntry = tarArchiveInputStream.getNextTarEntry();

        while (nextTarEntry != null && !nextTarEntry.getName().endsWith(".xmi")) {
            nextTarEntry = tarArchiveInputStream.getNextTarEntry();
        }
    }

    @Override
    public void getNext(CAS aCAS)
            throws IOException, CollectionException
    {
        // nextTarEntry cannot be null here!
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int size = IOUtils.copy(tarArchiveInputStream, buffer);

        String entryName = nextTarEntry.getName();
        getLogger().debug("Loaded " + size + " bytes from " + entryName);

        // and move forward
        fastForwardToNextValidEntry();

        // and now create JCas
        InputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        try {
            XmiCasDeserializer.deserialize(inputStream, aCAS, lenient);
        }
        catch (SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean hasNext()
            throws IOException, CollectionException
    {
        return nextTarEntry != null;
    }

    @Override
    public Progress[] getProgress()
    {
        // empty as we don't know how many entries are in the tar stream...
        return new Progress[] {};
    }

    public static void main(String[] args)
            throws Exception
    {
        String in = "/tmp/out.tar.gz";
        SimplePipeline.runPipeline(
                CollectionReaderFactory.createReaderDescription(
                        CompressedXmiReader.class,
                        CompressedXmiReader.PARAM_SOURCE_LOCATION, in
                )
        );
    }
}
