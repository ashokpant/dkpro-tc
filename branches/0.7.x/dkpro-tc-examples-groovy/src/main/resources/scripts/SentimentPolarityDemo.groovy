package de.tudarmstadt.ukp.dkpro.tc.demo.sentimentpolarity;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription

import org.apache.uima.analysis_engine.AnalysisEngineDescription
import org.apache.uima.resource.ResourceInitializationException

import weka.classifiers.functions.SMO
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter
import de.tudarmstadt.ukp.dkpro.lab.Lab
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy
import de.tudarmstadt.ukp.dkpro.tc.core.Constants
import de.tudarmstadt.ukp.dkpro.tc.core.GroovyExperiment
import de.tudarmstadt.ukp.dkpro.tc.examples.io.MovieReviewCorpusReader
import de.tudarmstadt.ukp.dkpro.tc.features.length.NrOfTokensDFE
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.LuceneNGramDFE
import de.tudarmstadt.ukp.dkpro.tc.features.syntax.QuestionsRatioFeatureExtractor
import de.tudarmstadt.ukp.dkpro.tc.weka.WekaClassificationAdapter;
import de.tudarmstadt.ukp.dkpro.tc.weka.writer.WekaDataWriter
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentCrossValidation
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentTrainTest
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchCrossValidationReport;
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchOutcomeIDReport;
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchTrainTestReport;

/**
 * Groovy-Version of the SentimentPolarityExperiment
 *
 * @author Oliver Ferschke
 * @author daxenberger
 */
public class SentimentPolarityDemo implements GroovyExperiment, Constants {

    // === PARAMETERS===========================================================

    def experimentName = "SentimentPolarity";
    def corpusFilePathTrain = "src/main/resources/data/sentiment/train-small";
    def corpusFilePathTest  ="src/main/resources/data/sentiment/test-small";
    def languageCode = "en";
    def numFolds = 2;

    // === DIMENSIONS===========================================================

    def dimReaders = Dimension.createBundle("readers", [
        readerTest: MovieReviewCorpusReader.class,
        readerTestParams: [
            MovieReviewCorpusReader.PARAM_SOURCE_LOCATION,
            corpusFilePathTest,
            MovieReviewCorpusReader.PARAM_LANGUAGE,
            languageCode,
            MovieReviewCorpusReader.PARAM_PATTERNS,
            MovieReviewCorpusReader.INCLUDE_PREFIX + "*/*.txt"
        ],
        readerTrain: MovieReviewCorpusReader.class,
        readerTrainParams: [
            MovieReviewCorpusReader.PARAM_SOURCE_LOCATION,
            corpusFilePathTrain,
            MovieReviewCorpusReader.PARAM_LANGUAGE,
            languageCode,
            MovieReviewCorpusReader.PARAM_PATTERNS,
            MovieReviewCorpusReader.INCLUDE_PREFIX + "*/*.txt"
        ]
    ]);

    def dimLearningMode = Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL);
    def dimFeatureMode = Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT);

    def dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
    [
        SMO.class.name,
        "-C",
        "10"
    ]);

    def dimFeatureSets = Dimension.create(
    DIM_FEATURE_SET,
    [
        QuestionsRatioFeatureExtractor.name,
        NrOfTokensDFE.name,
        LuceneNGramDFE.name
    ]
    );

    def dimPipelineParameters = Dimension.create(
    DIM_PIPELINE_PARAMS,
    [
        LuceneNGramDFE.PARAM_NGRAM_USE_TOP_K,
        "500",
        LuceneNGramDFE.PARAM_NGRAM_MIN_N,
        1,
        LuceneNGramDFE.PARAM_NGRAM_MAX_N,
        3
    ],
    [
        LuceneNGramDFE.PARAM_NGRAM_USE_TOP_K,
        "5000",
        LuceneNGramDFE.PARAM_NGRAM_MIN_N,
        1,
        LuceneNGramDFE.PARAM_NGRAM_MAX_N,
        3
    ]
    );

    // === Experiments =========================================================

    /**
     * Crossvalidation setting
     *
     * @throws Exception
     */
    protected void runCrossValidation() throws Exception
    {

        ExperimentCrossValidation batchTask = [
            experimentName: experimentName + "-CV-Groovy",
            // we need to explicitly set the name of the batch task, as the constructor of the groovy setup must be zero-arg
            type: "Evaluation-"+ experimentName +"-CV-Groovy",
            preprocessing: getPreprocessing(),
            machineLearningAdapter: WekaClassificationAdapter,
            parameterSpace : [
                dimReaders,
                dimLearningMode,
                dimFeatureMode,
                dimClassificationArgs,
                dimFeatureSets,
                dimPipelineParameters
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [BatchCrossValidationReport],
            numFolds: numFolds];

        Lab.getInstance().run(batchTask);
    }
    /**
     * TrainTest Setting
     *
     * @throws Exception
     */
    protected void runTrainTest() throws Exception
    {

        ExperimentTrainTest batchTask = [
            experimentName: experimentName + "-TrainTest-Groovy",
            // we need to explicitly set the name of the batch task, as the constructor of the groovy setup must be zero-arg
            type: "Evaluation-"+ experimentName +"-TrainTest-Groovy",
            preprocessing:	getPreprocessing(),
            machineLearningAdapter: WekaClassificationAdapter,
            parameterSpace : [
                dimReaders,
                dimLearningMode,
                dimFeatureMode,
                dimClassificationArgs,
                dimFeatureSets,
                dimPipelineParameters
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
                BatchTrainTestReport,
                BatchOutcomeIDReport]
        ];

        // Run
        Lab.getInstance().run(batchTask);
    }

    private AnalysisEngineDescription getPreprocessing()
    throws ResourceInitializationException
    {
        return createEngineDescription(
        createEngineDescription(BreakIteratorSegmenter.class),
        createEngineDescription(OpenNlpPosTagger.class)
        );
    }

    public void run()
    {
        new SentimentPolarityDemo().runTrainTest();
        new SentimentPolarityDemo().runCrossValidation();
    }

}
