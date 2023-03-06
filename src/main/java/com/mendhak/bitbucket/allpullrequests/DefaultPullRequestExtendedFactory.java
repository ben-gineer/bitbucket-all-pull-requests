/**
 *
 */
package com.mendhak.bitbucket.allpullrequests;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.comment.CommentSearchRequest;
import com.atlassian.bitbucket.comment.CommentSeverity;
import com.atlassian.bitbucket.property.PropertyMap;
import com.atlassian.bitbucket.pull.*;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bitbucket.scm.ScmService;
import com.mendhak.bitbucket.allpullrequests.utils.PropertiesMapper;

import javax.annotation.Nonnull;


/**
 * @author jwagan
 */
public class DefaultPullRequestExtendedFactory implements PullRequestExtendedFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPullRequestExtendedFactory.class);
    public static final String Bitbucket_PROPERTIES_FILENAME = "bitbucket-all-pull-requests.properties";

    private final PullRequestService pullRequestService;

    private final CommentService commentService;

    private final ScmService scmService;

    private final Properties properties;

    public DefaultPullRequestExtendedFactory(PullRequestService pullRequestService,
                                             ScmService scmService, CommentService commentService) throws IOException {
        this.pullRequestService = pullRequestService;
        this.scmService = scmService;
        this.commentService = commentService;

        this.properties = initializeProperties(Bitbucket_PROPERTIES_FILENAME);
    }

    @Override
    public PullRequestExtended getPullRequestExtended(PullRequest pullRequest) {

        Page<Comment> taskComments = getTaskCountPerPullRequest(pullRequest);
        PullRequestExtended pullRequestExtended;

        if (pullRequest.getState().equals(PullRequestState.OPEN)) {
            pullRequestExtended = new PullRequestExtended(pullRequest, pullRequestService.canMerge(
                    pullRequest.getToRef().getRepository().getId(), pullRequest.getId()), taskComments);

            if (isConflicted(pullRequest)) {
                CustomPullRequestMergeVeto veto = new CustomPullRequestMergeVeto(
                        properties.getProperty(PropertiesMapper.MERGE_CONFLICT_SUMMARY_MESSAGE),
                        properties.getProperty(PropertiesMapper.MERGE_CONFLICT_DETAILED_MESSAGE));
                pullRequestExtended.addCustomMergeVeto(veto);
            }
        } else {
            final PullRequestMergeability mergeability = new PullRequestMergeability() {

                @Override
                public boolean isConflicted() {
                    return false;
                }

                @Override
                public Collection<PullRequestMergeVeto> getVetoes() {
                    return new ArrayList<PullRequestMergeVeto>();
                }

                @Override
                public boolean canMerge() {
                    return false;
                }

                @Nonnull
                @Override
                public PullRequestMergeOutcome getOutcome() {
                    return PullRequestMergeOutcome.CLEAN;
                }

                @Nonnull
                @Override
                public PropertyMap getProperties() {
                    return PropertyMap.EMPTY;
                }
            };

            pullRequestExtended = new PullRequestExtended(pullRequest, mergeability, taskComments);
            String message;
            if (pullRequest.getState().equals(PullRequestState.MERGED)) {
                message = properties.getProperty("pullRequest.mergeability.alreadymerged");
                //DECLINED
            } else {
                message = properties.getProperty("pullRequest.mergeability.declined");
            }

            pullRequestExtended.addCustomMergeVeto(new CustomPullRequestMergeVeto(message, message));
        }

        return pullRequestExtended;
    }


    private Page<Comment> getTaskCountPerPullRequest(PullRequest pullRequest) {
        return commentService.search(
                new CommentSearchRequest.Builder(pullRequest)
                        .severity(CommentSeverity.BLOCKER)
                        .build(), new PageRequestImpl(1, 10)
        );
    }

    private boolean isConflicted(PullRequest pullRequest) {
        //Boolean canMerge = this.scmService.getPullRequestCommandFactory(pullRequest).canMerge().call();
        PullRequestMergeResult mergeResult = this.scmService.getPullRequestCommandFactory(pullRequest).tryMerge(pullRequest).call();

        //return ((canMerge != null) && (!(canMerge.booleanValue())));
        return mergeResult.getOutcome() == PullRequestMergeOutcome.CONFLICTED;
    }

    private Properties initializeProperties(String fileName) throws IOException {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            LOGGER.warn("Unable to load properties from given file: %s. Error mesage: %s", fileName, e.getMessage());
            throw e;
        }
        return properties;
    }

    class CustomPullRequestMergeVeto implements PullRequestMergeVeto {

        private final String summaryMessage;
        private final String detailedMessage;

        public CustomPullRequestMergeVeto(String summaryMessage, String detailedMessage) {
            this.summaryMessage = summaryMessage;
            this.detailedMessage = detailedMessage;
        }

        @Override
        public String getSummaryMessage() {
            return summaryMessage;
        }

        @Override
        public String getDetailedMessage() {
            return detailedMessage;
        }

    }

}
