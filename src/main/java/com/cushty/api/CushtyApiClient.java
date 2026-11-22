package com.cushty.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to the configured server. All calls are asynchronous on the OkHttp thread pool; callbacks are
 * invoked on that pool too, never on the client thread.
 */
@Slf4j
@Singleton
public class CushtyApiClient
{
	private static final MediaType PNG = MediaType.parse("image/png");

	private final OkHttpClient http;
	private final Gson gson;

	@Inject
	CushtyApiClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	public interface ResultListener
	{
		void onSuccess(String message);

		void onFailure(String message);
	}

	public interface ValueListener<T>
	{
		void onSuccess(T value);

		void onFailure(String message);
	}

	/**
	 * Validates the configured server URL. HTTPS is required so tokens and screenshots are never
	 * sent in the clear; plain HTTP is only allowed for a bot running on this machine.
	 *
	 * @return the parsed URL, or null if it is unusable
	 */
	public static HttpUrl parseServerUrl(String value)
	{
		if (value == null || value.isBlank())
		{
			return null;
		}
		HttpUrl url = HttpUrl.parse(value.trim());
		if (url == null)
		{
			return null;
		}
		boolean local = url.host().equals("localhost") || url.host().equals("127.0.0.1");
		return url.isHttps() || local ? url : null;
	}

	public void sendEvent(HttpUrl server, String token, EventPayload payload, byte[] png, ResultListener listener)
	{
		RequestBody body = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("payload", gson.toJson(payload))
			.addFormDataPart("screenshot", "screenshot.png", RequestBody.create(PNG, png))
			.build();

		Request request = new Request.Builder()
			.url(server.newBuilder().addPathSegments("v1/events").build())
			.header("Authorization", "Bearer " + token)
			.post(body)
			.build();

		execute(request, listener, json -> listener.onSuccess("Posted to Discord"));
	}

	public void checkConnection(HttpUrl server, String token, ResultListener listener)
	{
		Request request = new Request.Builder()
			.url(server.newBuilder().addPathSegments("v1/me").build())
			.header("Authorization", "Bearer " + token)
			.get()
			.build();

		execute(request, listener, json ->
		{
			String name = json != null && json.has("displayName") ? json.get("displayName").getAsString() : "unknown";
			listener.onSuccess("Connected as " + name);
		});
	}

	public void fetchWatchlist(HttpUrl server, String token, ValueListener<BingoModels.Watchlist> listener)
	{
		Request request = new Request.Builder()
			.url(server.newBuilder().addPathSegments("v1/bingo/watchlist").build())
			.header("Authorization", "Bearer " + token)
			.get()
			.build();

		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Bingo watchlist request failed", e);
				listener.onFailure("Could not reach the Cushty server");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody responseBody = response.body())
				{
					if (response.code() == 404)
					{
						// Bot without bingo enabled.
						BingoModels.Watchlist empty = new BingoModels.Watchlist();
						empty.setActive(false);
						listener.onSuccess(empty);
						return;
					}
					String raw = responseBody != null ? responseBody.string() : null;
					if (!response.isSuccessful())
					{
						listener.onFailure(messageFrom(raw, response.code()));
						return;
					}
					BingoModels.Watchlist watchlist = gson.fromJson(raw, BingoModels.Watchlist.class);
					listener.onSuccess(watchlist != null ? watchlist : new BingoModels.Watchlist());
				}
				catch (IOException | JsonParseException e)
				{
					listener.onFailure("Could not read bingo watchlist");
				}
			}
		});
	}

	public void fetchBoard(HttpUrl server, String token, ValueListener<BingoModels.Board> listener)
	{
		Request request = new Request.Builder()
			.url(server.newBuilder().addPathSegments("v1/bingo/board").build())
			.header("Authorization", "Bearer " + token)
			.get()
			.build();

		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Bingo board request failed", e);
				listener.onFailure("Could not reach the Cushty server");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody responseBody = response.body())
				{
					String raw = responseBody != null ? responseBody.string() : null;
					if (!response.isSuccessful())
					{
						listener.onFailure(messageFrom(raw, response.code()));
						return;
					}
					BingoModels.Board board = gson.fromJson(raw, BingoModels.Board.class);
					if (board == null)
					{
						listener.onFailure("Empty bingo board response");
						return;
					}
					listener.onSuccess(board);
				}
				catch (IOException | JsonParseException e)
				{
					listener.onFailure("Could not read bingo board");
				}
			}
		});
	}

	private void execute(Request request, ResultListener listener, Consumer<JsonObject> onOk)
	{
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Cushty request failed", e);
				listener.onFailure("Could not reach the Cushty server");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody responseBody = response.body())
				{
					JsonObject json = parse(responseBody);
					if (response.isSuccessful())
					{
						onOk.accept(json);
						return;
					}
					String message = json != null && json.has("message")
						? json.get("message").getAsString()
						: "Server returned HTTP " + response.code();
					listener.onFailure(message);
				}
			}
		});
	}

	private JsonObject parse(ResponseBody body)
	{
		if (body == null)
		{
			return null;
		}
		try
		{
			return gson.fromJson(body.charStream(), JsonObject.class);
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return null;
		}
	}

	private String messageFrom(String raw, int code)
	{
		if (raw != null && !raw.isBlank())
		{
			try
			{
				JsonObject json = gson.fromJson(raw, JsonObject.class);
				if (json != null && json.has("message"))
				{
					return json.get("message").getAsString();
				}
			}
			catch (JsonParseException ignored)
			{
				// fall through
			}
		}
		return "Server returned HTTP " + code;
	}
}
