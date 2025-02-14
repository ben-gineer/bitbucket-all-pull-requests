package com.mendhak.bitbucket.allpullrequests.utils;


import java.io.File;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.atlassian.sal.api.ApplicationProperties;

/**
 * Programmatically configure our logging.
 * 
 * For details, see: http://logback.qos.ch/manual/configuration.html
 * 
 * @author cmyers
 * 
 */
public class PluginLoggerFactory {

    private static final String ROOT = "com.mendhak.bitbucket.allpullrequests";
    private static final Logger Bitbucket_ROOT_LOGGER = LoggerFactory.getLogger("ROOT");

    private LoggerContext context;

    private final String homeDir;

    public PluginLoggerFactory(ApplicationProperties applicationProperties) {
        homeDir = applicationProperties.getHomeDirectory().getAbsolutePath();
        init();
    }

    public PluginLoggerFactory() {
        homeDir = new File(".").getAbsolutePath();
        init();
    }

    private void init() {
        // Assumes LSF4J is bound to logback
        context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // store the home dir to use for relative paths
        context.putProperty("Bitbucket.home", homeDir);

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);

        InputStream is;
        is = this.getClass().getClassLoader().getResourceAsStream("logback-test.xml");
        if (is != null) {
            Bitbucket_ROOT_LOGGER.info("Using logback-test.xml for logger settings");
        } else {
            Bitbucket_ROOT_LOGGER.info("Using logback.xml for logger settings");
            is = this.getClass().getClassLoader().getResourceAsStream("logback.xml");
        }

        try {
            configurator.doConfigure(is);
        } catch (JoranException e) {
            System.err.println("Error configuring logging framework" + e);
        }
    }

    public Logger getLogger() {
        return getLogger(ROOT);
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }

    public Logger getLogger(Class<? extends Object> clazz) {
        String className = clazz.toString();
        if (className.startsWith("class ")) {
            className = className.replaceFirst("class ", "");
        }

        return context.getLogger(className);

    }

    public Logger getLoggerForThis(Object obj) {
        String className = obj.getClass().toString();
        if (className.startsWith("class ")) {
            className = className.replaceFirst("class ", "");
        }

        return context.getLogger(className);

    }
}
