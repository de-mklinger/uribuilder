/*
 * Copyright mklinger GmbH - https://www.mklinger.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mklinger.micro.uribuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * URI builder.
 *
 * <p>
 * Create a new instance using one of the {@code UriBuilder.of(...)} factory
 * methods.
 * </p>
 *
 * <p>
 * Semantics and internal state of this class is modeled after the internals of
 * the {@link URI} class, except that only host-based authorities are supported.
 * </p>
 *
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class UriBuilder {
	private static final String UTF_8 = "UTF-8";

	// based on http://stackoverflow.com/questions/2849756/list-of-valid-characters-for-the-fragment-identifier-in-an-url
	// and https://tools.ietf.org/html/rfc3986#section-3.5
	private static final String ADDITIONAL_ALLOWED_FRAGMENT_CHARACTERS = "!$&'()*+,;=_~:@/?";

	private String scheme;
	private String fragment;

	// Server-based authority: [<userInfo>@]<host>[:<port>]
	private String userInfo;
	private String host;
	private int port = -1; // -1 ==> undefined

	// Only one of path and pathComponents may be non-null at the same time
	private String path;
	private boolean absolutePath = true;
	private List<String> pathComponents;

	// Only one of query and queryParameters may be non-null at the same time
	private String query;
	private List<QueryParameter> queryParameters;

	/**
	 * Factory method.
	 */
	public static UriBuilder of(final URI url) {
		return new UriBuilder(
				url.getScheme(),
				url.getRawUserInfo(),
				url.getHost(),
				url.getPort(),
				url.getRawPath(),
				url.getRawQuery(),
				url.getRawFragment());
	}

	/**
	 * Factory method.
	 */
	public static UriBuilder of(final URL url) {
		Objects.requireNonNull(url);
		try {
			return of(url.toURI());
		} catch (final URISyntaxException e) {
			throw new UncheckedURISyntaxException(e);
		}
	}

	/**
	 * Factory method.
	 */
	public static UriBuilder of(final String url) {
		requireText(url);
		try {
			return of(new URI(url.trim()));
		} catch (final URISyntaxException e) {
			throw new UncheckedURISyntaxException(e);
		}
	}

	private UriBuilder(final String scheme,
			final String userInfo, final String host, final int port,
			final String path, final String query, final String fragment) {
		scheme(scheme);
		rawUserInfo(userInfo);
		host(host);
		port(port);
		rawPath(path);
		rawQuery(query);
		rawFragment(fragment);
	}

	private static String emptyToNull(final String s) {
		if (s != null && s.isEmpty()) {
			return null;
		}
		return s;
	}

	/**
	 * Set the scheme.
	 * @param scheme The scheme, e.g. "https" - <code>null</code> to not use a
	 * 		scheme in the URL.
	 */
	public UriBuilder scheme(final String scheme) {
		this.scheme = emptyToNull(scheme);
		return this;
	}

	/**
	 * Set the raw fragment.
	 * @param fragment The URL-encoded fragment without leading "#" -
	 * 		<code>null</code> to not use a fragment in the URL
	 */
	public UriBuilder rawFragment(final String fragment) {
		requireUrlEncoded(fragment, ADDITIONAL_ALLOWED_FRAGMENT_CHARACTERS);
		this.fragment = emptyToNull(fragment);
		return this;
	}

	/**
	 * Set the fragment.
	 * @param fragment The non-encoded fragment without leading "#" -
	 * 		<code>null</code> to not use a fragment in the URL
	 */
	public UriBuilder fragment(final String fragment) {
		this.fragment = encodeFragment(emptyToNull(fragment));
		return this;
	}

	/**
	 * Set the raw user info.
	 * @param userInfo The URL-encoded user info, e.g. "username:password" -
	 * 		<code>null</code> to not use user info in the URL
	 */
	public UriBuilder rawUserInfo(final String userInfo) {
		requireUrlEncoded(userInfo, ":");
		this.userInfo = emptyToNull(userInfo);
		return this;
	}

	/**
	 * Set the user info.
	 * @param username The non-URL-encoded username
	 * @param password The non-URL-encoded password
	 */
	public UriBuilder userInfo(final String username, final String password) {
		if (username == null) {
			this.userInfo = null;
			return this;
		}
		Objects.requireNonNull(password);
		this.userInfo = urlEncode(username).concat(":").concat(urlEncode(password));
		return this;
	}

	/**
	 * Set the host.
	 * @param host The host, e.g. "www.haufe-lexware.com" -
	 * 		<code>null</code> to not use a host in the URL
	 */
	public UriBuilder host(final String host) {
		this.host = emptyToNull(host);
		return this;
	}

	/**
	 * Set the port.
	 * @param port The port, e.g. 8080 -
	 * 		<code>-1</code> to not use a port in the URL
	 */
	public UriBuilder port(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * Set the raw path.
	 * @param path The URL-encoded path, e.g. "/mypath" -
	 * 		<code>null</code> to not use a path in the URL
	 */
	public UriBuilder rawPath(final String path) {
		requireUrlEncoded(path, "/");
		this.pathComponents = null;
		this.path = emptyToNull(path);
		return this;
	}

	/**
	 * Append the given path to the current path.
	 * @param path The URL-encoded path, e.g. "/mypath".
	 */
	public UriBuilder appendRawPath(final String path) {
		requireUrlEncoded(path, "/");
		initPath();
		if (this.path == null) {
			return rawPath(path);
		}
		if (path.startsWith("/")) {
			if (this.path.endsWith("/")) {
				if (path.length() > 1) {
					this.path = this.path.concat(path.substring(1));
				}
			} else {
				this.path = this.path.concat(path);
			}
		} else {
			if (this.path.endsWith("/")) {
				this.path = this.path.concat(path);
			} else {
				this.path = this.path.concat("/").concat(path);
			}
		}
		return this;
	}

	/**
	 * Treat the path as relative. Relative paths are illegal if a host is set.
	 * By default, the path is treated absolute.
	 */
	public UriBuilder relativePath(final boolean relativePath) {
		return absolutePath(!relativePath);
	}

	/**
	 * Treat the path as absolute. Relative paths are illegal if a host is set.
	 * By default, the path is treated absolute.
	 */
	public UriBuilder absolutePath(final boolean absolutePath) {
		initPathComponents();
		this.absolutePath = absolutePath;
		return this;
	}

	/**
	 * Add a path component.
	 * @param path The path component non-URL-escaped
	 */
	public UriBuilder pathComponent(final String path) {
		requireText(path);
		initPathComponents();
		pathComponents.add(urlEncode(path));
		return this;
	}

	/**
	 * Add path components.
	 * @param paths The path components non-URL-escaped
	 */
	public UriBuilder pathComponents(final String... paths) {
		Objects.requireNonNull(paths);
		initPathComponents();
		for (final String path : paths) {
			requireText(path);
			pathComponents.add(urlEncode(path));
		}
		return this;
	}

	private void initPathComponents() {
		if (pathComponents == null) {
			pathComponents = new ArrayList<>();
			if (path != null && !path.isEmpty()) {
				absolutePath = path.startsWith("/");
				final StringTokenizer st = new StringTokenizer(path, "/");
				while (st.hasMoreTokens()) {
					pathComponents.add(st.nextToken());
				}
			}
			path = null;
		} else if (path != null) {
			// both are set
			throw new IllegalStateException();
		}
	}

	private void initPath() {
		if (path == null) {
			if (pathComponents != null && !pathComponents.isEmpty()) {
				final StringBuilder sb = new StringBuilder();
				for (final String pathComponent : pathComponents) {
					if (absolutePath || sb.length() != 0) {
						sb.append('/');
					}
					sb.append(pathComponent);
				}
				path = sb.toString();
			}
			pathComponents = null;
		} else if (pathComponents != null) {
			// both are set
			throw new IllegalStateException();
		}
	}

	/**
	 * Set the raw query.
	 * @param query The URL-encoded query without leading "?", e.g.
	 * 		"key=value&x=y" - <code>null</code> to not use a query in the URL
	 */
	public UriBuilder rawQuery(final String query) {
		requireUrlEncoded(query, "&=");
		this.query = emptyToNull(query);
		return this;
	}

	/**
	 * Add a parameter to the query. Multiple calls to this method with an equal
	 * name will append this parameter multiple times.
	 * @param name The parameter key, non-URL-encoded
	 * @param value The parameter value, non-URL-encoded, may be <code>null</code>
	 */
	public UriBuilder addParameter(final String name, final String value) {
		if (queryParameters == null) {
			initQueryParameters();
		}
		queryParameters.add(new QueryParameter(name, value));
		return this;
	}

	/**
	 * Add a parameter to the query. Multiple calls to this method with an equal
	 * name will append this parameter multiple times.
	 * @param name The parameter key, non-URL-encoded
	 * @param value The parameter value
	 */
	public UriBuilder addParameter(final String name, final int value) {
		return addParameter(name, String.valueOf(value));
	}

	/**
	 * Add a parameter to the query. Multiple calls to this method with an equal
	 * name will append this parameter multiple times.
	 * @param name The parameter key, non-URL-encoded
	 * @param value The parameter value
	 */
	public UriBuilder addParameter(final String name, final long value) {
		return addParameter(name, String.valueOf(value));
	}

	/**
	 * Add a parameter to the query. Multiple calls to this method with an equal
	 * name will append this parameter multiple times.
	 * @param name The parameter key, non-URL-encoded
	 * @param value The parameter value
	 */
	public UriBuilder addParameter(final String name, final boolean value) {
		return addParameter(name, String.valueOf(value));
	}

	/**
	 * Add parameters to the query.
	 * @param parameters A map of non-URL-encoded keys to non-URL-encoded values
	 */
	public UriBuilder addParameters(final Map<String, String> parameters) {
		if (queryParameters == null) {
			initQueryParameters();
		}
		for (final Entry<String, String> e : parameters.entrySet()) {
			queryParameters.add(new QueryParameter(e.getKey(), e.getValue()));
		}
		return this;
	}

	/**
	 * Remove all parameters with the given name.
	 */
	public UriBuilder removeParameters(final String name) {
		Objects.requireNonNull(name);
		if (queryParameters == null) {
			initQueryParameters();
		}
		for (final Iterator<QueryParameter> iterator = queryParameters.iterator(); iterator.hasNext();) {
			final QueryParameter p = iterator.next();
			if (name.equals(p.name)) {
				iterator.remove();
			}
		}
		return this;
	}

	/**
	 * Remove all existing parameters with the given name and add a new
	 * parameter with given name and value.
	 */
	public UriBuilder setParameter(final String name, final String value) {
		return removeParameters(name).addParameter(name, value);
	}

	/**
	 * Remove all existing parameters with the given name and add a new
	 * parameter with given name and value.
	 */
	public UriBuilder setParameter(final String name, final int value) {
		return removeParameters(name).addParameter(name, value);
	}

	/**
	 * Remove all existing parameters with the given name and add a new
	 * parameter with given name and value.
	 */
	public UriBuilder setParameter(final String name, final long value) {
		return removeParameters(name).addParameter(name, value);
	}

	/**
	 * Remove all existing parameters with the given name and add a new
	 * parameter with given name and value.
	 */
	public UriBuilder setParameter(final String name, final boolean value) {
		return removeParameters(name).addParameter(name, value);
	}

	/**
	 * Get the scheme.
	 * @return The scheme or <code>null</code> if the scheme is undefined.
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Get the user info, URL-encoded.
	 * @return The user info or <code>null</code> if the user info is undefined
	 */
	public String getRawUserInfo() {
		return userInfo;
	}

	/**
	 * Get the host.
	 * @return the host or <code>null</code> if the host is undefined
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Get the port.
	 * @return The port or <code>-1</code> if the port is undefined.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Get the raw, URL-encoded path.
	 * @return the path or <code>null</code> if the path is undefined
	 */
	public String getRawPath() {
		initPath();
		return path;
	}

	/**
	 * Get the not URL encoded path components.
	 * @return The path components or an empty list if the path is
	 * 		undefined - never <code>null</code>.
	 */
	public List<String> getPathComponents() {
		initPathComponents();
		if (pathComponents == null) {
			throw new IllegalStateException();
		}
		if (pathComponents.isEmpty()) {
			return Collections.emptyList();
		}
		if (pathComponents.size() == 1) {
			return Collections.singletonList(urlDecode(pathComponents.get(0)));
		}
		final List<String> result = new ArrayList<>(pathComponents.size());
		for (final String pathComponent : pathComponents) {
			result.add(urlDecode(pathComponent));
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Return <code>true</code> if the path is absolute. In case of an
	 * undefined path, the return value of this method is undefined.
	 * @return <code>true</code> if the path is absolute. In case of an
	 * 		undefined path, the return value of this method is undefined.
	 */
	public boolean isAbsolutePath() {
		return absolutePath;
	}

	/**
	 * Return <code>true</code> if the path is relative. In case of an
	 * undefined path, the return value of this method is undefined.
	 * @return <code>true</code> if the path is relative. In case of an
	 * 		undefined path, the return value of this method is undefined.
	 */
	public boolean isRelativePath() {
		return !absolutePath;
	}

	/**
	 * Get the current fragment string, decoded, without leading '#'.
	 * @return the fragment or <code>null</code> if the fragment is undefined
	 */
	public String getFragment() {
		if (fragment == null || fragment.isEmpty()) {
			return fragment;
		}
		return urlDecode(fragment);
	}

	/**
	 * Get the current fragment string, URL encoded, without leading '#'.
	 * @return the fragment or <code>null</code> if the fragment is undefined
	 */
	public String getRawFragment() {
		return fragment;
	}

	/**
	 * Get the current query string, URL encoded, without leading '?'.
	 * @return the query or <code>null</code> if the query is undefined
	 */
	public String getRawQuery() {
		initQuery();
		return query;
	}

	/**
	 * Get the value for the given parameter name. If multiple parameters
	 * with the given name exist, only the first one is returned. A return
	 * value of <code>null</code> may indicate two different things: <ol>
	 * <li>There is no such parameter</li>
	 * <li>The first parameter with this name has no value (e.g. a query like
	 * {@code "?name"})</li></ol>
	 */
	public String getParameterValue(final String name) {
		Objects.requireNonNull(name);
		if (queryParameters == null) {
			initQueryParameters();
		}
		for (final QueryParameter p : queryParameters) {
			if (name.equals(p.name)) {
				return p.value;
			}
		}
		return null;
	}

	/**
	 * Get all parameter values for the given parameter name. The list returned
	 * is never <code>null</code>, but may contain <code>null</code> values
	 * (e.g. for a query like {@code "?name"}).
	 */
	public List<String> getParameterValues(final String name) {
		Objects.requireNonNull(name);
		if (queryParameters == null) {
			initQueryParameters();
		}
		List<String> values = null;
		for (final QueryParameter p : queryParameters) {
			if (name.equals(p.name)) {
				if (values == null) {
					values = new ArrayList<>(1);
				}
				values.add(p.value);
			}
		}
		if (values == null) {
			return Collections.emptyList();
		}
		return values;
	}

	/**
	 * Get all unique parameter names.
	 * @return A set of all parameter names - never <code>null</code>.
	 */
	public Set<String> getParameterNames() {
		if (queryParameters == null) {
			initQueryParameters();
		}
		if (queryParameters.isEmpty()) {
			return Collections.emptySet();
		}
		final Set<String> parameterNames = new HashSet<>();
		for (final QueryParameter queryParameter : queryParameters) {
			parameterNames.add(queryParameter.name);
		}
		return parameterNames;
	}

	/**
	 * Get a URI object of this builder.
	 */
	public URI toUri() {
		try {
			return new URI(toString());
		} catch (final URISyntaxException e) {
			throw new UncheckedURISyntaxException(e);
		}
	}

	/**
	 * Get a URI object of this builder.
	 */
	public URI build() {
		return toUri();
	}

	/**
	 * Returns the content of the URI built so far as a US-ASCII string.
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		appendScheme(sb);
		appendHost(sb);
		appendPath(sb);
		appendQuery(sb);
		appendFragment(sb);
		return sb.toString();
	}

	/**
	 * Returns the scheme and host part of this URI, without path, parameters and
	 * other additional information as a US-ASCII string.
	 */
	public String toHostString() {
		final StringBuilder sb = new StringBuilder();
		appendScheme(sb);
		appendHost(sb);
		return sb.toString();
	}

	private void appendScheme(final StringBuilder sb) {
		if (scheme != null) {
			sb.append(scheme);
			sb.append(':');
		}
	}

	private void appendHost(final StringBuilder sb) {
		if (host != null) {
			sb.append("//");
			if (userInfo != null) {
				sb.append(userInfo);
				sb.append('@');
			}
			final boolean needBrackets = ((host.indexOf(':') >= 0)
					&& !host.startsWith("[")
					&& !host.endsWith("]"));
			if (needBrackets) {
				sb.append('[');
			}
			sb.append(host);
			if (needBrackets) {
				sb.append(']');
			}
			if (port != -1
					&& !(port == 80 && "http".equals(scheme))
					&& !(port == 443 && "https".equals(scheme))) {
				sb.append(':');
				sb.append(port);
			}
		}
	}

	private void appendPath(final StringBuilder sb) {
		initPath();
		if (path != null) {
			if (!path.startsWith("/") && host != null) {
				// make path absolute on-the-fly
				sb.append('/');
			}
			sb.append(path);
		}
	}

	private void appendQuery(final StringBuilder sb) {
		initQuery();
		if (query != null) {
			sb.append('?');
			sb.append(query);
		}
	}

	private void appendFragment(final StringBuilder sb) {
		if (fragment != null) {
			sb.append('#');
			sb.append(fragment);
		}
	}

	private void initQueryParameters() {
		if (queryParameters == null) {
			queryParameters = new ArrayList<>();
			if (query != null && !query.isEmpty()) {
				final StringTokenizer st = new StringTokenizer(query, "&");
				while (st.hasMoreTokens()) {
					final String pair = st.nextToken();
					queryParameters.add(QueryParameter.ofEncodedPair(pair));
				}
			}
			query = null;
		} else if (query != null) {
			// both are set
			throw new IllegalStateException();
		}
	}

	private void initQuery() {
		if (query == null) {
			if (queryParameters != null) {
				final StringBuilder sb = new StringBuilder();
				for (final QueryParameter parameter : queryParameters) {
					if (sb.length() > 0) {
						sb.append('&');
					}
					parameter.appendEncodedPair(sb);
				}
				query = sb.toString();
			}
			queryParameters = null;
		} else if (queryParameters != null) {
			// both are set
			throw new IllegalStateException();
		}
	}

	private static class QueryParameter {
		private final String name;
		private final String value;

		/**
		 * @param value may be <code>null</code>
		 */
		public QueryParameter(final String name, final String value) {
			requireText(name);
			this.name = name;
			this.value = value;
		}

		public static QueryParameter ofEncodedPair(final String s) {
			if (s.isEmpty()) {
				throw new IllegalArgumentException();
			}
			final int idx = s.indexOf('=');
			if (idx == 0) {
				throw new IllegalArgumentException();
			}
			if (idx == -1) {
				// only name is given, treat as null value
				return ofEncoded(s, null);
			} else if (idx == s.length() - 1) {
				// name= is given, treat as empty string value
				return ofEncoded(s.substring(0, idx), "");
			} else {
				final String encodedName = s.substring(0, idx);
				final String encodedValue = s.substring(idx + 1);
				return ofEncoded(encodedName, encodedValue);
			}
		}

		public static QueryParameter ofEncoded(final String name, final String value) {
			return new QueryParameter(urlDecode(name), urlDecode(value));
		}

		public void appendEncodedPair(final StringBuilder sb) {
			sb.append(urlEncode(name));
			if (value != null) {
				sb.append('=');
				sb.append(urlEncode(value));
			}
		}
	}

	private void requireUrlEncoded(final String s, final String additionalAllowedCharacters) {
		if (s == null || s.isEmpty()) {
			return;
		}
		for (int i = 0; i < s.length(); i++) {
			final char c = s.charAt(i);
			if (!(isUnreservedUrlCharacter(c)
					// additional legal in URL escaped strings:
					|| c == '+' || c == '%'
					// additional legal as per use case:
					|| additionalAllowedCharacters.indexOf(c) != -1)) {
				throw new IllegalArgumentException("Unsufficient URL encoding: " + s);
			}
		}
	}

	/*
	 * RFC 2396 states:
	 * -----
	 * Data characters that are allowed in a URI but do not have a
	 * reserved purpose are called unreserved.  These include upper
	 * and lower case letters, decimal digits, and a limited set of
	 * punctuation marks and symbols.
	 *
	 * unreserved  = alphanum | mark
	 *
	 * mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
	 */
	private static boolean isUnreservedUrlCharacter(final char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
				|| c == '-' || c == '_' || c == '.' || c == '!' || c == '~' || c == '*'
				|| c == '\'' || c == '(' || c == ')';
	}

	private static String encodeFragment(final String fragment) {
		final StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < fragment.length(); idx++) {
			final char c = fragment.charAt(idx);
			if (needsEncoding(c, ADDITIONAL_ALLOWED_FRAGMENT_CHARACTERS)) {
				sb.append(urlEncode(String.valueOf(c)));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static boolean needsEncoding(final char c, final String additionalAllowedCharacters) {
		return !(isUnreservedUrlCharacter(c)
				// additional legal as per use case:
				|| additionalAllowedCharacters.indexOf(c) != -1);
	}

	private static String urlEncode(final String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		try {
			return URLEncoder.encode(s, UTF_8);
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static String urlDecode(final String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		try {
			return URLDecoder.decode(s, UTF_8);
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static String requireText(final String s) {
		if (s == null || s.isEmpty() || s.trim().isEmpty()) {
			throw new IllegalArgumentException();
		}
		return s;
	}
}
