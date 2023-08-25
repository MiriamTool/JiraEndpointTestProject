package endpoints

import com.adaptavist.hapi.jira.issues.exceptions.IssueRetrievalException
import com.atlassian.jira.issue.changehistory.ChangeHistory
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager
import com.atlassian.jira.issue.history.ChangeItemBean
import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.user.UserProfile
import groovy.json.JsonBuilder
import beans.DataHistory

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MultivaluedMap
import java.time.LocalDate

class IssueHistory {

    final static String allowedUsername = 'test'

    String getHistory(MultivaluedMap queryParams, HttpServletRequest request) {

        if (!validateUser(request)) {
            throw new Exception("Your user does not have an access to this endpoint")
        }

        String issueKey = queryParams.getFirst("issueKey")
        String date = queryParams.getFirst("sinceDate")

        List<String> errorList = validateParameters(issueKey, date)

        if (!errorList.isEmpty()) {
            throw new Exception("Error processing queryParameters. Error List: " + errorList.join(", "))
        }
        Issue issue
        try {
            issue = Issues.getByKey(issueKey)
        } catch (IssueRetrievalException e) {
            throw new Exception("No issue with provided key: " + issueKey + " was found")
        }

        ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager()

        List<DataHistory> changesList = []
        List<ChangeHistory> changeHistoryList

        if (date) {
            changeHistoryList = changeHistoryManager.getChangeHistoriesSince(issue, LocalDate.parse(date).toDate())
        } else {
            changeHistoryList = changeHistoryManager.getChangeHistories(issue)
        }

        for (changes in changeHistoryList) {

            for (ChangeItemBean item in changes.changeItemBeans) {
                DataHistory historyCurrent = new DataHistory(
                        from: item.from,
                        to: item.to,
                        date: item.created.dateString,
                        field: item.field,
                        author: changes.authorObject.username)
                changesList.add(historyCurrent)
            }

        }

        return new JsonBuilder(changesList).toString()
    }

    private Boolean validateUser(HttpServletRequest request) {
        UserManager userManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
        UserProfile userProfile = userManager.getRemoteUser(request)
        return userProfile.username == allowedUsername
    }

    private List<String> validateParameters(String issueKey, String date) {
        List<String> errorList = []
        if (!issueKey) {
            errorList.add("No issueKey provided")
        }
        if (date) {
            try {
                LocalDate.parse(date)
            } catch (Exception e) {
                errorList.add("Date can not be parsed. Check format of your input")
            }
        }
        return errorList
    }
}