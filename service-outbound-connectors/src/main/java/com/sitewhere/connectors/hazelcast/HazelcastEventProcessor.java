/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.connectors.hazelcast;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hazelcast.core.ITopic;
import com.sitewhere.communication.hazelcast.IHazelcastConfiguration;
import com.sitewhere.connectors.FilteredOutboundConnector;
import com.sitewhere.device.marshaling.DeviceCommandInvocationMarshalHelper;
import com.sitewhere.rest.model.device.event.DeviceAlert;
import com.sitewhere.rest.model.device.event.DeviceCommandInvocation;
import com.sitewhere.rest.model.device.event.DeviceCommandResponse;
import com.sitewhere.rest.model.device.event.DeviceLocation;
import com.sitewhere.rest.model.device.event.DeviceMeasurements;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.event.IDeviceAlert;
import com.sitewhere.spi.device.event.IDeviceCommandInvocation;
import com.sitewhere.spi.device.event.IDeviceCommandResponse;
import com.sitewhere.spi.device.event.IDeviceEventContext;
import com.sitewhere.spi.device.event.IDeviceLocation;
import com.sitewhere.spi.device.event.IDeviceMeasurements;
import com.sitewhere.spi.device.event.IDeviceStateChange;
import com.sitewhere.spi.server.hazelcast.ISiteWhereHazelcast;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;

/**
 * Sends processed device events out on Hazelcast topics for further processing.
 * 
 * @author Derek
 */
public class HazelcastEventProcessor extends FilteredOutboundConnector {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Hazelcast configuration */
    private IHazelcastConfiguration hazelcastConfiguration;

    /** Topic for device measurements */
    private ITopic<DeviceMeasurements> measurementsTopic;

    /** Topic for device locations */
    private ITopic<DeviceLocation> locationsTopic;

    /** Topic for device alerts */
    private ITopic<DeviceAlert> alertsTopic;

    /** Topic for device command invocations */
    private ITopic<DeviceCommandInvocation> commandInvocationsTopic;

    /** Topic for device command responses */
    private ITopic<DeviceCommandResponse> commandResponsesTopic;

    /** Used for marshaling command invocations */
    private DeviceCommandInvocationMarshalHelper invocationHelper;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.device.event.processor.FilteredOutboundEventProcessor#start
     * (com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void start(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	// Required for filters.
	super.start(monitor);

	this.invocationHelper = new DeviceCommandInvocationMarshalHelper(null, true);
	this.measurementsTopic = getHazelcastConfiguration().getHazelcastInstance()
		.getTopic(ISiteWhereHazelcast.TOPIC_MEASUREMENTS_ADDED);
	this.locationsTopic = getHazelcastConfiguration().getHazelcastInstance()
		.getTopic(ISiteWhereHazelcast.TOPIC_LOCATION_ADDED);
	this.alertsTopic = getHazelcastConfiguration().getHazelcastInstance()
		.getTopic(ISiteWhereHazelcast.TOPIC_ALERT_ADDED);
	this.commandInvocationsTopic = getHazelcastConfiguration().getHazelcastInstance()
		.getTopic(ISiteWhereHazelcast.TOPIC_COMMAND_INVOCATION_ADDED);
	this.commandResponsesTopic = getHazelcastConfiguration().getHazelcastInstance()
		.getTopic(ISiteWhereHazelcast.TOPIC_COMMAND_RESPONSE_ADDED);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#getLogger()
     */
    @Override
    public Logger getLogger() {
	return LOGGER;
    }

    /*
     * @see com.sitewhere.outbound.FilteredOutboundEventProcessor#
     * onMeasurementsNotFiltered(com.sitewhere.spi.device.event.IDeviceEventContext,
     * com.sitewhere.spi.device.event.IDeviceMeasurements)
     */
    @Override
    public void onMeasurementsNotFiltered(IDeviceEventContext context, IDeviceMeasurements measurements)
	    throws SiteWhereException {
	DeviceMeasurements marshaled = DeviceMeasurements.copy(measurements);
	measurementsTopic.publish(marshaled);
	LOGGER.debug("Published measurements event to Hazelcast (id=" + measurements.getId() + ")");
    }

    /*
     * @see
     * com.sitewhere.outbound.FilteredOutboundEventProcessor#onLocationNotFiltered(
     * com.sitewhere.spi.device.event.IDeviceEventContext,
     * com.sitewhere.spi.device.event.IDeviceLocation)
     */
    @Override
    public void onLocationNotFiltered(IDeviceEventContext context, IDeviceLocation location) throws SiteWhereException {
	DeviceLocation marshaled = DeviceLocation.copy(location);
	locationsTopic.publish(marshaled);
	LOGGER.debug("Published location event to Hazelcast (id=" + location.getId() + ")");
    }

    /*
     * @see
     * com.sitewhere.outbound.FilteredOutboundEventProcessor#onAlertNotFiltered(com.
     * sitewhere.spi.device.event.IDeviceEventContext,
     * com.sitewhere.spi.device.event.IDeviceAlert)
     */
    @Override
    public void onAlertNotFiltered(IDeviceEventContext context, IDeviceAlert alert) throws SiteWhereException {
	DeviceAlert marshaled = DeviceAlert.copy(alert);
	alertsTopic.publish(marshaled);
	LOGGER.debug("Published alert event to Hazelcast (id=" + alert.getId() + ")");
    }

    /*
     * @see com.sitewhere.outbound.FilteredOutboundEventProcessor#
     * onStateChangeNotFiltered(com.sitewhere.spi.device.event.IDeviceEventContext,
     * com.sitewhere.spi.device.event.IDeviceStateChange)
     */
    @Override
    public void onStateChangeNotFiltered(IDeviceEventContext context, IDeviceStateChange state)
	    throws SiteWhereException {
	LOGGER.debug(
		"Hazelcast received state change of type: " + state.getCategory().name() + ":" + state.getNewState());
    }

    /*
     * @see com.sitewhere.outbound.FilteredOutboundEventProcessor#
     * onCommandInvocationNotFiltered(com.sitewhere.spi.device.event.
     * IDeviceEventContext, com.sitewhere.spi.device.event.IDeviceCommandInvocation)
     */
    @Override
    public void onCommandInvocationNotFiltered(IDeviceEventContext context, IDeviceCommandInvocation invocation)
	    throws SiteWhereException {
	DeviceCommandInvocation converted = invocationHelper.convert(invocation);
	commandInvocationsTopic.publish(converted);
	LOGGER.debug("Published command invocation event to Hazelcast (id=" + invocation.getId() + ")");
    }

    /*
     * @see com.sitewhere.outbound.FilteredOutboundEventProcessor#
     * onCommandResponseNotFiltered(com.sitewhere.spi.device.event.
     * IDeviceEventContext, com.sitewhere.spi.device.event.IDeviceCommandResponse)
     */
    @Override
    public void onCommandResponseNotFiltered(IDeviceEventContext context, IDeviceCommandResponse response)
	    throws SiteWhereException {
	DeviceCommandResponse marshaled = DeviceCommandResponse.copy(response);
	commandResponsesTopic.publish(marshaled);
	LOGGER.debug("Published command response event to Hazelcast (id=" + response.getId() + ")");
    }

    public IHazelcastConfiguration getHazelcastConfiguration() {
	return hazelcastConfiguration;
    }

    public void setHazelcastConfiguration(IHazelcastConfiguration hazelcastConfiguration) {
	this.hazelcastConfiguration = hazelcastConfiguration;
    }
}