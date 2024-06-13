package com.hkshenoy.jaltantraloopsb.logging;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.PrintStream;

@Component
public class ConsoleInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleInterceptor.class);

    @PostConstruct
    public void init() {
        // Redirect System.out to the logging system
        System.setOut(new PrintStream(System.out) {
            @Override
            public void println(String message) {
                logger.info(message);
            }
        });

        // Redirect System.err to the logging system
        System.setErr(new PrintStream(System.err) {
            @Override
            public void println(String message) {
                logger.error(message);
            }
        });
    }
}
