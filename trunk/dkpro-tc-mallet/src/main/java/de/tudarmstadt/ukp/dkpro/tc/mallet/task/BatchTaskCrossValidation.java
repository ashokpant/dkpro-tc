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
package de.tudarmstadt.ukp.dkpro.tc.mallet.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.reporting.Report;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.task.ExtractFeaturesTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.MetaInfoTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.PreprocessTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.ValidityCheckTask;
import de.tudarmstadt.ukp.dkpro.tc.mallet.report.BatchTrainTestReport;
import de.tudarmstadt.ukp.dkpro.tc.mallet.report.OutcomeIDReport;
import de.tudarmstadt.ukp.dkpro.tc.mallet.util.MalletFoldDimensionBundle;

/**
 * Mallet Cross-validation setup
 * 
 * @author Krish Perumal
 * 
 */
public class BatchTaskCrossValidation
    extends BatchTask
{

    private String experimentName;
    private AnalysisEngineDescription preprocessingPipeline;
    private List<String> operativeViews;
    private int numFolds;
    private boolean addInstanceId = false;
    private List<Class<? extends Report>> innerReports;

    private ValidityCheckTask checkTask;
    private PreprocessTask preprocessTask;
    private MetaInfoTask metaTask;
    private ExtractFeaturesTask extractFeaturesTrainTask;
    private ExtractFeaturesTask extractFeaturesTestTask;
    private TestTask testTask;

    public BatchTaskCrossValidation()
    {/* needed for Groovy */
    }

    /**
     * Preconfigured crossvalidation setup which should work out-of-the-box. You might want to set a
     * report to collect the results.
     * 
     * @param aExperimentName
     *            name of the experiment
     * @param aReader
     *            collection reader for input data
     * @param preprocessingPipeline
     *            preprocessing analysis engine aggregate
     * @param aDataWriterClassName
     *            data writer class name
     * @param aNumFolds
     *            the number of folds for crossvalidation (default 10)
     */
    public BatchTaskCrossValidation(String aExperimentName,
            AnalysisEngineDescription preprocessingPipeline,
            int aNumFolds)
    {
        setExperimentName(aExperimentName);
        setPreprocessingPipeline(preprocessingPipeline);
        setNumFolds(aNumFolds);
        // set name of overall batch task
        setType("Evaluation-" + experimentName);
    }

    /**
     * Initializes the experiment. This is called automatically before execution. It's not done
     * directly in the constructor, because we want to be able to use setters instead of the
     * three-argument constructor.
     * 
     * @throws IllegalStateException
     *             if not all necessary arguments have been set.
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void init()
        throws IllegalStateException, InstantiationException, IllegalAccessException,
        ClassNotFoundException
    {

        if (experimentName == null || preprocessingPipeline == null) {
            throw new IllegalStateException(
                    "You must set experiment name, datawriter and preprocessing aggregate.");
        }

        // check the validity of the experiment setup first
        checkTask = new ValidityCheckTask();

        // preprocessing on the entire data set and only once
        preprocessTask = new PreprocessTask();
        preprocessTask.setPreprocessingPipeline(preprocessingPipeline);
        preprocessTask.setTesting(false);
        preprocessTask.setOperativeViews(operativeViews);
        preprocessTask.setType(preprocessTask.getType() + "-" + experimentName);

        // inner batch task (carried out numFolds times)
        BatchTask crossValidationTask = new BatchTask()
        {
            @Override
            public void execute(TaskContext aContext)
                throws Exception
            {
                File xmiPathRoot = aContext.getStorageLocation(PreprocessTask.OUTPUT_KEY_TRAIN,
                        AccessMode.READONLY);
                Collection<File> files = FileUtils.listFiles(xmiPathRoot, new String[] { "bin" },
                        true);
                String[] fileNames = new String[files.size()];
                int i = 0;
                for (File f : files) {
                    // adding file paths, not names
                    fileNames[i] = f.getAbsolutePath();
                    i++;
                }
                Arrays.sort(fileNames);
                if (numFolds == Constants.LEAVE_ONE_OUT) {
                    numFolds = fileNames.length;
                }
                // don't change any names!!
                MalletFoldDimensionBundle<String> foldDim = new MalletFoldDimensionBundle<String>(
                        "files", Dimension.create("", fileNames), numFolds);
                Dimension<File> filesRootDim = Dimension.create("filesRoot", xmiPathRoot);

                ParameterSpace pSpace = new ParameterSpace(foldDim, filesRootDim);
                setParameterSpace(pSpace);

                super.execute(aContext);
            }
        };

        // ================== SUBTASKS OF THE INNER BATCH TASK =======================

        // collecting meta features only on the training data (numFolds times)
        metaTask = new MetaInfoTask();
        metaTask.setOperativeViews(operativeViews);
        metaTask.setType(metaTask.getType() + experimentName);

        // extracting features from training data (numFolds times)
        extractFeaturesTrainTask = new ExtractFeaturesTask();
        extractFeaturesTrainTask.setTesting(false);
        extractFeaturesTrainTask.setType(extractFeaturesTrainTask.getType() + "-Train-"
                + experimentName);
        extractFeaturesTrainTask.addImport(metaTask, MetaInfoTask.META_KEY);

        // extracting features from test data (numFolds times)
        extractFeaturesTestTask = new ExtractFeaturesTask();
        extractFeaturesTestTask.setTesting(true);
        extractFeaturesTestTask.setType(extractFeaturesTestTask.getType() + "-Test-"
                + experimentName);
        extractFeaturesTestTask.addImport(metaTask, MetaInfoTask.META_KEY);

        // classification (numFolds times)
        testTask = new TestTask();
        testTask.setType(testTask.getType() + "-" + experimentName);
        if (innerReports != null) {
            for (Class<? extends Report> report : innerReports) {
                testTask.addReport(report);
            }
        }
        if (addInstanceId) {
            testTask.addReport(OutcomeIDReport.class);
        }
        testTask.addImport(extractFeaturesTrainTask, ExtractFeaturesTask.OUTPUT_KEY,
                TestTask.INPUT_KEY_TRAIN);
        testTask.addImport(extractFeaturesTestTask, ExtractFeaturesTask.OUTPUT_KEY,
                TestTask.INPUT_KEY_TEST);

        // ================== CONFIG OF THE INNER BATCH TASK =======================

        crossValidationTask.addImport(preprocessTask, PreprocessTask.OUTPUT_KEY_TRAIN);
        crossValidationTask.setType(crossValidationTask.getType() + experimentName);
        crossValidationTask.addTask(metaTask);
        crossValidationTask.addTask(extractFeaturesTrainTask);
        crossValidationTask.addTask(extractFeaturesTestTask);
        crossValidationTask.addTask(testTask);
        // report of the inner batch task (sums up results for the folds)
        // we want to re-use the old CV report, we need to collect the evaluation.bin files from
        // the test task here (with another report)
        if (innerReports != null) {
            crossValidationTask.addReport(BatchTrainTestReport.class);
        }

        // don't move! makes sure this task is executed at the beginning of the pipeline!
        addTask(checkTask);
        addTask(preprocessTask);
        addTask(crossValidationTask);
    }

    @Override
    public void execute(TaskContext aContext)
        throws Exception
    {
        init();
        super.execute(aContext);
    }

    public void setExperimentName(String experimentName)
    {
        this.experimentName = experimentName;
    }

    public void setPreprocessingPipeline(AnalysisEngineDescription preprocessingPipeline)
    {
        this.preprocessingPipeline = preprocessingPipeline;
    }

    public void setOperativeViews(List<String> operativeViews)
    {
        this.operativeViews = operativeViews;
    }

    public void setNumFolds(int numFolds)
    {
        this.numFolds = numFolds;
    }

    public void setAddInstanceId(boolean addInstanceId)
    {
        this.addInstanceId = addInstanceId;
    }

    /**
     * Adds a report for the inner test task
     * 
     * @param innerReport
     *            classification report or regression report
     */
    public void addInnerReport(Class<? extends Report> innerReport)
    {
        if (innerReports == null) {
            innerReports = new ArrayList<Class<? extends Report>>();
        }
        this.innerReports.add(innerReport);
    }
}