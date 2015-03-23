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
package de.tudarmstadt.ukp.dkpro.tc.ml;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.reporting.Report;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.TaskBase;
import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.ml.TCMachineLearningAdapter;
import de.tudarmstadt.ukp.dkpro.tc.core.task.ExtractFeaturesTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.MetaInfoTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.PreprocessTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.ValidityCheckTask;

/**
 * Train-Test setup
 * 
 * @author daxenberger
 * @author zesch
 * 
 */
public class ExperimentTrainTest
    extends BatchTask
{

    private String experimentName;
    private AnalysisEngineDescription preprocessing;
    private List<String> operativeViews;
    private List<Class<? extends Report>> innerReports;
    private TCMachineLearningAdapter mlAdapter;

    private ValidityCheckTask checkTaskTrain;
    private ValidityCheckTask checkTaskTest;
    private PreprocessTask preprocessTaskTrain;
    private PreprocessTask preprocessTaskTest;
    private MetaInfoTask metaTask;
    private ExtractFeaturesTask featuresTrainTask;
    private ExtractFeaturesTask featuresTestTask;
    private TaskBase testTask;

    public ExperimentTrainTest()
    {/* needed for Groovy */
    }

    /**
     * Preconfigured train-test setup.
     * 
     * @param aExperimentName
     *            name of the experiment
     * @param preprocessing
     *            preprocessing analysis engine aggregate
     */
    public ExperimentTrainTest(String aExperimentName, Class<? extends TCMachineLearningAdapter> mlAdapter,
            AnalysisEngineDescription preprocessing)
            throws TextClassificationException
    {
        setExperimentName(aExperimentName);
        setMachineLearningAdapter(mlAdapter);
        setPreprocessing(preprocessing);
        // set name of overall batch task
        setType("Evaluation-" + experimentName);
    }

    @Override
    public void execute(TaskContext aContext)
        throws Exception
    {
        init();
        super.execute(aContext);
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
    {
        if (experimentName == null || preprocessing == null)

        {
            throw new IllegalStateException(
                    "You must set Experiment Name, DataWriter and Aggregate.");
        }

        // check the validity of the experiment setup first
        checkTaskTrain = new ValidityCheckTask();
        checkTaskTrain.setType(checkTaskTrain.getType() + "-Train-" + experimentName);
        checkTaskTrain.setMlAdapter(mlAdapter);
        checkTaskTest = new ValidityCheckTask();
        checkTaskTest.setTesting(true);
        checkTaskTest.setType(checkTaskTest.getType() + "-Test-" + experimentName);
        checkTaskTest.setMlAdapter(mlAdapter);

        // preprocessing on training data
        preprocessTaskTrain = new PreprocessTask();
        preprocessTaskTrain.setPreprocessing(preprocessing);
        preprocessTaskTrain.setOperativeViews(operativeViews);
        preprocessTaskTrain.setTesting(false);
        preprocessTaskTrain.setType(preprocessTaskTrain.getType() + "-Train-" + experimentName);

        // preprocessing on test data
        preprocessTaskTest = new PreprocessTask();
        preprocessTaskTest.setPreprocessing(preprocessing);
        preprocessTaskTest.setOperativeViews(operativeViews);
        preprocessTaskTest.setTesting(true);
        preprocessTaskTest.setType(preprocessTaskTest.getType() + "-Test-" + experimentName);

        // get some meta data depending on the whole document collection that we need for training
        metaTask = new MetaInfoTask();
        metaTask.setOperativeViews(operativeViews);
        metaTask.setType(metaTask.getType() + "-" + experimentName);

        metaTask.addImport(preprocessTaskTrain, PreprocessTask.OUTPUT_KEY_TRAIN,
                MetaInfoTask.INPUT_KEY);

        // feature extraction on training data
        featuresTrainTask = new ExtractFeaturesTask();
        featuresTrainTask.setType(featuresTrainTask.getType() + "-Train-" + experimentName);
        featuresTrainTask.setMlAdapter(mlAdapter);
        featuresTrainTask.addImport(metaTask, MetaInfoTask.META_KEY);
        featuresTrainTask.addImport(preprocessTaskTrain, PreprocessTask.OUTPUT_KEY_TRAIN,
                ExtractFeaturesTask.INPUT_KEY);

        // feature extraction on test data
        featuresTestTask = new ExtractFeaturesTask();
        featuresTestTask.setType(featuresTestTask.getType() + "-Test-" + experimentName);
        featuresTestTask.setMlAdapter(mlAdapter);
        featuresTestTask.addImport(metaTask, MetaInfoTask.META_KEY);
        featuresTestTask.addImport(preprocessTaskTest, PreprocessTask.OUTPUT_KEY_TEST,
                ExtractFeaturesTask.INPUT_KEY);
        featuresTestTask.addImport(featuresTrainTask, ExtractFeaturesTask.OUTPUT_KEY);

        // test task operating on the models of the feature extraction train and test tasks
        testTask = mlAdapter.getTestTask();
        testTask.setType(testTask.getType() + "-" + experimentName);

        if (innerReports != null) {
            for (Class<? extends Report> report : innerReports) {
                testTask.addReport(report);
            }
        }

        // always add default report
        testTask.addReport(mlAdapter.getClassificationReportClass());
        // always add OutcomeIdReport
        testTask.addReport(mlAdapter.getOutcomeIdReportClass());

        testTask.addImport(featuresTrainTask, ExtractFeaturesTask.OUTPUT_KEY,
                Constants.TEST_TASK_INPUT_KEY_TRAINING_DATA);
        testTask.addImport(featuresTestTask, ExtractFeaturesTask.OUTPUT_KEY,
                Constants.TEST_TASK_INPUT_KEY_TEST_DATA);

        // DKPro Lab issue 38: must be added as *first* task
        addTask(checkTaskTrain);
        addTask(checkTaskTest);
        addTask(preprocessTaskTrain);
        addTask(preprocessTaskTest);
        addTask(metaTask);
        addTask(featuresTrainTask);
        addTask(featuresTestTask);
        addTask(testTask);
    }

    public void setMachineLearningAdapter(Class<? extends TCMachineLearningAdapter> mlAdapter)
        throws IllegalArgumentException
    {
        try {
			this.mlAdapter = mlAdapter.newInstance();
		} catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
		}
    }

    public void setExperimentName(String experimentName)
    {
        this.experimentName = experimentName;
    }

    public void setPreprocessing(AnalysisEngineDescription preprocessing)
    {
        this.preprocessing = preprocessing;
    }

    public void setOperativeViews(List<String> operativeViews)
    {
        this.operativeViews = operativeViews;
    }

    /**
     * Sets the report for the test task
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
