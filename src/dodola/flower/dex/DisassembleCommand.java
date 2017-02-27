package dodola.flower.dex;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.validators.PositiveInteger;
import org.jf.util.jcommander.ExtendedParameter;
import org.jf.util.jcommander.ExtendedParameters;

import java.io.File;
import java.util.List;

@Parameters(commandDescription = "Disassembles a dex file.") @ExtendedParameters(
        commandName = "disassemble",
        commandAliases = { "dis", "d" }) public class DisassembleCommand extends DexInputCommand {

    @Parameter(names = { "-h", "-?", "--help" }, help = true,
            description = "Show usage information for this command.") private boolean help;

    @Parameter(names = { "-j", "--jobs" },
            description = "The number of threads to use. Defaults to the number of cores available.",
            validateWith = PositiveInteger.class) @ExtendedParameter(argumentNames = "n") private int jobs =
            Runtime.getRuntime().availableProcessors();

    @Parameter(names = { "-o", "--output" },
            description = "The directory to write the disassembled files to.") @ExtendedParameter(argumentNames = "dir")
    private String outputDir = "out";

    @Parameter(names = "--classes",
            description = "A comma separated list of classes. Only disassemble these classes")
    @ExtendedParameter(argumentNames = "classes") private List<String> classes = null;

    public DisassembleCommand(List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    public void run() {
        if (help || inputList == null || inputList.isEmpty()) {
            usage();
            return;
        }

        if (inputList.size() > 1) {
            System.err.println("Too many files specified");
            usage();
            return;
        }

        String input = inputList.get(0);
        loadDexFile(input);

        File outputDirectoryFile = new File(outputDir);
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                System.err.println("Can't create the output directory " + outputDir);
                System.exit(-1);
            }
        }

        if (!BackSmaliMain.disassembleDexFile(dexFile, outputDirectoryFile, jobs)) {
            System.exit(-1);
        }
    }
}
