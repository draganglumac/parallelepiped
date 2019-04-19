package uk.co.gluedit.ppd;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class EligibilityTask extends RecursiveTask<List<Eligibility>> {

    private final String urlPattern = "/accounts/{{accountId}}/eligibility";

    private final Collection<String> accounts;

    public EligibilityTask(Collection<String> accountIds) {
        this.accounts = accountIds;
    }

    @Override
    protected List<Eligibility> compute() {
        if (splitCondition()) {
            return ForkJoinTask
                    .invokeAll(createSubtasks())
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
            Response response =
                    when()
                    .get(urlPattern.replace("{{accountId}}", account)).
            then()
                    .statusCode(200)
                    .body("accountId", equalTo(account))
            .extract()
                    .response();

            JsonPath json = new JsonPath(response.getBody().asString());
            results.add(new Eligibility(json.getString("accountId"), json.getBoolean("isEligible")));
        }
        return results;
    }

    private Collection<EligibilityTask> createSubtasks() {
        return null;
    }

    private boolean splitCondition() {
        return false;
    }
}
