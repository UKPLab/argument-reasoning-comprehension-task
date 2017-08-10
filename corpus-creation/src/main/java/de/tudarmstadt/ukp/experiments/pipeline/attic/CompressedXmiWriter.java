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

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.TypeSystemUtil;
import org.xml.sax.SAXException;

import java.io.*;

/**
 * A general-purpose XMI-based UIMA writer for storing all output XMI files in a gzipped-tar
 * file.
 *
 * @author Ivan Habernal
 */
@Deprecated
public class CompressedXmiWriter
        extends JCasConsumer_ImplBase
{
    private boolean typeSystemWritten;

    /**
     * Output tar.gz file
     */
    public static final String PARAM_OUTPUT_FILE = "outputFile";
    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    protected File outputFile;

    /*
    Compressed tar output stream
     */
    protected TarArchiveOutputStream outputStream;

    private int counter;

    @Override
    public void initialize(UimaContext aContext)
            throws ResourceInitializationException
    {
        super.initialize(aContext);

        // some param check
        if (!outputFile.getName().endsWith(".tar.gz")) {
            throw new ResourceInitializationException(
                    new IllegalArgumentException("Output file must have .tar.gz extension"));
        }

        typeSystemWritten = false;

        try {
            outputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(
                    new BufferedOutputStream(new FileOutputStream(outputFile))));
        }
        catch (IOException ex) {
            throw new ResourceInitializationException(ex);
        }
    }

    private void addSingleEntryToTar(byte[] singleEntryContent, String singleEntryName)
            throws IOException
    {
        // create new entry; it requires file for some reasons...
        File tmpFile = File.createTempFile("temp", "bin");
        OutputStream os = new FileOutputStream(tmpFile);
        IOUtils.write(singleEntryContent, os);

        TarArchiveEntry tarEntry = new TarArchiveEntry(tmpFile, singleEntryName);
        outputStream.putArchiveEntry(tarEntry);

        // copy streams
        IOUtils.copy(new ByteArrayInputStream(singleEntryContent), outputStream);
        outputStream.closeArchiveEntry();

        // delete the temp file
        FileUtils.forceDelete(tmpFile);
    }

    @Override
    public void collectionProcessComplete()
            throws AnalysisEngineProcessException
    {
        super.collectionProcessComplete();
        getLogger().info(counter + " XMI entries successfully written to " + outputFile);

        // close streams
        IOUtils.closeQuietly(outputStream);
    }

    @Override
    public void process(JCas aJCas)
            throws AnalysisEngineProcessException
    {
        try {
            java.io.ByteArrayOutputStream jCasOutputStream = new java.io.ByteArrayOutputStream();
            XmiCasSerializer.serialize(aJCas.getCas(), jCasOutputStream);

            // get name = id + .xmi
            String singleEntryName = DocumentMetaData.get(aJCas).getDocumentId() + ".xmi";
            // convert output stream to input stream
            //            InputStream inputStream = new ByteArrayInputStream(jCasOutputStream.toByteArray());

            // add to the tar
            addSingleEntryToTar(jCasOutputStream.toByteArray(), singleEntryName);

            if (!typeSystemWritten) {
                writeTypeSystem(aJCas);
                typeSystemWritten = true;
            }

            counter++;
        }
        catch (IOException | SAXException ex) {
            throw new AnalysisEngineProcessException(ex);
        }
    }

    /**
     * Writes "typesystem.xml" to the output tar.gz
     *
     * @param aJCas any jcas from the collection to get the currently active typesystem
     * @throws IOException IO Exception
     */
    private void writeTypeSystem(JCas aJCas)
            throws IOException
    {
        try {
            String name = "typesystem.xml";
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();

            TypeSystemUtil.typeSystem2TypeSystemDescription(aJCas.getTypeSystem()).toXML(stream);

            // add to tar
            addSingleEntryToTar(stream.toByteArray(), name);
        }
        catch (SAXException e) {
            throw new IOException(e);
        }
    }

    public static void main(String[] args)
            throws Exception
    {
        String in = "/tmp/temp-in";
        String out = "/tmp/out2.tar.gz";
        // test it
        SimplePipeline.runPipeline(CollectionReaderFactory.createReaderDescription(
                XmiReader.class,
                XmiReader.PARAM_SOURCE_LOCATION, in,
                XmiReader.PARAM_PATTERNS, XmiReader.INCLUDE_PREFIX + "*.xmi"
                ),
                AnalysisEngineFactory.createEngineDescription(
                        NoOpAnnotator.class
                ),
                AnalysisEngineFactory.createEngineDescription(
                        CompressedXmiWriter.class,
                        CompressedXmiWriter.PARAM_OUTPUT_FILE, out
                )
        );

    }
}
