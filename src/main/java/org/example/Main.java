package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            // Создаём клиент ProxyAPI
            ProxyAPIClient client = new ProxyAPIClient();
            
            // Получаем баланс в виде JSON строки
            String balanceJson = client.getBalance();
            System.out.println("Баланс (JSON): " + balanceJson);
            
            // Получаем баланс в виде объекта
            BalanceResponse balance = client.getBalanceResponse();
            System.out.println("Баланс: " + balance);
            
        } catch (Exception e) {
            System.err.println("Ошибка при получении баланса: " + e.getMessage());
            e.printStackTrace();
        }
    }
}