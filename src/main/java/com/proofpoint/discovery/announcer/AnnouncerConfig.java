package com.proofpoint.discovery.announcer;

import com.proofpoint.configuration.Config;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class AnnouncerConfig
{
    private Map<String, ServiceConfig> services = null;

    @NotNull
    public Map<String, ServiceConfig> getServices()
    {
        return services;
    }

    @Config("service")
    public AnnouncerConfig setServices(Map<String, ServiceConfig> services)
    {
        this.services = services;
        return this;
    }
}