package com.busbooking.service.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.CoachDtos;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.AuditAction;
import com.busbooking.model.Coach;
import com.busbooking.model.CoachStatus;
import com.busbooking.model.Operator;
import com.busbooking.model.TripStatus;
import com.busbooking.repository.CoachRepository;
import com.busbooking.repository.Specs;
import com.busbooking.repository.TripRepository;
import com.busbooking.service.AuditService;
import com.busbooking.service.Changes;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Locale;
import java.util.Map;

/** The fleet. A coach with upcoming trips can't be retired, sent to maintenance or resized. */
@Service
public class CoachAdminService {

    static final String ENTITY = "Coach";
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "plateNumber", "plateNumber", "operator", "operator.name", "type", "type", "seatRows", "seatRows",
            "manufactureYear", "manufactureYear", "status", "status", "createdAt", "createdAt", "id", "id");

    private final CoachRepository coaches;
    private final TripRepository trips;
    private final OperatorAdminService operators;
    private final AuditService audit;

    public CoachAdminService(CoachRepository coaches, TripRepository trips, OperatorAdminService operators,
                             AuditService audit) {
        this.coaches = coaches;
        this.trips = trips;
        this.operators = operators;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<CoachDtos.Response> list(CoachDtos.Filter f, PageParams paging) {
        Specification<Coach> spec = Specs.<Coach>containsAny(f.q(), "plateNumber")
                .and(Specs.equalTo("operator.id", f.operatorId()))
                .and(Specs.equalTo("type", f.type()))
                .and(Specs.equalTo("status", f.status()))
                .and(Specs.atLeast("seatRows", f.minSeats() == null ? null : (f.minSeats() + 3) / 4))
                .and(Specs.atMost("seatRows", f.maxSeats() == null ? null : f.maxSeats() / 4))
                .and(Specs.atLeast("manufactureYear", f.yearFrom()))
                .and(Specs.atMost("manufactureYear", f.yearTo()));
        return PageResponse.of(coaches.findAll(spec, paging.toPageable(SORT_FIELDS, Sort.by("plateNumber"))),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public CoachDtos.Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CoachDtos.Response create(CoachDtos.Request req) {
        String plate = req.plateNumber().trim().toUpperCase(Locale.ROOT);
        if (coaches.existsByPlateNumberIgnoreCase(plate)) {
            throw new ApiException(ErrorCode.DUPLICATE, "A coach with plate " + plate + " already exists");
        }
        checkYear(req.manufactureYear());
        Operator op = activeOperator(req.operatorId());
        Coach coach = new Coach(plate, op, req.type(), req.seatRows(), req.manufactureYear());
        if (req.status() == CoachStatus.MAINTENANCE) coach.setStatus(CoachStatus.MAINTENANCE);
        else if (req.status() == CoachStatus.RETIRED) throw retireWithDelete();
        coach = coaches.save(coach);
        audit.record(ENTITY, coach.getId(), AuditAction.CREATE, plate + " (" + op.getName() + ")");
        return toResponse(coach);
    }

    @Transactional
    public CoachDtos.Response update(Long id, CoachDtos.Request req) {
        Coach coach = find(id);
        if (coach.getStatus() == CoachStatus.RETIRED) {
            throw new ApiException(ErrorCode.CONFLICT, "This coach is retired. Restore it before editing");
        }
        String plate = req.plateNumber().trim().toUpperCase(Locale.ROOT);
        if (!plate.equalsIgnoreCase(coach.getPlateNumber()) && coaches.existsByPlateNumberIgnoreCase(plate)) {
            throw new ApiException(ErrorCode.DUPLICATE, "A coach with plate " + plate + " already exists");
        }
        checkYear(req.manufactureYear());
        CoachStatus status = req.status() == null ? coach.getStatus() : req.status();
        if (status == CoachStatus.RETIRED) throw retireWithDelete();

        long upcoming = upcomingTrips(coach);
        boolean resized = req.seatRows() != coach.getSeatRows();
        boolean operatorChanged = !req.operatorId().equals(coach.getOperator().getId());
        boolean outOfService = status == CoachStatus.MAINTENANCE && coach.getStatus() == CoachStatus.ACTIVE;
        if (upcoming > 0 && (resized || operatorChanged || outOfService)) {
            throw new ApiException(ErrorCode.IN_USE, "This coach has " + upcoming
                    + " upcoming trip(s). Move or cancel them before changing its size, operator or status");
        }

        Changes c = new Changes();
        coach.setPlateNumber(c.track("plateNumber", coach.getPlateNumber(), plate));
        if (operatorChanged) {
            Operator op = activeOperator(req.operatorId());
            c.track("operator", coach.getOperator().getName(), op.getName());
            coach.setOperator(op);
        }
        coach.setType(c.track("type", coach.getType(), req.type()));
        coach.setSeatRows(c.track("seatRows", coach.getSeatRows(), req.seatRows()));
        coach.setManufactureYear(c.track("manufactureYear", coach.getManufactureYear(), req.manufactureYear()));
        coach.setStatus(c.track("status", coach.getStatus(), status));
        if (!c.isEmpty()) audit.record(ENTITY, id, AuditAction.UPDATE, c.toString());
        return toResponse(coach);
    }

    /** Soft delete: the coach is retired and kept for the history of its past trips. */
    @Transactional
    public CoachDtos.Response retire(Long id) {
        Coach coach = find(id);
        long upcoming = upcomingTrips(coach);
        if (upcoming > 0) {
            throw new ApiException(ErrorCode.IN_USE,
                    "This coach has " + upcoming + " upcoming trip(s). Move or cancel them first");
        }
        if (coach.getStatus() != CoachStatus.RETIRED) {
            coach.setStatus(CoachStatus.RETIRED);
            audit.record(ENTITY, id, AuditAction.DELETE, coach.getPlateNumber());
        }
        return toResponse(coach);
    }

    @Transactional
    public CoachDtos.Response restore(Long id) {
        Coach coach = find(id);
        if (coach.getStatus() == CoachStatus.RETIRED) {
            activeOperator(coach.getOperator().getId());
            coach.setStatus(CoachStatus.ACTIVE);
            audit.record(ENTITY, id, AuditAction.RESTORE, coach.getPlateNumber());
        }
        return toResponse(coach);
    }

    Coach find(Long id) {
        return coaches.findById(id).orElseThrow(() -> new NotFoundException("Coach " + id + " not found"));
    }

    private Operator activeOperator(Long operatorId) {
        Operator op = operators.find(operatorId);
        if (!op.isActive()) {
            throw new ApiException(ErrorCode.CONFLICT, op.getName() + " has been removed. Restore it first");
        }
        return op;
    }

    private long upcomingTrips(Coach coach) {
        return trips.countByCoachAndStatusAndDepartureTimeAfter(coach, TripStatus.SCHEDULED, LocalDateTime.now());
    }

    private static void checkYear(int year) {
        if (year > Year.now().getValue()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Manufacture year can't be in the future");
        }
    }

    private static ApiException retireWithDelete() {
        return new ApiException(ErrorCode.VALIDATION_FAILED, "To retire a coach, use DELETE");
    }

    private CoachDtos.Response toResponse(Coach c) {
        return CoachDtos.Response.from(c, upcomingTrips(c));
    }
}
