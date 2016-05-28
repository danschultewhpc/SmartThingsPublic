/**
 *  PetSafe Electronic SmartDoor
 *
 *  Copyright 2016 Daniel Schulte
 *
 */
metadata {
	definition (name: "Electronic SmartDoor", namespace: "danschultewhpc", author: "Daniel Schulte") {
		capability "Battery"
		capability "Polling"
		capability "Actuator"
		capability "Refresh"
		capability "Lock"

		fingerprint profileId: "0104", inClusters: "0000 0001 0003 0009 000A 0101", outClusters: "0000 0001 0003 0009 000A 0101"
	}


	simulator {
		// status "locked": "command: 9881, payload: 00 62 03 FF 00 00 FE FE"
		// status "unlocked": "command: 9881, payload: 00 62 03 00 00 00 FE FE"

		// reply "9881006201FF,delay 4200,9881006202": "command: 9881, payload: 00 62 03 FF 00 00 FE FE"
		// reply "988100620100,delay 4200,9881006202": "command: 9881, payload: 00 62 03 00 00 00 FE FE"
	}

	tiles {
		standardTile("toggle", "device.lock", width: 2, height: 2) {
			state "locked", label:'locked', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821", nextState:"unlocking"
			state "unlocked", label:'unlocked', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff", nextState:"locking"
			state "unknown", label:"unknown", action:"lock.lock", icon:"st.locks.lock.unknown", backgroundColor:"#ffffff", nextState:"locking"
			state "locking", label:'locking', icon:"st.locks.lock.locked", backgroundColor:"#79b821"
			state "unlocking", label:'unlocking', icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff"
		}
		standardTile("lock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'lock', action:"lock.lock", icon:"st.locks.lock.locked", nextState:"locking"
		}
		standardTile("unlock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'unlock', action:"lock.unlock", icon:"st.locks.lock.unlocked", nextState:"unlocking"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		standardTile("refresh", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "toggle"
		details(["toggle", "lock", "unlock", "battery", "refresh"])
	}
}

def parse(String description) {
    log.trace "parse(String) - Enter: " + description
    
    def results = []
    if (description) {
    	if (description.startsWith("catchall:")) {
            def cluster = zigbee.parse(description)
	
            boolean ignoredMessage = cluster.profileId != 0x0104 ||
                cluster.command == 0x0B || // 0x0B is default response indicating message got through
                cluster.command == 0x07 || // 0x07 is bind message
                (cluster.data.size() > 0 && cluster.data.first() == 0x3e)

			if (!ignoredMessage) {
                switch(cluster.clusterId) {
                    case 0x0001:
                        results << createEvent(getBatteryResult(cluster.data.last()))
                        break
                }
            }
        }
        else if (description.startsWith("read attr -")) {
            Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
                def nameAndValue = param.split(":")
                map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
            }

            //log.debug "Desc Map: $descMap"
            if (descMap.cluster == "0001" && descMap.attrId == "0020") {
                log.debug "Received battery level report"
                
                def rawBatteryValue = Integer.parseInt(descMap.value, 16)
                
                //log.debug 'Battery'
                def linkText = getLinkText(device)
                def batteryResult = [ name: 'battery' ]

                def volts = rawValue / 10
                def descriptionText
                if (volts > 6.5) {
                    batteryResult.descriptionText = "${linkText} battery has too much power (${volts} volts)."
                }
                else {
                    def minVolts = 4.0
                    def maxVolts = 6.0
                    def pct = (volts - minVolts) / (maxVolts - minVolts)
                    batteryResult.value = Math.min(100, (int) pct * 100)
                    batteryResult.descriptionText = "${linkText} battery was ${result.value}%"
                }
                
                results = createEvent(batteryResult)
            }
            else if (descMap.cluster == "0101" && descMap.attrId == "0000") {
                log.debug "Received lock status"
                def linkText = getLinkText(device)

                if(descMap.value == "01"){
                    results = createEvent( name: 'lock' , value: "locked", descriptionText: "${linkText} is locked")
                }
                else if(descMap.value == "02") {
                    results = createEvent( name: 'lock' , value: "unlocked", descriptionText: "${linkText} is unlocked")
                }

            }
        }
    }
    
    log.trace "parse(String) - Exit: " + results
    
	return results
}

def poll() {
	log.trace "poll() - Enter"
    
    def result = refresh()
    
    log.trace "poll() - Exit: " + result
    
    return result
}

def refresh() {
	log.trace "refresh() - Enter"
    
    def result = [
        "st rattr 0x${device.deviceNetworkId} 0x0C 0x101 0", "delay 500",
        "st rattr 0x${device.deviceNetworkId} 0x0C 1 0x20"
    ]
    
    log.trace "refresh() - Exit: " + result
    
    return result
}

def updated() {
	log.trace "updated() - Enter"
    
	def result = configure()
    
    log.trace "updated() - Exit: " + result
    
    return result
}

def lock() {
	log.trace "lock() - Enter"
    
    sendEvent(name: "lock", value: "locked")
	def result = "st cmd 0x${device.deviceNetworkId} 0x0C 0x101 0 {}"
    
    log.trace "lock() - Exit: " + result
    
    return result
}

def unlock() {
	log.trace "unlock() - Enter"
    
    sendEvent(name: "lock", value: "unlocked")
	def result = "st cmd 0x${device.deviceNetworkId} 0x0C 0x101 1 {}"
    
    log.trace "unlock() - Exit: " + result
    
    return result
}

def configure() {
	log.trace "configure() - Enter"
    
	def decodedHexArray = device.hub.zigbeeId.decodeHex()
    
    int lastIndex = decodedHexArray.length - 1
    for (int i = 0; i < decodedHexArray.length / 2; ++i) {
    	int swapIndex = lastIndex - i
    	byte temp = decodedHexArray[i]
        decodedHexArray[i] = decodedHexArray[swapIndex]
        decodedHexArray[swapIndex] = temp
    }
    
    String zigbeeId = decodedHexArray.encodeHex()
    
	log.debug "Configuring Reporting and Bindings."
	def configCmds = [	

        //Lock Reporting
        "zcl global send-me-a-report 0x101 0 0x30 0 3600 {01}", "delay 500",
        "send 0x${device.deviceNetworkId} 1 1", "delay 1000",

        //Battery Reporting
        "zcl global send-me-a-report 1 0x20 0x20 5 3600 {}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 1", "delay 1500",

        "zdo bind 0x${device.deviceNetworkId} 0x0C 1 0x101 {${device.zigbeeId}} {}", "delay 500",
        "zdo bind 0x${device.deviceNetworkId} 0x0C 1 1 {${device.zigbeeId}} {}"
    
    ]
    def result = configCmds + refresh() // send refresh cmds as part of config
    
    log.trace "configure() - Exit: " + result
    
    return result
}