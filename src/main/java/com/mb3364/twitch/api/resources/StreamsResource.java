package com.mb3364.twitch.api.resources;

import com.mb3364.http.RequestParams;
import com.mb3364.twitch.api.auth.Scopes;
import com.mb3364.twitch.api.handlers.*;
import com.mb3364.twitch.api.models.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The {@link StreamsResource} provides the functionality
 * to access the <code>/streams</code> endpoints of the Twitch API.
 *
 * @author Matthew Bell
 */
public class StreamsResource extends AbstractResource
{

    Stream stream = null;
    /**
     * Construct the resource using the Twitch API base URL and specified API version.
     *
     * @param baseUrl    the base URL of the Twitch API
     * @param apiVersion the requested version of the Twitch API
     */
    public StreamsResource(String baseUrl, int apiVersion) {
        super(baseUrl, apiVersion);
    }

    final StreamResponseHandler streamResponseHandler = new StreamResponseHandler()
    {

        @Override
        public void onSuccess(Stream stream)
        {
            StreamsResource.this.stream = stream;
            setLastRequestSuccessful(true);
        }

        @Override
        public void onFailure(int statusCode, String statusMessage, String errorMessage)
        {
            System.out.println("Twitch API get failed: " + statusCode);
            setLastRequestSuccessful(false);
        }

        @Override
        public void onFailure(Throwable throwable)
        {
            System.out.println("Twitch API get failed (throw): " + throwable.getMessage());
            setLastRequestSuccessful(false);
        }
    };

    final TwitchHttpResponseHandler twitchHttpResponseHandler = new TwitchHttpResponseHandler(streamResponseHandler) {
        @Override
        public void onSuccess(int statusCode, Map<String, List<String>> headers, String content) {
            try {
                System.out.println(content);
                StreamContainer value = objectMapper.readValue(content, StreamContainer.class);
                streamResponseHandler.onSuccess(value.getStream());
            } catch (IOException e) {
                streamResponseHandler.onFailure(e);
            }
        }
    };
    /**
     * Returns a stream object.
     * <p>The stream object in the onSuccess() response will be <code>null</code> if the stream is offline.</p>
     *
     * @param channelName the name of the Channel
     * @param handler     the response handler
     */
    public void get(final String channelName, final StreamResponseHandler handler) {
        String url = String.format("%s/streams/%s", getBaseUrl(), channelName);

        HTTP_ASYNC.get(url, new TwitchHttpResponseHandler(handler) {
            @Override
            public void onSuccess(int statusCode, Map<String, List<String>> headers, String content) {
                try {
                    StreamContainer value = objectMapper.readValue(content, StreamContainer.class);
                    handler.onSuccess(value.getStream());
                } catch (IOException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    /**
     * Synchronous version of com.mb3364.twitch.api.resources.ChannelsResource#get(com.mb3364.twitch.api.handlers.ChannelResponseHandler)
     * Returns a channel object of authenticated user. Channel object includes stream key.
     * <p>Authenticated, required scope: {@link Scopes#CHANNEL_READ}</p>
     */
    public Stream get(String channelName) {
        String url = String.format("%s/streams/%s", getBaseUrl(), channelName);
        HTTP_SYNC.get(url, twitchHttpResponseHandler);
        System.out.println(stream);
        if (isLastRequestSuccessful())
            return stream;
        else
            return null;
    };

    /**
     * Returns a list of stream objects that are queried by a number of parameters
     * sorted by number of viewers descending.
     *
     * @param params  the optional request parameters:
     *                <ul>
     *                <li><code>game</code>:  Streams categorized under <code>game</code>.</li>
     *                <li><code>channel</code>:  Streams from a comma separated list of channels.</li>
     *                <li><code>limit</code>:  Maximum number of objects in array. Default is 25. Maximum is 100.</li>
     *                <li><code>offset</code>: Object offset for pagination. Default is 0.</li>
     *                <li><code>client_id</code>: Only shows streams from applications of <code>client_id</code>.</li>
     *                </ul>
     * @param handler the response handler
     */
    public void get(final RequestParams params, final StreamsResponseHandler handler) {
        String url = String.format("%s/streams", getBaseUrl());

        HTTP_ASYNC.get(url, params, new TwitchHttpResponseHandler(handler) {
            @Override
            public void onSuccess(int statusCode, Map<String, List<String>> headers, String content) {
                try {
                    Streams value = objectMapper.readValue(content, Streams.class);
                    handler.onSuccess(value.getTotal(), value.getStreams());
                } catch (IOException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    /**
     * Returns a list of stream objects that are queried by a number of parameters
     * sorted by number of viewers descending.
     *
     * @param handler the response handler
     */
    public void get(final StreamsResponseHandler handler) {
        get(new RequestParams(), handler);
    }

    /**
     * Returns a list of featured (promoted) stream objects.
     *
     * @param params  the optional request parameters:
     *                <ul>
     *                <li><code>limit</code>:  Maximum number of objects in array. Default is 25. Maximum is 100.</li>
     *                <li><code>offset</code>: Object offset for pagination. Default is 0.</li>
     *                </ul>
     * @param handler the response handler
     */
    public void getFeatured(final RequestParams params, final FeaturedStreamResponseHandler handler) {
        String url = String.format("%s/streams/featured", getBaseUrl());

        HTTP_ASYNC.get(url, params, new TwitchHttpResponseHandler(handler) {
            @Override
            public void onSuccess(int statusCode, Map<String, List<String>> headers, String content) {
                try {
                    FeaturedStreamContainer value = objectMapper.readValue(content, FeaturedStreamContainer.class);
                    handler.onSuccess(value.getFeatured());
                } catch (IOException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    /**
     * Returns a list of featured (promoted) stream objects.
     *
     * @param handler the response handler
     */
    public void getFeatured(final FeaturedStreamResponseHandler handler) {
        getFeatured(new RequestParams(), handler);
    }

    /**
     * Returns a summary of current streams.
     *
     * @param game    Only show stats for the set game
     * @param handler the response handler
     */
    public void getSummary(final String game, final StreamsSummaryResponseHandler handler) {
        String url = String.format("%s/streams/summary", getBaseUrl());
        RequestParams params = new RequestParams();
        params.put("game", game);

        HTTP_ASYNC.get(url, params, new TwitchHttpResponseHandler(handler) {
            @Override
            public void onSuccess(int statusCode, Map<String, List<String>> headers, String content) {
                try {
                    StreamsSummary value = objectMapper.readValue(content, StreamsSummary.class);
                    handler.onSuccess(value);
                } catch (IOException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    /**
     * Returns a summary of current streams.
     *
     * @param handler the response handler
     */
    public void getSummary(final StreamsSummaryResponseHandler handler) {
        String url = String.format("%s/streams/summary", getBaseUrl());

        HTTP_ASYNC.get(url, new TwitchHttpResponseHandler(handler) {
            @Override
            public void onSuccess(int statusCode, Map<String, List<String>> headers, String content) {
                try {
                    StreamsSummary value = objectMapper.readValue(content, StreamsSummary.class);
                    handler.onSuccess(value);
                } catch (IOException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    /**
     * Returns a list of stream objects that the authenticated user is following.
     * Authenticated, required scope: {@link Scopes#USER_READ}
     *
     * @param params  the optional request parameters:
     *                <ul>
     *                <li><code>limit</code>:  Maximum number of objects in array. Default is 25. Maximum is 100.</li>
     *                <li><code>offset</code>: Object offset for pagination. Default is 0.</li>
     *                </ul>
     * @param handler the response handler
     */
    public void getFollowed(final RequestParams params, final StreamsResponseHandler handler) {
        String url = String.format("%s/streams/followed", getBaseUrl());

        HTTP_ASYNC.get(url, params, new TwitchHttpResponseHandler(handler) {
            @Override
            public void onSuccess(int statusCode, Map<String, List<String>> headers, String content) {
                try {
                    Streams value = objectMapper.readValue(content, Streams.class);
                    handler.onSuccess(value.getTotal(), value.getStreams());
                } catch (IOException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    /**
     * Returns a list of stream objects that the authenticated user is following.
     * Authenticated, required scope: {@link Scopes#USER_READ}
     *
     * @param handler the response handler
     */
    public void getFollowed(final StreamsResponseHandler handler) {
        getFollowed(new RequestParams(), handler);
    }
}
