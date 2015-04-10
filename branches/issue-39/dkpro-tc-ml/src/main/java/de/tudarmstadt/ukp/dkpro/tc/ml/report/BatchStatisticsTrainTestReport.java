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
package de.tudarmstadt.ukp.dkpro.tc.ml.report;


import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;
import de.tudarmstadt.ukp.dkpro.lab.reporting.BatchReportBase;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.StringAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;
import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.util.ReportConstants;
import de.tudarmstadt.ukp.dkpro.tc.evaluation.evaluator.EvaluatorBase;
import de.tudarmstadt.ukp.dkpro.tc.evaluation.evaluator.EvaluatorFactory;
import de.tudarmstadt.ukp.dkpro.tc.ml.report.util.PrettyPrintUtils;


/**
 * Collects statistical evaluation results from TestTasks. Can be run on BatchTask level in
 * TrainTest setups, or on CV BatchTask level in CV setups.
 * 
 * @author Johannes Daxenberger
 * 
 */
public class BatchStatisticsTrainTestReport
    extends BatchReportBase
    implements Constants, ReportConstants
{

    @Override
    public void execute()
        throws Exception
    {
        StringWriter sWriter = new StringWriter();
        CSVWriter csv = new CSVWriter(sWriter, ';');

        HashSet<String> variableClassifier = new HashSet<String>();
        HashSet<String> variableFeature = new HashSet<String>();

        boolean experimentHasBaseline = false;

        for (TaskContextMetadata subcontext : getSubtasks()) {
        	// FIXME this is a bad hack
            if (subcontext.getType().contains("TestTask")) {
  

                Map<String, String> discriminatorsMap = getContext().getStorageService().retrieveBinary(subcontext.getId(),
                        Task.DISCRIMINATORS_KEY, new PropertiesAdapter()).getMap();
                File id2outcomeFile = getContext().getStorageService().getStorageFolder(subcontext.getId(), ID_OUTCOME_KEY);
                String mode = getDiscriminatorValue(discriminatorsMap, DIM_LEARNING_MODE);

                                
                String blCl = getDiscriminatorValue(discriminatorsMap, DIM_BASELINE_CLASSIFICATION_ARGS);
                String blFs = getDiscriminatorValue(discriminatorsMap, DIM_BASELINE_FEATURE_SET);
                String blPp = getDiscriminatorValue(discriminatorsMap, DIM_BASELINE_PIPELINE_PARAMS);
                
                String trainFiles;
                String testFiles;
                
                // CV
                if(!getDiscriminatorValue(discriminatorsMap, "files_training").equals("null") &&
                        !getDiscriminatorValue(discriminatorsMap, "files_validation").equals("null")){
                     trainFiles = String.valueOf(getDiscriminatorValue(discriminatorsMap, "files_training").hashCode());
                     testFiles = String.valueOf(getDiscriminatorValue(discriminatorsMap, "files_validation").hashCode());
                }
                // TrainTest
                else{
                    trainFiles = String.valueOf(getDiscriminatorValue(discriminatorsMap, DIM_READER_TRAIN_PARAMS).hashCode());
                    testFiles = String.valueOf(getDiscriminatorValue(discriminatorsMap, DIM_READER_TEST_PARAMS).hashCode());
                }
                
                String experimentName = subcontext.getType().split("\\-")[1];
                String train = experimentName + "." + trainFiles;
                String test = experimentName + "." + testFiles;
                
                String cl = getDiscriminatorValue(discriminatorsMap, DIM_CLASSIFICATION_ARGS);
                String fs = getDiscriminatorValue(discriminatorsMap, DIM_FEATURE_SET); 
        		String pp = getDiscriminatorValue(discriminatorsMap, DIM_PIPELINE_PARAMS);
        		
        		int isBaseline = 0;
                if (blCl.equals(cl) && blFs.equals(fs) && blPp.equals(pp)) {
                    isBaseline = 1;
                    experimentHasBaseline = true;
                }
                
                EvaluatorBase evaluator = EvaluatorFactory.createEvaluator(id2outcomeFile,
                        mode, true, false);
                Map<String, Double> resultMap = evaluator.calculateMicroEvaluationMeasures();

                for (String mString : resultMap.keySet()) {
                    String mName = mString;
                    String mValue = String.valueOf(resultMap.get(mString));
                    String clShort = PrettyPrintUtils.prettyPrintClassifier(cl);
                    String fsShort = PrettyPrintUtils.prettyPrintFeatureSet(fs, true);
                    String ppShort = PrettyPrintUtils.prettyPrintFeatureArgs(pp);
                    String fAllShort = fsShort + ", " + ppShort;
                    // expected format: Train;Test;Classifier;FeatureSet;Measure;Value;IsBaseline
                    csv.writeNext(Arrays.asList(train, test, clShort, fAllShort, mName, mValue,
                            String.valueOf(isBaseline)).toArray(new String[] {}));
                    variableClassifier.add(clShort);
                    variableFeature.add(fAllShort);
                }
            }
        }
        String s = sWriter.toString();
        csv.close();
        if (variableClassifier.size() > 1 && variableFeature.size() > 1 && experimentHasBaseline) {
            throw new TextClassificationException("If you configure a baseline, you may test either only one classifier (arguments) or one feature set (arguments).");
        }

        getContext().storeBinary(STATISTICS_REPORT_FILENAME, new StringAdapter(s));
    }
    
    private String getDiscriminatorValue(Map<String, String> discriminatorsMap, String discriminatorName)
        throws TextClassificationException
    {
    	for (String key : discriminatorsMap.keySet()) {
			if(key.split("\\|")[1].equals(discriminatorName)){
				return discriminatorsMap.get(key);
			}
		}
    	throw new TextClassificationException(discriminatorName + " not found in discriminators set.");
    }
}

