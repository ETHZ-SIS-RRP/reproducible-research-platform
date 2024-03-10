package ch.ethz.id.sis.rrp.transport;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;

class Parser {
    private ArgumentParser p;
    private List<Argument> args;

    public Parser(ArgumentParser p) {
        this.p = p;
        this.args = new ArrayList<>();
    }

    public ArgumentParser p() {
        return p;
    }

    public List<Argument> getArgumentList() {
        return this.args;
    }

    Argument addArgument(String name) {
        var argument = p.addArgument(name);
        args.add(argument);
        return argument;
    }

}
