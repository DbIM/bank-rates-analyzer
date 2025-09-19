package bankanalyzer;

import bankanalyzer.web.WebScraper;
import bankanalyzer.ml.InvestmentRecommender;
import bankanalyzer.data.BankData;
import bankanalyzer.data.DataStorage;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        WebScraper scraper = new WebScraper();
        InvestmentRecommender recommender = new InvestmentRecommender();
        DataStorage storage = new DataStorage();

        Scanner scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║               АНАЛИЗАТОР БАНКОВСКИХ СТАВОК               ║");
        System.out.println("║                 ИНВЕСТИЦИОННЫХ ПРЕДЛОЖЕНИЙ               ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Пробуем загрузить исторические данные
        System.out.println("📊 Загрузка исторических данных...");
        List<BankData> historicalData = storage.loadData();
        System.out.println("   Загружено исторических записей: " + historicalData.size());

        // Собираем текущие данные
        System.out.println("🌐 Сбор текущих данных о банковских ставках...");
        List<BankData> currentData = scraper.scrapeBankRates();
        System.out.println("   Собрано текущих данных: " + currentData.size() + " банков");

        // Объединяем данные
        if (!historicalData.isEmpty()) {
            currentData.addAll(historicalData);
            System.out.println("   Общее количество данных для анализа: " + currentData.size());
        }

        // Сохраняем обновленные данные
        System.out.println("💾 Сохранение данных...");
        storage.saveData(currentData);

        // Получаем входные данные от пользователя
        System.out.println("\n💵 Введите параметры инвестирования:");
        System.out.print("   Сумма для инвестирования (руб.): ");
        double amount = scanner.nextDouble();

        System.out.print("   Срок инвестирования (дней): ");
        int term = scanner.nextInt();

        System.out.println("\n🔍 Анализ данных и генерация рекомендаций...");

        try {
            long startTime = System.currentTimeMillis();

            List<BankData> recommendations = recommender.getInvestmentRecommendations(currentData, amount, term);
            String report = recommender.generateRecommendationReport(recommendations, amount);

            long endTime = System.currentTimeMillis();
            double analysisTime = (endTime - startTime) / 1000.0;

            System.out.println(report);
            System.out.printf("⏱️  Время анализа: %.2f секунд\n", analysisTime);

            // Дополнительная информация по лучшему варианту
            if (!recommendations.isEmpty()) {
                System.out.println(recommender.generateDetailedReport(recommendations.get(0), amount));
            }

            // Сохраняем рекомендации
            System.out.print("💾 Сохранить рекомендации в файл? (y/n): ");
            String saveChoice = scanner.next();
            if (saveChoice.equalsIgnoreCase("y")) {
                saveRecommendations(recommendations, amount);
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка при анализе данных: " + e.getMessage());
            System.out.println("🔄 Используются базовые рекомендации...");

            // Резервные рекомендации
            List<BankData> basicRecommendations = currentData.stream()
                    .sorted((a, b) -> Double.compare(b.getDepositRate(), a.getDepositRate()))
                    .limit(3)
                    .collect(java.util.stream.Collectors.toList());

            String basicReport = recommender.generateRecommendationReport(basicRecommendations, amount);
            System.out.println(basicReport);
        }

        System.out.println("\n✨ Анализ завершен. Хороших инвестиций!");
        scanner.close();
    }

    private static void saveRecommendations(List<BankData> recommendations, double amount) {
        try (java.io.FileWriter writer = new java.io.FileWriter("рекомендации.txt")) {
            writer.write("Рекомендации по инвестированию\n");
            writer.write("Дата: " + java.time.LocalDateTime.now() + "\n");
            writer.write("Сумма инвестиций: " + amount + " руб.\n\n");

            for (int i = 0; i < recommendations.size(); i++) {
                BankData bank = recommendations.get(i);
                double profit = amount * bank.getInvestmentReturn() / 100;

                writer.write((i + 1) + ". " + bank.getBankName() + "\n");
                writer.write("   Доходность: " + String.format("%.2f", bank.getInvestmentReturn()) + "%\n");
                writer.write("   Прибыль: " + String.format("%,.0f", profit) + " руб.\n");
                writer.write("   Ставка по депозиту: " + String.format("%.2f", bank.getDepositRate()) + "%\n");
                writer.write("   Ставка по кредиту: " + String.format("%.2f", bank.getLoanRate()) + "%\n\n");
            }

            System.out.println("✅ Рекомендации сохранены в файл 'рекомендации.txt'");
        } catch (java.io.IOException e) {
            System.err.println("❌ Ошибка при сохранении рекомендаций: " + e.getMessage());
        }
    }
}