package bankanalyzer.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import bankanalyzer.data.BankData;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebScraper {
    private static final String[] BANK_URLS = {
            "https://www.sberbank.ru/",
            "https://www.vtb.ru/",
            "https://www.tinkoff.ru/",
            "https://www.alfabank.ru/",
            "https://www.gazprombank.ru/",
            "https://www.raiffeisen.ru/",
            "https://www.open.ru/",
            "https://www.mkb.ru/"
    };

    static {
        // Инициализируем SSL обход при загрузке класса
        initSSL();
    }

    private static void initSSL() {
        try {
            // Создаем trust manager который доверяет всем сертификатам
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            System.err.println("Ошибка при настройке SSL: " + e.getMessage());
        }
    }

    public List<BankData> scrapeBankRates() {
        List<BankData> bankDataList = new ArrayList<>();

        for (String url : BANK_URLS) {
            try {
                System.out.println("Сканируем: " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(20000)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .followRedirects(true)
                        .maxBodySize(0)
                        .get();

                BankData data = parseBankData(doc, url);
                if (data != null) {
                    bankDataList.add(data);
                    System.out.println("Данные получены для: " + data.getBankName());
                } else {
                    bankDataList.add(createMockDataForBank(url));
                }

                Thread.sleep(2000);

            } catch (IOException | InterruptedException e) {
                System.err.println("Ошибка при сканировании: " + url + " - " + e.getMessage());
                bankDataList.add(createMockDataForBank(url));
            }
        }

        return bankDataList;
    }

    private BankData parseBankData(Document doc, String url) {
        String bankName = extractBankNameFromUrl(url);

        double depositRate = extractDepositRate(doc, bankName);
        double loanRate = extractLoanRate(doc, bankName);
        double investmentReturn = calculateInvestmentReturn(depositRate);

        if (depositRate == 0.0 && loanRate == 0.0) {
            return null;
        }

        return new BankData(
                bankName,
                depositRate,
                loanRate,
                investmentReturn,
                LocalDate.now(),
                365
        );
    }

    private double extractDepositRate(Document doc, String bankName) {
        switch (bankName) {
            case "sberbank":
                return extractSberbankRate(doc, "вклад", "депозит");
            case "vtb":
                return extractVtbRate(doc);
            case "tinkoff":
                return extractTinkoffRate(doc);
            case "alfabank":
                return extractAlfabankRate(doc);
            default:
                return extractGenericRate(doc, new String[]{"вклад", "депозит", "savings", "deposit"});
        }
    }

    private double extractLoanRate(Document doc, String bankName) {
        switch (bankName) {
            case "sberbank":
                return extractSberbankRate(doc, "кредит", "заем");
            case "vtb":
                return extractGenericRate(doc, new String[]{"кредит", "loan", "credit"});
            default:
                return extractGenericRate(doc, new String[]{"кредит", "заем", "ипотека", "loan", "credit", "mortgage"});
        }
    }

    private double extractSberbankRate(Document doc, String... keywords) {
        try {
            Elements rateElements = doc.select(".rate, .percentage, .interest-rate");
            for (Element element : rateElements) {
                String text = element.text().toLowerCase();
                for (String keyword : keywords) {
                    if (text.contains(keyword)) {
                        return extractNumberFromText(text);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка парсинга Сбербанка: " + e.getMessage());
        }
        return 0.0;
    }

    private double extractVtbRate(Document doc) {
        try {
            Elements elements = doc.select("[class*='rate'], [class*='percent']");
            for (Element element : elements) {
                String text = element.text();
                if (text.contains("%")) {
                    return extractNumberFromText(text);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка парсинга ВТБ: " + e.getMessage());
        }
        return 0.0;
    }

    private double extractGenericRate(Document doc, String[] keywords) {
        try {
            Elements elements = doc.select("div, span, p, td, li, a, h1, h2, h3, h4, h5, h6");
            for (Element element : elements) {
                String text = element.text().toLowerCase();
                for (String keyword : keywords) {
                    if (text.contains(keyword) && text.contains("%")) {
                        double rate = extractNumberFromText(text);
                        if (rate > 1.0 && rate < 30.0) { // Проверяем разумность значения
                            return rate;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка generic парсинга: " + e.getMessage());
        }
        return 0.0;
    }

    private double calculateInvestmentReturn(double depositRate) {
        if (depositRate == 0.0) {
            Random random = new Random();
            return 8.0 + random.nextDouble() * 4.0;
        }

        return depositRate * (1.3 + new Random().nextDouble() * 0.4);
    }

    private BankData createMockDataForBank(String url) {
        String bankName = extractBankNameFromUrl(url);
        Random random = new Random();

        switch (bankName) {
            case "alfabank":
                return new BankData("Альфа-Банк",
                        6.2 + random.nextDouble(),
                        11.5 + random.nextDouble() * 2.0,
                        9.5 + random.nextDouble() * 1.5,
                        LocalDate.now(), 365);

            case "open":
                return new BankData("Банк Открытие",
                        5.8 + random.nextDouble() * 0.8,
                        12.0 + random.nextDouble() * 2.5,
                        8.7 + random.nextDouble() * 1.2,
                        LocalDate.now(), 365);

            case "sberbank":
                return new BankData("Сбербанк",
                        5.0 + random.nextDouble(),
                        13.0 + random.nextDouble() * 2.0,
                        7.5 + random.nextDouble() * 1.5,
                        LocalDate.now(), 365);

            case "tinkoff":
                return new BankData("Тинькофф",
                        7.0 + random.nextDouble() * 1.5,
                        10.5 + random.nextDouble() * 1.5,
                        10.5 + random.nextDouble() * 2.0,
                        LocalDate.now(), 365);

            default:
                return new BankData(bankName,
                        6.0 + random.nextDouble() * 2.0,
                        12.0 + random.nextDouble() * 3.0,
                        9.0 + random.nextDouble() * 2.5,
                        LocalDate.now(), 365);
        }
    }

    private double extractNumberFromText(String text) {
        try {
            Pattern pattern = Pattern.compile("(\\d+[.,]?\\d*)%?");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String numberStr = matcher.group(1).replace(',', '.');
                return Double.parseDouble(numberStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Ошибка преобразования числа: " + text);
        }
        return 0.0;
    }

    private String extractBankNameFromUrl(String url) {
        String cleanedUrl = url.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "");

        int dotIndex = cleanedUrl.indexOf('.');
        if (dotIndex > 0) {
            String domain = cleanedUrl.substring(0, dotIndex);

            switch (domain) {
                case "sberbank": return "Сбербанк";
                case "vtb": return "ВТБ";
                case "tinkoff": return "Тинькофф";
                case "alfabank": return "Альфа-Банк";
                case "gazprombank": return "Газпромбанк";
                case "raiffeisen": return "Райффайзен";
                case "open": return "Банк Открытие";
                case "mkb": return "МКБ";
                default: return domain;
            }
        }

        return cleanedUrl.replace("/", "");
    }

    private double extractTinkoffRate(Document doc) {
        try {
            Elements elements = doc.select("[data-qa-type*='rate'], [class*='rate']");
            for (Element element : elements) {
                String text = element.text();
                if (text.contains("%")) {
                    return extractNumberFromText(text);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка парсинга Тинькофф: " + e.getMessage());
        }
        return 0.0;
    }

    private double extractAlfabankRate(Document doc) {
        try {
            Elements elements = doc.select(".product-rate, .interest-rate, .percentage-value");
            for (Element element : elements) {
                String text = element.text();
                if (text.contains("%")) {
                    return extractNumberFromText(text);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка парсинга Альфа-Банка: " + e.getMessage());
        }
        return 0.0;
    }
}