package org.openmrs.module.bedmanagement.atomfeed;

import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.joda.time.DateTime;
import org.openmrs.module.bedmanagement.BedDetails;
import org.openmrs.module.bedmanagement.BedPatientAssignment;
import org.springframework.aop.AfterReturningAdvice;

import java.lang.reflect.Method;
import java.net.URI;
import java.sql.SQLException;
import java.util.UUID;

public class BedAssignmentAdvice implements AfterReturningAdvice {

    private static final String TEMPLATE = "/openmrs/ws/rest/v1/bedPatientAssignment/%s?v=custom:(uuid,startDatetime,endDatetime,bed,patient,encounter:(uuid,encounterDatetime,encounterType:(uuid,name),visit:(uuid,startDatetime,visitType)))";
    public static final String CATEGORY = "encounter";
    public static final String TITLE = "Bed-Assignment";
    private static final String ASSIGN_BED_METHOD = "assignPatientToBed";
    private static final String UNASSIGN_BED_METHOD = "unAssignPatientFromBed";

    private EventService eventService;

    public BedAssignmentAdvice() throws SQLException {
        AllEventRecordsJdbcImpl records = new AllEventRecordsJdbcImpl(new OpenMRSConnectionProvider());
        this.eventService = new EventServiceImpl(records);
    }

    public BedAssignmentAdvice(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        String execMethodName = method.getName();
        System.out.println("In Advice, method caleld " + execMethodName);
        if (execMethodName.equals(ASSIGN_BED_METHOD)) {
            BedDetails bedDetails = (BedDetails) returnValue;
            publishBedAssignment(bedDetails.getLastAssignment());
            publishBedAssignment(bedDetails.getCurrentAssignment());
        } else if (execMethodName.equals(UNASSIGN_BED_METHOD)) {
            BedDetails bedDetails = (BedDetails) returnValue;
            publishBedAssignment(bedDetails.getLastAssignment());
        }
    }

    private void publishBedAssignment(BedPatientAssignment assignment) {
        if (assignment != null) {
            String contents = String.format(TEMPLATE, assignment.getUuid());
            Event event = new Event(UUID.randomUUID().toString(), TITLE, DateTime.now(), (URI) null, contents, CATEGORY);
            eventService.notify(event);
        }
    }
}
