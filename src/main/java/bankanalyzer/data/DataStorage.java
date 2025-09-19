package bankanalyzer.data;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {
    private static final String DATA_FILE = "data/bank_data.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String CSV_HEADER = "bankName,depositRate,loanRate,investmentReturn,date,termDays";

    public void saveData(List<BankData> data) {
        try {
            new File("data").mkdirs();
            try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
                writer.println(CSV_HEADER);

                for (BankData item : data) {
                    writer.printf("%s,%.2f,%.2f,%.2f,%s,%d%n",
                            item.getBankName().replace(",", ";"),
                            item.getDepositRate(),
                            item.getLoanRate(),
                            item.getInvestmentReturn(),
                            item.getDate().format(DATE_FORMATTER),
                            item.getTermDays());
                }

                System.out.println("Данные успешно сохранены в CSV: " + DATA_FILE);
            }
        } catch (IOException e) {
            System.err.println("Ошибка сохранения данных: " + e.getMessage());
        }
    }

    public List<BankData> loadData() {
        List<BankData> data = new ArrayList<>();

        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) {
                System.out.println("Файл данных не существует: " + DATA_FILE);
                return data;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();

                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 6) {
                        BankData bankData = new BankData(
                                parts[0],
                                Double.parseDouble(parts[1]),
                                Double.parseDouble(parts[2]),
                                Double.parseDouble(parts[3]),
                                LocalDate.parse(parts[4], DATE_FORMATTER),
                                Integer.parseInt(parts[5])
                        );
                        data.add(bankData);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки данных: " + e.getMessage());
        }

        return data;
    }
}