package com.hkshenoy.jaltantraloopsb.helper;
import org.springframework.stereotype.Component;


@Component
public class CustomLogger {

    public void logd(String str) {
        System.err.println("FM: DEBUG: " + str);
    }

    public void logi(String str) {
        System.err.println("FM: INFO: " + str);
    }

    public void logw(String str) {
        System.err.println("FM: WARNING: " + str);
    }

    public void loge(String str) {
        System.err.println("FM: ERROR: " + str);
    }


}
