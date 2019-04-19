/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package uk.co.gluedit.ppd;

import io.restassured.RestAssured;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

public class App {

    public static void main(String[] args) {
        RestAssured.baseURI = "http://localhost:8080";
        ForkJoinPool pool = ForkJoinPool.commonPool();

        List<String> accounts = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            long randy = ThreadLocalRandom.current().nextLong(10000000000L, 99999999999L);
            accounts.add(Long.toString(randy));
        }
        System.out.println("There are " + accounts.size() + " accounts.");
        List<Eligibility> results = pool.invoke(new EligibilityTask(accounts));
        for (Eligibility res : results) {
            System.out.println("{\n" +
                               "  \"accountId\": " + "\"" + res.getAccountId() + "\",\n" +
                               "  \"isEligible\": " + res.isEligible() + "\n" +
                               "}");
        }
    }

    public String getGreeting() {
        return "Hullo from App!";
    }
}
