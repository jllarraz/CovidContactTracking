package com.altaureum.covid.tracking.common

object Actions {
    /**
     * Request Actions BLE Server
     */
    val ACTION_START_BLE_SERVER="com.altaureum.covid.tracking.common.action.START_BLE_SERVER"
    val ACTION_RESTART_BLE_SERVER="com.altaureum.covid.tracking.common.action.RESTART_BLE_SERVER"
    val ACTION_STOP_BLE_SERVER="com.altaureum.covid.tracking.common.action.STOP_BLE_SERVER"
    val ACTION_SEND_MESSAGE_SERVER="com.altaureum.covid.tracking.common.action.SEND_MESSAGE_SERVER"

    /**
     * Response Actions BLE Server
     */
    val ACTION_BLE_SERVER_STARTED="com.altaureum.covid.tracking.common.action.BLE_SERVER_STARTED"
    val ACTION_BLE_SERVER_STOPED="com.altaureum.covid.tracking.common.action.BLE_SERVER_STOPED"
    val ACTION_REQUEST_BLE_ENABLE="com.altaureum.covid.tracking.common.action.REQUEST_BLE_ENABLE"
    val ACTION_REQUEST_BLE_PERMISSIONS="com.altaureum.covid.tracking.common.action.REQUEST_BLE_PERMISSIONS"
    val ACTION_REQUEST_LOCATION_PERMISSIONS="com.altaureum.covid.tracking.common.action.REQUEST_LOCATION_PERMISSIONS"
    val ACTION_ERROR_BLE_ADVERTISMENT_SUCCESS="com.altaureum.covid.tracking.common.action.ERROR_BLE_ADVERTISMENT_SUCCESS"
    val ACTION_ERROR_BLE_NOT_SUPPORTED="com.altaureum.covid.tracking.common.action.ERROR_BLE_NOT_SUPPORTED"
    val ACTION_ERROR_BLE_ADVERTISMENT_NOT_SUPPORTED="com.altaureum.covid.tracking.common.action.ERROR_BLE_ADVERTISMENT_NOT_SUPPORTED"
    val ACTION_ERROR_BLE_ADVERTISMENT_FAILED="com.altaureum.covid.tracking.common.action.ERROR_BLE_ADVERTISMENT_FAILED"

    val ACTION_BLE_SERVER_DEVICE_ADDED="com.altaureum.covid.tracking.common.action.BLE_SERVER_DEVICE_ADDED"
    val ACTION_BLE_SERVER_DEVICE_REMOVED="com.altaureum.covid.tracking.common.action.BLE_SERVER_DEVICE_REMOVED"
    val ACTION_BLE_SERVER_MESSAGE_RECEIVED="com.altaureum.covid.tracking.common.action.BLE_SERVER_MESSAGE_RECEIVED"
    val ACTION_BLE_SERVER_NOTIFICATION_SENT="com.altaureum.covid.tracking.common.action.BLE_SERVER_NOTIFICATION_SENT"


    /**
     * Request Actions BLE Client
     */
    val ACTION_START_BLE_CLIENT="com.altaureum.covid.tracking.common.action.START_BLE_CLIENT"
    val ACTION_STOP_BLE_CLIENT="com.altaureum.covid.tracking.common.action.STOP_BLE_CLIENT"
    val ACTION_SEND_MESSAGE_CLIENT="com.altaureum.covid.tracking.common.action.SEND_MESSAGE_CLIENT"

    /**
     * Response Actions BLE Client
     */
    val ACTION_BLE_CLIENT_STARTED="com.altaureum.covid.tracking.common.action.BLE_CLIENT_STARTED"
    val ACTION_BLE_CLIENT_STOPED="com.altaureum.covid.tracking.common.action.BLE_CLIENT_STOPED"


    /**
     * Request Actions BLE Client LIGHT
     */
    val ACTION_START_SCAN_BLE_LIGHT_CLIENT="com.altaureum.covid.tracking.common.action.START_SCAN_BLE_LIGHT_CLIENT"
    val ACTION_STOP_SCAN_BLE_LIGHT_CLIENT="com.altaureum.covid.tracking.common.action.STOP_SCAN_BLE_LIGHT_CLIENT"

    /**
     * Request Actions BLE Client LIGHT
     */
    val ACTION_BLE_LIGHT_CLIENT_SCAN_STARTED="com.altaureum.covid.tracking.common.action.BLE_LIGHT_CLIENT_SCAN_STARTED"
    val ACTION_BLE_LIGHT_CLIENT_SCAN_STOPED="com.altaureum.covid.tracking.common.action.BLE_LIGHT_CLIENT_SCAN_STOPED"
    val ACTION_BLE_LIGHT_CLIENT_SCAN_FAILED="com.altaureum.covid.tracking.common.action.BLE_LIGHT_CLIENT_SCAN_FAILED"
    val ACTION_BLE_LIGHT_DEVICE_ADDED="com.altaureum.covid.tracking.common.action.BLE_LIGHT_DEVICE_ADDED"
    val ACTION_BLE_LIGHT_DEVICE_LIGHT_REMOVED="com.altaureum.covid.tracking.common.action.BLE_LIGHT_DEVICE_LIGHT_REMOVED"
}