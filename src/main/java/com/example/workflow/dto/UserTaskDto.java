/**
 * This class represents a Data Transfer Object (DTO) for a Camunda BPMN User Task.
 *  It contains the following information:
 *  id: the unique identifier of the user task
 *  name: the name of the user task
 *  assignee: the assignee expression of the user task
 *  candidateGroups: the list of candidate group expressions of the user task
 *  candidateUsers: the list of candidate user expressions of the user task
 *  formKey: the form key expression of the user task
 *  priority: the priority expression of the user task
 *  isDueDateSet: a boolean indicating if a due date is set for the user task
 *  dueDate: the due date expression of the user task
 *  isFollowUpDateSet: a boolean indicating if a follow-up date is set for the user task
 *  followUpDate: the follow-up date expression of the user task
 */

package com.example.workflow.dto;

import lombok.Value;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import java.util.List;

@Value
public class UserTaskDto {
    private String id;
    private String name;
    private String assignee;
    private String candidateGroups;
    private String candidateUsers;
    private String formKey;
    private String priority;
    private boolean isDueDateSet;
    private String dueDate;
    private boolean isFollowUpDateSet;
    private String followUpDate;


	


	/**
     * Creates a new UserTaskDto object based on the provided UserTask object.
     *
     * @param userTask the UserTask object to convert to a UserTaskDto
     * @return the newly created UserTaskDto object
     */

    public static UserTaskDto of(UserTask userTask) {
        return new UserTaskDto (
                userTask.getId(),
                userTask.getName(),
                userTask.getCamundaAssignee(),
                userTask.getCamundaCandidateGroups(),
                userTask.getCamundaCandidateUsers(),
                userTask.getCamundaFormKey(),
                userTask.getCamundaPriority(),
                userTask.getCamundaDueDate() != null,
                userTask.getCamundaDueDate(),
                userTask.getCamundaFollowUpDate() != null,
                userTask.getCamundaFollowUpDate());
    }
}