package mb.spoofax.core.language.cli;

import mb.common.util.ADT;
import mb.common.util.ListView;
import mb.spoofax.core.language.command.arg.ArgConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

@ADT
public abstract class CliParam {
    interface Cases<R> {
        R option(String paramId, ListView<String> names, boolean negatable, @Nullable String label, @Nullable String description, @Nullable ArgConverter<?> converter);

        R positional(String paramId, int index, @Nullable String label, @Nullable String description, @Nullable ArgConverter<?> converter);
    }

    public static CliParam option(String paramId, ListView<String> names, boolean negatable, @Nullable String label, @Nullable String description, @Nullable ArgConverter<?> converter) {
        return CliParams.option(paramId, names, negatable, label, description, converter);
    }

    public static CliParam positional(String paramId, int index, @Nullable String label, @Nullable String description, @Nullable ArgConverter<?> converter) {
        return CliParams.positional(paramId, index, label, description, converter);
    }


    public abstract <R> R match(Cases<R> cases);

    public CliParams.CaseOfMatchers.TotalMatcher_Option caseOf() {
        return CliParams.caseOf(this);
    }

    public String getParamId() {
        return CliParams.getParamId(this);
    }

    public @Nullable String getLabel() {
        return CliParams.getLabel(this);
    }

    public @Nullable String getDescription() {
        return CliParams.getDescription(this);
    }

    public @Nullable ArgConverter<?> getConverter() {
        return CliParams.getConverter(this);
    }


    @Override public abstract int hashCode();

    @Override public abstract boolean equals(@Nullable Object obj);

    @Override public abstract String toString();
}
