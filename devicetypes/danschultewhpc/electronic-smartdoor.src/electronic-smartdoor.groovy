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
        capability("Switch Level"); // http://docs.smartthings.com/en/latest/capabilities-reference.html#switch-level

		fingerprint(profileId: homeAutomationProfile(),
        			inClusters: [powerConfigurationCluster(),
                    			 doorLockCluster()
                                 ].join(" "),
                    outClusters: [].join(" ")
                   );
	});
    
    simulator({
    });
    
    tiles({
 		standardTile("refresh", "device.lock", inactiveLabel: false, decoration: "flat", {
 			state("default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh");
 		});
        
 		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", {
 			state("battery", label:'${currentValue}% battery', unit:"");
 		});
        
        valueTile("lock", "device.lock", inactiveLabel: false, decoration: "flat", {
        	state("lock", label:'${currentValue}', unit:"");
        });
        
        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", {
        	state("level", label:'${currentValue}', unit:"");
        });
 
 		main("refresh");
 		details(["refresh", "battery", "lock", "level"]);
 	});
});

 // http://docs.smartthings.com/en/latest/device-type-developers-guide/definition-metadata.html#fingerprinting
private homeAutomationProfile() { return "0104"; }

private analogInputCluster() { return "000C"; }

private powerConfigurationCluster() { return "0001"; }
private batteryVoltageAttribute() { return "0020"; }

private doorLockCluster() { return "0101"; }
private lockStateAttribute() { return "0000"; }
private doorStateAttribute() { return "0003"; }

// Defined in http://www.silabs.com/Support%20Documents/TechnicalDocs/AF-V2-API.pdf
private dataTypes()
{
	return [
        unsignedInteger8Bit: "20",
        
    	enumeration8Bit: "30"
    ];
}

def parse(String description)
{
    log.trace("parse(String) - Enter: ${description}");
    
    def result = [];
    if (description)
    {
    	if (description.startsWith("read attr - "))
        {
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
                    
                    def batteryResult = [:];
                    batteryResult.name = "battery";
                    batteryResult.value = deviceBatteryPercentage;
                    batteryResult.descriptionText = "${getLinkText(device)} battery is at ${deviceBatteryPercentage}%";
                    result = createEvent(batteryResult);
                    
                    log.debug("BatteryVoltage: ${deviceBatteryVoltage}");
                    log.info(batteryResult.descriptionText);
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
                	String lockState;
                    if (descriptionMap.value == "00")
                    {
                    	lockState = "Not fully locked";
                    }
                	else if (descriptionMap.value == "01")
                    {
                    	lockState = "Locked";
                    }
                    else if(descriptionMap.value == "02")
                    {
                    	lockState = "Unlocked";
                    }
                    else if(descriptionMap.value == "ff")
                    {
                    	lockState = "not defined";
                    }
                    else
                    {
                    	lockState = "Reserved (${descriptionMap.value})";
                    }
                    
                    if (lockState == "Locked" || lockState == "Unlocked")
                    {
                    	def lockStateResult = [:];
                    	lockStateResult.name = "lock";
                        lockStateResult.value = lockState.toLowerCase();
                    	lockStateResult.descriptionText = "${getLinkText(device)} is ${lockState}";
                        result = createEvent(lockStateResult);
                        
                        log.debug("LockState: ${lockState}");
                        log.info(lockStateResult.descriptionText);
                    }
                    else
                    {
                    	log.warn("Unrecognized LockState: ${lockState}");
                    }
                }
                else if (descriptionMap.attrId == doorStateAttribute())
                {
                	String doorState;
                    if (descriptionMap.value == "00")
                    {
                    	doorState = "Open";
                    }
                    else if (descriptionMap.value == "01")
                    {
                    	doorState = "Closed";
                    }
                    else if (descriptionMap.value == "02")
                    {
                    	doorState = "Error (jammed)";
                    }
                    else if (descriptionMap.value == "03")
                    {
                    	doorState = "Error (forced open)";
                    }
                    else if (descriptionMap.value == "04")
                    {
                    	doorState = "Error (unspecified)";
                    }
                    else if (descriptionMap.value == "ff")
                    {
                    	doorState = "not defined";
                    }
                    else
                    {
                    	doorState = "Reserved (${descriptionMap.value})";
                    }
                    
                    if (doorState == "Open" || doorState == "Closed" || doorState.startsWith("Error"))
                    {
                    	log.debug("DoorState: ${doorState}");
                    }
                    else
                    {
                    	log.warn("Unrecognized DoorState: ${doorState}");
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
        else if (description.startsWith("catchall:"))
        {
         	def catchall = zigbee.parse(description);
            
            String profileId = Integer.toHexString(catchall.profileId).padLeft(4, "0");
            if (profileId == homeAutomationProfile())
            {
            	String clusterId = Integer.toHexString(catchall.clusterId).padLeft(4, "0");
                if (clusterId == doorLockCluster())
                {
                	String command = Integer.toHexString(catchall.command).padLeft(4, "0");
                    if (command == "0020")
                    {
                    	def lockModeByte = catchall.data[1];
                    	def lockModeHex = Integer.toHexString(lockModeByte).padLeft(2, "0");
                        String lockMode;
                        if (lockModeHex == "0d")
                        {
                        	lockMode = "Locked";
                        }
                        else if (lockModeHex == "0e")
                        {
                        	lockMode = "Unlocked";
                        }
                        else if (lockModeHex == "0a")
                        {
                        	lockMode = "Automatic";
                        }
                        else
                        {
                        	log.warn("Unrecognized LockMode: ${lockModeHex}");
                        }
                        
                        if (lockMode == "Locked" || lockMode == "Unlocked" || lockMode == "Automatic")
                        {
                        	def lockModeResult = [:];
                            lockModeResult.name = "level";
                            lockModeResult.value = lockModeByte;
                            lockModeResult.descriptionText = "${getLinkText(device)} mode is ${lockMode}";
                            result = createEvent(lockModeResult);

                            log.debug("LockMode: ${lockMode}");
                            log.info(lockModeResult.descriptionText);
                        }
                    }
                    else
                    {
                    	log.warn("Unrecognized doorLockCluster command: ${command}");
                    }
                }
                else
                {
                	log.warn("Unrecognized clusterId: ${clusterId}");
                }
            }
            else
            {
            	log.warn("Unrecognized profileId: ${profileId}");
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
    
    def result = [
    	//Lock Reporting
        "zcl global send-me-a-report 0x${doorLockCluster()} 0x${lockStateAttribute()} 0x${dataTypes().enumeration8Bit} 0 300 {01}",
        "delay 500",
        "send 0x${device.deviceNetworkId} 1 1",
        "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 0x0C 0x01 0x${doorLockCluster()} {${device.zigbeeId}} {}",
        "delay 500",
        
        //Battery Reporting
        "zcl global send-me-a-report 0x${powerConfigurationCluster()} 0x${batteryVoltageAttribute()} 0x${dataTypes().unsignedInteger8Bit} 0 300 {}",
        "delay 500",
        "send 0x${device.deviceNetworkId} 1 1",
        "delay 500",
        "zdo bind 0x${device.deviceNetworkId} 0x0C 0x01 0x${powerConfigurationCluster()} {${device.zigbeeId}} {}"
    ];
    
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
                    "delay 500",
                    "st rattr 0x${device.deviceNetworkId} 0x${analogInputCluster()} 0x${doorLockCluster()} 0x${lockStateAttribute()}",
                    "delay 500",
                    "st rattr 0x${device.deviceNetworkId} 0x${analogInputCluster()} 0x${doorLockCluster()} 0x${doorStateAttribute()}"
    			 ];
    
    log.trace("refresh() - Exit: ${result}");
    
	return result;
}