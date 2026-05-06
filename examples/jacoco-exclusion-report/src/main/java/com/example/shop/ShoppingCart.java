package com.example.shop;

import com.arc_e_tect.sedr.utils.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;
import java.util.List;
import java.util.Objects;

/**
 * Example shopping-cart value object that demonstrates both forms of the
 * {@link ExcludeFromJacocoGeneratedCodeCoverage} annotation:
 *
 * <ul>
 *   <li>{@code toString()} – annotated <strong>without</strong> a justification.
 *       This is the pattern to avoid; prefer always supplying a justification so
 *       reviewers understand why the element is excluded from coverage.</li>
 *   <li>{@code equals()} and {@code hashCode()} – annotated <strong>with</strong>
 *       a justification, which is the recommended approach.</li>
 * </ul>
 */
public class ShoppingCart {

    private final String customerId;
    private final List<String> itemIds;

    public ShoppingCart(String customerId, List<String> itemIds) {
        this.customerId = customerId;
        this.itemIds    = List.copyOf(itemIds);
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<String> getItemIds() {
        return itemIds;
    }

    /**
     * Excluded from JaCoCo coverage <em>without</em> a justification.
     *
     * <p>Note: always prefer providing a justification so reviewers understand
     * why coverage is skipped for this element.
     */
    @ExcludeFromJacocoGeneratedCodeCoverage
    @Override
    public String toString() {
        return "ShoppingCart{customerId='" + customerId + "', items=" + itemIds + "}";
    }

    /**
     * Excluded from JaCoCo coverage <em>with</em> a justification.
     *
     * <p>The justification makes the intent clear to reviewers and satisfies
     * any ArchUnit convention checks that enforce non-blank justifications.
     */
    @ExcludeFromJacocoGeneratedCodeCoverage(
            justification = "IDE-generated equals – testing would only cover trivial boilerplate")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShoppingCart that)) return false;
        return Objects.equals(customerId, that.customerId)
                && Objects.equals(itemIds, that.itemIds);
    }

    @ExcludeFromJacocoGeneratedCodeCoverage(
            justification = "IDE-generated hashCode – testing would only cover trivial boilerplate")
    @Override
    public int hashCode() {
        return Objects.hash(customerId, itemIds);
    }
}
