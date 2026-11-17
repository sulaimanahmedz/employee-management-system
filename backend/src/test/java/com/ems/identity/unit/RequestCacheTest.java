package com.ems.identity.unit;

import com.ems.identity.service.RequestCacheConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class RequestCacheTest {
    @Test void rejectedRequestCannotCreateSessionStoreQueryOrReplay() {
        var cache = new RequestCacheConfiguration().requestCache();
        var request = new MockHttpServletRequest("GET", "/api/v1/employees");
        request.setQueryString("search=synthetic-private-value");
        var response = new MockHttpServletResponse();
        cache.saveRequest(request, response);
        assertNull(request.getSession(false));
        assertNull(cache.getRequest(request, response));
        assertNull(cache.getMatchingRequest(request, response));
        var session = request.getSession();
        session.setAttribute("unrelated", "preserved");
        cache.saveRequest(request, response); cache.removeRequest(request, response);
        assertEquals(java.util.List.of("unrelated"), java.util.Collections.list(session.getAttributeNames()));
        assertEquals("preserved", session.getAttribute("unrelated"));
        assertNull(cache.getRequest(request, response));
    }
}
