package mb.spoofax.compiler.util;

import mb.resource.hierarchical.ResourcePath;
import org.immutables.value.Value;

import java.io.Serializable;

@Value.Immutable
public interface TypeInfo extends Serializable {
    class Builder extends ImmutableTypeInfo.Builder {}

    static Builder builder() {
        return new Builder();
    }

    static ImmutableTypeInfo of(String packageId, String classId) {
        return ImmutableTypeInfo.of(packageId, classId);
    }

    static ImmutableTypeInfo ofBoolean() {
        return ImmutableTypeInfo.of("", "boolean");
    }

    static ImmutableTypeInfo ofChar() {
        return ImmutableTypeInfo.of("", "char");
    }

    static ImmutableTypeInfo ofShort() {
        return ImmutableTypeInfo.of("", "short");
    }

    static ImmutableTypeInfo ofInt() {
        return ImmutableTypeInfo.of("", "int");
    }

    static ImmutableTypeInfo ofLong() {
        return ImmutableTypeInfo.of("", "long");
    }

    static ImmutableTypeInfo ofFloat() {
        return ImmutableTypeInfo.of("", "float");
    }

    static ImmutableTypeInfo ofDouble() {
        return ImmutableTypeInfo.of("", "double");
    }

    static ImmutableTypeInfo ofString() {
        return ImmutableTypeInfo.of("java.lang", "String");
    }


    @Value.Parameter String packageId();

    default String packagePath() {
        if(isPrimitive()) {
            throw new IllegalStateException("Cannot get package path, type '" + id() + "' is a primitive");
        }
        return Conversion.packageIdToPath(packageId());
    }

    default boolean isPrimitive() {
        return packageId().isEmpty();
    }

    @Value.Parameter String id();

    default String qualifiedId() {
        if(isPrimitive()) {
            return id();
        } else {
            return packageId() + "." + id();
        }
    }

    default String nullableQualifiedId() {
        if(isPrimitive()) {
            return "@Nullable " + id();
        } else {
            return packageId() + ".@Nullable " + id();
        }
    }


    default String fileName() {
        if(isPrimitive()) {
            throw new IllegalStateException("Cannot get file name, type '" + id() + "' is a primitive");
        }
        return id() + ".java";
    }

    default String qualifiedPath() {
        if(isPrimitive()) {
            throw new IllegalStateException("Cannot get qualified path, type '" + id() + "' is a primitive");
        }
        return packagePath() + "/" + fileName();
    }

    default ResourcePath file(ResourcePath base) {
        if(isPrimitive()) {
            throw new IllegalStateException("Cannot get file, type '" + id() + "' is a primitive");
        }
        return base.appendRelativePath(qualifiedPath());
    }


    default String asVariableId() {
        return Conversion.classIdToVariableId(id());
    }
}
