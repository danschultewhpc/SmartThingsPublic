/**
 *  PetSafe Electronic SmartDoor
 *
 *  Copyright 2016 Daniel Schulte
 *
 */
metadata({
	definition(name: "Electronic SmartDoor", namespace: "danschultewhpc", author: "Daniel Schulte", {
    	capability("Configuration"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#configuration
		capability("Battery"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#battery
        capability("Lock"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#lock
		capability("Refresh"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#refresh

		fingerprint(profileId: "0104", // Home Automation: http://docs.smartthings.com/en/latest/device-type-developers-guide/definition-metadata.html#fingerprinting
        			// Cluster Definitions: http://www.zigbee.org/download/standards-zigbee-cluster-library/
        			inClusters: [powerConfigurationCluster(),
                    			 doorLockCluster()
                                 ].join(" "),
                    outClusters: [].join(" ")
                   );
	});
});

private analogInputCluster() { return "000C"; }

private powerConfigurationCluster() { return "0001"; }
private batteryVoltageAttribute() { return "0020"; }

private doorLockCluster() { return "0101"; }
private lockStateAttribute() { return "0000"; }

def parse(String description) {
    log.trace("parse(String) - Enter: ${description}");
    
    def result = [];
    if (description) {
    	if (description.startsWith("read attr - ")) {
            Map<String,String> descriptionMap = (description - "read attr - ").split(",").inject([:], { map, param ->
                def nameAndValue = param.split(":")
                map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
            });
            // descriptionMap.each({ key, value -> log.info("descriptionMap.${key}: ${value}"); });
            
            if (descriptionMap.cluster == powerConfigurationCluster())
            {
            	if (descriptionMap.attrId == batteryVoltageAttribute())
                {
                	def deviceBatteryVoltage = Integer.parseInt(descriptionMap.value, 16) * 0.1;
                    // log.debug("Device battery voltage: ${deviceBatteryVoltage}");
                    
                    def singleBatteryMaximumVoltage = 1.5;
                    def singleBatteryMinimumVoltage = 1.0;
                    def installedBatteries = 4;
                    
                    def deviceMinimumVoltage = singleBatteryMinimumVoltage * installedBatteries;
                    def deviceMaximumVoltage = singleBatteryMaximumVoltage * installedBatteries;
                    // log.debug("Device minimum voltage: ${deviceMinimumVoltage}");
                    
                    def deviceBatteryLevel = (deviceBatteryVoltage - deviceMinimumVoltage) / (deviceMaximumVoltage - deviceMinimumVoltage);
                    // log.debug("Device battery level: ${deviceBatteryLevel}");
                    
                    def deviceBatteryPercentage = deviceBatteryLevel * 100;
                    if (deviceBatteryPercetange > 100)
                    {
                    	deviceBatteryPercetange = 100;
                    }
                    log.debug("Device battery percetange: ${deviceBatteryPercentage}");
                    
                    def batteryResult = [:];
                    batteryResult.name = "battery";
                    batteryResult.value = deviceBatteryPercentage;
                    batteryResult.descriptionText = "${getLinkText(device)} battery is at ${deviceBatteryPercentage}%";
                    			
                    result = createEvent(batteryResult);
                }
                else
                {
                	log.warn("Unrecognized powerConfigurationCluster attribute id: ${descriptionMap.attrId}");
                }
            }
            else if (descriptionMap.cluster == doorLockCluster())
            {
            	if (descriptionMap.attrId == lockStateAttribute())
                {
                	def lockStateResult = [:];
                    lockStateResult.name = "lock";
                	if (descriptionMap.value == "01")
                    {
                    	lockStateResult.value = "locked";
                    }
                    else if(descriptionMap.value == "02")
                    {
                    	lockStateResult.value = "unlocked";
                    }
                    else
                    {
                    	log.warn("Unrecognized lockStateAttribute value: ${descriptionMap.value}");
                    }
                    
                    if (lockStateResult.value)
                    {
                    	log.debug("Valid lockStateResult.value: ${lockStateResult.value}");
                    	lockStateResult.descriptionText = "${getLinkText(device)} door is ${lockStateResult.value}";
                    	result = createEvent(lockStateResult);
                    }
                }
                else
                {
            		log.warn("Unrecognized doorLockCluster attribute id: ${descriptionMap.attrId}");
                }
            }
            else
            {
            	log.warn("Unrecognized cluster: ${descriptionMap.cluster}");
            }
        }
        else
        {
        	log.warn("Unrecognized description prefix: ${description}");
        }
    }
    else
    {
    	log.warn("Empty description");
    }
    
    log.trace("parse(String) - Exit: ${result}");
    
	return result;
}

def updated()
{
	log.trace("updated() - Enter");
    
    def result = configure();
    
    log.trace("updated() - Exit: ${result}");
    
    return result;
}

// capability("Configuration"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#configuration
def configure()
{
	log.trace("configure() - Enter");
    
    def result = [];
    
    log.trace("configure() - Exit: ${result}");
    
    return result;
}

// capability("Lock"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#lock
def lock()
{
	log.trace("lock() - Enter");
    
    def result = [];
    
    log.trace("lock() - Exit: ${result}");
    
    return result;
}

def unlock()
{
	log.trace("unlock() - Enter");
    
    def result = [];
    
    log.trace("unlock() - Exit: ${result}");
    
    return result;
}

// capability("Refresh"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#refresh
def refresh()
{
	log.trace("refresh() - Enter");
    
    def result = [
    				"st rattr 0x${device.deviceNetworkId} 0x${analogInputCluster()} 0x${powerConfigurationCluster()} 0x${batteryVoltageAttribute()}",
                    "delay 1000",
                    "st rattr 0x${device.deviceNetworkId} 0x${analogInputCluster()} 0x${doorLockCluster()} 0x${lockStateAttribute()}"
    			 ];
    
    log.trace("refresh() - Exit: ${result}");
    
	return result;
}