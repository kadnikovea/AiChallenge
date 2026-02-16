package org.example;

/**
 * Класс для представления ответа с балансом
 */
public class BalanceResponse {
    private double balance;
    private String currency;
    
    public BalanceResponse() {
    }
    
    public BalanceResponse(double balance, String currency) {
        this.balance = balance;
        this.currency = currency;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    /**
     * Простой парсер JSON ответа
     * В реальном проекте лучше использовать библиотеку типа Jackson или Gson
     */
    public static BalanceResponse fromJson(String json) {
        // Удаляем пробелы и фигурные скобки
        json = json.trim().replaceAll("[{}]", "");
        
        BalanceResponse response = new BalanceResponse();
        
        // Простой парсинг (для более сложных случаев используйте библиотеку)
        if (json.contains("\"balance\"")) {
            String balanceStr = json.substring(json.indexOf("\"balance\"") + 10);
            balanceStr = balanceStr.substring(0, balanceStr.indexOf(","));
            balanceStr = balanceStr.replaceAll("\"", "").trim();
            try {
                response.balance = Double.parseDouble(balanceStr);
            } catch (NumberFormatException e) {
                // Игнорируем ошибку парсинга
            }
        }
        
        if (json.contains("\"currency\"")) {
            String currencyStr = json.substring(json.indexOf("\"currency\"") + 11);
            currencyStr = currencyStr.substring(0, currencyStr.indexOf(",") != -1 ? 
                    currencyStr.indexOf(",") : currencyStr.length());
            currencyStr = currencyStr.replaceAll("\"", "").trim();
            response.currency = currencyStr;
        }
        
        return response;
    }
    
    @Override
    public String toString() {
        return "BalanceResponse{" +
                "balance=" + balance +
                ", currency='" + currency + '\'' +
                '}';
    }
}
