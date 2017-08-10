/*
***************************************************************************
NTP License
https://opensource.org/licenses/NTP


      USC/ISI MACE Multi-Annotator Competence Estimation  

      USC Information Sciences Institute
      4676 Admiralty Way 
      Marina del Rey, CA 90292-6695 
      USA 

      Original Version: Natural Language Group, April 2013 
      Current Version:  Natural Language Group, April 2013

  Copyright (c) 2013 by the University of Southern California
  All rights reserved.

  Permission to use, copy, modify, and distribute this software and its
  documentation in source and binary forms for any purpose and without
  fee is hereby granted, provided that both the above copyright notice
  and this permission notice appear in all copies, and that any
  documentation, advertising materials, and other materials related to
  such distribution and use acknowledge that the software was developed
  in part by the University of Southern California, Information
  Sciences Institute.  The name of the University may not be used to
  endorse or promote products derived from this software without
  specific prior written permission.

  THE UNIVERSITY OF SOUTHERN CALIFORNIA makes no representations about
  the suitability of this software for any purpose.  THIS SOFTWARE IS
  PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES,
  INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.

  Other copyrights might apply to parts of this software and are so
  noted when applicable.

***************************************************************************
*/
package de.tudarmstadt.ukp.experiments.pipeline.gold;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * MACE: Multi-Annotator Competence Estimation
 * <p/>
 * EM-based learning of correct labels and annotator competence
 *
 * @author tberg, dirkh, avaswani
 */
public class MACE
{

    private static final String VERSION = "0.2";

    // defaults
    private static final int DEFAULT_RR = 10;
    private static final int DEFAULT_ITERATIONS = 50;
    private static final double DEFAULT_NOISE = 0.5;
    private static final double DEFAULT_ALPHA = 0.5;
    private static final double DEFAULT_BETA = 0.5;

    // fields
    private int numInstances;
    private int numAnnotators;
    private int numLabels;

    // training data
    // [d][ai]
    private int[][] whoLabeled;
    // [d][ai]
    private int[][] labels;

    // parameters
    // [a][2]
    private double[][] thetas;
    // [d][l]
    private double[][] strategies;

    // expected counts
    // [d][l]
    private double[][] goldLabelMarginals;
    // [a][l]
    private double[][] strategyExpectedCounts;
    // [a][2]
    private double[][] knowingExpectedCounts;

    // priors
    private double[][] thetaPriors;    // this controls how many of the annotators are actually good

    private double[][] strategyPriors;    // for now, these are 1.0

    private double logMarginalLikelhood;

    //hash stuff
    private Map<String, Integer> string2Int;
    private List<String> int2String;
    private int hashCounter;

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    private boolean debug = false;

    /**
     * Constructor
     *
     * @param csvFile: comma-separated file, one item per line, each value one annotation
     * @throws IOException exception
     */
    public MACE(String csvFile)
            throws IOException
    {

        numInstances = fileLineCount(csvFile);

        labels = new int[numInstances][];
        whoLabeled = new int[numInstances][];

        // hash stuff
        string2Int = new HashMap<>();
        int2String = new ArrayList<>();
        hashCounter = 0;

        // read in CSV file to get all basic information
        this.readFileData(csvFile);

        //this.numAnnotators = annotatorNumber;
        this.numLabels = int2String.size();

        this.goldLabelMarginals = new double[numInstances][numLabels];
        this.strategyExpectedCounts = new double[numAnnotators][numLabels];
        this.knowingExpectedCounts = new double[numAnnotators][2];

    }

    /**
     * initialize model parameters randomly
     */
    public void initialize(double initNoise)
    {
        Random rand = new Random();
        this.thetas = new double[numAnnotators][2];
        this.strategies = new double[numAnnotators][numLabels];
        for (int a = 0; a < numAnnotators; ++a) {
            Arrays.fill(thetas[a], 1.0);
            Arrays.fill(strategies[a], 1.0);
            thetas[a][0] += initNoise * rand.nextDouble();
            thetas[a][1] += initNoise * rand.nextDouble();
            for (int l = 0; l < numLabels; ++l) {
                strategies[a][l] += initNoise * rand.nextDouble();
            }
        }
        normalizeInPlace(thetas, 0.0);
        normalizeInPlace(strategies, 0.0);

        //System.out.println(thetas[0][0] + " " + thetas[0][1]);
    }

    /**
     * initialize and set prior matrices
     */
    public void initialize(double initNoise, double alpha, double beta)
    {
        this.initialize(initNoise);
        this.thetaPriors = new double[numAnnotators][2];
        this.strategyPriors = new double[numAnnotators][numLabels];
        for (int a = 0; a < numAnnotators; ++a) {
            thetaPriors[a][0] = alpha;
            thetaPriors[a][1] = beta;
            Arrays.fill(strategyPriors[a], 10.0);
        }
    }

    /**
     * compute expected counts when control items are provided
     */
    public void EStep(Map<Integer, Integer> controls)
    {

        // reset counts

        for (int d = 0; d < numInstances; ++d) {
            for (int l = 0; l < numLabels; ++l) {
                goldLabelMarginals[d][l] = 0.0;
            }
        }
        for (int a = 0; a < numAnnotators; ++a) {
            knowingExpectedCounts[a][0] = 0.0;
            knowingExpectedCounts[a][1] = 0.0;
            for (int l = 0; l < numLabels; ++l) {
                strategyExpectedCounts[a][l] = 0.0;
            }
        }

        // compute marginals

        logMarginalLikelhood = 0.0;

        for (int d = 0; d < numInstances; ++d) {
            double instanceMarginal = 0.0;

            for (int l = 0; l < numLabels; ++l) {
                double goldLabelMarginal = (1.0 / numLabels);
                for (int ai = 0; ai < labels[d].length; ++ai) {
                    int a = whoLabeled[d][ai];

                    goldLabelMarginal *=
                            thetas[a][0] * strategies[a][labels[d][ai]] + (l == labels[d][ai] ?
                                    thetas[a][1] :
                                    0.0);
                }

                if (!controls.containsKey(d) || (controls.containsKey(d) && l == controls.get(d))) {
                    instanceMarginal += goldLabelMarginal;
                    goldLabelMarginals[d][l] = goldLabelMarginal;
                }
            }

            logMarginalLikelhood += Math.log(instanceMarginal);

            for (int ai = 0; ai < labels[d].length; ++ai) {
                int a = whoLabeled[d][ai];
                double strategyMarginal = 0.0;

                if (controls.containsKey(d)) {
                    if (labels[d][ai] == controls.get(d)) {
                        {
                            int l = controls.get(d);
                            strategyMarginal += goldLabelMarginals[d][l] / (
                                    thetas[a][0] * strategies[a][labels[d][ai]] + (
                                            l == labels[d][ai] ? thetas[a][1] : 0.0));
                        }
                        strategyMarginal *= thetas[a][0] * strategies[a][labels[d][ai]];
                        strategyExpectedCounts[a][labels[d][ai]] +=
                                strategyMarginal / instanceMarginal;
                        knowingExpectedCounts[a][0] += strategyMarginal / instanceMarginal;
                        knowingExpectedCounts[a][1] +=
                                (goldLabelMarginals[d][labels[d][ai]] * thetas[a][1] / (
                                        thetas[a][0] * strategies[a][labels[d][ai]] + thetas[a][1]))
                                        / instanceMarginal;
                    }
                    else {
                        strategyExpectedCounts[a][labels[d][ai]] += 1.0;
                        knowingExpectedCounts[a][0] += 1.0;
                    }
                }
                else {
                    for (int l = 0; l < numLabels; ++l) {
                        strategyMarginal += goldLabelMarginals[d][l] / (
                                thetas[a][0] * strategies[a][labels[d][ai]] + (l == labels[d][ai] ?
                                        thetas[a][1] :
                                        0.0));
                    }
                    strategyMarginal *= thetas[a][0] * strategies[a][labels[d][ai]];
                    strategyExpectedCounts[a][labels[d][ai]] += strategyMarginal / instanceMarginal;
                    knowingExpectedCounts[a][0] += strategyMarginal / instanceMarginal;
                    knowingExpectedCounts[a][1] +=
                            (goldLabelMarginals[d][labels[d][ai]] * thetas[a][1] / (
                                    thetas[a][0] * strategies[a][labels[d][ai]] + thetas[a][1]))
                                    / instanceMarginal;
                }
            }
        }

    }

    /**
     * normalize expected counts
     */
    public void MStep(double smoothing)
    {
        thetas = normalize(knowingExpectedCounts, smoothing);
        strategies = normalize(strategyExpectedCounts, smoothing);
    }

    /**
     * normalize using priors
     */
    public void variationalMStep()
    {
        thetas = variationalNormalize(knowingExpectedCounts, thetaPriors);
        strategies = variationalNormalize(strategyExpectedCounts, strategyPriors);
    }

    /**
     * find best answer under the current model, ignore instance above threshold
     *
     * @return answer vector
     */
    public String[] decode(double threshold)
    {
        // get entropies
        double[] entropies = getLabelEntropies();
        double entropyThreshold = getEntropyForThreshold(threshold);

        String[] result = new String[numInstances];
        for (int d = 0; d < numInstances; ++d) {
            double bestProb = Double.NEGATIVE_INFINITY;
            int bestLabel = -1;

            // ignore instances above threshold
            if (entropies[d] <= entropyThreshold) {
                for (int l = 0; l < numLabels; ++l) {

                    if (goldLabelMarginals[d][l] > bestProb) {
                        bestProb = goldLabelMarginals[d][l];
                        bestLabel = l;
                    }
                }
                result[d] = int2String.get(bestLabel);
            }
            else
                result[d] = "";
        }

        return result;
    }

    /**
     * @return the entropies of each instance
     */
    public double[] getLabelEntropies()
    {
        double[] result = new double[numInstances];

        for (int d = 0; d < numInstances; ++d) {
            double norm = 0.0;
            double entropy = 0.0;
            for (int l = 0; l < numLabels; ++l) {
                norm += goldLabelMarginals[d][l];
            }
            for (int l = 0; l < numLabels; ++l) {
                double p = goldLabelMarginals[d][l] / norm;
                if (p > 0.0) {
                    entropy += -p * Math.log(p);
                }
            }
            result[d] = entropy;
        }

        return result;
    }

    /**
     * run EM with the specified parameters
     *
     * @param beta         beta
     * @param numIters:    number of iterations
     * @param smoothing:   smoothing added to expected counts before normalizing
     * @param numRestarts: number of restarts
     * @throws IOException
     */
    public void run(int numIters, double smoothing, int numRestarts, double alpha, double beta,
            boolean variational, String controlsFile)
            throws IOException
    {
        Map<Integer, Integer> controls;
        if (controlsFile != null) {
            controls = this.readControls(controlsFile);
        }
        else {
            controls = new HashMap<>();
        }

        double[][] bestThetas = new double[numAnnotators][2];
        double[][] bestStrategies = new double[numAnnotators][numLabels];
        double bestLogMarginalLikelihood = Double.NEGATIVE_INFINITY;
        int rrBestModelOccurredAt = 0;

        System.out.println("Running training with the following settings:");
        System.out.println("\t" + numIters + " iterations");
        System.out.println("\t" + numRestarts + " restarts");
        System.out.println("\tsmoothing = " + smoothing);
        if (variational) {
            System.out.println("\talpha = " + alpha);
            System.out.println("\tbeta = " + beta);
        }

        double start = System.currentTimeMillis();
        for (int rr = 0; rr < numRestarts; rr++) {
            debug("\n============");
            debug("Restart " + (rr + 1));
            debug("============");

            // initialize
            if (variational)
                initialize(DEFAULT_NOISE, alpha, beta);
            else
                initialize(DEFAULT_NOISE);

            // run first E-Step to get counts
            EStep(controls);
            debug("initial log marginal likelihood = " + logMarginalLikelhood);

            // iterate
            for (int t = 0; t < numIters; ++t) {
                if (variational)
                    variationalMStep();
                else
                    MStep(smoothing);
                EStep(controls);
                //System.out.println("iter "+t);
                //System.out.println("log marginal likelihood "+logMarginalLikelhood);
            }
            debug("final log marginal likelihood = " + logMarginalLikelhood);

            // renormalize thetas
            //normalizeInPlace(thetas, 0.0);
            //normalizeInPlace(strategies, 0.0);

            if (logMarginalLikelhood > bestLogMarginalLikelihood) {
                //if (rr>0) System.out.println("NEW BEST MODEL!\n");
                rrBestModelOccurredAt = rr + 1;
                bestLogMarginalLikelihood = logMarginalLikelhood;
                bestThetas = thetas.clone();
                bestStrategies = strategies.clone();
            }
        }
        System.out.println(
                "\nTraining completed in " + ((System.currentTimeMillis() - start) / 1000) + "sec");
        System.out.println("Best model came from random restart number " + rrBestModelOccurredAt
                + " (log marginal likelihood: " + bestLogMarginalLikelihood + ")");
        logMarginalLikelhood = bestLogMarginalLikelihood;
        thetas = bestThetas;
        strategies = bestStrategies;

        // run E-step to get marginals of latest model
        EStep(controls);

    }

    private void debug(String message)
    {
        if (this.debug) {
            System.out.println(message);
        }
    }

    /**
     * normalize a matrix by row
     *
     * @return normalized matrix
     */
    public static double[][] normalize(double[][] mat, double smoothing)
    {
        double[][] result = new double[mat.length][mat[0].length];
        for (int i = 0; i < result.length; ++i) {
            double norm = 0.0;
            for (int j = 0; j < result[0].length; ++j) {
                norm += mat[i][j] + smoothing;
            }
            for (int j = 0; j < result[0].length; ++j) {
                if (norm > 0.0)
                    result[i][j] = (mat[i][j] + smoothing) / norm;
            }
        }
        return result;
    }

    /**
     * normalize a matrix by row using hyperparameters
     *
     * @param mat
     * @param hyperparameters: a matrix with the priors
     * @return normalized matrix
     */
    public static double[][] variationalNormalize(double[][] mat, double[][] hyperparameters)
    {
        double[][] result = new double[mat.length][mat[0].length];
        for (int i = 0; i < result.length; ++i) {
            double norm = 0.0;
            for (int j = 0; j < result[0].length; ++j) {
                norm += mat[i][j] + hyperparameters[i][j];
            }
            norm = Math.exp(digamma(norm));
            for (int j = 0; j < result[0].length; ++j) {
                if (norm > 0.0) {
                    result[i][j] = Math.exp(digamma((mat[i][j] + hyperparameters[i][j]))) / norm;
                }
            }
        }
        return result;
    }

    /**
     * normalize a matrix by row, in place
     */
    public static void normalizeInPlace(double[][] mat, double smoothing)
    {
        for (int i = 0; i < mat.length; ++i) {
            double norm = 0.0;
            for (int j = 0; j < mat[0].length; ++j) {
                norm += mat[i][j] + smoothing;
            }
            for (int j = 0; j < mat[0].length; ++j) {
                if (norm > 0.0)
                    mat[i][j] = (mat[i][j] + smoothing) / norm;
            }
        }
    }

    /**
     * read in a file with control items
     *
     * @throws IOException
     */
    public Map<Integer, Integer> readControls(String fileName)
            throws IOException
    {
        Map<Integer, Integer> controls = new HashMap<>();

        String line;
        try {
            FileReader fr1 = new FileReader(fileName);
            BufferedReader br1 = new BufferedReader(fr1);

            System.out.println("Reading controls file " + fileName);

            int lineNumber = 0;
            while ((line = br1.readLine()) != null) {
                line = line.trim();

                // record item
                if (!line.equals("")) {

                    // record value if not hashed yet
                    if (!string2Int.containsKey(line)) {
                        string2Int.put(line, hashCounter++);
                        int2String.add(line);
                    }

                    controls.put(lineNumber, string2Int.get(line));
                }

                lineNumber++;
            }

            return controls;

        }
        catch (IOException e) {
            throw new IOException(e.getMessage());
        }

    }

    /**
     * read CSV file and record data
     *
     * @throws IOException
     */
    public void readFileData(String fileName)
            throws IOException
    {
        readFileData(new BufferedReader(new FileReader(fileName)));
    }

    /**
     * read CSV file and record data
     *
     * @throws IOException
     */
    public void readFileData(BufferedReader bufferedReader)
            throws IOException
    {
        String line;

        System.out.println("Reading CSV file");

        int lineNumber = 0;
        while ((line = bufferedReader.readLine()) != null) {
            if (lineNumber > 0) {
                if (lineNumber % 5 == 0)
                    System.out.print(".");
                if (lineNumber % 100 == 0)
                    System.out.println(lineNumber);
            }

            List<Integer> annotatorsOnItem = new ArrayList<>();
            List<Integer> itemValues = new ArrayList<>();

            // split into items
            StringBuilder token = new StringBuilder("");
            int readPosition = 0;
            int annotatorNumber = 0;
            char[] charArray = line.toCharArray();
            for (char c : charArray) {
                // record values
                if (c != ',')
                    token.append(c);

                // record after a comma or at the end of the line
                if (c == ',' || readPosition + 1 == charArray.length) {

                    // last item can be comma or value, so check for both before you break
                    String item = token.toString();

                    // reset token
                    token = new StringBuilder("");

                    // record item
                    if (!item.equals("")) {

                        // record which annotator gave an answer
                        annotatorsOnItem.add(annotatorNumber);

                        // record value
                        if (!string2Int.containsKey(item)) {
                            string2Int.put(item, hashCounter++);
                            int2String.add(item);
                        }
                        itemValues.add(string2Int.get(item));
                    }

                }

                // advance reading position
                readPosition++;

                // separate annotators
                if (c == ',')
                    annotatorNumber++;

            }

            if (numAnnotators > 0 && annotatorNumber + 1 != this.numAnnotators) {
                throw new IOException("number of annotations in line " + (lineNumber + 1)
                        + " differs from previous line!");
            }
            this.numAnnotators = annotatorNumber + 1;

            // store as int[][]
            labels[lineNumber] = toIntArray(itemValues);
            whoLabeled[lineNumber] = toIntArray(annotatorsOnItem);

            lineNumber++;
        }// while there are lines left

        System.out.println(
                "\nstats:\n\t" + lineNumber + " instances,\n\t" + int2String.size() + " labels "
                        + int2String + ",\n\t" + numAnnotators + " annotators\n");

        //printIntArray(whoLabeled);

    }

    /**
     * print the arrays
     */
    public void printIntArray(int[][] someArray)
    {
        for (int i = 0; i < someArray.length; i++) {
            printIntArray(someArray[i]);
        }
    }

    /**
     * print the arrays
     */
    public void printIntArray(int[] someArray)
    {
        for (int i = 0; i < someArray.length; i++) {
            System.out.print(String.valueOf(someArray[i]) + " ");
        }
        System.out.println();
    }

    /**
     * turn an ArrayList into a primitive array
     *
     * @return int[]
     */
    public int[] toIntArray(List<Integer> list)
    {
        int[] ret = new int[list.size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = list.get(i);
        return ret;
    }

    /**
     * count the number of lines in a file (to initialize arrays)
     *
     * @throws IOException
     */
    public int fileLineCount(String filename)
            throws IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
        int counter = 0;
        while (bufferedReader.readLine() != null) {
            counter++;
        }

        return counter;

        //        old code
        //        InputStream inStream = new BufferedInputStream(new FileInputStream(filename));
        //        try {
        //            byte[] character = new byte[1024];
        //            int lineCount = 0;
        //            int readChars = 0;
        //            while ((readChars = inStream.read(character)) != -1) {
        //                for (int i = 0; i < readChars; ++i) {
        //                    if (character[i] == '\n')
        //                        ++lineCount;
        //                }
        //            }
        //            return lineCount;
        //        }
        //        finally {
        //            inStream.close();
        //        }
    }

    /**
     * write a double[] array to a file, using a delimiter between elements (usually either tab or newline)
     *
     * @throws IOException
     */
    public void writeArrayToFile(Object[] array, String fileName, String delimiter)
            throws IOException
    {
        PrintWriter pr = null;
        System.out.print("writing to file '" + fileName + "'...");
        pr = new PrintWriter(fileName);
        int i = 0;
        for (; array.length > 0 && i < array.length - 1; i++) {
            pr.print(array[i]);
            pr.print(delimiter);
        }

        pr.println(array[i]);
        pr.close();
        System.out.print("done\n");
    }

    /**
     * sort entropies, get value corresponding to n
     *
     * @return the entropy value at n% of the entropy values
     */

    public double getEntropyForThreshold(double threshold)
    {
        double result;
        int pivot;

        if (threshold == 0.0)
            pivot = 0;
        else if (threshold == 1.0)
            pivot = numInstances - 1;
        else
            pivot = (int) (numInstances * threshold);

        double[] entropyArray = getLabelEntropies();
        Arrays.sort(entropyArray);
        result = entropyArray[pivot];

        return result;
    }

    /**
     * evaluate the current model by comparing to a test file with gold labels
     *
     * @return the accuracy
     * @throws IOException
     */
    public double test(String testFile, String[] predictions)
            throws IOException
    {
        double accuracy = 0.0;
        double correct = 0.0;
        double total = 0.0;

        int numLinesInTest = fileLineCount(testFile);
        if (numLinesInTest != numInstances) {
            throw new IOException("Number of lines in test file does not match!");
        }

        String line;
        FileReader fr1 = new FileReader(testFile);
        BufferedReader br1 = new BufferedReader(fr1);

        System.out.println("Reading test file");

        int lineNumber = 0;
        while ((line = br1.readLine()) != null) {
            // only consider instances that were below the threshold
            if (!predictions[lineNumber].equals("")) {
                line = line.trim();

                total++;
                if (line.equals(predictions[lineNumber]))
                    correct++;
            }
            lineNumber++;
        }
        System.out.print("Coverage: " + total / numInstances + "\t");
        accuracy = correct / total;

        return accuracy;
    }

    //==========================================================================
    private static void doc()
    {
        System.out.println("MACE -- Multi-Annotator Confidence Estimation");
        System.out.println("============================================");
        MACE.getVersion();
        System.out.println("Authors: Taylor Berg-Kirkpatrick, Dirk Hovy, Ashish Vaswani");

        System.out.println("Usage:");
        System.out.println("Options:");
        System.out.println("=========");
        System.out.println(
                "\t--controls <FILE>:\tsupply a file with annotated control items. Each line corresponds to one item,\n"
                        + "\t\t\t\tso the number of lines MUST match the input CSV file.\n"
                        + "\t\t\t\tThe control items serve as semi-supervised input. Controls usually improve accuracy.\n");
        System.out.println(
                "\t--alpha <FLOAT>:\tfirst hyper-parameter of beta prior that controls whether an annotator knows or guesses. Default:"
                        + MACE.DEFAULT_ALPHA + "\n");
        System.out.println(
                "\t--beta <FLOAT>:\t\tsecond hyper-parameter of beta prior that controls whether an annotator knows or guesses. Default:"
                        + MACE.DEFAULT_BETA + "\n");
        System.out.println(
                "\t--entropies:\t\twrite the entropy of each instance to a separate file '[prefix.]entropy'\n");
        System.out.println("\t--help:\t\t\tdisplay this information\n");
        System.out.println(
                "\t--iterations <1-1000>:\tnumber of iterations for each EM start. Default: "
                        + MACE.DEFAULT_ITERATIONS + "\n");
        System.out.println("\t--prefix <STRING>:\tprefix used for output files.\n");
        System.out.println("\t--restarts <1-1000>:\tnumber of random restarts to perform. Default: "
                + MACE.DEFAULT_RR + "\n");
        System.out.println(
                "\t--smoothing <0.0-1.0>:\tsmoothing added to fractional counts before normalization.\n"
                        + "\t\t\t\tHigher values mean smaller changes. Default: 0.01/|values|\n");
        System.out.println(
                "\t--test <FILE>:\t\tsupply a test file. Each line corresponds to one item in the CSV file,\n"
                        + "\t\t\t\tso the number of lines must match. If a test file is supplied,\n"
                        + "\t\t\t\tMACE outputs the accuracy of the predictions\n");
        System.out.println(
                "\t--threshold <0.0-1.0>:\tonly predict the label for instances whose entropy is among the top n%, ignore others.\n"
                        + "\t\t\t\tThus '--threshold 0.0' will ignore all instances, '--threshold 1.0' includes all.\n"
                        + "\t\t\t\tThis improves accuracy at the expense of coverage. Default: 1.0\n");

        System.out.println();
        System.out.println("To cite MACE in publications, please refer to:");
        System.out.println(
                "Dirk Hovy, Taylor Berg-Kirkpatrick, Ashish Vaswani and Eduard Hovy (2013): Learning Whom to Trust With MACE. In: Proceedings");

        System.out.println();
        System.out.println(
                "This is research software that is not actively maintained. If you have any questions, please write to <dirkh@isi.edu>");

        System.exit(0);
    }

    private static void getVersion()
    {
        System.out.println("Version: " + MACE.VERSION + "\n");
    }

    public static void main(String[] args)
    {
        MACE em;
        try {
            if (args.length == 0 || args[0].equals("--help")) {
                MACE.doc();
            }
            if (args[0].equals("--version")) {
                MACE.getVersion();
                System.exit(0);
            }

            int numberOfArgs = args.length;
            String file = args[numberOfArgs - 1];
            em = new MACE(file);

            // default settings
            int iterations = MACE.DEFAULT_ITERATIONS;
            int restarts = MACE.DEFAULT_RR;
            double smoothing = 0.01 / (double) em.numLabels;
            double threshold = 1.0;
            String test = null;
            String controls = null;
            String prefix = null;
            boolean entropies = false;
            boolean variational = false;
            double alpha = MACE.DEFAULT_ALPHA;
            double beta = MACE.DEFAULT_BETA;

            String outputPredictions = null;
            String outputCompetence = null;

            // process all but last arg (which is the CSV file)
            for (int i = 0; i < numberOfArgs - 1; i++) {
                String arg = args[i];

                if (arg.equals("--smoothing")) {
                    smoothing = Double.valueOf(args[++i]);
                    if (smoothing < 0.0)
                        throw new IllegalArgumentException("smoothing less than 0.0");
                }

                else if (arg.equals("--threshold")) {
                    threshold = Double.valueOf(args[++i]);
                    if (threshold < 0.0 || threshold > 1.0)
                        throw new IllegalArgumentException("threshold not between 0.0 and 1.0");
                }

                else if (arg.equals("--restarts")) {
                    restarts = Integer.valueOf(args[++i]);
                    if (restarts < 1 || restarts > 1000)
                        throw new IllegalArgumentException("restarts not between 1 and 1000");
                }

                else if (arg.equals("--iterations")) {
                    iterations = Integer.valueOf(args[++i]);
                    if (iterations < 1 || iterations > 1000)
                        throw new IllegalArgumentException("iterations not between 1 and 1000");
                }

                else if (arg.equals("--prefix")) {
                    prefix = args[++i];
                }

                else if (arg.equals("--entropies")) {
                    entropies = true;
                }

                else if (arg.equals("--controls")) {
                    controls = args[++i];
                }

                else if (arg.equals("--test")) {
                    test = args[++i];
                }

                else if (arg.equals("--help")) {
                    MACE.doc();
                }

                else if (arg.equals("--version")) {
                    MACE.getVersion();
                }

                else if (arg.equals("--alpha")) {
                    alpha = Double.valueOf(args[++i]);
                    variational = true;
                }
                else if (arg.equals("--beta")) {
                    beta = Double.valueOf(args[++i]);
                    variational = true;
                }
                else if (arg.equals("--outputPredictions")) {
                    outputPredictions = args[++i];
                }
                else if (arg.equals("--outputCompetence")) {
                    outputCompetence = args[++i];
                }
                else {
                    throw new IllegalArgumentException("argument '" + arg + "' not recognized");
                }
            }

            // run with configuration
            em.run(iterations, smoothing, restarts, alpha, beta, variational, controls);

            // write results to files
            // generate predictions
            String[] predictions = em.decode(threshold);
            String predictionName = prefix == null ? "prediction" : prefix + ".prediction";
            if (outputPredictions != null) {
                predictionName = outputPredictions;
            }
            em.writeArrayToFile(predictions, predictionName, "\n");

            // generate competence scores
            Object[] competence = new Object[em.numAnnotators];
            for (int i = 0; i < em.numAnnotators; i++) {
                competence[i] = em.thetas[i][1];
            }
            String competenceName = prefix == null ? "competence" : prefix + ".competence";
            if (outputCompetence != null) {
                competenceName = outputCompetence;
            }
            em.writeArrayToFile(competence, competenceName, "\t");

            // generate entropies
            if (entropies) {
                Object[] entropy = new Object[em.numInstances];
                double[] entropyArray = em.getLabelEntropies();
                for (int i = 0; i < em.numAnnotators; i++)
                    entropy[i] = entropyArray[i];
                String entropyName = prefix == null ? "entropies" : prefix + ".entropies";
                em.writeArrayToFile(entropy, entropyName, "\n");
            }

            if (test != null) {
                System.out.println("Accuracy on test set: " + em.test(test, predictions));
            }

        }
        catch (IOException e) {
            System.out
                    .println("\n*****************************************************************");
            System.out.println("\tFILE ERROR:");
            System.out.println("\t" + e.getMessage());
            System.out.println("*****************************************************************");
        }
        catch (IllegalArgumentException e) {
            System.out
                    .println("\n*****************************************************************");
            System.out.println("\tARGUMENT ERROR:");
            System.out.println("\t" + e.getMessage());
            System.out.println("*****************************************************************");
            MACE.doc();
        }
    }

    /**
     * Returns the value of the digamma function for the specified
     * value.  The returned values are accurate to at least 13
     * decimal places.
     * <p>
     * <p>The digamma function is the derivative of the log of the
     * gamma function;
     * <p>
     * <blockquote><pre>
     * &Psi;(z)
     * = <i>d</i> log &Gamma;(z) / <i>d</i>z
     * = &Gamma;'(z) / &Gamma;(z)
     * </pre></blockquote>
     * <p>
     * <p>The numerical approximation is derived from:
     * <p>
     * <ul>
     * <li>Richard J. Mathar. 2005.
     * <a href="http://arxiv.org/abs/math.CA/0403344">Chebyshev Series Expansion of Inverse Polynomials</a>.
     * <li>
     * <li>Richard J. Mathar. 2005.
     * <a href="http://www.strw.leidenuniv.nl/~mathar/progs/digamma.c">digamma.c</a>.
     * (C Program implementing algorithm.)
     * </li>
     * </ul>
     * <p>
     * <i>Implementation Note:</i> The recursive calls in the C
     * implementation have been transformed into loops and
     * accumulators, and the recursion for values greater than three
     * replaced with a simpler reduction.  The number of loops
     * required before the fixed length expansion is approximately
     * integer value of the absolute value of the input.  Each loop
     * requires a floating point division, two additions and a local
     * variable assignment.  The fixed portion of the algorithm is
     * roughly 30 steps requiring four multiplications, three
     * additions, one static final array lookup, and four assignments per
     * loop iteration.
     *
     * @param x Value at which to evaluate the digamma function.
     * @return The value of the digamma function at the specified
     * value.
     */
    public static double digamma(double x)
    {
        if (x <= 0.0 && (x == (double) ((long) x)))
            return Double.NaN;

        double accum = 0.0;
        if (x < 0.0) {
            accum += java.lang.Math.PI / java.lang.Math.tan(java.lang.Math.PI * (1.0 - x));
            x = 1.0 - x;
        }

        if (x < 1.0) {
            while (x < 1.0)
                accum -= 1.0 / x++;
        }

        if (x == 1.0)
            return accum - NEGATIVE_DIGAMMA_1;

        if (x == 2.0)
            return accum + 1.0 - NEGATIVE_DIGAMMA_1;

        if (x == 3.0)
            return accum + 1.5 - NEGATIVE_DIGAMMA_1;

        // simpler recursion than Mahar to reduce recursion
        if (x > 3.0) {
            while (x > 3.0)
                accum += 1.0 / --x;
            return accum + digamma(x);
        }

        x -= 2.0;
        double tNMinus1 = 1.0;
        double tN = x;
        double digamma = DIGAMMA_COEFFS[0] + DIGAMMA_COEFFS[1] * tN;
        for (int n = 2; n < DIGAMMA_COEFFS.length; n++) {
            double tN1 = 2.0 * x * tN - tNMinus1;
            digamma += DIGAMMA_COEFFS[n] * tN1;
            tNMinus1 = tN;
            tN = tN1;
        }
        return accum + digamma;
    }

    private static final double DIGAMMA_COEFFS[] = { .30459198558715155634315638246624251,
            .72037977439182833573548891941219706, -.12454959243861367729528855995001087,
            .27769457331927827002810119567456810e-1, -.67762371439822456447373550186163070e-2,
            .17238755142247705209823876688592170e-2, -.44817699064252933515310345718960928e-3,
            .11793660000155572716272710617753373e-3, -.31253894280980134452125172274246963e-4,
            .83173997012173283398932708991137488e-5, -.22191427643780045431149221890172210e-5,
            .59302266729329346291029599913617915e-6, -.15863051191470655433559920279603632e-6,
            .42459203983193603241777510648681429e-7, -.11369129616951114238848106591780146e-7,
            .304502217295931698401459168423403510e-8, -.81568455080753152802915013641723686e-9,
            .21852324749975455125936715817306383e-9, -.58546491441689515680751900276454407e-10,
            .15686348450871204869813586459513648e-10, -.42029496273143231373796179302482033e-11,
            .11261435719264907097227520956710754e-11, -.30174353636860279765375177200637590e-12,
            .80850955256389526647406571868193768e-13, -.21663779809421233144009565199997351e-13,
            .58047634271339391495076374966835526e-14, -.15553767189204733561108869588173845e-14,
            .41676108598040807753707828039353330e-15, -.11167065064221317094734023242188463e-15 };
    /**
     * The &gamma; constant for computing the digamma function.
     * <p>
     * <p>The value is defined as the negative of the digamma funtion
     * evaluated at 1:
     * <p>
     * <blockquote><pre>
     * &gamma; = - &Psi;(1)
     */
    static double NEGATIVE_DIGAMMA_1 = 0.5772156649015328606065120900824024;

}


