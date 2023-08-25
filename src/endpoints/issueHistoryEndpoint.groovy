package endpoints

import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript


import javax.ws.rs.core.MultivaluedMap
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response

@BaseScript CustomEndpointDelegate delegate

issueHistory(httpMethod: "GET", groups: ["jira-software"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->

    IssueHistory issueHistory = new IssueHistory()

    try {
        String response = issueHistory.getHistory(queryParams, request)
        return Response.ok(response).build()
    } catch (Exception e) {
        return Response.serverError().entity([error: e.message]).build()
    }
}