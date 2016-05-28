/**
 *  PetSafe Electronic SmartDoor
 *
 *  Copyright 2016 Daniel Schulte
 *
 */
metadata({
	definition(name: "Electronic SmartDoor", namespace: "danschultewhpc", author: "Daniel Schulte", {
		capability("Battery"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#battery
		capability("Refresh"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#refresh

		fingerprint(profileId: "0104", // Home Automation: http://docs.smartthings.com/en/latest/device-type-developers-guide/definition-metadata.html#fingerprinting
        			// Cluster Definitions: http://www.zigbee.org/download/standards-zigbee-cluster-library/
        			inClusters: [//"0000", // Basic
                                 //powerConfigurationCluster()
                                 //"0003", // Identify
                                 //"0009", // Alarms
                                 //"000A", // Time
                                 //"0101"  // Door lock
                                 ].join(" "),
                    outClusters: [//"0000", // Basic
                                 //powerConfigurationCluster()
                                 //"0003", // Identify
                                 //"0009", // Alarms
                                 //"000A", // Time
                                 //"0101"  // Door lock
                                 ].join(" ")
                   );
	});
});

private powerConfigurationCluster() { return "0001"; }
private batteryVoltageAttribute() { return "0020"; }

def parse(String description) {
    log.trace("parse(String) - Enter: ${description}");
    
    def results = [];
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
                    log.debug("Device battery voltage: ${deviceBatteryVoltage}");
                    
                    def singleBatteryMaximumVoltage = 1.5;
                    def singleBatteryMinimumVoltage = 1.0;
                    def installedBatteries = 4;
                    
                    def deviceMinimumVoltage = singleBatteryMinimumVoltage * installedBatteries;
                    def deviceMaximumVoltage = singleBatteryMaximumVoltage * installedBatteries;
                    log.debug("Device minimum voltage: ${deviceMinimumVoltage}");
                    
                    def deviceBatteryLevel = (deviceBatteryVoltage - deviceMinimumVoltage) / (deviceMaximumVoltage - deviceMinimumVoltage);
                    log.debug("Device battery level: ${deviceBatteryLevel}");
                    
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
                    			
                    results = createEvent(batteryResult);
                }
            }
        }
        else {
        	log.warn("Unrecognized description prefix");
        }
    }
    else {
    	log.warn("Invalid description");
    }
    
    log.trace("parse(String) - Exit: ${results}");
    
	return results;
}

def refresh() {
	log.trace("refresh() - Enter");
    
    def result = [
        "st rattr 0x${device.deviceNetworkId} 0x000C 0x0001 0x0020"
    ];
    
    log.trace("refresh() - Exit: ${result}");
    
    return result
}