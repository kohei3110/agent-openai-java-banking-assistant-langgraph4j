package com.microsoft.openai.samples.assistant.langgraph4j;

import java.util.Arrays;
import java.util.List;

public enum Intent {
    TransactionHistoryAgent,
    AccountAgent,
    PaymentAgent;

    /**
     * Returns a list of all possible names of the {@code Intent} enum constants.
     *
     * @return an unmodifiable list containing the names of the {@code Intent} enum constants
     */
    public static List<String> names() {
        return Arrays.stream(Intent.values())
                .map(Enum::name)
                .toList();
    }
}
