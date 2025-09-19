package bankanalyzer.ml;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import bankanalyzer.data.BankData;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class RatePredictor {
    private MultiLayerNetwork model;
    private final String MODEL_PATH = "models/rate_predictor.zip";
    private boolean isTrained = false;

    public void trainModel(List<BankData> trainingData) {
        if (trainingData == null || trainingData.isEmpty()) {
            System.out.println("Нет данных для обучения. Используются случайные значения.");
            return;
        }

        try {
            // Подготовка данных
            INDArray features = prepareFeatures(trainingData);
            INDArray labels = prepareLabels(trainingData);

            DataSet dataSet = new DataSet(features, labels);

            // Создание модели
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(12345)
                    .updater(new Adam(0.001))
                    .weightInit(WeightInit.XAVIER)
                    .list()
                    .layer(new DenseLayer.Builder()
                            .nIn(4)
                            .nOut(10)
                            .activation(Activation.RELU)
                            .build())
                    .layer(new DenseLayer.Builder()
                            .nIn(10)
                            .nOut(10)
                            .activation(Activation.RELU)
                            .build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(10)
                            .nOut(1)
                            .activation(Activation.IDENTITY)
                            .build())
                    .build();

            model = new MultiLayerNetwork(conf);
            model.init();

            // Обучение
            for (int i = 0; i < 500; i++) {
                model.fit(dataSet);
                if (i % 100 == 0) {
                    System.out.println("Эпоха обучения: " + i);
                }
            }

            // Сохранение модели
            saveModel();
            isTrained = true;
            System.out.println("Модель успешно обучена и сохранена");

        } catch (Exception e) {
            System.err.println("Ошибка при обучении модели: " + e.getMessage());
            isTrained = false;
        }
    }

    public double predictReturn(BankData currentData) {
        if (!isTrained) {
            // Если модель не обучена, используем эвристику
            return calculateHeuristicReturn(currentData);
        }

        try {
            INDArray input = Nd4j.create(new double[]{
                    currentData.getDepositRate(),
                    currentData.getLoanRate(),
                    currentData.getInvestmentReturn(),
                    currentData.getTermDays() / 365.0
            }, new int[]{1, 4});

            INDArray output = model.output(input);
            return output.getDouble(0);

        } catch (Exception e) {
            System.err.println("Ошибка предсказания: " + e.getMessage());
            return calculateHeuristicReturn(currentData);
        }
    }

    private double calculateHeuristicReturn(BankData data) {
        // Простая эвристика: средняя доходность + премия за стабильность
        double baseReturn = data.getDepositRate() * 1.3;
        Random random = new Random();
        return baseReturn + (random.nextDouble() * 2 - 1); // ±1% случайность
    }

    private INDArray prepareFeatures(List<BankData> data) {
        double[][] featuresArray = new double[data.size()][4];
        for (int i = 0; i < data.size(); i++) {
            BankData bankData = data.get(i);
            featuresArray[i][0] = bankData.getDepositRate();
            featuresArray[i][1] = bankData.getLoanRate();
            featuresArray[i][2] = bankData.getInvestmentReturn();
            featuresArray[i][3] = bankData.getTermDays() / 365.0;
        }
        return Nd4j.create(featuresArray);
    }

    private INDArray prepareLabels(List<BankData> data) {
        double[] labelsArray = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            labelsArray[i] = data.get(i).getInvestmentReturn();
        }
        return Nd4j.create(labelsArray, new int[]{data.size(), 1});
    }

    private void saveModel() {
        try {
            new File("models").mkdirs();
            ModelSerializer.writeModel(model, new File(MODEL_PATH), true);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения модели: " + e.getMessage());
        }
    }

    public boolean isModelTrained() {
        return isTrained;
    }
}