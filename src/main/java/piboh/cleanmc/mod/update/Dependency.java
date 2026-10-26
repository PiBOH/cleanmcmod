package piboh.cleanmc.mod.update;

import java.util.Objects;

/**
 * One entry of a Modrinth version's {@code dependencies} array.
 *
 * <p>{@code type} is Modrinth's {@code dependency_type}: {@code required},
 * {@code optional}, {@code incompatible} or {@code embedded}.</p>
 */
public record Dependency(String projectId, String versionId, String type) {

    public static final String REQUIRED = "required";
    public static final String INCOMPATIBLE = "incompatible";

    public Dependency {
        Objects.requireNonNull(projectId, "projectId");
    }

    public boolean required() {
        return REQUIRED.equals(type);
    }

    public boolean incompatible() {
        return INCOMPATIBLE.equals(type);
    }
}
