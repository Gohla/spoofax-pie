package mb.spoofax.compiler.command;

import mb.spoofax.compiler.util.TypeInfo;
import mb.spoofax.core.language.command.CommandContextType;
import mb.spoofax.core.language.command.CommandExecutionType;
import org.immutables.value.Value;

import java.util.List;
import java.util.Set;

@Value.Immutable
public interface CommandDefRepr {
    class Builder extends ImmutableCommandDefRepr.Builder {}

    static Builder builder() {
        return new Builder();
    }


    TypeInfo type();

    TypeInfo taskDefType();

    TypeInfo argType();

    String displayName();

    Set<CommandExecutionType> supportedExecutionTypes();

    Set<CommandContextType> requiredContextTypes();

    List<ParamRepr> params();
}
