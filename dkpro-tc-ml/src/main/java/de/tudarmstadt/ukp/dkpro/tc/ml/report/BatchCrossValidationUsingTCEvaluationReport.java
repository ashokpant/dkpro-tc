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
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import de.tudarmstadt.ukp.dkpro.lab.reporting.BatchReportBase;
import de.tudarmstadt.ukp.dkpro.lab.reporting.FlexTable;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.util.ReportUtils;
import de.tudarmstadt.ukp.dkpro.tc.evaluation.Id2Outcome;
import de.tudarmstadt.ukp.dkpro.tc.evaluation.evaluator.EvaluatorBase;
import de.tudarmstadt.ukp.dkpro.tc.evaluation.evaluator.EvaluatorFactory;
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentCrossValidation;

/**
 * Collects the final evaluation results in a cross validation setting.
 * 
 * @author zesch
 * @author daxenberger
 * 
 */
public class BatchCrossValidationUsingTCEvaluationReport
    extends BatchReportBase
    implements Constants
{
  	boolean softEvaluation = true;
	boolean individualLabelMeasures = false;


    @Override
    public void execute()
        throws Exception
    {
     	
        StorageService store = getContext().getStorageService();

        FlexTable<String> table = FlexTable.forClass(String.class);

        for (TaskContextMetadata subcontext : getSubtasks()) {
            // FIXME this is a hack
            String name = ExperimentCrossValidation.class.getSimpleName();
            // one CV batch (which internally ran numFolds times)
            if (subcontext.getLabel().startsWith(name)) {
                Map<String, String> discriminatorsMap = store.retrieveBinary(subcontext.getId(), Constants.DISCRIMINATORS_KEY_TEMP, new PropertiesAdapter()).getMap();
                
                File fileToEvaluate = store.getStorageFolder(subcontext.getId(), 
                		Constants.TEST_TASK_OUTPUT_KEY + "/" + Constants.SERIALIZED_ID_OUTCOME_KEY);
                
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(fileToEvaluate));
                Id2Outcome id2Outcome = (Id2Outcome) inputStream.readObject();
                inputStream.close();
                
                EvaluatorBase evaluator = EvaluatorFactory.createEvaluator(id2Outcome, softEvaluation, individualLabelMeasures);
                Map<String, Double> resultTempMap = evaluator.calculateEvaluationMeasures();
                Map<String, String> resultMap = new HashMap<String, String>();
                for (String key : resultTempMap.keySet()) {
                	Double value = resultTempMap.get(key);
					resultMap.put(key, String.valueOf(value));
				}

                Map<String, String> values = new HashMap<String, String>();
                values.putAll(discriminatorsMap);
                values.putAll(resultMap);

                table.addRow(subcontext.getLabel(), values);
            }
        }

        /*
         * TODO: make rows to columns 
         * e.g. create a new table and set columns to rows of old table and rows to columns
         * but than must be class FlexTable in this case adapted accordingly: enable setting
         */
        
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
}
