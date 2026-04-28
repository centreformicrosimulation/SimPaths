package simpaths.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a lagged value to be updated at the start of each simulation time interval.
 *
 * <p>At each period update the annotated field will receive the value of its specified source,
 * read from the same object before any lag assignments are written. This read-first semantics
 * ensures correct behaviour for multi-period lag chains (e.g. L3 ← L2 ← L1 ← current)
 * regardless of field declaration order.</p>
 *
 * <p>Specify the source using exactly one of:</p>
 * <ul>
 *   <li>{@link #field()} — name of a field on the same object to copy directly.</li>
 *   <li>{@link #getter()} — name of a no-argument getter method on the same object to invoke.</li>
 * </ul>
 *
 * <p>If both are specified, {@code getter} takes precedence. If neither is specified,
 * {@link UpdateManager#applyAnnotations(Object)} will throw an {@link IllegalArgumentException}.</p>
 *
 * <p>Example — direct field reference:</p>
 * <pre>{@code
 *   @Lag(field = "labC4")
 *   private Les_c4 labC4L1;
 * }</pre>
 *
 * <p>Example — getter method:</p>
 * <pre>{@code
 *   @Lag(getter = "getHouseholdStatus")
 *   private Indicator demStatusHhL1;
 * }</pre>
 *
 * <p>Example — multi-period lag chain (L2 ← L1, then L1 ← current getter):</p>
 * <pre>{@code
 *   @Lag(field = "yEmpPersGrossMonthL1")
 *   private Double yEmpPersGrossMonthL2;
 *
 *   @Lag(getter = "getYEmpPersGrossMonth")
 *   private Double yEmpPersGrossMonthL1;
 * }</pre>
 *
 * @see NullInitialised
 * @see UpdateManager
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Lag {

    /**
     * Name of the field on the same object whose value should be copied into the annotated field.
     * Ignored if {@link #getter()} is also specified.
     */
    String field() default "";

    /**
     * Name of the no-argument method on the same object to invoke to obtain the source value.
     * Takes precedence over {@link #field()} when both are specified.
     */
    String getter() default "";
}
