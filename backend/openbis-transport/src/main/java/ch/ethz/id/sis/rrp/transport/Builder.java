package ch.ethz.id.sis.rrp.transport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

abstract class Builder<T> {
    private final String[] args;
    private String name;
    private Function<Parser, Void> init;
    private Parser parser;

    public Builder(String[] args, String name, Function<Parser, Void> init) {
        this.args = args;
        this.name = name;
        this.init = init;
        this.parser = new Parser(ArgumentParsers.newFor(name).build());
    }

    private Optional<String> checkThat(Path path, Function<Path, Boolean> f, String msg) {
        return f.apply(path) ? Optional.empty() : Optional.of(String.format(msg, path));
    }

    @SafeVarargs
    private Optional<String> compose(Optional<String>... errors) {
        for (var result : errors) {
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    protected Optional<String> isInputFile(Path path) {
        return compose(
                checkThat(path, Files::exists, "File %s does not exist"),
                checkThat(path, Files::isReadable, "File %s is not readable"),
                checkThat(path, Files::isRegularFile, "%s is not a file"));
    }

    protected Optional<String> isOutputDirectory(Path path) {
        return compose(
                checkThat(path, Files::exists, "Directory %s does not exist"),
                checkThat(path, Files::isDirectory, "%s is not a directory"),
                checkThat(path, Files::isWritable, "Directory %s is not writable"));
    }

    protected Optional<String> isInputDirectory(Path path) {
        return compose(
                checkThat(path, Files::exists, "Directory %s does not exist"),
                checkThat(path, Files::isDirectory, "%s is not a directory"),
                checkThat(path, Files::isReadable, "Directory %s is not readable"));
    }

    protected void checkThat(Path path, Function<Path, Optional<String>> f)
            throws ArgumentParserException {
        var error = f.apply(path);
        if (error.isPresent()) {
            throw new ArgumentParserException(error.get(), parser.p());
        }
    }

    public abstract T createFrom(Namespace namespace) throws ArgumentParserException;

    public T build() {
        try {
            init.apply(parser);
            var namespace = parser.p().parseArgs(args);

            System.out.printf("%s called with following arguments:\n", name);
            parser.getArgumentList()
                    .forEach(arg -> System.out.printf("\t%s: %s\n", arg.textualName(),
                            namespace.get(arg.getDest())));
            System.out.println();

            return createFrom(namespace);
        } catch (ArgumentParserException e) {
            parser.p().handleError(e);
            System.exit(-1);
            return null;
        }
    }
}
