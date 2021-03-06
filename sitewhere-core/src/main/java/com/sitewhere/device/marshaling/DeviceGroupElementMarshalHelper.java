/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.device.marshaling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.rest.model.device.Device;
import com.sitewhere.rest.model.device.group.DeviceGroup;
import com.sitewhere.rest.model.device.group.DeviceGroupElement;
import com.sitewhere.rest.model.device.marshaling.MarshaledDeviceGroupElement;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.asset.IAssetResolver;
import com.sitewhere.spi.device.IDevice;
import com.sitewhere.spi.device.IDeviceManagement;
import com.sitewhere.spi.device.group.IDeviceGroup;
import com.sitewhere.spi.device.group.IDeviceGroupElement;

/**
 * Configurable helper class that allows {@link DeviceGroupElement} model
 * objects to be created from {@link IDeviceGroupElement} SPI objects.
 * 
 * @author dadams
 */
public class DeviceGroupElementMarshalHelper {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Device Management */
    private IDeviceManagement deviceManagement;

    /**
     * Indicates whether detailed device or device group information is to be
     * included
     */
    private boolean includeDetails = false;

    /** Helper class for enriching device information */
    private DeviceMarshalHelper deviceHelper;

    public DeviceGroupElementMarshalHelper(IDeviceManagement deviceManagement) {
	this.deviceManagement = deviceManagement;
	this.deviceHelper = new DeviceMarshalHelper(deviceManagement).setIncludeDeviceType(true).setIncludeAsset(true)
		.setIncludeAssignment(true);
    }

    /**
     * Convert the SPI object to a model object for marshaling.
     * 
     * @param source
     * @param manager
     * @return
     * @throws SiteWhereException
     */
    public MarshaledDeviceGroupElement convert(IDeviceGroupElement source, IAssetResolver assets)
	    throws SiteWhereException {
	MarshaledDeviceGroupElement result = new MarshaledDeviceGroupElement();
	result.setGroupId(source.getGroupId());
	result.setIndex(source.getIndex());
	result.setType(source.getType());
	result.setElementId(source.getElementId());
	result.getRoles().addAll(source.getRoles());
	if (isIncludeDetails()) {
	    switch (source.getType()) {
	    case Device: {
		IDevice device = deviceManagement.getDevice(source.getElementId());
		if (device != null) {
		    Device inflated = deviceHelper.convert(device, assets);
		    result.setDevice(inflated);
		} else {
		    LOGGER.warn("Group references invalid device: " + source.getElementId());
		}
		break;
	    }
	    case Group: {
		IDeviceGroup group = deviceManagement.getDeviceGroup(source.getElementId());
		if (group != null) {
		    DeviceGroup inflated = DeviceGroup.copy(group);
		    result.setDeviceGroup(inflated);
		} else {
		    LOGGER.warn("Group references invalid subgroup: " + source.getElementId());
		}
		break;
	    }
	    }
	}
	return result;
    }

    public boolean isIncludeDetails() {
	return includeDetails;
    }

    public DeviceGroupElementMarshalHelper setIncludeDetails(boolean includeDetails) {
	this.includeDetails = includeDetails;
	return this;
    }
}