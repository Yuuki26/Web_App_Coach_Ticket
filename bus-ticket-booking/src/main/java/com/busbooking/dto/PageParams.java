package com.busbooking.dto;

import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Paging and sorting from the query string: {@code ?page=2&size=20&sort=price,asc&sort=departureTime,desc}.
 * <ul>
 *   <li>{@code page} starts at 1 (default 1)</li>
 *   <li>{@code size} is one of 10, 20, 50, 100 (default 20)</li>
 *   <li>{@code sort} may repeat to sort by several columns; only the listed fields are allowed</li>
 * </ul>
 */
public record PageParams(Integer page, Integer size, List<String> sort) {

    public static final Set<Integer> ALLOWED_SIZES = Set.of(10, 20, 50, 100);

    /**
     * @param fields  sort names the API accepts, mapped to entity property paths
     * @param defaults sort used when none is given
     */
    public Pageable toPageable(Map<String, String> fields, Sort defaults) {
        int p = page == null ? 1 : page;
        int s = size == null ? 20 : size;
        if (p < 1) throw invalid("page must be 1 or more");
        if (!ALLOWED_SIZES.contains(s)) throw invalid("size must be one of 10, 20, 50 or 100");

        // Spring splits "price,desc" into ["price", "desc"], so read the values as a token stream:
        // a field name starts a new order, "asc"/"desc" sets the direction of the one before it.
        List<Sort.Order> orders = new ArrayList<>();
        for (String raw : sort == null ? List.<String>of() : sort) {
            for (String token : raw.split(",")) {
                String t = token.trim();
                if (t.isEmpty()) continue;
                String lower = t.toLowerCase(Locale.ROOT);
                if (lower.equals("asc") || lower.equals("desc")) {
                    if (orders.isEmpty()) throw invalid("Put a field name before '" + t + "' in sort");
                    Sort.Order last = orders.remove(orders.size() - 1);
                    orders.add(lower.equals("asc") ? last.with(Sort.Direction.ASC) : last.with(Sort.Direction.DESC));
                    continue;
                }
                String path = fields.get(t);
                if (path == null) {
                    throw invalid("Can't sort by '" + t + "'. Use one of: " + String.join(", ", fields.keySet()));
                }
                orders.add(Sort.Order.asc(path));
            }
        }
        Sort order = orders.isEmpty() ? defaults : Sort.by(orders).and(Sort.by("id")); // id keeps pages stable
        return PageRequest.of(p - 1, s, order);
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, message);
    }
}
