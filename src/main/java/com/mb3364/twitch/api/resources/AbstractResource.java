package com.mb3364.twitch.api.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.mb3364.http.AsyncHttpClient;
import com.mb3364.http.HttpClient;
import com.mb3364.http.StringHttpResponseHandler;
import com.mb3364.http.SyncHttpClient;
import com.mb3364.twitch.api.handlers.BaseFailureHandler;
import com.mb3364.twitch.api.models.Error;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AbstractResource is the abstract base class of a Twitch resource.
 * A resource provides the functionality to access the REST endpoints of the Twitch API.
 *
 * @author Matthew Bell
 */
public abstract class AbstractResource {

    protected static final ObjectMapper objectMapper = new ObjectMapper(); // can reuse
    protected static final AsyncHttpClient HTTP_ASYNC = new AsyncHttpClient(); // can reuse
    protected static final SyncHttpClient HTTP_SYNC = new SyncHttpClient(); // can reuse
    private long lastSuccessfulUpdate = 0;
    private boolean lastRequestSuccessful = false;
    private final String baseUrl; // Base url for twitch rest api

    /**
     * Construct a resource using the Twitch API base URL and specified API version.
     *
     * @param baseUrl    the base URL of the Twitch API
     * @param apiVersion the requested version of the Twitch API
     */
    protected AbstractResource(String baseUrl, int apiVersion) {
        this.baseUrl = baseUrl;
        HTTP_ASYNC.setHeader("ACCEPT", "application/vnd.twitchtv.v" + Integer.toString(apiVersion) + "+json"); // Specify API version
        HTTP_SYNC.setHeader("ACCEPT", "application/vnd.twitchtv.v" + Integer.toString(apiVersion) + "+json"); // Specify API version
        configureObjectMapper();
    }

    protected void setLastSuccessfulUpdate(long millis) {
        lastSuccessfulUpdate = millis;
    }

    public long getLastSuccessfulUpdate() {
        return lastSuccessfulUpdate;
    }

    public void setLastRequestSuccessful(boolean success) {
        lastRequestSuccessful = success;
        if (success)
            setLastSuccessfulUpdate(System.currentTimeMillis());
    }

    public boolean isLastRequestSuccessful() {
        return lastRequestSuccessful;
    }

    /**
     * Configure the Jackson JSON {@link ObjectMapper} to properly parse the API responses.
     */
    private void configureObjectMapper() {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }

    /**
     * Sets the authentication access token to be included in the HTTP headers of each
     * API request.
     *
     * @param accessToken the user's authentication access token
     */
    public void setAuthAccessToken(String accessToken) {
        if (accessToken != null && accessToken.length() > 0) {
            HTTP_ASYNC.setHeader("Authorization", String.format("OAuth %s", accessToken));
            HTTP_SYNC.setHeader("Authorization", String.format("OAuth %s", accessToken));
        } else {
            HTTP_ASYNC.removeHeader("Authorization");
            HTTP_SYNC.removeHeader("Authorization");
        }
    }

    /**
     * Sets the application's client ID to be included in the HTTP headers of each API request.
     *
     * @param clientId the application's client ID
     */
    public void setClientId(String clientId) {
        if (clientId != null && clientId.length() > 0) {
            HTTP_ASYNC.setHeader("Client-ID", clientId);
            HTTP_SYNC.setHeader("Client-ID", clientId);
        } else {
            HTTP_ASYNC.removeHeader("Client-ID");
            HTTP_SYNC.removeHeader("Client-ID");
        }
    }

    /**
     * Get the base URL to the Twitch API. Intended to be called by subclasses when generating
     * their resource URL endpoint.
     *
     * @return the base URL to the Twitch API
     */
    protected String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Handles HTTP response's from the Twitch API.
     * <p>Since all Http failure logic is the same, we handle it all in one place: here.</p>
     */
    protected static abstract class TwitchHttpResponseHandler extends StringHttpResponseHandler {

        private BaseFailureHandler apiHandler;

        public TwitchHttpResponseHandler(BaseFailureHandler apiHandler) {
            this.apiHandler = apiHandler;
        }

        @Override
        public abstract void onSuccess(int statusCode, Map<String, List<String>> headers, String content);

        @Override
        public void onFailure(int statusCode, Map<String, List<String>> headers, String content) {
            try {
                if (content.length() > 0) {
                    Error error = objectMapper.readValue(content, Error.class);
                    apiHandler.onFailure(statusCode, error.getStatusText(), error.getMessage());
                } else {
                    apiHandler.onFailure(statusCode, "", "");
                }
            } catch (IOException e) {
                apiHandler.onFailure(e);
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            apiHandler.onFailure(throwable);
        }
    }
}
