package com.example.htmx.examples.multistepform;

import jakarta.validation.groups.Default;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class TravelApplicationForm {
    public interface SharedFields extends Default {}
    public interface ApplicantStep extends SharedFields {}
    public interface TravelStep extends SharedFields {}

    @NotBlank(message = "申請者名を入力してください。", groups = ApplicantStep.class)
    @Size(max = 40, message = "申請者名は40文字以内で入力してください。", groups = SharedFields.class)
    private String applicantName;

    @NotBlank(message = "部署を入力してください。", groups = ApplicantStep.class)
    @Size(max = 40, message = "部署は40文字以内で入力してください。", groups = SharedFields.class)
    private String department;

    @NotBlank(message = "行き先を入力してください。", groups = TravelStep.class)
    @Size(max = 80, message = "行き先は80文字以内で入力してください。", groups = SharedFields.class)
    private String destination;

    @NotNull(message = "開始日を入力してください。", groups = TravelStep.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "終了日を入力してください。", groups = TravelStep.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotBlank(message = "目的を入力してください。", groups = TravelStep.class)
    @Size(max = 200, message = "目的は200文字以内で入力してください。", groups = SharedFields.class)
    private String purpose;

    @NotBlank(message = "画面が古くなっています。再読み込みしてください。", groups = SharedFields.class)
    @Size(max = 36, message = "画面が古くなっています。再読み込みしてください。", groups = SharedFields.class)
    private String revision;

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }

    static TravelApplicationForm from(TravelApplicationState.Snapshot snapshot) {
        var form = new TravelApplicationForm();
        form.setApplicantName(snapshot.applicantName());
        form.setDepartment(snapshot.department());
        form.setDestination(snapshot.destination());
        form.setStartDate(snapshot.startDate());
        form.setEndDate(snapshot.endDate());
        form.setPurpose(snapshot.purpose());
        form.setRevision(snapshot.revision());
        return form;
    }
}
