package simpaths.model.annotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection-based processor that applies {@link Lag} and {@link NullInitialised} annotations
 * at the start of each simulation time interval.
 *
 * <h3>Processing order</h3>
 * <ol>
 *   <li><b>Read phase</b> — all {@link Lag}-annotated fields have their source values read
 *       (via field reference or getter) and cached <em>before</em> any writes occur.</li>
 *   <li><b>Lag write phase</b> — all cached lag values are written to their annotated fields.</li>
 *   <li><b>Null phase</b> — all {@link NullInitialised}-annotated fields are set to
 *       {@code null}.</li>
 * </ol>
 *
 * <p>Reading all sources before writing any lag field guarantees correct behaviour for
 * multi-period lag chains regardless of field declaration order. For example:</p>
 * <pre>
 *   L3 ← L2  (reads L2 before L2 is overwritten by the L2←L1 assignment)
 *   L2 ← L1  (reads L1 before L1 is overwritten by the L1←current assignment)
 *   L1 ← current
 * </pre>
 *
 * <h3>Typical usage in an entity's update method</h3>
 * <pre>{@code
 *   // In Person.updateLaggedVariables() or BenefitUnit.updateAttributes():
 *   UpdateManager.applyAnnotations(this);
 * }</pre>
 *
 * @see Lag
 * @see NullInitialised
 */
public final class UpdateManager {

    private UpdateManager() {}

    /**
     * Applies all {@link Lag} and {@link NullInitialised} annotations on the given entity,
     * walking the full class hierarchy.
     *
     * @param entity the object whose annotated fields should be updated; must not be {@code null}
     * @throws IllegalArgumentException if a {@link Lag} annotation specifies neither
     *         {@code field} nor {@code getter}, or if {@link NullInitialised} is placed on a
     *         primitive field
     * @throws RuntimeException wrapping any reflection error encountered during processing
     */
    public static void applyAnnotations(Object entity) {
        if (entity == null) throw new IllegalArgumentException("entity must not be null");

        Class<?> clazz = entity.getClass();
        List<LagAssignment> lagAssignments = new ArrayList<>();
        List<Field> nullFields = new ArrayList<>();

        // --- Discovery pass: collect all assignments without writing ---
        for (Field field : getAllFields(clazz)) {

            Lag lag = field.getAnnotation(Lag.class);
            if (lag != null) {
                Object value = resolveSource(entity, clazz, lag, field);
                lagAssignments.add(new LagAssignment(field, value));
            }

            if (field.isAnnotationPresent(NullInitialised.class)) {
                if (field.getType().isPrimitive()) {
                    throw new IllegalArgumentException(
                        "@NullInitialised cannot be applied to primitive field '"
                        + field.getName() + "' in " + clazz.getName()
                        + ". Use a wrapper type (e.g. Double instead of double).");
                }
                nullFields.add(field);
            }
        }

        // --- Lag write phase ---
        for (LagAssignment assignment : lagAssignments) {
            try {
                assignment.field().setAccessible(true);
                assignment.field().set(entity, assignment.value());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(
                    "Failed to write lag field '" + assignment.field().getName() + "'", e);
            }
        }

        // --- Null phase ---
        for (Field field : nullFields) {
            try {
                field.setAccessible(true);
                field.set(entity, null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(
                    "Failed to null-initialise field '" + field.getName() + "'", e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static Object resolveSource(Object entity, Class<?> clazz, Lag lag, Field lagField) {
        if (!lag.getter().isEmpty()) {
            return invokeGetter(entity, clazz, lag.getter(), lagField);
        }
        if (!lag.field().isEmpty()) {
            return readField(entity, clazz, lag.field(), lagField);
        }
        throw new IllegalArgumentException(
            "@Lag on field '" + lagField.getName() + "' in " + clazz.getName()
            + " must specify either 'field' or 'getter'.");
    }

    private static Object invokeGetter(Object entity, Class<?> clazz, String getterName, Field lagField) {
        try {
            Method method = findMethod(clazz, getterName);
            method.setAccessible(true);
            return method.invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to invoke getter '" + getterName + "' for @Lag field '"
                + lagField.getName() + "' in " + clazz.getName(), e);
        }
    }

    private static Object readField(Object entity, Class<?> clazz, String fieldName, Field lagField) {
        try {
            Field source = findField(clazz, fieldName);
            source.setAccessible(true);
            return source.get(entity);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to read field '" + fieldName + "' for @Lag field '"
                + lagField.getName() + "' in " + clazz.getName(), e);
        }
    }

    /** Returns all declared fields across the full class hierarchy (class → superclass). */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                fields.add(f);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /** Finds a field by name, walking up the class hierarchy. */
    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(
            "Field '" + name + "' not found in class hierarchy of " + clazz.getName());
    }

    /** Finds a no-argument method by name, walking up the class hierarchy. */
    private static Method findMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(
            "Method '" + name + "()' not found in class hierarchy of " + clazz.getName());
    }

    /** Immutable pair holding a lag field and its resolved source value. */
    private record LagAssignment(Field field, Object value) {}
}
