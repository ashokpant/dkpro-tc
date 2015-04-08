/*
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.dkpro.tc.svmhmm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualTreeBidiMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.ml.TCMachineLearningAdapter;
import de.tudarmstadt.ukp.dkpro.tc.svmhmm.SVMHMMAdapter;
import de.tudarmstadt.ukp.dkpro.tc.svmhmm.writer.SVMHMMDataWriter;

/**
 * @author Ivan Habernal
 */
public final class SVMHMMUtils
{
    /**
     * File name of serialized mapping from String labels to numbers
     */
    public static final String LABELS_TO_INTEGERS_MAPPING_FILE_NAME =
            "labelsToIntegersMapping_DualTreeBidiMap.bin";

    /**
     * CSV file comment
     */
    public static final String CSV_COMMENT = "Columns: gold, predicted, token, seqID";

    /**
     * Format of CSV files
     */
    public static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withCommentMarker('#');

    /**
     * Where the gold outcomes, predicted outcomes, and tokens are stored
     */
    public static final String GOLD_PREDICTED_OUTCOMES_CSV = "outcomesGoldPredicted.csv";

    private SVMHMMUtils()
    {
        // empty
    }

    /**
     * Extract all outcomes from featureVectorsFiles (training, test) that are in
     * LIBSVM format - each line is a feature vector and the first token is the outcome
     * label
     *
     * @param files files in LIBSVM format
     * @return set of all unique outcomes
     * @throws java.io.IOException
     */
    public static SortedSet<String> extractOutcomeLabelsFromFeatureVectorFiles(File... files)
            throws IOException
    {
        SortedSet<String> result = new TreeSet<>();

        for (File file : files) {
            result.addAll(extractOutcomeLabels(file));
        }

        return result;
    }

    /**
     * Maps names to numbers (numbers are required by SVMLight format)
     *
     * @param names names (e.g., features, outcomes)
     * @return bidirectional map of name:number
     */
    public static BidiMap mapVocabularyToIntegers(SortedSet<String> names)
    {
        BidiMap result = new DualTreeBidiMap();

        // start numbering from 1
        int index = 1;
        for (String featureName : names) {
            result.put(featureName, index);
            index++;
        }

        return result;
    }

    /**
     * Creates a new file in the same directory as {@code featureVectorsFile} and replaces the first
     * token (outcome label) by its corresponding integer number from the bi-di map
     *
     * @param featureVectorsFile file
     * @param labelsToIntegers   mapping
     * @return new file
     */
    public static File replaceLabelsWithIntegers(File featureVectorsFile, BidiMap labelsToIntegers)
            throws IOException
    {
        File result = new File(featureVectorsFile.getParent(),
                "mappedLabelsToInt_" + featureVectorsFile.getName());
        PrintWriter pw = new PrintWriter(new FileOutputStream(result));

        for (String line : FileUtils.readLines(featureVectorsFile)) {
            // split on the first whitespaces, keep the rest
            String[] split = line.split("\\s", 2);
            String label = split[0];
            String remainingContent = split[1];

            // find the integer
            Integer intOutput = (Integer) labelsToIntegers.get(label);

            // print to the output stream
            pw.printf("%d %s%n", intOutput, remainingContent);
        }

        IOUtils.closeQuietly(pw);

        return result;
    }

    /**
     * Saves label-integer mapping to a file
     *
     * @param mapping    mapping
     * @param outputFile file
     * @throws IOException
     */
    public static void saveMapping(BidiMap mapping, File outputFile)
            throws IOException
    {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                new FileOutputStream(outputFile));
        objectOutputStream.writeObject(mapping);

        IOUtils.closeQuietly(objectOutputStream);
    }

    /**
     * Saves the feature mapping to readable format, each line is a feature id and feature name,
     * sorted by feature id
     *
     * @param mapping    mapping (name:id)
     * @param outputFile output file
     * @throws IOException
     */
    public static void saveMappingTextFormat(BidiMap mapping, File outputFile)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(new FileOutputStream(outputFile));

        // sort values (feature indexes)
        SortedSet<Object> featureIndexes = new TreeSet<Object>(mapping.values());

        for (Object featureIndex : featureIndexes) {
            pw.printf(Locale.ENGLISH, "%5d %s%n", (int) featureIndex,
                    mapping.getKey(featureIndex).toString());
        }

        IOUtils.closeQuietly(pw);
    }

    /**
     * Loads a serialized BidiMap from file
     *
     * @param inputFile input file
     * @return BidiMap
     * @throws IOException
     */
    public static BidiMap loadMapping(File inputFile)
            throws IOException
    {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(inputFile));

        try {
            return (BidiMap) inputStream.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Extracts the outcome labels from the file; it corresponds to the first token
     * on each line.
     *
     * @param featureVectorsFile featureVectors file
     * @return list of outcome labels
     * @throws IOException
     */
    public static List<String> extractOutcomeLabels(File featureVectorsFile)
            throws IOException
    {
        List<String> result = new ArrayList<>();
        List<String> lines = FileUtils.readLines(featureVectorsFile);
        for (String line : lines) {
            String label = line.split("\\s")[0];

            result.add(label);
        }
        return result;
    }

    /**
     * Reads the featureVectorsFile and splits comment on each line into a list of strings, i.e.
     * "TAG qid:4 1:1 2:1 4:2 # token TAG 4" produces "token", "TAG", "4"
     *
     * @param featureVectorsFileStream featureVectors file stream
     * @return list (for each line) of list of comment parts
     * @throws IOException
     */
    protected static List<List<String>> extractComments(InputStream featureVectorsFileStream
            //            int expectedFieldsCount
    )
            throws IOException, IllegalArgumentException
    {
        List<List<String>> result = new ArrayList<>();

        List<String> lines = IOUtils.readLines(featureVectorsFileStream);
        IOUtils.closeQuietly(featureVectorsFileStream);
        for (String line : lines) {
            String comment = line.split("#", 2)[1];

            List<String> list = new ArrayList<>();

            String[] tokens = comment.split("\\s+");
            // filter empty tokens
            for (String token : tokens) {
                String trim = token.trim();
                if (!trim.isEmpty()) {
                    // decode from URL representation
                    String s = URLDecoder.decode(trim, "utf-8");
                    list.add(s);
                }
            }

            result.add(list);
        }
        return result;
    }

    /**
     * Extracts original tokens that are stored in the comment part of the featureVectorsFile
     *
     * @param featureVectorsFile featureVectors file
     * @return list of original tokens
     * @throws IOException
     */
    public static List<String> extractOriginalTokens(File featureVectorsFile)
            throws IOException
    {
        List<String> result = new ArrayList<>();

        List<List<String>> comments = extractComments(new FileInputStream(featureVectorsFile));

        for (List<String> comment : comments) {
            // original token is the first one in comments
            result.add(comment.get(2));
        }
        return result;
    }

    /**
     * Reads the prediction file (each line is a integer) and converts them into original outcome
     * labels using the mapping provided by the bi-directional map
     *
     * @param predictionsFile         predictions from classifier
     * @param labelsToIntegersMapping mapping outcomeLabel:integer
     * @return list of outcome labels
     * @throws IOException
     */
    public static List<String> extractOutcomeLabelsFromPredictions(File predictionsFile,
            BidiMap labelsToIntegersMapping)
            throws IOException
    {
        List<String> result = new ArrayList<>();

        for (String line : FileUtils.readLines(predictionsFile)) {
            Integer intLabel = Integer.valueOf(line);

            String outcomeLabel = (String) labelsToIntegersMapping.getKey(intLabel);

            result.add(outcomeLabel);
        }

        return result;
    }

    /**
     * Returns a list of original sequence IDs extracted from comments
     *
     * @param featureVectorsFile featureVectors file
     * @return list of integers
     * @throws IOException
     */
    public static List<Integer> extractOriginalSequenceIDs(File featureVectorsFile)
            throws IOException
    {
        List<Integer> result = new ArrayList<>();

        List<List<String>> comments = extractComments(new FileInputStream(featureVectorsFile));

        for (List<String> comment : comments) {
            // sequence number is the third token in the comment token
            result.add(Integer.valueOf(comment.get(1)));
        }

        return result;
    }

    /**
     * Given confusion matrix, it writes it in CSV and LaTeX form to the tasks output directory,
     * and also prints evaluations (F-measure, Precision, Recall)
     *
     * @param context         task context
     * @param confusionMatrix confusion matrix
     * @throws java.io.IOException
     */
    public static void writeOutputResults(TaskContext context, ConfusionMatrix confusionMatrix)
            throws IOException
    {
        writeOutputResults(context, confusionMatrix, null);
    }

    /**
     * Given confusion matrix, it writes it in CSV and LaTeX form to the tasks output directory,
     * and also prints evaluations (F-measure, Precision, Recall)
     *
     * @param context         task context
     * @param confusionMatrix confusion matrix
     * @param filePrefix      prefix of output files
     * @throws java.io.IOException
     */
    public static void writeOutputResults(TaskContext context, ConfusionMatrix confusionMatrix,
            String filePrefix)
            throws IOException
    {
        // storing the results as latex confusion matrix
        String confMatrixFileTex = (filePrefix != null ? filePrefix : "") + "confusionMatrix.tex";
        File evaluationFileLaTeX = new File(context.getStorageLocation(
                Constants.TEST_TASK_OUTPUT_KEY,
                StorageService.AccessMode.READWRITE), confMatrixFileTex);
        FileUtils.writeStringToFile(evaluationFileLaTeX, confusionMatrix.toStringLatex());

        // as CSV confusion matrix
        String confMatrixFileCsv = (filePrefix != null ? filePrefix : "") + "confusionMatrix.csv";
        File evaluationFileCSV = new File(context.getStorageLocation(Constants.TEST_TASK_OUTPUT_KEY,
                StorageService.AccessMode.READWRITE), confMatrixFileCsv);

        CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(evaluationFileCSV),
                CSVFormat.DEFAULT);
        csvPrinter.printRecords(confusionMatrix.toStringMatrix());
        IOUtils.closeQuietly(csvPrinter);

        // and results
        File evaluationFile = new File(context.getStorageLocation(Constants.TEST_TASK_OUTPUT_KEY,
                StorageService.AccessMode.READWRITE), new SVMHMMAdapter().getFrameworkFilename(
                TCMachineLearningAdapter.AdapterNameEntries.evaluationFile));

        PrintWriter pw = new PrintWriter(evaluationFile);
        pw.println(confusionMatrix.printNiceResults());
        pw.println(confusionMatrix.printLabelPrecRecFm());
        pw.println(confusionMatrix.printClassDistributionGold());
        IOUtils.closeQuietly(pw);
    }

    public static List<SortedMap<String, String>> extractMetaDataFeatures(File featureVectorsFile)
            throws IOException
    {
        InputStream inputStream = new FileInputStream(featureVectorsFile);

        List<SortedMap<String, String>> result = new ArrayList<>();

        List<List<String>> allComments = extractComments(inputStream);
        for (List<String> instanceComments : allComments) {
            SortedMap<String, String> instanceResult = new TreeMap<>();

            for (String comment : instanceComments) {
                if (comment.startsWith(SVMHMMDataWriter.META_DATA_FEATURE_PREFIX)) {
                    String[] split = comment.split(":");
                    String key = split[0];
                    String value = split[1];

                    instanceResult.put(key, value);
                }
            }

            result.add(instanceResult);
        }

        IOUtils.closeQuietly(inputStream);

        return result;
    }
}
