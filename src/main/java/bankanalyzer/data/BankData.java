package bankanalyzer.data;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankData {
    private String bankName;
    private double depositRate;
    private double loanRate;
    private double investmentReturn;
    private LocalDate date;
    private int termDays;
}