package com.microsoft.openai.samples.assistant.langchain4j.agent.openapi;

import com.microsoft.langchain4j.agent.AgentExecutionException;
import com.microsoft.langchain4j.agent.openapi.OpenAPIImporterMetadata;
import com.microsoft.langchain4j.agent.openapi.OpenAPIToolAgent;
import com.microsoft.langchain4j.agent.AgentMetadata;
import com.microsoft.openai.samples.assistant.invoice.DocumentIntelligenceInvoiceScanHelper;
import com.microsoft.openai.samples.assistant.langchain4j.agent.tools.InvoiceScanTool;

import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class PaymentAgent extends OpenAPIToolAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentAgent.class);

    private final Prompt agentPrompt;

    private static final String PAYMENT_AGENT_SYSTEM_MESSAGE = """
        you are a personal financial advisor who help the user with their recurrent bill payments. The user may want to pay the bill uploading a photo of the bill, or it may start the payment checking transactions history for a specific payee.
        For the bill payment you need to know the: bill id or invoice number, payee name, the total amount.
        If you don't have enough information to pay the bill ask the user to provide the missing information.
        If the user submit a photo, always ask the user to confirm the extracted data from the photo.
        Always check if the bill has been paid already based on payment history before asking to execute the bill payment.
        Ask for the payment method to use based on the available methods on the user account.
        if the user wants to pay using bank transfer, check if the payee is in account registered beneficiaries list. If not ask the user to provide the payee bank code.
        Check if the payment method selected by the user has enough funds to pay the bill. Don't use the account balance to evaluate the funds.
        Before submitting the payment to the system ask the user confirmation providing the payment details.
        Include in the payment description the invoice id or bill id as following: payment for invoice 1527248.
        When submitting payment always use the available functions to retrieve accountId, paymentMethodId.
        If the payment succeeds provide the user with the payment confirmation. If not provide the user with the error message.
        Use HTML list or table to display bill extracted data, payments, account or transaction details.
        Always use the below logged user details to retrieve account info:
        '{{loggedUserName}}'
        Current timestamp:
        '{{currentDateTime}}'
        Don't try to guess accountId,paymentMethodId from the conversation.When submitting payment always use functions to retrieve accountId, paymentMethodId.
    """;

    public PaymentAgent(ChatLanguageModel chatModel, DocumentIntelligenceInvoiceScanHelper documentIntelligenceInvoiceScanHelper, String loggedUserName, String transactionAPIUrl, String accountAPIUrl, String paymentsAPIUrl) {
        super(chatModel, List.of(
            new OpenAPIImporterMetadata("account-api", "account.yaml", accountAPIUrl),
            new OpenAPIImporterMetadata("transaction-api", "transaction-history.yaml", transactionAPIUrl),
            new OpenAPIImporterMetadata("payments-api", "payments.yaml", paymentsAPIUrl)
        ));

        extendToolMap(documentIntelligenceInvoiceScanHelper);

        if (loggedUserName == null || loggedUserName.isEmpty()) {
            throw new IllegalArgumentException("loggedUserName cannot be null or empty");
        }

        PromptTemplate promptTemplate = PromptTemplate.from(PAYMENT_AGENT_SYSTEM_MESSAGE);
        var datetimeIso8601 = ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toString();

        this.agentPrompt = promptTemplate.apply(Map.of(
            "loggedUserName", loggedUserName,
            "currentDateTime", datetimeIso8601
        ));
    }

    @Override
    public String getName() {
        return "PaymentAgent";
    }

    @Override
    public AgentMetadata getMetadata() {
        return new AgentMetadata(
            "Personal financial advisor for managing bill payments.",
            List.of("PayBill", "ScanInvoice", "SubmitPayment")
        );
    }

    @Override
    protected String getSystemMessage() {
        return agentPrompt.text();
    }


    protected void extendToolMap(DocumentIntelligenceInvoiceScanHelper documentIntelligenceInvoiceScanHelper) {
        try {
            Method scanInvoiceMethod = InvoiceScanTool.class.getMethod("scanInvoice", String.class);
            InvoiceScanTool invoiceScanTool = new InvoiceScanTool(documentIntelligenceInvoiceScanHelper);

            this.toolSpecifications.addAll(ToolSpecifications.toolSpecificationsFrom(InvoiceScanTool.class));
            this.toolExecutorMap.put("scanInvoice", new DefaultToolExecutor(invoiceScanTool, scanInvoiceMethod));
        } catch (NoSuchMethodException e) {
            throw new AgentExecutionException("scanInvoice method not found in InvoiceScanTool class. Align class code to be used by Payment Agent", e);
        }
    }
}
