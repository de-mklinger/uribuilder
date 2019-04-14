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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class UriBuilderTest {
	private static final String FULL_URL = "http://user:pwd@www.example.com:8080/path1/path2?name1=value1&name1=value2&name2=value3#fragment";
	private static final String NORMAL_URL = "http://www.example.com/path1/path2?name1=value1&name1=value2&name2=value3#fragment";
	private static final String SHORT_URL = "http://www.example.com/path1/path2";
	private static final String HOST_ABSOLUTE_URL = "/path1/path2?name1=value1&name1=value2&name2=value3#fragment";
	private static final String RELATIVE_URL = "path2?name1=value1&name1=value2&name2=value3#fragment";
	private static final String QUERY_ONLY_URL = "?name1=value1&name1=value2&name2=value3#fragment";
	private static final String FRAGMENT_ONLY_URL = "#fragment";

	@Test
	public void testEquality() throws MalformedURLException {
		testEqualities(FULL_URL);
		testEqualities(NORMAL_URL);
		testEqualities(SHORT_URL);
		testEqualities(HOST_ABSOLUTE_URL);
		testEqualities(RELATIVE_URL);
		testEqualities(QUERY_ONLY_URL);
		testEqualities(FRAGMENT_ONLY_URL);
	}

	private void testEqualities(final String url) throws MalformedURLException {
		assertEquals(url, UriBuilder.of(url).toString());
		assertEquals(URI.create(url).toASCIIString(), UriBuilder.of(url).toString());
		assertEquals(URI.create(url), UriBuilder.of(url).toUri());
		assertEquals(URI.create(url).toASCIIString(), UriBuilder.of(URI.create(url)).toString());
		assertEquals(URI.create(url), UriBuilder.of(URI.create(url)).toUri());
		if (url.startsWith("http")) {
			assertEquals(URI.create(url), UriBuilder.of(new URL(url)).toUri());
		}
	}

	@Test
	public void testNulling() {
		assertEquals("//user:pwd@www.example.com:8080/path1/path2?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).scheme(null).toString());
		assertEquals("http://www.example.com:8080/path1/path2?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).rawUserInfo(null).toString());
		assertEquals("http:/path1/path2?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).host(null).toString());
		assertEquals("http://user:pwd@www.example.com/path1/path2?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).port(-1).toString());
		assertEquals("http://user:pwd@www.example.com:8080?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).rawPath(null).toString());
		assertEquals("http://user:pwd@www.example.com:8080/path1/path2#fragment", UriBuilder.of(FULL_URL).rawQuery(null).toString());
		assertEquals("http://user:pwd@www.example.com:8080/path1/path2?name1=value1&name1=value2&name2=value3", UriBuilder.of(FULL_URL).rawFragment(null).toString());
	}

	@Test
	public void testChanging() {
		assertEquals("https://user:pwd@www.example.com:8080/path1/path2?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).scheme("https").toString());
		assertEquals("http://other:pwd2@www.example.com:8080/path1/path2?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).rawUserInfo("other:pwd2").toString());
		assertEquals("http://user:pwd@otherhost:8080/path1/path2?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).host("otherhost").toString());
		assertEquals("http://user:pwd@www.example.com:443/path1/path2?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).port(443).toString());
		assertEquals("http://user:pwd@www.example.com:8080/otherpath?name1=value1&name1=value2&name2=value3#fragment", UriBuilder.of(FULL_URL).rawPath("/otherpath").toString());
		assertEquals("http://user:pwd@www.example.com:8080/path1/path2?yeah#fragment", UriBuilder.of(FULL_URL).rawQuery("yeah").toString());
		assertEquals("http://user:pwd@www.example.com:8080/path1/path2?name1=value1&name1=value2&name2=value3#yeah", UriBuilder.of(FULL_URL).rawFragment("yeah").toString());

		assertEquals("https://other:pwd2@otherhost:8443/otherpath?yeah#yeah", UriBuilder.of(FULL_URL)
				.scheme("https")
				.rawUserInfo("other:pwd2")
				.host("otherhost")
				.port(8443)
				.rawPath("/otherpath")
				.rawQuery("yeah")
				.rawFragment("yeah")
				.toString());
	}

	@Test
	public void testPath() {
		UriBuilder urlBuilder = UriBuilder.of("path");
		urlBuilder.pathComponent("bla");
		assertEquals("path/bla", urlBuilder.toString());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("path");
		urlBuilder.pathComponent("bla/bla");
		assertEquals("path/bla%2Fbla", urlBuilder.toString());
		assertEquals("path/bla%2Fbla", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("path", "bla/bla"), urlBuilder.getPathComponents());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("path");
		urlBuilder.pathComponent("bla");
		Assert.assertTrue(urlBuilder.isRelativePath());
		Assert.assertFalse(urlBuilder.isAbsolutePath());
		urlBuilder.absolutePath(true);
		Assert.assertFalse(urlBuilder.isRelativePath());
		Assert.assertTrue(urlBuilder.isAbsolutePath());
		assertEquals("/path/bla", urlBuilder.toString());
		assertEquals("/path/bla", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("path", "bla"), urlBuilder.getPathComponents());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("/path");
		urlBuilder.pathComponent("bla");
		urlBuilder.relativePath(true);
		assertEquals("path/bla", urlBuilder.toString());
		assertEquals("path/bla", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("path", "bla"), urlBuilder.getPathComponents());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("?");
		urlBuilder.pathComponent("bla");
		assertEquals("/bla", urlBuilder.toString());
		assertEquals("/bla", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("bla"), urlBuilder.getPathComponents());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("path");
		urlBuilder.pathComponents("bla", "blub");
		assertEquals("path/bla/blub", urlBuilder.toString());
		assertEquals("path/bla/blub", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("path", "bla", "blub"), urlBuilder.getPathComponents());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("path");
		urlBuilder.pathComponents("bla/bla", "blub");
		assertEquals("path/bla%2Fbla/blub", urlBuilder.toString());
		assertEquals("path/bla%2Fbla/blub", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("path", "bla/bla", "blub"), urlBuilder.getPathComponents());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("path");
		urlBuilder.pathComponents("bla", "blub");
		urlBuilder.absolutePath(true);
		assertEquals("/path/bla/blub", urlBuilder.toString());
		assertEquals("/path/bla/blub", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("path", "bla", "blub"), urlBuilder.getPathComponents());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("/path");
		urlBuilder.pathComponents("bla", "blub");
		urlBuilder.relativePath(true);
		assertEquals("path/bla/blub", urlBuilder.toString());
		assertEquals("path/bla/blub", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("path", "bla", "blub"), urlBuilder.getPathComponents());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("?");
		urlBuilder.pathComponents("bla", "blub");
		assertEquals("/bla/blub", urlBuilder.toString());
		assertEquals("/bla/blub", urlBuilder.getRawPath());
		assertEquals(Arrays.asList("bla", "blub"), urlBuilder.getPathComponents());
		urlBuilder.toUri();
	}

	@Test
	public void testPathComponents() {
		final UriBuilder urlBuilder = UriBuilder.of("?");
		assertEquals(0, urlBuilder.getPathComponents().size());
	}

	@Test
	public void testParameters() {
		UriBuilder urlBuilder = UriBuilder.of("?name1=value1&name1=value2&name2=value3");
		assertEquals("?name1=value1&name1=value2&name2=value3&name1=value4&name1=value5", urlBuilder
				.addParameter("name1", "value4")
				.addParameters(Collections.singletonMap("name1", "value5"))
				.toString());
		urlBuilder.toUri();

		assertEquals("?xxx", urlBuilder
				.rawQuery("xxx")
				.toString());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("?name1=value1&name1=value2&name2=value3");
		assertEquals("?name1=value1&name1=value2&name2=value3&name1=value4&name1=value5", urlBuilder
				.addParameters(Collections.singletonMap("name1", "value4"))
				.addParameter("name1", "value5")
				.toString());
		urlBuilder.toUri();

		assertEquals("?xxx", urlBuilder
				.rawQuery("xxx")
				.toString());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("?");
		urlBuilder.addParameter("x", null);
		assertEquals("?x", urlBuilder.toString());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("?x&y");
		assertEquals("?x&y&z", urlBuilder.addParameter("z", null).toString());
		urlBuilder.toUri();

		urlBuilder = UriBuilder.of("#");
		assertEquals("?x=1&y=2&z=true", urlBuilder.addParameter("x", 1).addParameter("y", 2L).addParameter("z", true).toString());
		urlBuilder.toUri();

	}

	@Test
	public void testFragment() {
		final UriBuilder urlBuilder = UriBuilder.of("#x");
		urlBuilder.fragment("y/z");
		assertEquals("#y/z", urlBuilder.toString());
		urlBuilder.toUri();

		urlBuilder.fragment("y#z");
		assertEquals("#y%23z", urlBuilder.toString());
		assertEquals("y#z", urlBuilder.getFragment());
		urlBuilder.toUri();

		urlBuilder.fragment("!$&'()*+,;=_~:@/?");
		assertEquals("#!$&'()*+,;=_~:@/?", urlBuilder.toString());
		urlBuilder.toUri();

		urlBuilder.rawFragment("yz");
		assertEquals("#yz", urlBuilder.toString());
		urlBuilder.toUri();

		urlBuilder.rawFragment("!$&'()*+,;=_~:@/?");
		assertEquals("#!$&'()*+,;=_~:@/?", urlBuilder.toString());
		urlBuilder.toUri();

		urlBuilder.fragment("y%z");
		assertEquals("#y%25z", urlBuilder.toString());
		assertEquals("y%z", urlBuilder.getFragment());
		urlBuilder.toUri();
	}

	@Test
	public void testGetFragment() {
		final UriBuilder urlBuilder = UriBuilder.of("#x");
		assertEquals("x", urlBuilder.getFragment());
		urlBuilder.fragment("y/z");
		assertEquals("y/z", urlBuilder.getFragment());
		urlBuilder.fragment("y#z");
		assertEquals("y#z", urlBuilder.getFragment());
		urlBuilder.rawFragment("yz");
		assertEquals("yz", urlBuilder.getFragment());
		urlBuilder.rawFragment("y/z");
		assertEquals("y/z", urlBuilder.getFragment());
	}

	@Test
	public void testGetEmptyFragment() {
		final UriBuilder urlBuilder = UriBuilder.of("#");
		assertNull(urlBuilder.getFragment());
	}

	@Test
	public void testGetEmptyRawFragment() {
		final UriBuilder urlBuilder = UriBuilder.of("#");
		assertNull(urlBuilder.getRawFragment());
	}

	@Test
	public void testGetNullFragment() {
		final UriBuilder urlBuilder = UriBuilder.of("?");
		assertNull(urlBuilder.getFragment());
	}

	@Test
	public void testGetNullRawFragment() {
		final UriBuilder urlBuilder = UriBuilder.of("?");
		assertNull(urlBuilder.getRawFragment());
	}

	@Test
	public void testGetRawFragment() {
		final UriBuilder urlBuilder = UriBuilder.of("#x");
		assertEquals("x", urlBuilder.getRawFragment());
		urlBuilder.fragment("y/z");
		assertEquals("y/z", urlBuilder.getFragment());
		urlBuilder.rawFragment("yz");
		assertEquals("yz", urlBuilder.getRawFragment());
		urlBuilder.rawFragment("y%2Fz");
		assertEquals("y%2Fz", urlBuilder.getRawFragment());
	}

	@Test
	public void testGetRawQuery() {
		final UriBuilder urlBuilder = UriBuilder.of("?x");
		assertEquals("x", urlBuilder.getRawQuery());
		urlBuilder.rawQuery("y%2Fz");
		assertEquals("y%2Fz", urlBuilder.getRawQuery());
		urlBuilder.rawQuery("yz");
		assertEquals("yz", urlBuilder.getRawQuery());
		urlBuilder.rawQuery(null);
		urlBuilder.setParameter("name1", "a/b");
		urlBuilder.setParameter("name2", "value2");
		assertEquals("name1=a%2Fb&name2=value2", urlBuilder.getRawQuery());
	}

	@Test
	public void testUserInfo() {
		final UriBuilder urlBuilder = UriBuilder.of("http://www.example.com");
		urlBuilder.userInfo("x?y", "bla/blub");
		assertEquals("http://x%3Fy:bla%2Fblub@www.example.com", urlBuilder.toString());
		assertEquals("x%3Fy:bla%2Fblub", urlBuilder.getRawUserInfo());
		urlBuilder.toUri();

		urlBuilder.userInfo(null, null);
		assertEquals("http://www.example.com", urlBuilder.toString());
		assertNull(urlBuilder.getRawUserInfo());
		urlBuilder.toUri();
	}

	@Test
	public void testDefaultPorts() {
		assertEquals("http://www.example.com", UriBuilder.of("http://www.example.com:80").toString());
		assertEquals("https://www.example.com", UriBuilder.of("https://www.example.com:443").toString());
		assertEquals("https://www.example.com:80", UriBuilder.of("https://www.example.com:80").toString());
		assertEquals("http://www.example.com:443", UriBuilder.of("http://www.example.com:443").toString());
		assertEquals("http://www.example.com:8080", UriBuilder.of("http://www.example.com:8080").toString());
		assertEquals("http://www.example.com:8443", UriBuilder.of("http://www.example.com:8443").toString());
	}

	@Test
	public void testGetPort() {
		assertEquals(8080, UriBuilder.of("http://www.example.com:8080").getPort());
		assertEquals(-1, UriBuilder.of("http://www.example.com").getPort());
		assertEquals(-1, UriBuilder.of("http://www.example.com:8080").port(-1).getPort());
		assertEquals(9090, UriBuilder.of("http://www.example.com:8080").port(9090).getPort());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr1() {
		UriBuilder.of("?=").addParameter("x", "y");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr2() {
		UriBuilder.of("?x=y").addParameter("", "y");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr3() {
		UriBuilder.of("?x=y").addParameter(null, "y");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr4() {
		UriBuilder.of("?=").addParameters(Collections.singletonMap("x", "y"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr5() {
		UriBuilder.of("?x=y").addParameters(Collections.singletonMap("", "y"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr6() {
		UriBuilder.of("?x=y").addParameters(Collections.singletonMap(null, "y"));
	}

	@Test(expected = UncheckedURISyntaxException.class)
	public void testErr7() {
		UriBuilder.of("?").host("a b").toUri();
	}

	@Test(expected = UncheckedURISyntaxException.class)
	public void testErr8() {
		UriBuilder.of("?").scheme("a%20b=x y").toUri();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr9() {
		UriBuilder.of("?").rawFragment("x#y");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr10() {
		UriBuilder.of("?").rawPath("x/y/z/"); // ok
		UriBuilder.of("?").rawPath("x/y/z/?"); // not ok
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr11() {
		UriBuilder.of("?").rawPath("x/y/z/"); // ok
		UriBuilder.of("?").rawPath("x/y/z/?"); // not ok
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr12() {
		UriBuilder.of("?").rawQuery("äöü"); // not ok
	}

	@Test(expected = IllegalArgumentException.class)
	public void testErr13() {
		UriBuilder.of("?").rawUserInfo("username:pwd"); // ok
		UriBuilder.of("?").rawUserInfo("username:pwd?"); // not ok
	}

	@Test(expected = UncheckedURISyntaxException.class)
	public void testErr14() throws MalformedURLException {
		UriBuilder.of(new URL("file:/Name With Spaces"));
	}

	@Test(expected = UncheckedURISyntaxException.class)
	public void testErr15() {
		UriBuilder.of("file:/Name With Spaces");
	}

	@Test
	public void testEmptyParam() {
		final String s = UriBuilder.of("?test1=&test2").addParameter("test3", null).addParameter("test4", "").toString();
		assertEquals("?test1=&test2&test3&test4=", s);
	}

	@Test
	public void testSetParameter() {
		final String s = UriBuilder.of("?test1=&test2")
				.addParameter("test3", null)
				.addParameter("test1", "again")
				.addParameter("test4", "")
				.setParameter("test1", "xxx").toString();
		assertEquals("?test2&test3&test4=&test1=xxx", s);
	}

	@Test
	public void testSetParameterInt() {
		final String s = UriBuilder.of("?test1=&test2")
				.addParameter("test3", null)
				.addParameter("test1", "again")
				.addParameter("test4", "")
				.setParameter("test1", 1).toString();
		assertEquals("?test2&test3&test4=&test1=1", s);
	}

	@Test
	public void testSetParameterLong() {
		final String s = UriBuilder.of("?test1=&test2")
				.addParameter("test3", null)
				.addParameter("test1", "again")
				.addParameter("test4", "")
				.setParameter("test1", 1L).toString();
		assertEquals("?test2&test3&test4=&test1=1", s);
	}

	@Test
	public void testSetParameterBoolean() {
		final String s = UriBuilder.of("?test1=&test2")
				.addParameter("test3", null)
				.addParameter("test1", "again")
				.addParameter("test4", "")
				.setParameter("test1", true).toString();
		assertEquals("?test2&test3&test4=&test1=true", s);
	}

	@Test
	public void testGetParameterValues() {
		final UriBuilder urlBuilder = UriBuilder.of("?test1=&test2&test1=value1&test2=value2");
		assertEquals("", urlBuilder.getParameterValue("test1"));
		assertNull(urlBuilder.getParameterValue("test2"));
		assertNull(urlBuilder.getParameterValue("doesnotexist"));
		assertEquals(Arrays.asList("", "value1"), urlBuilder.getParameterValues("test1"));
		assertEquals(Arrays.asList(null, "value2"), urlBuilder.getParameterValues("test2"));
		assertEquals(Collections.emptyList(), urlBuilder.getParameterValues("doesnotexist"));
	}

	@Test
	public void testGetParameterNames() {
		assertEquals(newHashSet("test1", "test2"), UriBuilder.of("?test1=&test2&test1=value1&test2=value2").getParameterNames());
		assertEquals(newHashSet("test1"), UriBuilder.of("?test1").getParameterNames());
		assertEquals(Collections.emptySet(), UriBuilder.of("http://test.com").getParameterNames());
	}

	private Object newHashSet(final String... s) {
		return new HashSet<>(Arrays.asList(s));
	}

	@Test
	public void testRelativePath() {
		final UriBuilder builder = UriBuilder.of("/test");
		builder.absolutePath(true);
		assertEquals("/test", builder.toString());
		builder.relativePath(true);
		assertEquals("test", builder.toString());
		builder.absolutePath(true);
		assertEquals("/test", builder.toString());
		builder.relativePath(false);
		assertEquals("/test", builder.toString());
		builder.absolutePath(false);
		assertEquals("test", builder.toString());
	}

	@Test
	public void testPathInternalsSwitch() {
		final UriBuilder builder = UriBuilder.of("/test");
		builder.pathComponent("next");
		builder.rawPath("/new");
		builder.pathComponent("next");
		assertEquals("/new/next", builder.toString());
	}

	@Test
	public void testAppendRawPath() {
		assertEquals("http://test.com/", UriBuilder.of("http://test.com/").appendRawPath("/").toString());
		assertEquals("http://test.com/test", UriBuilder.of("http://test.com").appendRawPath("test").toString());
		assertEquals("http://test.com/test/", UriBuilder.of("http://test.com").appendRawPath("test/").toString());
		assertEquals("http://test.com/test", UriBuilder.of("http://test.com").appendRawPath("/test").toString());
		assertEquals("http://test.com/test/", UriBuilder.of("http://test.com").appendRawPath("/test/").toString());
		assertEquals("/test/", UriBuilder.of("/test").appendRawPath("/").toString());
		assertEquals("/test/test2", UriBuilder.of("/test").appendRawPath("test2").toString());
		assertEquals("/test/test2", UriBuilder.of("/test/").appendRawPath("test2").toString());
		assertEquals("/test/test2", UriBuilder.of("/test").appendRawPath("/test2").toString());
		assertEquals("/test/test2", UriBuilder.of("/test/").appendRawPath("/test2").toString());
		assertEquals("/test/test2/", UriBuilder.of("/test").appendRawPath("test2/").toString());
		assertEquals("/test/test2/", UriBuilder.of("/test/").appendRawPath("test2/").toString());
		assertEquals("/test/test2/", UriBuilder.of("/test").appendRawPath("/test2/").toString());
		assertEquals("/test/test2/", UriBuilder.of("/test/").appendRawPath("/test2/").toString());
	}

	@Test
	public void testScheme() {
		assertEquals("http", UriBuilder.of("http://test.com/").getScheme());
		assertEquals("https", UriBuilder.of("https://test.com/").getScheme());
		assertEquals("something", UriBuilder.of("something://test.com/").getScheme());
		assertEquals("ftp", UriBuilder.of("something://test.com/").scheme("ftp").getScheme());
		assertNull(UriBuilder.of("//test.com/").getScheme());
		assertNull(UriBuilder.of("something://test.com/").scheme(null).getScheme());
	}

	@Test
	public void testHost() {
		assertEquals("test.com", UriBuilder.of("http://test.com/").getHost());
		assertEquals("test.com", UriBuilder.of("https://test.com/").getHost());
		assertEquals("test.com", UriBuilder.of("something://test.com/").getHost());
		assertEquals("foo.bar", UriBuilder.of("something://test.com/").host("foo.bar").getHost());
		assertNull(UriBuilder.of("/bla").getHost());
		assertNull(UriBuilder.of("something://test.com/").host(null).getHost());
	}

	@Test
	public void testBuild() {
		assertEquals(URI.create("http://test.com/"), UriBuilder.of("http://test.com/").build());
		assertEquals(URI.create("http://test.com/bla"), UriBuilder.of("http://test.com/bla").build());
		assertEquals(URI.create("//test.com/bla"), UriBuilder.of("//test.com/bla").build());
		assertEquals(URI.create("/xxx"), UriBuilder.of("/xxx").build());

		assertEquals(URI.create("http://test.com/?x=y"), UriBuilder.of("http://test.com/").addParameter("x", "y").build());
		assertEquals(URI.create("http://test.com/bla?x=y"), UriBuilder.of("http://test.com/bla").addParameter("x", "y").build());
		assertEquals(URI.create("//test.com/bla?x=y"), UriBuilder.of("//test.com/bla").addParameter("x", "y").build());
		assertEquals(URI.create("/xxx?x=y"), UriBuilder.of("/xxx").addParameter("x", "y").build());
	}

	@Test
	public void testIpv6() {
		assertEquals("https://[::1]/path", UriBuilder.of("https://[::1]/path").toString());
		assertEquals("https://[::1]/path", UriBuilder.of("https://test.com/path").host("::1").toString());
	}

	@Test
	public void testToHostString() {
		assertEquals("http://example.com", UriBuilder.of("http://example.com").toHostString());
		assertEquals("http://example.com", UriBuilder.of("http://example.com/").toHostString());
		assertEquals("http://example.com", UriBuilder.of("http://example.com/bla").toHostString());
		assertEquals("http://example.com", UriBuilder.of("http://example.com?x=y").toHostString());
		assertEquals("http://example.com", UriBuilder.of("http://example.com/bla?x=y").toHostString());
		assertEquals("http://example.com", UriBuilder.of("http://example.com#fff").toHostString());
		assertEquals("http://example.com", UriBuilder.of("http://example.com?x=y#fff").toHostString());
		assertEquals("http://example.com", UriBuilder.of("http://example.com/bla?x=y#fff").toHostString());
	}
}
