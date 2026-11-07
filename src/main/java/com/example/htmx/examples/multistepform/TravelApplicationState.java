package com.example.htmx.examples.multistepform;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

@Component
@SessionScope
public class TravelApplicationState {
    private String applicantName = "";
    private String department = "";
    private String destination = "";
    private LocalDate startDate;
    private LocalDate endDate;
    private String purpose = "";
    private int currentStep = 1;
    private boolean completed;
    private String revision = newRevision();

    public synchronized Snapshot snapshot() {
        return new Snapshot(applicantName, department, destination, startDate, endDate, purpose,
                currentStep, completed, revision);
    }

    public synchronized void requireCurrentStep(int expectedStep, String expectedRevision) {
        requireRevision(expectedRevision);
        if (completed || currentStep != expectedStep) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    public synchronized void advanceApplicant(TravelApplicationForm form) {
        requireCurrentStep(1, form.getRevision());
        applicantName = form.getApplicantName();
        department = form.getDepartment();
        currentStep = 2;
        revision = newRevision();
    }

    public synchronized void advanceTravel(TravelApplicationForm form) {
        requireCurrentStep(2, form.getRevision());
        saveTravelFields(form);
        currentStep = 3;
        revision = newRevision();
    }

    public synchronized void returnToApplicant(TravelApplicationForm form) {
        requireCurrentStep(2, form.getRevision());
        saveTravelFields(form);
        currentStep = 1;
        revision = newRevision();
    }

    public synchronized void returnToTravel(String expectedRevision) {
        requireCurrentStep(3, expectedRevision);
        currentStep = 2;
        revision = newRevision();
    }

    public synchronized void complete(String expectedRevision) {
        requireCurrentStep(3, expectedRevision);
        completed = true;
        currentStep = 4;
        revision = newRevision();
    }

    public synchronized void reset(String expectedRevision) {
        requireRevision(expectedRevision);
        applicantName = "";
        department = "";
        destination = "";
        startDate = null;
        endDate = null;
        purpose = "";
        currentStep = 1;
        completed = false;
        revision = newRevision();
    }

    private void saveTravelFields(TravelApplicationForm form) {
        destination = form.getDestination();
        startDate = form.getStartDate();
        endDate = form.getEndDate();
        purpose = form.getPurpose();
    }

    private void requireRevision(String expectedRevision) {
        if (!revision.equals(expectedRevision)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    private static String newRevision() {
        return UUID.randomUUID().toString();
    }

    public record Snapshot(String applicantName, String department, String destination,
                           LocalDate startDate, LocalDate endDate, String purpose,
                           int currentStep, boolean completed, String revision) {}
}
