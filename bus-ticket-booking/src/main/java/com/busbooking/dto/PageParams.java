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

        List<Sort.Order> orders = new ArrayList<>();
        for (String entry : sort == null ? List.<String>of() : sort) {
            String[] parts = entry.split(",");
            String field = parts[0].trim();
            String path = fields.get(field);
            if (path == null) {
                throw invalid("Can't sort by '" + field + "'. Use one of: " + String.join(", ", fields.keySet()));
            }
            String dir = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
            if (!dir.equals("asc") && !dir.equals("desc")) throw invalid("Sort direction must be asc or desc");
            orders.add(dir.equals("asc") ? Sort.Order.asc(path) : Sort.Order.desc(path));
        }
        Sort order = orders.isEmpty() ? defaults : Sort.by(orders).and(Sort.by("id")); // id keeps pages stable
        return PageRequest.of(p - 1, s, order);
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, message);
    }
}
