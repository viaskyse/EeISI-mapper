package com.infocert.eigor.api;

import com.google.common.base.Preconditions;
import it.infocert.eigor.api.ConversionResult;
import it.infocert.eigor.api.IConversionIssue;
import it.infocert.eigor.api.conversion.AbstractConversionCallback;
import it.infocert.eigor.api.conversion.ConversionContext;
import it.infocert.eigor.model.core.dump.DumpVisitor;
import it.infocert.eigor.model.core.model.BG0000Invoice;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static it.infocert.eigor.test.Utils.invoiceAsStream;
import static org.junit.Assert.assertTrue;

/** A support class that ease the management of conversions. */
public class ConversionUtil {

    private final EigorApi api;

    public ConversionUtil(EigorApi api) {
        this.api = Preconditions.checkNotNull( api );
    }

    ImprovedConversionResult<byte[]> assertConversionWithoutErrors(String invoice, String source, String target) {
        Predicate<IConversionIssue> predicate = new KeepAll();
        return assertConversionWithoutErrors(invoice, source, target, predicate);
    }

    ImprovedConversionResult<byte[]> assertConversionWithoutErrors(String invoiceStreamSource, String source, String target, Predicate<IConversionIssue> errorsToKeep) {
        InputStream invoiceStream = invoiceAsStream(invoiceStreamSource);

        return assertConversionWithoutErrors(invoiceStream, source, target, errorsToKeep);
    }

    ImprovedConversionResult<byte[]> assertConversionWithoutErrors(InputStream invoiceStream, String source, String target, Predicate<IConversionIssue> errorsToKeep) {
        final BG0000Invoice[] intermediateInvoice = new BG0000Invoice[1];

        class MyConversionCallback extends AbstractConversionCallback {
            @Override
            public void onTerminatedConversion(ConversionContext ctx) throws Exception {
                ConversionResult<BG0000Invoice> toCenResult = ctx.getToCenResult();
                if(toCenResult.hasResult()){
                    intermediateInvoice[0] = toCenResult.getResult();
                }
            }
        }

        MyConversionCallback mcc = new MyConversionCallback();

        ConversionResult<byte[]> convert = api.convert(source, target, invoiceStream, "invoice", mcc);
        ImprovedConversionResult<byte[]> tmp = new ImprovedConversionResult<>(intermediateInvoice[0], convert.getIssues(), convert.getResult());

        List<IConversionIssue> issues = convert.getIssues().stream().filter( errorsToKeep ).collect(Collectors.toList());

        String messageInCaseOfFailedTest = buildMsgForFailedAssertion(convert, errorsToKeep, intermediateInvoice[0]);

        assertTrue(messageInCaseOfFailedTest, issues.isEmpty() );
        return tmp;
    }

    String buildMsgForFailedAssertion(ConversionResult<byte[]> convert, Predicate<IConversionIssue> predicate, BG0000Invoice intermediateCenInvoice){

        List<IConversionIssue> conversionIssues = convert.getIssues().stream().filter( predicate ).collect(Collectors.toList());

        StringBuilder issuesDescription = new StringBuilder();
        boolean areThereIssues = !conversionIssues.isEmpty();
        if(areThereIssues){

            issuesDescription.append("\n\n====== " + conversionIssues.size() + " Issues: ======\n\n");

            issuesDescription.append(describeConversionIssues(conversionIssues));


            issuesDescription.append("\n\n====== Converted Invoice: ======\n\n");

            issuesDescription.append(describeConvertedInvoice(convert))
                    .append("\n\n");

            issuesDescription.append("\n\n====== Intermediate CEN Invoice: ======\n\n");

            if(intermediateCenInvoice!=null) {
                issuesDescription.append(describeIntermediateInvoice(intermediateCenInvoice));
            }else{
                issuesDescription.append("The conversion failed before producing any intermediate CEN invoice.");
            }
        }
        return issuesDescription.toString();
    }

    public static String describeIntermediateInvoice(BG0000Invoice cenInvoice) {
        DumpVisitor v = new DumpVisitor();
        cenInvoice.accept( v );
        return v.toString();
    }

    public static String describeIntermediateInvoice(ImprovedConversionResult<byte[]> conversionResult) {
        return describeIntermediateInvoice(conversionResult.getCenInvoice());
    }

    public static String describeConvertedInvoice(ConversionResult<byte[]> convert) {
        return new String(convert.getResult());
    }

    public static String describeConversionIssues(Iterable<IConversionIssue> conversionIssues) {
        StringBuilder issuesDescriptionBuilder = new StringBuilder();
        for (IConversionIssue issue : conversionIssues) {
            issuesDescriptionBuilder
                    .append( issue.getMessage() )
                    .append("\n")
                    .append("   ►►► ")
                    .append( issue.isError() ? "ERROR" : "WARN" )
                    .append("\n");

            if(issue.getCause()!=null) {
                issuesDescriptionBuilder
                        .append("   ►►► ")
                        .append(issue.getCause().getMessage())
                        .append("\n");

                StringWriter sw = new StringWriter();
                issue.getCause().printStackTrace( new PrintWriter(sw) );
                issuesDescriptionBuilder
                        .append("   ►►► ")
                        .append(sw.toString())
                        .append("\n");



            }
            issuesDescriptionBuilder.append("\n\n");
        }
        return issuesDescriptionBuilder.toString();
    }

    public static Predicate<IConversionIssue> keepErrorsNotWarnings() {
        return new KeepErrorsNotWarnings();
    }

    /** This filter will ignore any error, it corresponds to a "forced" conversion. */
    public static Predicate<IConversionIssue> ignoreAll() {
        return new DiscardAll();
    }

    static class DiscardAll implements  Predicate<IConversionIssue> {
        @Override
        public boolean test(IConversionIssue iConversionIssue) {
            return false;
        }
    }

    static class KeepByErrorCode implements Predicate<IConversionIssue> {
        private final String errorCode;

        public KeepByErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        @Override
        public boolean test(@Nullable IConversionIssue input) {
            return input.getErrorMessage() != null
                    && input.getErrorMessage().getErrorCode()!=null
                    && input.getErrorMessage().getErrorCode().toString().equals(errorCode);
        }
    }

    static class KeepAll implements Predicate<IConversionIssue> {

        @Override
        public boolean test(@Nullable IConversionIssue input) {
            return true;
        }
    }

    private static class KeepErrorsNotWarnings implements Predicate<IConversionIssue> {

        @Override
        public boolean test(@Nullable IConversionIssue issue) {

            return !issue.isWarning();
        }
    }

    static class KeepXSDErrorsOnly implements Predicate<IConversionIssue> {

        @Override
        public boolean test(@Nullable IConversionIssue input) {
            try{
                return input.getErrorMessage().getErrorCode().getAction().toString().equals("XSD_VALIDATION");
            }catch(NullPointerException npe){
                return false;
            }
        }
    }

    public static class ImprovedConversionResult<T> extends ConversionResult<T> {

        private final BG0000Invoice cenInvoice;

        public ImprovedConversionResult(BG0000Invoice cenInvoice, List<IConversionIssue> issues, T result) {
            super(issues, result);
            this.cenInvoice = cenInvoice;
        }

        public ImprovedConversionResult(BG0000Invoice cenInvoice, T result) {
            super(result);
            this.cenInvoice = cenInvoice;
        }

        public BG0000Invoice getCenInvoice() {
            return cenInvoice;
        }
    }

}
