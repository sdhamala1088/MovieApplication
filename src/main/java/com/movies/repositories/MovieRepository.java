package com.movies.repositories;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.InternalMultiBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.movies.models.MoviesAndAggregations;
import com.movies.models.Artist;
import com.movies.models.Director;
import com.movies.models.Movie;
import com.movies.models.MovieSearchFilter;
import com.movies.models.MovieSearchFilter.RangeYear;

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

	private static ObjectMapper ObjectMapper = new ObjectMapper();
	private static RestHighLevelClient RestHighLevelClient;
	private static final String ELASTIC_MOVIE_INDEX = "movie";
	private static final String OMDB_API_KEY_VARIABLE = "apiKey";
	private static final String OMDB_MOVIE_NAME_VARIABLE = "t";
	private static final String MOVIE = "movie";
	private static final String DOT = ".";

	/**
	 * Get the movie from Omdb database using movie name
	 * 
	 * @param getRequest
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws URISyntaxException
	 */
	public JsonObject getJsonObjectFromOmdb(String name)
			throws IOException, ClientProtocolException, URISyntaxException {
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
	 * Rewrite to use querybuilder & add mapping
	 * 
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public MoviesAndAggregations getMoviesByPrefix(String name) throws IOException {
		try {
			makeConnection(host, portOne, portTwo, scheme);
			SearchRequest searchRequest = new SearchRequest(ELASTIC_MOVIE_INDEX);
			QueryBuilder queryBuilder = QueryBuilders.matchQuery(Movie.MovieVariables.NAME.getValue(), name)
					.operator(Operator.AND).fuzziness(Fuzziness.ONE);
			addQueriesToSearchRequest(queryBuilder, null, searchRequest);
			return getMoviesAndAggregations(searchRequest);
		} finally {
			closeConnection();
		}
	}

	private MoviesAndAggregations getMoviesAndAggregations(SearchRequest searchRequest) {
		MoviesAndAggregations moviesAndAggregations = new MoviesAndAggregations();
		List<Movie> movies = new ArrayList<>();
		SearchResponse searchResponse = null;
		try {
			searchResponse = RestHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (java.io.IOException e) {
			e.getLocalizedMessage();
		}
		return restCallResponseToReturnType(moviesAndAggregations, movies, searchResponse);
	}

	private MoviesAndAggregations restCallResponseToReturnType(MoviesAndAggregations moviesAndAggregations,
			List<Movie> movies, SearchResponse searchResponse) {
		if (searchResponse.getHits() != null) {
			SearchHit[] results = searchResponse.getHits().getHits();
			Arrays.stream(results).forEach(hit -> {
				movies.add(ObjectMapper.convertValue(hit.getSourceAsMap(), Movie.class));
			});
			moviesAndAggregations.setMovies(movies);
		}
		if (searchResponse.getAggregations() != null) {
			mapAggregation(searchResponse.getAggregations(), moviesAndAggregations);
		}
		return moviesAndAggregations;
	}

	private void mapAggregation(Aggregations aggregations, MoviesAndAggregations moviesAndAggregations) {

		for (org.elasticsearch.search.aggregations.Aggregation aggregation : aggregations) {
			MoviesAndAggregations.Facets facet = new MoviesAndAggregations.Facets();
			extractAggregation(moviesAndAggregations, facet, (MultiBucketsAggregation) aggregation);
		}
	}

	private <T extends MultiBucketsAggregation> void extractAggregation(MoviesAndAggregations moviesAndAggregations,
			MoviesAndAggregations.Facets facet, T aggregate) {
		facet.setName(aggregate.getName());
		for (MultiBucketsAggregation.Bucket bucket : aggregate.getBuckets()) {
			if (bucket.getDocCount() != 0) {
				Map<String, Long> keyDocCount = new HashMap<>();
				String key = bucket.getKeyAsString();
				long docCount = bucket.getDocCount();
				if (bucket instanceof Histogram.Bucket) {
					String startYear = bucket.getKeyAsString().substring(0, 4);
					String endYear = String.valueOf(Integer.valueOf(startYear) + 1);
					key = startYear + "-" + endYear;
				}
				keyDocCount.put(key, docCount);
				facet.addKeyDocCount(keyDocCount);
			}
		}
		moviesAndAggregations.addAggregation(facet);
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

	/**
	 * Get all movies in local database To do - add scroll & size if db too large
	 * >10000 items
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<Movie> getAllMovies() throws IOException {
		try {
			makeConnection(host, portOne, portTwo, scheme);
			SearchRequest searchRequest = new SearchRequest(MOVIE);
			return getMoviesAndAggregations(searchRequest).getMovies();
		} finally {
			closeConnection();
		}
	}

	public <T> Movie updateMovie(String variable, T value, String movieName) throws IOException {
		makeConnection(host, portOne, portTwo, scheme);
		UpdateRequest updateRequest = new UpdateRequest(ELASTIC_MOVIE_INDEX, movieName);
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		builder.field(variable, value);
		builder.endObject();
		updateRequest.doc(builder);
		RestHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
		closeConnection();
		return getMovieFromLocalDb(movieName);
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

	public MoviesAndAggregations getRankedMoviesAndAggregations() throws IOException {
		try {
			makeConnection(host, portOne, portTwo, scheme);
			SearchRequest searchRequest = new SearchRequest(ELASTIC_MOVIE_INDEX);
			QueryBuilder hasRankQuery = QueryBuilders.existsQuery(Movie.MovieVariables.RANK.getValue());
			QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(hasRankQuery);
			AggregationBuilder yearAggregation = AggregationBuilders.dateHistogram("moviesFiveYears")
					.field(Movie.MovieVariables.YEAR.getValue()).calendarInterval(DateHistogramInterval.YEAR);
			String nameAttribute = DOT + Movie.MovieVariables.NAME.getValue();
			List<AggregationBuilder> termsAggregations = addTermsAggregations(Arrays.asList(
					Movie.MovieVariables.GENRE.getValue(), Movie.MovieVariables.DIRECTOR.getValue() + nameAttribute,
					Movie.MovieVariables.ARTIST.getValue() + nameAttribute, Movie.MovieVariables.LANGUAGE.getValue()));

			addQueriesToSearchRequest(queryBuilder,
					Stream.concat(Arrays.asList(yearAggregation).stream(), termsAggregations.stream())
							.collect(Collectors.toList()),
					searchRequest);
			return getMoviesAndAggregations(searchRequest);
		} finally {
			closeConnection();
		}
	}

	private void addQueriesToSearchRequest(QueryBuilder searchQuery, List<AggregationBuilder> aggregationQueries,
			SearchRequest searchRequest) {
		if (searchQuery == null & aggregationQueries == null) {
			return;
		}
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		if (searchQuery != null) {
			sourceBuilder.query(searchQuery);
		}
		if (aggregationQueries != null) {
			for (AggregationBuilder ab : aggregationQueries) {
				sourceBuilder.aggregation(ab);
			}
		}
		searchRequest.source(sourceBuilder);
	}

	private List<AggregationBuilder> addTermsAggregations(List<String> terms) {
		List<AggregationBuilder> termsAggregations = new ArrayList<>();
		for (String term : terms) {
			AggregationBuilder termAggregation = AggregationBuilders.terms(term).field(term);
			termsAggregations.add(termAggregation);
		}
		return termsAggregations;
	}

	private QueryBuilder shouldQueryBuilder(Queue<QueryBuilder> queriesToOr, BoolQueryBuilder sQuery) {
		if (queriesToOr.size() == 0) {
			return sQuery;
		}
		sQuery.should((QueryBuilder) queriesToOr.peek());
		queriesToOr.poll();
		return shouldQueryBuilder(queriesToOr, sQuery);
	}

	private <T> QueryBuilder termQueryBuilder(String field, T value) {
		return QueryBuilders.termQuery(field, value);
	}

	private QueryBuilder mustQueryBuilder(Queue<QueryBuilder> queriesToAnd, BoolQueryBuilder mQuery) {
		if (queriesToAnd.size() == 0) {
			return mQuery;
		}
		mQuery.must((QueryBuilder) queriesToAnd.peek());
		queriesToAnd.poll();
		return mustQueryBuilder(queriesToAnd, mQuery);
	}

	private <T extends Number> QueryBuilder rangeQueryBuilder(String field, T from, T to) {
		RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(field);
		rangeQueryBuilder.from(from);
		rangeQueryBuilder.to(to);
		return rangeQueryBuilder;
	}

	public List<Movie> getFilteredMovies(MovieSearchFilter movieSearchFilter) throws IOException {
		try {
			makeConnection(host, portOne, portTwo, scheme);
			SearchRequest searchRequest = new SearchRequest(ELASTIC_MOVIE_INDEX);
			addQueriesToSearchRequest(buildQueryOffOfMovieSearchFilters(movieSearchFilter), null, searchRequest);
			return getMoviesAndAggregations(searchRequest).getMovies();
		} finally {
			closeConnection();
		}
	}

	private QueryBuilder buildQueryOffOfMovieSearchFilters(MovieSearchFilter movieSearchFilter) {
		rangeQueryBuilder(RangeYear.getField(), movieSearchFilter.getRangeYear().getFromYear(),
				movieSearchFilter.getRangeYear().getToYear());
		List<QueryBuilder> directorQueries = movieSearchFilter.getDirectorList().stream().map(Director::getName)
				.map(x -> termQueryBuilder(Movie.MovieVariables.DIRECTOR.getValue(), x)).collect(Collectors.toList());
		List<QueryBuilder> artistQueries = movieSearchFilter.getArtists().stream().map(Artist::getName)
				.map(x -> termQueryBuilder(Movie.MovieVariables.ARTIST.getValue(), x)).collect(Collectors.toList());
		List<QueryBuilder> genreQueries = movieSearchFilter.getGenres().stream()
				.map(x -> termQueryBuilder(Movie.MovieVariables.GENRE.getValue(), x)).collect(Collectors.toList());
		List<QueryBuilder> languageQueries = movieSearchFilter.getLanguages().stream()
				.map(x -> termQueryBuilder(Movie.MovieVariables.LANGUAGE.getValue(), x)).collect(Collectors.toList());

		QueryBuilder directorsShould = shouldQueryBuilder(new LinkedList<>(directorQueries), new BoolQueryBuilder());
		QueryBuilder artistsShould = shouldQueryBuilder(new LinkedList<>(artistQueries), new BoolQueryBuilder());
		QueryBuilder genresShould = shouldQueryBuilder(new LinkedList<>(genreQueries), new BoolQueryBuilder());
		QueryBuilder languagesShould = shouldQueryBuilder(new LinkedList<>(languageQueries), new BoolQueryBuilder());
		return mustQueryBuilder(
				new LinkedList<>(Arrays.asList(directorsShould, artistsShould, genresShould, languagesShould)),
				new BoolQueryBuilder());
	}
}
