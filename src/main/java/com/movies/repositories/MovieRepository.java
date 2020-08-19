package com.movies.repositories;

import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.movies.models.Movie;

@Repository
public class MovieRepository {

	@Value("${omdb.database.url}")
	private String omdbUrl;

	@Value("${omdb.api.key}")
	private String omdbKey;

	@Value("${elasticsearch.host.server}")
	String host;

	@Value("${elasticsearch.host.port.one}")
	int portOne;

	@Value("${elasticsearch.host.port.two}")
	int portTwo;

	@Value("${elasticsearch.scheme}")
	String scheme;

	private static RestHighLevelClient RestHighLevelClient;
	private static ObjectMapper ObjectMapper = new ObjectMapper();

	private static final String ELASTIC_MOVIE_INDEX = "movie";
	private static final String OMDB_API_KEY_VARIABLE = "apiKey";
	private static final String OMDB_MOVIE_NAME_VARIABLE = "t";

	/**
	 * Get the movie from Omdb database using movie name
	 * 
	 * @param getRequest
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws URISyntaxException
	 */
	public JsonObject getMovieFromOmdb(String name) throws IOException, ClientProtocolException, URISyntaxException {
		URIBuilder urlb = new URIBuilder(omdbUrl);
		urlb.setParameter(OMDB_API_KEY_VARIABLE, omdbKey).setParameter(OMDB_MOVIE_NAME_VARIABLE, name);
		HttpGet getRequest = new HttpGet(urlb.build());
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(getRequest)) {

			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					// return it as a String
					String result = EntityUtils.toString(entity);
					JsonElement jsonElement = JsonParser.parseString(result);
					JsonObject omdbJsonObject = jsonElement.getAsJsonObject();
					return omdbJsonObject;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
	}

	/**
	 * Get the movie from local database using name
	 * 
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public Movie getMovieFromLocalDb(String name) throws IOException {
		try {
			makeConnection(host, portOne, portTwo, scheme);
			GetRequest getMovieRequest = new GetRequest(ELASTIC_MOVIE_INDEX, name);
			GetResponse getResponse = null;
			try {
				getResponse = RestHighLevelClient.get(getMovieRequest, RequestOptions.DEFAULT);
			} catch (java.io.IOException e) {
				e.getLocalizedMessage();
			}
			if (getResponse.isExists()) {
				return ObjectMapper.convertValue(getResponse.getSourceAsMap(), Movie.class);
			} else {
				return null;
			}
		} finally {
			closeConnection();
		}
	}

	/**
	 * Make connection to Local ElasticSearch
	 * 
	 * @param host
	 * @param portOne
	 * @param portTwo
	 * @param scheme
	 */
	private synchronized void makeConnection(String host, int portOne, int portTwo, String scheme) {

		if (RestHighLevelClient == null) {
			RestHighLevelClient = new RestHighLevelClient(
					RestClient.builder(new HttpHost(host, portOne, scheme), new HttpHost(host, portTwo, scheme)));
		}
	}

	/**
	 * Close connection to Local ElasticSearch
	 * 
	 * @throws IOException
	 */
	private synchronized void closeConnection() throws IOException {
		RestHighLevelClient.close();
		RestHighLevelClient = null;
	}

	/**
	 * Insert movie document into ElasticSearch index Movie
	 * 
	 * To do - Make it async if database large
	 * 
	 * @param movie
	 * @throws IOException 
	 */
	public void insertMovie(Movie movie) throws IOException {

		makeConnection(host, portOne, portTwo, scheme);
		byte[] bytes = ObjectMapper.writeValueAsBytes(movie);
		IndexRequest indexRequest = new IndexRequest(ELASTIC_MOVIE_INDEX).id(movie.getName()).source(bytes,
				XContentType.JSON);
		try {
			RestHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
		} catch (ElasticsearchException e) {
			e.getDetailedMessage();
		} catch (java.io.IOException ex) {
			ex.getLocalizedMessage();
			// System.exit
		}
		closeConnection();
	}
}
