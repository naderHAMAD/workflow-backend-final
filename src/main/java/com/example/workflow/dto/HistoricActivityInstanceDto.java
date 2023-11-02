package com.example.workflow.dto;

import lombok.Value;
import org.camunda.bpm.engine.history.HistoricActivityInstance;

import java.util.Date;

@Value
public class HistoricActivityInstanceDto {
    private String id;
    private String processInstanceId;
    private String activityName;
    private String activityType;
    private Date startTime;
    private Date endTime;
    private Long durationInMillis;

    // Constructeur sans argument nécessaire pour la désérialisation JSON
    public HistoricActivityInstanceDto() {
        this.id = null;
        this.processInstanceId = null;
        this.activityName = null;
        this.activityType = null;
        this.startTime = null;
        this.endTime = null;
        this.durationInMillis = null;
    }

    public HistoricActivityInstanceDto(String id, String processInstanceId, String activityName, String activityType,
			Date startTime, Date endTime, Long durationInMillis) {
		super();
		this.id = id;
		this.processInstanceId = processInstanceId;
		this.activityName = activityName;
		this.activityType = activityType;
		this.startTime = startTime;
		this.endTime = endTime;
		this.durationInMillis = durationInMillis;
	}

	// Méthode de fabrication pour créer une instance de HistoricActivityInstanceDto
	public static HistoricActivityInstanceDto of(HistoricActivityInstance historicActivityInstance) {
        return new HistoricActivityInstanceDto(
                historicActivityInstance.getId(),
                historicActivityInstance.getProcessInstanceId(),
                historicActivityInstance.getActivityName(),
                historicActivityInstance.getActivityType(),
                historicActivityInstance.getStartTime(),
                historicActivityInstance.getEndTime(),
                historicActivityInstance.getDurationInMillis()
        );
    }
}
