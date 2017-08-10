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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import de.tudarmstadt.ukp.experiments.pipeline.containers.HITBufferElement;
import de.tudarmstadt.ukp.experiments.pipeline.containers.MTurkHITContainerReasonClaimWarrantOriginal;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * (c) 2017 Ivan Habernal
 */
public class Step7aOriginalWarrantHITCreator
        extends Step6aAlternativeWarrantValidationHITCreator
{

    public Step7aOriginalWarrantHITCreator(boolean sandbox)
    {
        super(sandbox);
    }

    @Override
    protected List<ReasonClaimWarrantContainer> preFilterList(
            List<ReasonClaimWarrantContainer> list)
    {
        Set<String> uniqueIDs = new TreeSet<>();
        // check we have unique ID's!!!
        for (ReasonClaimWarrantContainer claimWarrantContainer : list) {
            String id = claimWarrantContainer.getReasonClaimWarrantId();
            if (uniqueIDs.contains(id)) {
                throw new IllegalStateException("Repeated ID: " + id);
            }

            uniqueIDs.add(id);
        }

        // do some statistics first
        DescriptiveStatistics statHard = new DescriptiveStatistics();
        DescriptiveStatistics statLogic = new DescriptiveStatistics();

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("Random");
        for (ReasonClaimWarrantContainer container : list) {
            series.add(container.getLogicScore(), container.getHardScore());
            statHard.addValue(container.getHardScore());
            statLogic.addValue(container.getLogicScore());
        }
        dataset.addSeries(series);

        //        System.out.println(statHard);
        //        System.out.println(statLogic);

        JFrame jFrame = new JFrame();
        // This will create the dataset
        // based on the dataset we create the chart
        JFreeChart chart = ChartFactory
                .createScatterPlot("title", "Logic", "Hard", dataset, PlotOrientation.HORIZONTAL,
                        false, false, false);
        // we put the chart into a panel
        ChartPanel chartPanel = new ChartPanel(chart);
        // default size
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        // add it to our application
        jFrame.setContentPane(chartPanel);

        //        jFrame.pack();
        //        jFrame.setVisible(true);

        // ok, take only those with logic > 0.68
        return list.stream()
                .filter(reasonClaimWarrantContainer -> reasonClaimWarrantContainer.getLogicScore()
                        >= 0.68).collect(Collectors.toList());
    }

    @Override
    protected void process(ReasonClaimWarrantContainer reasonClaimWarrantContainer)
    {
        HITBufferElement reasonClaimWarrant = new MTurkHITContainerReasonClaimWarrantOriginal.HITReasonClaimWarrantOriginal(
                reasonClaimWarrantContainer.getDebateMetaData().getTitle(),
                reasonClaimWarrantContainer.getDebateMetaData().getDescription(),
                reasonClaimWarrantContainer.getReasonClaimWarrantId(),
                reasonClaimWarrantContainer.getAnnotatedStance(),
                reasonClaimWarrantContainer.getAlternativeWarrant(),
                reasonClaimWarrantContainer.getReasonGist());

        this.buffer.add(reasonClaimWarrant);
    }

    @Override
    protected int getArgumentsPerHIT()
    {
        return 4;
    }

    public static void main(String[] args)
            throws Exception
    {
        // 90 pilot
        //        new Step15bOriginalWarrantHITCreator(false).prepareBatchFromTo(new File(
        //                        "mturk/annotation-task/data/80-aw-validation-batch-0001-5000-all-batches-3791reason-claim-pairs.xml.gz"),
        //                new File("mturk/annotation-task/html/90-original-warrant-pilot"), 0, 30,
        //                "original-warrant.mustache");

        // 91 batch
//        new Step15bOriginalWarrantHITCreator(false).prepareBatchFromTo(new File(
//                        "mturk/annotation-task/data/80-aw-validation-batch-0001-5000-all-batches-3791reason-claim-pairs.xml.gz"),
//                new File("mturk/annotation-task/html/91-original-warrant-batch-0001-5000-batch-0100-0600"), 0, 600,
//                "original-warrant.mustache");

        // 91 batch -- changed to 4 items per HIT
        new Step7aOriginalWarrantHITCreator(false).prepareBatchFromTo(new File(
                        "mturk/annotation-task/data/80-aw-validation-batch-0001-5000-all-batches-3791reason-claim-pairs.xml.gz"),
                new File("mturk/annotation-task/html/92-original-warrant-batch-0001-5000-batch-0600-2613"), 600, 2613,
                "original-warrant.mustache");
    }
}
