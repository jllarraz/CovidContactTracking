package com.altaureum.covid.tracking.common

object Actions {
    /**
     * Request Actions BLE Server
     */
    val ACTION_START_BLE_SERVER="com.altaureum.covid.tracking.common.action.START_BLE_SERVER"
    val ACTION_RESTART_BLE_SERVER="com.altaureum.covid.tracking.common.action.RESTART_BLE_SERVER"
    val ACTION_STOP_BLE_SERVER="com.altaureum.covid.tracking.common.action.STOP_BLE_SERVER"

    val ACTION_SEND_RESPONSE="com.altaureum.covid.tracking.common.action.SEND_RESPONSE"

    /**
     * Response Actions BLE Server
     */
    val ACTION_BLE_SERVER_STARTED="com.altaureum.covid.tracking.common.action.BLE_SERVER_STARTED"
    val ACTION_BLE_SERVER_STOPED="com.altaureum.covid.tracking.common.action.BLE_SERVER_STOPED"
    val ACTION_REQUEST_BLE_PERMISSIONS="com.altaureum.covid.tracking.common.action.REQUEST_BLE_PERMISSIONS"
    val ACTION_ERROR_BLE_ADVERTISMENT_SUCCESS="com.altaureum.covid.tracking.common.action.ERROR_BLE_ADVERTISMENT_SUCCESS"
    val ACTION_ERROR_BLE_NOT_SUPPORTED="com.altaureum.covid.tracking.common.action.ERROR_BLE_NOT_SUPPORTED"
    val ACTION_ERROR_BLE_ADVERTISMENT_NOT_SUPPORTED="com.altaureum.covid.tracking.common.action.ERROR_BLE_ADVERTISMENT_NOT_SUPPORTED"
    val ACTION_ERROR_BLE_ADVERTISMENT_FAILED="com.altaureum.covid.tracking.common.action.ERROR_BLE_ADVERTISMENT_FAILED"

    val ACTION_BLE_SERVER_DEVICE_ADDED="com.altaureum.covid.tracking.common.action.BLE_SERVER_DEVICE_ADDED"
    val ACTION_BLE_SERVER_DEVICE_REMOVED="com.altaureum.covid.tracking.common.action.BLE_SERVER_DEVICE_REMOVED"
    val ACTION_BLE_SERVER_MESSAGE_RECEIVED="com.altaureum.covid.tracking.common.action.BLE_SERVER_MESSAGE_RECEIVED"
    val ACTION_BLE_SERVER_NOTIFICATION_SENT="com.altaureum.covid.tracking.common.action.BLE_SERVER_NOTIFICATION_SENT"
}