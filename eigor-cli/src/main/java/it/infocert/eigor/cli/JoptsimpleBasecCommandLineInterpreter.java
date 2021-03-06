package it.infocert.eigor.cli;

import com.google.common.collect.Sets;
import com.infocert.eigor.api.EigorApi;
import com.infocert.eigor.api.EigorApiBuilder;
import it.infocert.eigor.api.configuration.DefaultEigorConfigurationLoader;
import it.infocert.eigor.api.configuration.EigorConfiguration;
import it.infocert.eigor.cli.commands.ConversionCommand;
import it.infocert.eigor.cli.commands.HelpCommand;
import it.infocert.eigor.cli.commands.ReportFailuereCommand;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static java.nio.file.StandardOpenOption.READ;

/**
 * These are the accepted parameters.
 * <pre>
 * --input
 *   path to the file of the invoice to transform.
 * --source
 *   format of the invoice specified with '--input', as 'fatt-pa', 'ubl', ...
 * --output
 *   path of the folder where the converted invoice will be stored along with other files.
 * --target
 *   format of the transformed invoice, support the same formats of '--source'.
 * </pre>
 */
public class JoptsimpleBasecCommandLineInterpreter implements CommandLineInterpreter {

    public static final String INPUT = "input";
    public static final String OUTPUT = "output";
    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String FORCE = "force";
    public static final String INTERMEDIATE_VALIDATION = "intermediate-validation";
    private final EigorConfiguration configuration;
    private EigorApi api;
    private final static Logger log = LoggerFactory.getLogger(JoptsimpleBasecCommandLineInterpreter.class);

    public JoptsimpleBasecCommandLineInterpreter() {
        this.configuration = DefaultEigorConfigurationLoader.configuration();
    }

    @Override
    public CliCommand parseCommandLine(String[] args) {

        // Parses command line
        // ===================================================
        OptionParser parser = new OptionParser();
        parser.accepts(INPUT).withRequiredArg();
        parser.accepts(OUTPUT).withRequiredArg();
        parser.accepts(SOURCE).withRequiredArg();
        parser.accepts(TARGET).withRequiredArg();
        parser.accepts(FORCE);
        parser.accepts(INTERMEDIATE_VALIDATION);
        OptionSet options = parser.parse(args);

        String source;
        String target;

        // Validates all params
        // ===================================================

        // if all params are missing then print the help
        if (!options.hasOptions()) {
            return new HelpCommand();
        }

        // input: path to input invoice
        Path inputInvoice;
        Path outputFolder;
        InputStream invoiceInSourceFormat;
        boolean intermediateValidation;
        boolean forceConversion;
        {


            if (!options.has(INPUT)) {
                return new ReportFailuereCommand("Input file missing, please specify the path of the invoice to trasform with the --input parameter.");
            }


            inputInvoice = FileSystems.getDefault().getPath((String) options.valueOf(INPUT));


            if (Files.notExists(inputInvoice)) {
                return new ReportFailuereCommand("Input invoice '%s' does not exist.", inputInvoice);
            }


        }

        // output: path to output folder
        {

            if (!options.has(OUTPUT)) {
                return new ReportFailuereCommand("Output folder missing, please specify the output path with the --output parameter.", inputInvoice);
            }

            outputFolder = FileSystems.getDefault().getPath((String) options.valueOf("output"));
            if (Files.notExists(outputFolder)) {
                return new ReportFailuereCommand("Output folder '%s' does not exist.", outputFolder);
            }
        }


        // force flag: force conversion to continue even if there are errors
        {
            forceConversion = options.has(FORCE);
        }

        try {
            api = forceConversion ? new EigorApiBuilder().enableForce().build() : new EigorApiBuilder().build();
        } catch (Exception e) {
            return new ReportFailuereCommand("Error initializing API");
        }

        // source format: should be supported
        {

            if (!options.has(SOURCE)) {
                return new ReportFailuereCommand("Source format missing, please specify the format of the original invoice with the --source parameter.", inputInvoice);
            }

            source = (String) options.valueOf("source");
            Set<String> supportedSourceFormats = api.supportedSourceFormats();
            if (!supportedSourceFormats.contains(source)) {
                return new ReportFailuereCommand("Source format '%s' is not supported. Please choose one among: %s.", source, supportedSourceFormats);
            }
        }

        // target format: should be supported
        {

            if (!options.has(TARGET)) {
                return new ReportFailuereCommand("Target format missing, please specify the format of the target invoice with the --target parameter.", inputInvoice);
            }

            target = (String) options.valueOf("target");
            Set<String> supportedTargetFormats = api.supportedTargetFormats();
            if (!supportedTargetFormats.contains(target)) {
                return new ReportFailuereCommand("Target format '%s' is not supported. Please choose one among: %s.", target, supportedTargetFormats);
            }
        }

        // should validate the core model?
        {
            intermediateValidation = options.has(INTERMEDIATE_VALIDATION);
        }

        try {
            invoiceInSourceFormat = Files.newInputStream(inputInvoice, READ);
        } catch (IOException e) {
            return new ReportFailuereCommand(e.getMessage());
        }

        return new ConversionCommand.ConversionCommandBuilder()
                .setSourceFormat(source)
                .setTargetFormat(target)
                .setOutputFolder(outputFolder)
                .setInvoiceInSourceFormat(invoiceInSourceFormat)
                .setForceConversion(forceConversion)
                .setConfiguration(configuration)
                .setRunIntermediateValidation(intermediateValidation)
                .setApi(api)
                .setInvoiceInName(inputInvoice.toFile().getName())
                .build();
    }

    public Set<String> getSupportedSourceFormats() {
        if (api == null) {
            try {
                api = new EigorApiBuilder().build();
            } catch (Exception e) {
                return Sets.newHashSet();
            }
        }
        return api.supportedSourceFormats();
    }

    public Set<String> getSupportedTargetFormats() {
        if (api == null) {
            try {
                api = new EigorApiBuilder().build();
            } catch (Exception e) {
                return Sets.newHashSet();
            }
        }
        return api.supportedTargetFormats();
    }
}
