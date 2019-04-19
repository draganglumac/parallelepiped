package uk.co.gluedit.ppd;

public class Eligibility {

    public Eligibility(String accountId, boolean isEligible) {
        this.accountId = accountId;
        this.isEligible = isEligible;
    }

    private boolean isEligible;
    private String accountId;

    public boolean isEligible() {
        return isEligible;
    }

    public String getAccountId() {
        return accountId;
    }
}
