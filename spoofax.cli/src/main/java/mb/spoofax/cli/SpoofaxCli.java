package mb.spoofax.cli;

import mb.pie.api.PieSession;
import mb.resource.ResourceService;
import mb.spoofax.core.language.LanguageComponent;
import mb.spoofax.core.language.LanguageInstance;
import mb.spoofax.core.language.cli.CliCommand;
import mb.spoofax.core.language.cli.CliParam;
import mb.spoofax.core.language.command.CommandContext;
import mb.spoofax.core.language.command.CommandDef;
import mb.spoofax.core.language.command.arg.ArgConverter;
import mb.spoofax.core.language.command.arg.ArgConverters;
import mb.spoofax.core.language.command.arg.Param;
import mb.spoofax.core.language.command.arg.ParamDef;
import mb.spoofax.core.language.command.arg.RawArgFromProviders;
import org.checkerframework.checker.nullness.qual.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.ISetter;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

import javax.inject.Inject;
import java.io.Serializable;

public class SpoofaxCli {
    private final ResourceService resourceService;
    private final ArgConverters argConverters;

    @Inject
    public SpoofaxCli(ResourceService resourceService, ArgConverters argConverters) {
        this.resourceService = resourceService;
        this.argConverters = argConverters;
    }

    public int run(String[] args, LanguageComponent languageComponent) {
        final LanguageInstance languageInstance = languageComponent.getLanguageInstance();
        try(final PieSession pieSession = languageComponent.newPieSession()) {
            final CommandSpec commandSpec = toCommandSpec(languageInstance.getCliCommand(), pieSession);
            final CommandLine commandLine = new CommandLine(commandSpec);
            for(ArgConverter<?> converter : argConverters.allConverters.values()) {
                registerConverter(commandLine, new TypeConverter<>(converter));
            }
            return commandLine.execute(args);
        }
    }

    private CommandSpec toCommandSpec(CliCommand cliCommand, PieSession pieSession) {
        final CommandSpec commandSpec;
        final String name = cliCommand.getName();
        final @Nullable CommandDef<?> commandDef = cliCommand.getCommandDef();
        if(commandDef != null) { // CLI command with actual command definition that can be executed.
            final CommandRunner<?> commandRunner = new CommandRunner<>(resourceService, pieSession, commandDef, argConverters);
            commandSpec = CommandSpec.wrapWithoutInspection(commandRunner);
            final ParamDef commandParams = commandDef.getParamDef();
            for(CliParam cliParam : cliCommand.getParams()) {
                commandSpec.add(cliParam.caseOf()
                    .option((paramId, names, negatable, label, description, converter) -> {
                        final @Nullable Param param = commandParams.params.get(paramId);
                        if(param == null) {
                            throw new RuntimeException("Could not create command '" + name + "', no parameter was found for ID '" + paramId + "'");
                        }
                        final OptionSpec.Builder builder = OptionSpec.builder(names.toArray(new String[0]));
                        builder.type(param.getType());
                        builder.required(param.isRequired());
                        builder.negatable(negatable);
                        final @Nullable Serializable provided = RawArgFromProviders.get(param, new CommandContext());
                        if(provided != null) {
                            builder.defaultValue(provided.toString());
                            builder.showDefaultValue(CommandLine.Help.Visibility.ALWAYS);
                        }
                        // noinspection ConstantConditions (label can really be null)
                        if(label != null) {
                            builder.paramLabel(label);
                        }
                        // noinspection ConstantConditions (label can really be null)
                        if(description != null) {
                            builder.description(description);
                        }
                        // noinspection ConstantConditions (converter can really be null)
                        if(converter != null) {
                            builder.converters(new TypeConverter<>(converter));
                        }
                        builder.setter(new ISetter() {
                            @Override
                            public <T> @Nullable T set(@Nullable T value) throws IllegalArgumentException {
                                commandRunner.set(paramId, value);
                                return null;
                            }
                        });
                        return (ArgSpec)builder.build();
                    })
                    .positional((paramId, index, label, description, converter) -> {
                        final @Nullable Param param = commandParams.params.get(paramId);
                        if(param == null) {
                            throw new RuntimeException("Could not create command '" + name + "', no parameter was found for ID '" + paramId + "'");
                        }
                        final PositionalParamSpec.Builder builder = PositionalParamSpec.builder();
                        builder.index(Integer.toString(index));
                        builder.type(param.getType());
                        builder.required(param.isRequired());
                        final @Nullable Serializable provided = RawArgFromProviders.get(param, new CommandContext());
                        if(provided != null) {
                            builder.defaultValue(provided.toString());
                            builder.showDefaultValue(CommandLine.Help.Visibility.ALWAYS);
                        }
                        // noinspection ConstantConditions (label can really be null)
                        if(label != null) {
                            builder.paramLabel(label);
                        }
                        // noinspection ConstantConditions (label can really be null)
                        if(description != null) {
                            builder.description(description);
                        }
                        // noinspection ConstantConditions (converter can really be null)
                        if(converter != null) {
                            builder.converters(new TypeConverter<>(converter));
                        }
                        builder.setter(new ISetter() {
                            @Override
                            public <T> @Nullable T set(@Nullable T value) throws IllegalArgumentException {
                                commandRunner.set(paramId, value);
                                return null;
                            }
                        });
                        return (ArgSpec)builder.build();
                    }));
            }
        } else {
            // CLI command without actual command to run: just a container for sub-commands.
            final DelegateRunnable delegateRunnable = new DelegateRunnable();
            commandSpec = CommandSpec.wrapWithoutInspection(delegateRunnable);
            // Show usage when this command is executed.
            delegateRunnable.setRunnable(() -> {
                new CommandLine(commandSpec).usage(System.out);
            });
        }

        // Standard CLI command options.
        commandSpec.mixinStandardHelpOptions(true);
        final @Nullable String description = cliCommand.getDescription();
        commandSpec.name(name);
        if(description != null) {
            commandSpec.usageMessage().description(description);
        }

        // Add sub-commands.
        for(CliCommand subCommand : cliCommand.getSubCommands()) {
            commandSpec.addSubcommand(subCommand.getName(), toCommandSpec(subCommand, pieSession));
        }

        return commandSpec;
    }

    private static <T extends Serializable> void registerConverter(CommandLine commandLine, TypeConverter<T> converter) {
        commandLine.registerConverter(converter.getOutputClass(), converter);
    }
}
