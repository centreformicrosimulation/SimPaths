package simpaths.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a current-period value that should be set to {@code null} at the start
 * of each simulation time interval.
 *
 * <p>Use this annotation on fields whose values are computed fresh within the current period
 * and must not carry over from the previous period. When
 * {@link UpdateManager#applyAnnotations(Object)} is called, all {@code @NullInitialised}
 * fields are cleared <em>after</em> all {@link Lag} assignments have been applied, so a field
 * annotated with both will end up {@code null}.</p>
 *
 * <p>This annotation is only valid on fields of reference (non-primitive) type. Applying it
 * to a primitive field will cause {@link UpdateManager} to throw an
 * {@link IllegalArgumentException}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 *   // Computed each period during LabourMarketAndIncomeUpdate; must be null at period start.
 *   @NullInitialised
 *   private Labour labourSupplyWeekly;
 * }</pre>
 *
 * @see Lag
 * @see UpdateManager
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NullInitialised {
}
