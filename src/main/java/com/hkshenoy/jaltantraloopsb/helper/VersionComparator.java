package com.hkshenoy.jaltantraloopsb.helper;


import org.springframework.stereotype.Component;

@Component
public class VersionComparator {

    // ensure client version is same as server version
    public static int compareVersion(String currVersion, String clientVersion) {
        if(clientVersion == null)
            return 1;
        String[] currParts = currVersion.split("\\.");
        String[] clientParts = clientVersion.split("\\.");
        int length = Math.max(currParts.length, clientParts.length);
        for(int i = 0; i < length; i++) {
            int currPart = i < currParts.length ?
                    Integer.parseInt(currParts[i]) : 0;
            int clientPart = i < clientParts.length ?
                    Integer.parseInt(clientParts[i]) : 0;
            if(currPart < clientPart)
                return -1;
            if(currPart > clientPart)
                return 1;
        }
        return 0;
    }

}
