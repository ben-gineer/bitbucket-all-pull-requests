package sk.oxygene.stash.allpullrequests.rest;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.stash.i18n.I18nService;
import com.atlassian.stash.pull.PullRequestDirection;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.pull.PullRequestState;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.atlassian.stash.rest.util.ResponseFactory;
import com.atlassian.stash.rest.util.RestResource;
import com.atlassian.stash.rest.util.RestUtils;
import com.atlassian.stash.user.PermissionValidationService;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageRequest;
import com.atlassian.stash.util.PageRequestImpl;
import com.google.common.collect.Maps;
import com.sun.jersey.spi.resource.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({RestUtils.APPLICATION_JSON_UTF8})
@Singleton
@AnonymousAllowed
public class AllPullRequestsResource extends RestResource {

    private static final Logger log = LoggerFactory.getLogger(AllPullRequestsResource.class);

    private final PullRequestService pullRequestService;
    private final RepositoryService repositoryService;
    private final PermissionValidationService permissionValidationService;

    public AllPullRequestsResource(PullRequestService pullRequestService, RepositoryService repositoryService, PermissionValidationService permissionValidationService, I18nService i18nService) {
        super(i18nService);
        this.pullRequestService = pullRequestService;
        this.repositoryService = repositoryService;
        this.permissionValidationService = permissionValidationService;
     }

    @GET
    @Path("count")
    public Response getPullRequestCount(@QueryParam("project") String projectKey) {
        permissionValidationService.validateAuthenticated();

        PageRequest pageRequest = new PageRequestImpl(0, 10);
        long count = 0;
        while (pageRequest != null) {
            Page<? extends Repository> page = repositoryService.findByProjectKey(projectKey, pageRequest);
            for (Repository repository : page.getValues()) {
                count += pullRequestService.countInDirection(PullRequestDirection.INCOMING, repository.getId(), PullRequestState.OPEN);
            }
            if (page.getIsLastPage()) {
                break;
            }
            pageRequest = page.getNextPageRequest();
        }

        Map<String, Long> response = Maps.newHashMap();
        response.put("count", count);

        return ResponseFactory.ok(response).build();
    }

}
