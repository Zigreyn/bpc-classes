package com.bpcbt.lessons.spring.task1;

import com.bpcbt.lessons.spring.task1.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class SimpleController {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public SimpleController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void run() {
        transfer("Dmitry","Ilya",20L, "EUR");
        printAllDataFromDatabase();
    }


    private void select() {
        jdbcTemplate.query("select * from customers where id between ? and ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, 1);
                    preparedStatement.setInt(2, 2);
                },
                resultSet -> {
                    System.out.println(resultSet.getString("name"));
                }
        );
    }

    private Account getCustomerAccount(String name) {
        Account customerAccount = new Account();
        jdbcTemplate.query("select * from customers left outer join accounts on customers.account_id = " +
                        "accounts.id where customers.name = ?",
                preparedStatement -> preparedStatement.setString(1, name),
                resultSet -> {
                    customerAccount.setId(resultSet.getInt("account_id"));
                    customerAccount.setAccountNumber(resultSet.getInt("account_number"));
                    customerAccount.setCurrency(resultSet.getString("currency"));
                    customerAccount.setAmount(resultSet.getInt("amount"));
                }
        );
        return customerAccount;
    }

    private float getMultiplier(String currencyFrom, String currencyTo){

        if(!currencyFrom.equals(currencyTo)) {
            AtomicReference<Float> res = new AtomicReference<>((float) 0);
            jdbcTemplate.query("select * from currency_rates where currency_rates.currency_from = ?  and currency_rates.currency_to = ?",
                    preparedStatement -> {
                        preparedStatement.setString(1,currencyFrom);
                        preparedStatement.setString(2, currencyTo);
                    },
                    resultSet -> {
                        res.set(resultSet.getFloat("multiplier"));
                    }
            );
            return res.get();
        }
        return 1.0f;
    }

    private int calcTransfer(Account customerAccount, Long amount, String targetCurrency, int typeOfTransaction){
        float transferMultiplierCustomer = getMultiplier(targetCurrency , customerAccount.getCurrency());
        float amountInCustomerCurrency = amount * transferMultiplierCustomer;

        if(typeOfTransaction == -1){
            if(customerAccount.getAmount() >= amount) {
                int customerTempAmount = (int) Math.floor(customerAccount.getAmount() - amountInCustomerCurrency);
                return customerTempAmount;
            }else {
                System.out.println("Not enough");
                return customerAccount.getAmount();
            }
        }else if(typeOfTransaction == 1){
            int customerTempAmount = (int) Math.floor(customerAccount.getAmount() + amountInCustomerCurrency);
            return customerTempAmount;
        }else{
            System.out.println("Wrong transaction type");
            return customerAccount.getAmount();
        }
    }

    private void transfer(String customerFrom, String customerTo, Long amount, String targetCurrency) {
        Account customerFromAccount = getCustomerAccount(customerFrom);
        Account customerToAccount = getCustomerAccount(customerTo);
        System.out.println("First customer amount before transfer: " + customerFromAccount.getAmount() +" "
                + customerFromAccount.getCurrency());
        System.out.println("Second customer amount before transfer: " + customerToAccount.getAmount() + " "
                +customerToAccount.getCurrency()+"\n=======================================================");

        int customerFromBeforeTransfer = customerFromAccount.getAmount();
        customerFromAccount.setAmount(calcTransfer(customerFromAccount, amount, targetCurrency, -1));
        System.out.println("First customer amount after transfer: " + customerFromAccount.getAmount()
                + " " + customerFromAccount.getCurrency());

        if (customerFromBeforeTransfer!= customerFromAccount.getAmount()) {
            customerToAccount.setAmount(calcTransfer(customerToAccount, amount, targetCurrency, 1));
            System.out.println("Second customer amount after transfer: " + customerToAccount.getAmount()
                    + " " + customerToAccount.getCurrency());
        }

        int i = jdbcTemplate.update("update accounts set amount=? where accounts.id=?",
                preparedStatement -> {
                    preparedStatement.setInt(1, customerFromAccount.getAmount());
                    preparedStatement.setInt(2, customerFromAccount.getId());
                }
        );

        int j = jdbcTemplate.update("update accounts set amount = ? where accounts.id = ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, customerToAccount.getAmount());
                    preparedStatement.setInt(2, customerToAccount.getId());

                }
        );
    }

    private void printAllDataFromDatabase() {
        System.out.println("\n======================================================="+
                "\nBase status: ");
        jdbcTemplate.query("select * from customers inner join accounts on customers.account_id = accounts.id",
                resultSet -> {
                    System.out.println(resultSet.getString("name") +" "+
                            resultSet.getInt("amount") +" "+
                            resultSet.getString("currency"));
                }
        );
    }
}
