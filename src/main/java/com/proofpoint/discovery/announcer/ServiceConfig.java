package com.proofpoint.discovery.announcer;

import com.google.common.base.Objects;
import com.proofpoint.configuration.Config;
import com.proofpoint.units.Duration;
import com.proofpoint.units.MinDuration;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ServiceConfig
{
    private URI checkUri = null;
    private Duration checkInterval = new Duration(10, TimeUnit.SECONDS);
    private Map<String, String> properties = null;

    @NotNull
    public URI getCheckUri()
    {
        return checkUri;
    }

    @Config("check.uri")
    public ServiceConfig setCheckUri(URI checkUri)
    {
        this.checkUri = checkUri;
        return this;
    }

    @MinDuration("1s")
    public Duration getCheckInterval()
    {
        return checkInterval;
    }

    @Config("check.interval")
    public ServiceConfig setCheckInterval(Duration checkInterval)
    {
        this.checkInterval = checkInterval;
        return this;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    @Config("property")
    public ServiceConfig setProperties(Map<String, String> properties)
    {
        this.properties = properties;
        return this;
    }

    // equals/hashcode only for unit tests
    @Override
    public int hashCode()
    {
        return Objects.hashCode(checkUri, checkInterval, properties);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ServiceConfig other = (ServiceConfig) obj;
        return Objects.equal(this.checkUri, other.checkUri) && Objects.equal(this.checkInterval, other.checkInterval) && Objects.equal(this.properties, other.properties);
    }
}
