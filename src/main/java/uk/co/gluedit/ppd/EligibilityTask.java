package uk.co.gluedit.ppd;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

public class EligibilityTask extends RecursiveTask<List<Eligibility>> {

    private final int threshold = 16;

    private final String urlPattern = "/accounts/{{accountId}}/eligibility";

    private final List<String> accounts;
    private final CircuitBreaker breaker;

    EligibilityTask(List<String> accountIds, CircuitBreaker breaker) {
        this.breaker = breaker;
        this.accounts = accountIds;
    }

    @Override
    protected List<Eligibility> compute() {
        if (splitCondition()) {
            Collection<EligibilityTask> subtasks = createSubtasks();
            return ForkJoinTask
                    .invokeAll(subtasks)
                    .stream()
                    .map(ForkJoinTask::join)
                    .flatMap(pps -> pps.stream().filter(Eligibility::isEligible))
                    .collect(Collectors.toList());
        } else {
            return process()
                    .stream()
                    .filter(Eligibility::isEligible)
                    .collect(Collectors.toList());
        }
    }

    private Collection<Eligibility> process() {
        List<Eligibility> results = new ArrayList<>();
        for (String account : accounts) {
            if (breaker.breakCircuit) {
                return results;
            }

            Response response =
                    when().
                            get(urlPattern.replace("{{accountId}}", account)).
                    then().
                            statusCode(200).
                            body("accountId", equalTo(account)).
                    extract().
                            response();

            JsonPath json = new JsonPath(response.getBody().asString());
            if (json.getBoolean("isEligible")) {
                results.add(
                        new Eligibility(
                                json.getString("accountId"),
                                json.getBoolean("isEligible")
                        )
                );
                breaker.breakCircuit = true;
                return results;
            }
        }
        return results;
    }

    private Collection<EligibilityTask> createSubtasks() {
        List<EligibilityTask> tasks = new ArrayList<>();
        int start = 0;
        int end = threshold;

        while (end < accounts.size()) {
            tasks.add(new EligibilityTask(accounts.subList(start, end), breaker));
            start = end;
            end = Math.min(end + threshold, accounts.size());
        }

        return tasks;
    }

    private boolean splitCondition() {
        return accounts.size() > threshold;
    }
}
