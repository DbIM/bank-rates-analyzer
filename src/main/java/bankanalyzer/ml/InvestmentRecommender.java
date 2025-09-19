package bankanalyzer.ml;

import bankanalyzer.data.BankData;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class InvestmentRecommender {
    private final RatePredictor ratePredictor;

    public InvestmentRecommender() {
        this.ratePredictor = new RatePredictor();
    }

    public List<BankData> getInvestmentRecommendations(List<BankData> bankDataList,
                                                       double investmentAmount,
                                                       int investmentTerm) {
        System.out.println("Анализ данных для " + bankDataList.size() + " банков...");

        // Обучаем модель на имеющихся данных
        if (!ratePredictor.isModelTrained()) {
            System.out.println("Обучение модели на текущих данных...");
            ratePredictor.trainModel(bankDataList);
        }

        // Прогнозируем доходность для каждого банка
        List<BankData> predictions = bankDataList.stream()
                .map(bank -> {
                    double predictedReturn = ratePredictor.predictReturn(bank);
                    return new BankData(
                            bank.getBankName(),
                            bank.getDepositRate(),
                            bank.getLoanRate(),
                            predictedReturn,
                            bank.getDate(),
                            investmentTerm // Используем указанный пользователем срок
                    );
                })
                .collect(Collectors.toList());

        // Сортируем по прогнозируемой доходности
        return predictions.stream()
                .sorted(Comparator.comparingDouble(BankData::getInvestmentReturn).reversed())
                .limit(5) // Топ-5 рекомендаций
                .collect(Collectors.toList());
    }

    public String generateRecommendationReport(List<BankData> recommendations,
                                               double investmentAmount) {
        StringBuilder report = new StringBuilder();
        report.append("╔══════════════════════════════════════════════════════════╗\n");
        report.append("║               РЕКОМЕНДАЦИИ ПО ИНВЕСТИРОВАНИЮ             ║\n");
        report.append("╠══════════════════════════════════════════════════════════╣\n");

        if (recommendations.isEmpty()) {
            report.append("║                 Нет данных для рекомендаций              ║\n");
            report.append("╚══════════════════════════════════════════════════════════╝\n");
            return report.toString();
        }

        report.append(String.format("║ %-15s %-8s %-10s %-12s %-10s ║\n",
                "Банк", "Доходн.", "Прибыль", "Итого", "Депозит"));
        report.append("╠══════════════════════════════════════════════════════════╣\n");

        for (int i = 0; i < recommendations.size(); i++) {
            BankData bank = recommendations.get(i);
            double expectedProfit = investmentAmount * bank.getInvestmentReturn() / 100;
            double expectedTotal = investmentAmount + expectedProfit;

            String bankName = (i == 0 ? "🥇 " : i == 1 ? "🥈 " : i == 2 ? "🥉 " : "   ") + bank.getBankName();
            if (bankName.length() > 15) bankName = bankName.substring(0, 15);

            report.append(String.format("║ %-15s %-7.2f%% %-9.0fр. %-11.0fр. %-8.2f%% ║\n",
                    bankName,
                    bank.getInvestmentReturn(),
                    expectedProfit,
                    expectedTotal,
                    bank.getDepositRate()));
        }

        report.append("╠══════════════════════════════════════════════════════════╣\n");

        BankData best = recommendations.get(0);
        double bestProfit = investmentAmount * best.getInvestmentReturn() / 100;

        report.append(String.format("║ Лучший вариант: %-30s ║\n", best.getBankName()));
        report.append(String.format("║ Прогнозируемая доходность: %-22.2f%% ║\n", best.getInvestmentReturn()));
        report.append(String.format("║ Ожидаемая прибыль: %-26.0fр. ║\n", bestProfit));
        report.append(String.format("║ Общая сумма через срок: %-21.0fр. ║\n", investmentAmount + bestProfit));

        report.append("╚══════════════════════════════════════════════════════════╝\n\n");

        report.append("ДОПОЛНИТЕЛЬНЫЕ РЕКОМЕНДАЦИИ:\n");
        report.append("• Диверсифицируйте инвестиции между несколькими банками\n");
        report.append("• Учитывайте надежность банка (рейтинги, отзывы)\n");
        report.append("• Проверяйте условия досрочного снятия\n");
        report.append("• Уточняйте актуальные ставки на официальных сайтах\n\n");

        report.append("Примечание: Прогнозы основаны на машинном обучении и исторических данных.\n");
        report.append("Реальные результаты могут отличаться. Инвестируйте осознанно!\n");

        return report.toString();
    }

    public String generateDetailedReport(BankData bank, double investmentAmount) {
        double expectedProfit = investmentAmount * bank.getInvestmentReturn() / 100;
        double expectedTotal = investmentAmount + expectedProfit;
        double monthlyProfit = expectedProfit / (bank.getTermDays() / 30.0);

        return String.format(
                "\nДЕТАЛЬНЫЙ АНАЛИЗ: %s\n" +
                        "──────────────────────────────────────────────────\n" +
                        "Прогнозируемая доходность:   %.2f%% годовых\n" +
                        "Срок инвестирования:         %d дней\n" +
                        "Сумма инвестиций:            %,.0f руб.\n" +
                        "Ожидаемая прибыль:           %,.0f руб.\n" +
                        "Ежемесячный доход:           %,.0f руб.\n" +
                        "Общая сумма к получению:     %,.0f руб.\n" +
                        "Ставка по депозитам:         %.2f%%\n" +
                        "Ставка по кредитам:          %.2f%%\n",
                bank.getBankName(),
                bank.getInvestmentReturn(),
                bank.getTermDays(),
                investmentAmount,
                expectedProfit,
                monthlyProfit,
                expectedTotal,
                bank.getDepositRate(),
                bank.getLoanRate()
        );
    }
}