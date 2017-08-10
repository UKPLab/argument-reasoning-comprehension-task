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

package de.tudarmstadt.ukp.experiments.segmenter;

import edu.arizona.sista.discourse.rstparser.DiscourseTree;
import edu.arizona.sista.processors.Document;
import edu.arizona.sista.processors.Processor;
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor;
import org.apache.commons.io.FileUtils;
import scala.Enumeration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * @author Ivan Habernal
 */
public class ExternalEDUSegmenter
{
    // create the processor
    Processor proc = new CoreNLPProcessor(false, true, 1000);

    public List<List<String>> collectEDUs(String tokens)
    {
        Document doc = proc.annotate(tokens, true);
        StringBuilder sb = new StringBuilder();
        List<String> eduList = new ArrayList<>();
        collect(doc.discourseTree().get(), sb, 0, true, true, eduList);

        //                System.out.println(sb);
        List<List<String>> result = new ArrayList<>();

        for (String edu : eduList) {
            List<String> eduTokens = Arrays.asList(edu.split(" "));
            result.add(eduTokens);

        }

        System.out.println(result);

        return result;
    }

    private static void collect(DiscourseTree tree, final StringBuilder os, final int offset,
            final boolean printChildren, final boolean printKind, List<String> eduBuffer)
    {
        for (int i = 0; i < offset; ++i) {
            os.append(" ");
        }

        boolean printedText = false;
        if (printKind) {
            os.append(tree.kind());
            printedText = true;
        }

        if (tree.relationLabel().length() > 0) {
            if (printKind) {
                os.append(":");
            }

            label49:
            {
                os.append(tree.relationLabel());
                Enumeration.Value var8 = tree.relationDirection();
                Enumeration.Value var7 = edu.arizona.sista.discourse.rstparser.RelationDirection
                        .None();
                if (var8 == null) {
                    if (var7 == null) {
                        break label49;
                    }
                }
                else if (var8.equals(var7)) {
                    break label49;
                }

                os.append(" (");
                os.append(tree.relationDirection());
                os.append(")");
            }

            printedText = true;
        }

        if (tree.rawText() != null) {
            if (printedText) {
                os.append(" ");
            }

            os.append("TEXT");
            if (tree.charOffsets()._1$mcI$sp() != -1) {
                os.append("(");
                os.append(tree.charOffsets()._1$mcI$sp());
                os.append(", ");
                os.append(tree.charOffsets()._2$mcI$sp());
                os.append(")");
            }

            os.append(":");
            os.append(tree.rawText());
        }

        if (printChildren) {
            os.append("\n");
            if (!tree.isTerminal()) {
                for (DiscourseTree dt : tree.children()) {

                    collect(dt, os, offset + 2, printChildren, printKind, eduBuffer);
                }
            }
            else {
                //                System.out.println("Terminal");
                //                System.out.println(tree.rawText());
                //                System.out.println(tree);

                eduBuffer.add(tree.rawText());
            }
        }
    }

    public void processFile(File inputFile, File outputFile)
            throws IOException
    {
        SortedMap<String, List<List<String>>> output = new TreeMap<>();

        List<String> lines = FileUtils.readLines(inputFile);
        for (String line : lines) {
            // ID
            String[] split = line.split("\t");
            String id = split[0];
            String text = split[1];

            List<List<String>> collectEDUs = collectEDUs(text);

            output.put(id, collectEDUs);
        }

        // save to binary inputFile
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(outputFile));
        os.writeObject(output);
        os.close();
    }

    public static void main(String[] args)
            throws IOException
    {

        // first param: 04-segmented-arguments-rfd/exported-plaint-text-for-edu-segmentation.txt
        // second param: 04-segmented-arguments-rfd/segmented-edu-serialized.bin
        new ExternalEDUSegmenter().processFile(new File(args[0]), new File(args[1]));
    }

}
