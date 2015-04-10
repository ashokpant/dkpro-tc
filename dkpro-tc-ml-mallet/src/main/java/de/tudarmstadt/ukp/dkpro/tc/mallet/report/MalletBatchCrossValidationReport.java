/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.tc.mallet.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.summary.Sum;

import de.tudarmstadt.ukp.dkpro.lab.reporting.BatchReportBase;
import de.tudarmstadt.ukp.dkpro.lab.reporting.FlexTable;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.util.ReportUtils;

/**
 * Collects the final evaluation results in a cross validation setting.
 * 
 * @deprecated As of release 0.7.0, only dkpro-tc-ml-crfsuite is supported
 * @author zesch
 * @author daxenberger
 * 
 */
public class MalletBatchCrossValidationReport
    extends BatchReportBase
    implements Constants
{

    private static final List<String> discriminatorsToExclude = Arrays.asList(new String[] {
            "files_validation", "files_training" });
    private static final List<String> nonAveragedResultsMeasures = Arrays.asList(new String[] {
            MalletReportConstants.CORRECT, MalletReportConstants.INCORRECT, MalletReportConstants.NUMBER_EXAMPLES });

    @Override
    public void execute()
        throws Exception
    {
        StorageService store = getContext().getStorageService();

        FlexTable<String> table = FlexTable.forClass(String.class);

        Map<String, List<Double>> key2resultValues = new HashMap<String, List<Double>>();

        for (TaskContextMetadata subcontext : getSubtasks()) {
            String name = BatchTask.class.getSimpleName() + "CrossValidation";
            // one CV batch (which internally ran numFolds times)
            if (subcontext.getLabel().startsWith(name)) {
                Map<String, String> discriminatorsMap = store.retrieveBinary(subcontext.getId(),
                        Task.DISCRIMINATORS_KEY, new PropertiesAdapter()).getMap();

                File eval = store.getStorageFolder(subcontext.getId(), EVAL_FILE_NAME + SUFFIX_CSV);

                Map<String, String> resultMap = new HashMap<String, String>();

                String[][] evalMatrix = null;

                int i = 0;
                for (String line : FileUtils.readLines(eval)) {
                    String[] tokenizedLine = StrTokenizer.getCSVInstance(line).getTokenArray();
                    if (evalMatrix == null) {
                        evalMatrix = new String[FileUtils.readLines(eval).size()][tokenizedLine.length];
                    }
                    evalMatrix[i] = tokenizedLine;
                    i++;
                }

                // columns
                for (int j = 0; j < evalMatrix[0].length; j++) {
                    String header = evalMatrix[0][j];
                    String[] vals = new String[evalMatrix.length - 1];
                    // rows
                    for (int k = 1; k < evalMatrix.length; k++) {
                        if (evalMatrix[k][j].equals("null")) {
                            vals[k - 1] = String.valueOf(0.);
                        }
                        else {
                            vals[k - 1] = evalMatrix[k][j];
                        }
                    }
                    Mean mean = new Mean();
                    Sum sum = new Sum();
                    StandardDeviation std = new StandardDeviation();

                    double[] dVals = new double[vals.length];
                    Set<String> sVals = new HashSet<String>();
                    for (int k = 0; k < vals.length; k++) {
                        try {
                            dVals[k] = Double.parseDouble(vals[k]);
                            sVals = null;
                        }
                        catch (NumberFormatException e) {
                            dVals = null;
                            sVals.add(vals[k]);
                        }
                    }

                    if (dVals != null) {
                        if (nonAveragedResultsMeasures.contains(header)) {
                            resultMap.put(header, String.valueOf(sum.evaluate(dVals)));
                        }
                        else {
                            resultMap.put(
                                    header,
                                    String.valueOf(mean.evaluate(dVals)) + "\u00B1"
                                            + String.valueOf(std.evaluate(dVals)));
                        }
                    }
                    else {
                        if (sVals.size() > 1) {
                            resultMap.put(header, "---");
                        }
                        else {
                            resultMap.put(header, vals[0]);
                        }
                    }
                }

                String key = getKey(discriminatorsMap);

                List<Double> results;
                if (key2resultValues.get(key) == null) {
                    results = new ArrayList<Double>();
                }
                else {
                    results = key2resultValues.get(key);

                }
                key2resultValues.put(key, results);

                Map<String, String> values = new HashMap<String, String>();
                Map<String, String> cleanedDiscriminatorsMap = new HashMap<String, String>();

                for (String disc : discriminatorsMap.keySet()) {
                    if (!ReportUtils.containsExcludePattern(disc, discriminatorsToExclude)) {
                        cleanedDiscriminatorsMap.put(disc, discriminatorsMap.get(disc));
                    }
                }
                values.putAll(cleanedDiscriminatorsMap);
                values.putAll(resultMap);

                table.addRow(subcontext.getLabel(), values);
            }
        }

        getContext().getLoggingService().message(getContextLabel(),
                ReportUtils.getPerformanceOverview(table));

        // Excel cannot cope with more than 255 columns
        if (table.getColumnIds().length <= 255) {
            getContext()
                    .storeBinary(EVAL_FILE_NAME + "_compact" + SUFFIX_EXCEL, table.getExcelWriter());
        }
        getContext().storeBinary(EVAL_FILE_NAME + "_compact" + SUFFIX_CSV, table.getCsvWriter());

        table.setCompact(false);
        // Excel cannot cope with more than 255 columns
        if (table.getColumnIds().length <= 255) {
            getContext().storeBinary(EVAL_FILE_NAME + SUFFIX_EXCEL, table.getExcelWriter());
        }
        getContext().storeBinary(EVAL_FILE_NAME + SUFFIX_CSV, table.getCsvWriter());

        // output the location of the batch evaluation folder
        // otherwise it might be hard for novice users to locate this
        File dummyFolder = store.getStorageFolder(getContext().getId(), "dummy");
        // TODO can we also do this without creating and deleting the dummy folder?
        getContext().getLoggingService().message(getContextLabel(),
                "Storing detailed results in:\n" + dummyFolder.getParent() + "\n");
        dummyFolder.delete();
    }

    private String getKey(Map<String, String> discriminatorsMap)
    {
        Set<String> sortedDiscriminators = new TreeSet<String>(discriminatorsMap.keySet());

        List<String> values = new ArrayList<String>();
        for (String discriminator : sortedDiscriminators) {
            values.add(discriminatorsMap.get(discriminator));
        }
        return StringUtils.join(values, "_");
    }
}
