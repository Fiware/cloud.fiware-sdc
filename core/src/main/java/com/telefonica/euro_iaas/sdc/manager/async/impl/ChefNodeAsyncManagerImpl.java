/**
 * Copyright 2014 Telefonica Investigación y Desarrollo, S.A.U <br>
 * This file is part of FI-WARE project.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 * </p>
 * <p>
 * You may obtain a copy of the License at:<br>
 * <br>
 * http://www.apache.org/licenses/LICENSE-2.0
 * </p>
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * </p>
 * <p>
 * See the License for the specific language governing permissions and limitations under the License.
 * </p>
 * <p>
 * For those usages not covered by the Apache version 2.0 License please contact with opensource@tid.es
 * </p>
 */

/**
 * 
 */
package com.telefonica.euro_iaas.sdc.manager.async.impl;

import static com.telefonica.euro_iaas.sdc.util.SystemPropertiesProvider.CHEF_NODE_BASE_URL;

import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telefonica.euro_iaas.sdc.exception.NodeExecutionException;
import com.telefonica.euro_iaas.sdc.manager.ChefNodeManager;
import com.telefonica.euro_iaas.sdc.manager.async.ChefNodeAsyncManager;
import com.telefonica.euro_iaas.sdc.manager.async.TaskManager;
import com.telefonica.euro_iaas.sdc.model.Task;
import com.telefonica.euro_iaas.sdc.model.Task.TaskStates;
import com.telefonica.euro_iaas.sdc.model.TaskError;
import com.telefonica.euro_iaas.sdc.model.TaskReference;
import com.telefonica.euro_iaas.sdc.util.SystemPropertiesProvider;
import com.telefonica.euro_iaas.sdc.util.TaskNotificator;

/**
 * @author jesus.movilla
 */
public class ChefNodeAsyncManagerImpl implements ChefNodeAsyncManager {

    private static Logger LOGGER = LoggerFactory.getLogger(ChefNodeAsyncManagerImpl.class);

    private TaskManager taskManager;
    private SystemPropertiesProvider propertiesProvider;
    private TaskNotificator taskNotificator;
    private ChefNodeManager chefNodeManager;

    /*
     * (non-Javadoc)
     * @see com.telefonica.euro_iaas.sdc.manager.async.ChefNodeAsyncManager#chefNodeDelete(java.lang.String,
     * com.telefonica.euro_iaas.sdc.model.Task, java.lang.String)
     */
    public void chefNodeDelete(String vdc, String chefNodename, Task task, String callback) {
        try {
            chefNodeManager.chefNodeDelete(vdc, chefNodename);
            updateSuccessTask(task, vdc, chefNodename);
            LOGGER.info("Node  " + chefNodename + " deleted from Chef Server successfully");
        } catch (NodeExecutionException e) {
            updateErrorTask(vdc, chefNodename, task, "The node " + chefNodename
                    + " can not be deleted due to an error executing in node.", e);
        } catch (Throwable e) {
            updateErrorTask(vdc, chefNodename, task, "The node " + chefNodename
                    + " can not be deleted due to unexpected error.", e);
        } finally {
            notifyTask(callback, task);
        }
    }

    // //////// PRIVATE METHODS ///////////

    /*
     * Update the task with necessary information when the task is success.
     */
    private void updateSuccessTask(Task task, String vdc, String chefNodename) {
        String piResource = MessageFormat.format(propertiesProvider.getProperty(CHEF_NODE_BASE_URL), vdc, chefNodename);

        task.setResult(new TaskReference(piResource));
        task.setEndTime(new Date());
        task.setStatus(TaskStates.SUCCESS);
        taskManager.updateTask(task);
    }

    /*
     * Update the task with necessary information when the task is wrong and the product instance exists in the system.
     */
    private void updateErrorTask(String vdc, String chefNodename, Task task, String message, Throwable t) {
        String piResource = MessageFormat.format(propertiesProvider.getProperty(CHEF_NODE_BASE_URL), vdc, chefNodename);

        task.setResult(new TaskReference(piResource));
        updateErrorTask(task, message, t);
    }

    /*
     * Update the task with necessary information when the task is wrong.
     */
    private void updateErrorTask(Task task, String message, Throwable t) {
        TaskError error = new TaskError(message);
        error.setMajorErrorCode(t.getMessage());
        error.setMinorErrorCode(t.getClass().getSimpleName());
        task.setEndTime(new Date());
        task.setStatus(TaskStates.ERROR);
        task.setError(error);
        taskManager.updateTask(task);
        LOGGER.info("An error occurs while deleting a node fromChef Server. See task " + task.getHref()
                + "for more information");
    }

    private void notifyTask(String url, Task task) {
        if (!StringUtils.isEmpty(url)) {
            taskNotificator.notify(url, task);
        }
    }

    // ////////// I.O.C ////////////

    /**
     * @param chefNodeManager
     *            the chefNodeManager to set
     */
    public void setChefNodeManager(ChefNodeManager chefNodeManager) {
        this.chefNodeManager = chefNodeManager;
    }

    /**
     * @param taskManager
     *            the taskManager to set
     */
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * @param propertiesProvider
     *            the propertiesProvider to set
     */
    public void setPropertiesProvider(SystemPropertiesProvider propertiesProvider) {
        this.propertiesProvider = propertiesProvider;
    }

    /**
     * @param taskNotificator
     *            the taskNotificator to set
     */
    public void setTaskNotificator(TaskNotificator taskNotificator) {
        this.taskNotificator = taskNotificator;
    }
}
