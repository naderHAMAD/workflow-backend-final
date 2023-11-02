/**
 * Service class for managing Camunda BPMN tasks.
 */

package com.example.workflow.service;

import  com.google.gson.JsonArray;
import  com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.example.workflow.dto.*;

import com.example.workflow.entity.Form;
import com.example.workflow.entity.Workflow;
import com.example.workflow.repository.FormRepository;
import com.example.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class TasksService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ProcessEngine processEngine;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private FormRepository formRepository;

    private String updatedDeploymentId = null;

    public TasksService(
            RepositoryService repositoryService,
            RuntimeService runtimeService,
            TaskService taskService,
            HistoryService historyService,
            ProcessEngine processEngine
    ) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.processEngine = processEngine;
    }
    /**
     * Retrieves a list of active tasks.
     * @return a list of active tasks as TaskDto objects.
     */
    public List<TaskDto> getActiveTasks() {
        return taskService
                .createTaskQuery()
                .active()
                .initializeFormKeys()
                .list()
                .stream()
                .map(TaskDto::of)
                .collect(toList());
    }


    /**
     * Retrieves a list of active User tasks for a simple user.
     * @return a list of active tasks as TaskDto objects.
     */
    public List<TaskDto> getSimpleUserActiveUserTasksByAssignee() {
        return taskService.createTaskQuery()
                .active()
                .taskAssignee("USER")
                .initializeFormKeys()
                .list()
                .stream()
                .map(TaskDto::of)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of active User tasks for a validator.
     * @return a list of active tasks as TaskDto objects.
     */
    public List<TaskDto> getValidatorActiveUserTasksByAssignee() {
        return taskService.createTaskQuery()
                .active()
                .taskAssignee("VALIDATOR")
                .initializeFormKeys()
                .list()
                .stream()
                .map(TaskDto::of)
                .collect(Collectors.toList());
    }

    /**
     * Claims a task with the given ID.
     * @param taskId the ID of the task to claim.
     */
    public void claimTask(String taskId) {
        String userId = taskService.createTaskQuery().taskId(taskId).singleResult().getAssignee();
        taskService.claim(taskId, userId);
    }

    /**
     * Completes a task with the given ID and variables.
     * @param taskId    the ID of the task to complete.
     * @param input     the value of the selected radio button.
     */
    public void completeTask(String taskId, Integer input) {
        // Claim the user task
        taskService.claim(taskId,null);

        // Set the "input" process variable to the selected value
        runtimeService.setVariable(taskService.createTaskQuery().taskId(taskId).singleResult().getProcessInstanceId(), "input", input);

        // Complete the user task
        taskService.complete(taskId);
    }

    /**
     * Retrieves a list of service task definitions for a process definition with the given deployment ID.
     * @param deploymentId the ID of the process definition deployment.
     * @return a list of service task definitions as ServiceTaskDto objects.
     */
    public List<ServiceTaskDto> getServiceTaskDefinitions(String deploymentId) {
        List<ServiceTaskDto> serviceTaskDefinitions = new ArrayList<>();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();

        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinition.getId());

        Collection<ServiceTask> serviceTasks = bpmnModelInstance.getModelElementsByType(ServiceTask.class);
        for (ServiceTask serviceTask : serviceTasks) {

            ServiceTaskDto serviceTaskDto = new ServiceTaskDto(
                    serviceTask.getId(),
                    serviceTask.getName(),
                    serviceTask.getCamundaTopic(),
                    serviceTask.getCamundaExpression(),
                    serviceTask.getCamundaDelegateExpression(),
                    serviceTask.getCamundaTaskPriority());

            serviceTaskDefinitions.add(serviceTaskDto);
        }
        return serviceTaskDefinitions;
    }

    /**
     * Retrieves a list of user task definitions for a process definition with the given deployment ID.
     * @param deploymentId the ID of the process definition deployment.
     * @return a list of user task definitions as UserTaskDto objects.
     */
    public List<UserTaskDto> getUserTaskDefinitions(String deploymentId) {
        List<UserTaskDto> userTaskDefinitions = new ArrayList<>();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();

        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinition.getId());

        Collection<UserTask> userTasks = bpmnModelInstance.getModelElementsByType(UserTask.class);
        for (UserTask userTask : userTasks) {

            UserTaskDto userTaskDto = new UserTaskDto(
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

            userTaskDefinitions.add(userTaskDto);
        }
        return userTaskDefinitions;
    }

    /**
     * Retrieves a list of send task definitions for a process definition with the given deployment ID.
     * @param deploymentId the ID of the process definition deployment.
     * @return a list of send task definitions as SendTaskDto objects.
     */
    public List<SendTaskDto> getSendTaskDefinitions(String deploymentId) {
        List<SendTaskDto> sendTaskDefinitions = new ArrayList<>();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();


        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinition.getId());

        Collection<SendTask> sendTasks = bpmnModelInstance.getModelElementsByType(SendTask.class);
        for (SendTask sendTask : sendTasks) {

            SendTaskDto sendTaskDto = new SendTaskDto(
                    sendTask.getId(),
                    sendTask.getName(),
                    sendTask.getCamundaType(),
                    sendTask.getCamundaTopic(),
                    sendTask.getCamundaDelegateExpression(),
                    sendTask.getCamundaResultVariable());

            sendTaskDefinitions.add(sendTaskDto);
        }
        return sendTaskDefinitions;
    }

    /**
     * Retrieves a list of exclusive gateway definitions for a process definition with the given deployment ID.
     * @param deploymentId the ID of the process definition deployment.
     * @return a list of exclusive gateway definitions as ExclusiveGatewayDto objects.
     */
    public List<ExclusiveGatewayDto> getExclusiveGatewayDefinitions(String deploymentId) {
        List<ExclusiveGatewayDto> exclusiveGatewayDefinitions = new ArrayList<>();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();

        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinition.getId());

        Collection<ExclusiveGateway> exclusiveGateways = bpmnModelInstance.getModelElementsByType(ExclusiveGateway.class);
        for (ExclusiveGateway exclusiveGateway : exclusiveGateways) {

            ExclusiveGatewayDto exclusiveGatewayDto = new ExclusiveGatewayDto(
                    exclusiveGateway.getId(),
                    exclusiveGateway.getName(),
                    exclusiveGateway.getOutgoing().stream().map(SequenceFlow::getId).collect(toList()));

            exclusiveGatewayDefinitions.add(exclusiveGatewayDto);
        }
        return exclusiveGatewayDefinitions;
    }

    /**
     * Retrieves a list of sequence flow definitions for exclusive gateways of a process definition with the given deployment ID.
     * @param deploymentId the ID of the process definition deployment.
     * @return a list of sequence flow definitions as GatewaySequenceFlowDto objects.
     */
    public List<GatewaySequenceFlowDto> getAllGatewaySequenceFlowsByDeploymentId(String deploymentId) {
        List<GatewaySequenceFlowDto> gatewaySequenceFlows = new ArrayList<>();

        // Retrieve process definition for deployment ID
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
                .createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();

        // Get BpmnModelInstance from process definition
        BpmnModelInstance modelInstance = processEngine.getRepositoryService()
                .getBpmnModelInstance(processDefinition.getId());

        // Get all gateways from BpmnModelInstance
        Collection<Gateway> gateways = modelInstance.getModelElementsByType(Gateway.class);

        // Iterate through gateways and get all outgoing sequence flows with condition expressions
        for (Gateway gateway : gateways) {
            for (SequenceFlow sequenceFlow : gateway.getOutgoing()) {
                if (sequenceFlow.getConditionExpression() != null) {
                    gatewaySequenceFlows.add(GatewaySequenceFlowDto.of(sequenceFlow));
                }
            }
        }

        return gatewaySequenceFlows;
    }

    /**
     *Deletes all tasks.
     */
    public void deleteAllTasks() {
        List<Task> tasks = taskService.createTaskQuery().list();
        for (Task task : tasks) {
            taskService.deleteTask(task.getId(), true);
        }
    }

    /**
     * Returns a list of completed task instances.
     * @return a list of completed task instances
     */
    public List<HistoryTaskDto> getCompletedTasks() {
        return historyService.createHistoricTaskInstanceQuery()
                .finished()
                .list()
                .stream()
                .map(HistoryTaskDto::of)
                .collect(toList());
    }

     /* @param assignee           the expression to set as the assignee of the user task (use null to clear the existing assignee)
        @param candidateUsers     a comma-separated string of expressions to set as the candidate users of the user task (use null to clear the existing candidate users) */

    /**
     * Updates the formKey of a user task within a specific process deployment.
     * @param deploymentId       the ID of the deployment of the process definition containing the user task
     * @param taskId             the ID of the user task to update
     * @param formKey            the formKey to assign to the user task to update
     * @param assignee     the candidateUsers of the user task to update
     */
    public void updateTask(String deploymentId, String taskId,String formKey,String assignee ) {
        // String assignee, String candidateUsers ,
        try {
            // Retrieve the process definition by deployment ID
            ProcessDefinition processDefinition = processEngine.getRepositoryService()
                    .createProcessDefinitionQuery()
                    .deploymentId(deploymentId)
                    .singleResult();

            // Check if process definition is null
            if (processDefinition == null) {
                throw new IllegalArgumentException("Invalid deployment ID: " + deploymentId);
            }

            BpmnModelInstance bpmnModelInstance = processEngine.getRepositoryService().getBpmnModelInstance(processDefinition.getId());
            // Retrieve the user task by ID
            UserTask userTask = bpmnModelInstance.getModelElementById(taskId);

            // Check if user task is null
            if (userTask == null) {
                throw new IllegalArgumentException("Invalid task ID: " + taskId);
            }
            // Retrieve the gateway that comes after the user task
            FlowNode gateway = null;
            Collection<SequenceFlow> outgoingFlows = userTask.getOutgoing();
            if (outgoingFlows.size() == 1 && outgoingFlows.iterator().next().getTarget() != null) {
                gateway = outgoingFlows.iterator().next().getTarget();
            }

            // Check if gateway is null or not a gateway type
            if (gateway == null || !(gateway instanceof Gateway)) {
                System.out.println("No Gatway after "+userTask.getId());
            }
            else {
                // Retrieve the form from the form repository
                Form form = formRepository.findByFormKey(formKey);

                // Convert the form content to a JSON object
                JsonObject formJson = JsonParser.parseString(form.getFormContent()).getAsJsonObject();

                // Update the form content to match the number of outgoing flows of the gateway
                JsonArray componentsJson = formJson.getAsJsonArray("components");
                JsonArray updatedComponentsJson = new JsonArray();
                boolean radioFound = false;
                for (int i = 0; i < componentsJson.size(); i++) {
                    JsonObject componentJson = componentsJson.get(i).getAsJsonObject();
                    if ("radio".equals(componentJson.get("type").getAsString())) {
                        // Update the radio buttons to match the outgoing flows of the gateway
                        JsonArray valuesJson = new JsonArray();
                        int count = 0;
                        for (SequenceFlow outgoingFlow : gateway.getOutgoing()) {
                            String value = String.valueOf(count);
                            String label = outgoingFlow.getName();
                            // String expression = String.valueOf(outgoingFlow.getConditionExpression());
                            JsonObject valueJson = new JsonObject();
                            valueJson.addProperty("value", value);
                            valueJson.addProperty("label", label);
                            // valueJson.addProperty("expression", expression);
                            valuesJson.add(valueJson);
                            count++;
                        }
                        componentJson.add("values", valuesJson);
                        componentJson.addProperty("key", "input");
                        radioFound = true;
                    }
                    updatedComponentsJson.add(componentJson);
                }

                // If no radio buttons found, add them to the form content
                if (!radioFound) {
                    JsonArray updatedValuesJson = new JsonArray();
                    int count = 0;
                    for (SequenceFlow outgoingFlow : gateway.getOutgoing()) {
                        String value = String.valueOf(count);
                        String label = outgoingFlow.getName();
                        JsonObject valueJson = new JsonObject();
                        valueJson.addProperty("value", value);
                        valueJson.addProperty("label", label);
                        count++;
                        updatedValuesJson.add(valueJson);
                    }
                    JsonObject radioJson = new JsonObject();
                    radioJson.addProperty("type", "radio");
                    radioJson.addProperty("label", "Radio");
                    radioJson.addProperty("key", "input");
                    radioJson.add("values", updatedValuesJson);
                    updatedComponentsJson.add(radioJson);
                }


                formJson.remove("components");
                formJson.add("components", updatedComponentsJson);

                // Update the form content with the new radio button values and labels
                String updatedFormContent = formJson.toString();

                // Set the updated form content on the form and save it to the repository
                form.setFormContent(updatedFormContent);
                formRepository.save(form);
            }
            // Set the assignee ,formKey and candidate users for the user task
            userTask.setCamundaAssignee(assignee);
            //userTask.setCamundaCandidateUsers(candidateUsers);
            userTask.setCamundaFormKey(formKey);
            // Deploy the updated process definition
            Deployment deployment = processEngine.getRepositoryService().createDeployment()
                    .addModelInstance(processDefinition.getResourceName(), bpmnModelInstance)
                    .deploy();

            // Convert the updated BPMN model instance to XML string
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Bpmn.writeModelToStream(outputStream, bpmnModelInstance);
            String updatedXmlContent = outputStream.toString();

            // Get workflow by deploymentId to update the deploymentId
            Workflow workflow = null;
            if (workflowRepository.findWorkflowByDeploymentId(deploymentId)!= null) {
                workflow = workflowRepository.findWorkflowByDeploymentId(deploymentId);
            } else {
                workflow = workflowRepository.findWorkflowByDeploymentId(updatedDeploymentId);
            }

            // Add a null check for the workflow object
            if (workflow == null) {
                throw new IllegalArgumentException("Invalid workflow: null");
            }

            /* Save BPMN XML file to /resources/static/bpmn folder
            String fileName = workflow.getXmlName() + ".xml";
            File bpmnFile = new File("src/main/resources/static/bpmns", fileName);
            FileWriter writer = new FileWriter(bpmnFile);
            Bpmn.writeModelToFile(bpmnFile, bpmnModelInstance);
            writer.close();*/

            updatedDeploymentId = deployment.getId();
            workflow.setDeploymentId(updatedDeploymentId);
            workflow.setXmlContent(updatedXmlContent);
            workflowRepository.save(workflow);

        } catch (Exception e) {
            // Handle any exceptions that occur during the process
            throw new RuntimeException("Failed to update task: " + e.getMessage(), e);
        }
    }
}