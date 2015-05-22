package com.proofpoint.discovery.announcer;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import com.proofpoint.discovery.client.announce.Announcer;
import com.proofpoint.discovery.client.announce.ServiceAnnouncement;
import com.proofpoint.http.client.HttpStatus;
import com.proofpoint.http.client.Request;
import com.proofpoint.http.client.Response;
import com.proofpoint.http.client.testing.TestingHttpClient;
import com.proofpoint.http.client.testing.TestingHttpClient.Processor;
import com.proofpoint.http.client.testing.TestingResponse;
import com.proofpoint.node.NodeInfo;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.assertEquals;

public class TestHealthChecker
{
    private Announcer announcer;
    private NodeInfo nodeInfo;

    private HttpStatus checkStatus;
    private boolean checkException;

    private final TestingHttpClient testingHttpClient = new TestingHttpClient(new Processor()
    {
        @Override
        public Response handle(Request request)
                throws Exception
        {
            if (checkException) {
                throw new Exception();
            }
            return new TestingResponse(checkStatus, ImmutableListMultimap.<String, String>of(), new byte[0]);
        }
    });

    @BeforeMethod
    public void setup()
    {
        announcer = mock(Announcer.class);
        nodeInfo = new NodeInfo("test-application",
                "test_environment",
                "test_pool",
                null,
                InetAddresses.forString("10.1.2.3"),
                "testhost.example.com",
                null,
                "external.example.com",
                null);
        checkStatus = HttpStatus.NO_CONTENT;
        checkException = false;
    }

    @Test
    public void testAnnounceHttp()
    {
        HealthChecker checker = new HealthChecker("testService", URI.create("http://localhost:1234/v1/check"), null, testingHttpClient, announcer, nodeInfo);

        checker.run();

        ArgumentCaptor<ServiceAnnouncement> announcementCaptor = ArgumentCaptor.forClass(ServiceAnnouncement.class);
        verify(announcer).addServiceAnnouncement(announcementCaptor.capture());

        ServiceAnnouncement announcement = announcementCaptor.getValue();
        assertEquals(announcement.getType(), "testService");
        assertEquals(announcement.getProperties(), ImmutableMap.of("http", "http://10.1.2.3:1234"));

        verifyNoMoreInteractions(announcer);
    }

    @Test
    public void testAnnounceHttps()
    {
        HealthChecker checker = new HealthChecker("testService", URI.create("https://localhost:1234/v1/check"), null, testingHttpClient, announcer, nodeInfo);

        checker.run();

        ArgumentCaptor<ServiceAnnouncement> announcementCaptor = ArgumentCaptor.forClass(ServiceAnnouncement.class);
        verify(announcer).addServiceAnnouncement(announcementCaptor.capture());

        ServiceAnnouncement announcement = announcementCaptor.getValue();
        assertEquals(announcement.getType(), "testService");
        assertEquals(announcement.getProperties(), ImmutableMap.of("https", "https://testhost.example.com:1234"));

        verifyNoMoreInteractions(announcer);
    }

    @Test
    public void testAnnounceCustomProperties()
    {
        Map<String, String> properties = ImmutableMap.of("foo", "bar", "baz", "quux");
        HealthChecker checker = new HealthChecker("testService", URI.create("http://localhost:1234/v1/check"), properties, testingHttpClient, announcer, nodeInfo);

        checker.run();

        ArgumentCaptor<ServiceAnnouncement> announcementCaptor = ArgumentCaptor.forClass(ServiceAnnouncement.class);
        verify(announcer).addServiceAnnouncement(announcementCaptor.capture());

        ServiceAnnouncement announcement = announcementCaptor.getValue();
        assertEquals(announcement.getType(), "testService");
        assertEquals(announcement.getProperties(), properties);

        verifyNoMoreInteractions(announcer);
    }

    @Test
    public void testNoAnnouncementExceptionFailure()
    {
        HealthChecker checker = new HealthChecker("testService", URI.create("http://localhost:1234/v1/check"), null, testingHttpClient, announcer, nodeInfo);

        checkException = true;
        checker.run();

        verifyNoMoreInteractions(announcer);
    }

    @Test
    public void testNoAnnouncementBadStatus()
    {
        HealthChecker checker = new HealthChecker("testService", URI.create("http://localhost:1234/v1/check"), null, testingHttpClient, announcer, nodeInfo);

        checkStatus = HttpStatus.SERVICE_UNAVAILABLE;
        checker.run();

        verifyNoMoreInteractions(announcer);
    }

    @Test
    public void testRevokeAnnouncement()
    {
        HealthChecker checker = new HealthChecker("testService", URI.create("http://localhost:1234/v1/check"), null, testingHttpClient, announcer, nodeInfo);

        checker.run();
        verify(announcer).addServiceAnnouncement(any(ServiceAnnouncement.class));
        verifyNoMoreInteractions(announcer);

        checkStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        checker.run();
        verify(announcer).addServiceAnnouncement(any(ServiceAnnouncement.class));
        verifyNoMoreInteractions(announcer);

        checkException = true;
        checker.run();
        ArgumentCaptor<ServiceAnnouncement> announcementCaptor = ArgumentCaptor.forClass(ServiceAnnouncement.class);
        verify(announcer).addServiceAnnouncement(announcementCaptor.capture());
        verify(announcer).removeServiceAnnouncement(announcementCaptor.getValue().getId());
        verifyNoMoreInteractions(announcer);
    }

    @Test
    public void testAnnounceAfterFailure()
    {
        HealthChecker checker = new HealthChecker("testService", URI.create("http://localhost:1234/v1/check"), null, testingHttpClient, announcer, nodeInfo);

        checkStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        checker.run();
        verifyNoMoreInteractions(announcer);

        checkException = true;
        checker.run();
        verifyNoMoreInteractions(announcer);

        checkStatus = HttpStatus.OK;
        checkException = false;
        checker.run();
        verify(announcer).addServiceAnnouncement(any(ServiceAnnouncement.class));
        verifyNoMoreInteractions(announcer);
    }

    @Test
    public void testReissueAnnouncement()
    {
        HealthChecker checker = new HealthChecker("testService", URI.create("http://localhost:1234/v1/check"), null, testingHttpClient, announcer, nodeInfo);

        checker.run();
        verify(announcer).addServiceAnnouncement(any(ServiceAnnouncement.class));
        verifyNoMoreInteractions(announcer);

        checkStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        checker.run();
        verify(announcer).addServiceAnnouncement(any(ServiceAnnouncement.class));
        verifyNoMoreInteractions(announcer);

        checkException = true;
        checker.run();
        verify(announcer).addServiceAnnouncement(any(ServiceAnnouncement.class));
        verify(announcer).removeServiceAnnouncement(any(UUID.class));
        verifyNoMoreInteractions(announcer);

        checkStatus = HttpStatus.NO_CONTENT;
        checkException = false;
        checker.run();
        verify(announcer, times(2)).addServiceAnnouncement(any(ServiceAnnouncement.class));
        verify(announcer).removeServiceAnnouncement(any(UUID.class));
        verifyNoMoreInteractions(announcer);
    }
}
