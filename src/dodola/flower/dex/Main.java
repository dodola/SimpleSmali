

package dodola.flower.dex;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;
import org.jf.util.jcommander.Command;
import org.jf.util.jcommander.ExtendedCommands;
import org.jf.util.jcommander.ExtendedParameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

@ExtendedParameters(
        includeParametersInUsage = true,
        commandName = "ssmali",
        postfixDescription = "See ssmali help <command> for more information about a specific command")
public class Main extends Command {
    public static final String VERSION = loadVersion();

    @Parameter(names = { "--help", "-h", "-?" }, help = true,
            description = "Show usage information") private boolean help;

    @Parameter(names = { "--version", "-v" }, help = true,
            description = "Print the version of ssmali and then exit") public boolean version;

    private JCommander jc;

    public Main() {
        super(Lists.<JCommander>newArrayList());
    }

    @Override public void run() {
    }

    @Override protected JCommander getJCommander() {
        return jc;
    }

    public static void main(String[] args) {
        Main main = new Main();
        JCommander jc = new JCommander(main);
        main.jc = jc;
        jc.setProgramName("ssmali");
        List<JCommander> commandHierarchy = main.getCommandHierarchy();

        ExtendedCommands.addExtendedCommand(jc, new DisassembleCommand(commandHierarchy));

        jc.parse(args);

        if (main.version) {
            version();
        }

        if (jc.getParsedCommand() == null || main.help) {
            main.usage();
            return;
        }

        Command command = (Command) jc.getCommands().get(jc.getParsedCommand()).getObjects().get(0);
        command.run();
    }

    protected static void version() {
        System.out.println("simplesmali " + VERSION + " (https://github.com/dodola)");
        System.exit(0);
    }

    private static String loadVersion() {
        return "1.0";
    }
}
