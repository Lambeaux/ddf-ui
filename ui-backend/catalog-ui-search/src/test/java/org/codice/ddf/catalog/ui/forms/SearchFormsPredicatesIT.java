/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.forms;

import static java.lang.String.format;
import static org.codice.ddf.catalog.ui.forms.SearchFormsTestSupport.getAttributeRegistry;
import static org.codice.ddf.catalog.ui.forms.SearchFormsTestSupport.getAvailablePort;
import static org.codice.ddf.catalog.ui.forms.SearchFormsTestSupport.getContentsOfFile;
import static org.codice.ddf.catalog.ui.forms.SearchFormsTestSupport.getWriter;
import static org.codice.ddf.catalog.ui.forms.SearchFormsTestSupport.removePrettyPrintingOnJson;
import static org.codice.ddf.catalog.ui.forms.SearchFormsTestSupport.removePrettyPrintingOnXml;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static spark.Spark.stop;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.codice.ddf.catalog.ui.config.ConfigurationApplication;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import spark.Spark;

/**
 * Refer to the {@link SearchFormsSymbolsIT} javadoc first.
 *
 * <p>This is a separate conformance suite that targets the conversion accuracy of specific
 * predicates and is NOT parameterized like the symbols test counterpart.
 */
@RunWith(JUnit4.class)
public class SearchFormsPredicatesIT {

  private static final String FILTER_TEMPLATE_KEY = "filterTemplate";

  /*
   * ---------------------------------------------------------------------------------------------
   * Test Data
   * ---------------------------------------------------------------------------------------------
   */

  private static final String FORM_FILTER_MIN_XML =
      getContentsOfFile("/forms/predicates-it/form-filter-minimal-xml.txt");

  private static final String FORM_MIN_JSON =
      getContentsOfFile("/forms/predicates-it/form-minimal-json.txt");

  private static final String FORM_RESP_JSON =
      getContentsOfFile("/forms/predicates-it/form-response-json.txt");

  private static String createJsonFilterTemplate(String type, String prop, String val) {
    return "\"filterTemplate\": { "
        + String.format(
            "\"type\": \"%s\", \"property\": \"%s\", \"value\": \"%s\"", type, prop, val)
        + " }";
  }

  private static String createJsonFilterTemplate(
      String type, String prop, String val, String from, String to) {
    return "\"filterTemplate\": { "
        + String.format(
            "\"type\": \"%s\", \"property\": \"%s\", \"value\": \"%s\", \"from\": \"%s\", \"to\": \"%s\"",
            type, prop, val, from, to)
        + " }";
  }

  private static String getXmlFilter(String type, String prop, String val) {
    Map<String, String> keyvals = new HashMap<>();
    keyvals.put("type", type);
    keyvals.put("prop", prop);
    keyvals.put("val", val);

    StrSubstitutor subs = new StrSubstitutor(keyvals);
    return removePrettyPrintingOnXml(subs.replace(FORM_FILTER_MIN_XML));
  }

  private static String getJsonFilter(String type, String prop, String val) {
    StrSubstitutor subs =
        new StrSubstitutor(
            Collections.singletonMap(
                FILTER_TEMPLATE_KEY, createJsonFilterTemplate(type, prop, val)));
    return removePrettyPrintingOnJson(subs.replace(FORM_MIN_JSON));
  }

  private static String getJsonResponse(String type, String prop, String val) {
    StrSubstitutor subs =
        new StrSubstitutor(
            Collections.singletonMap(
                FILTER_TEMPLATE_KEY, createJsonFilterTemplate(type, prop, val)));
    return removePrettyPrintingOnJson(subs.replace(FORM_RESP_JSON));
  }

  private static String getJsonResponse(
      String type, String prop, String val, String from, String to) {
    StrSubstitutor subs =
        new StrSubstitutor(
            Collections.singletonMap(
                FILTER_TEMPLATE_KEY, createJsonFilterTemplate(type, prop, val, from, to)));
    return removePrettyPrintingOnJson(subs.replace(FORM_RESP_JSON));
  }

  /*
   * ---------------------------------------------------------------------------------------------
   * Test Vars
   * ---------------------------------------------------------------------------------------------
   */

  private static final Header CONTENT_IS_JSON = new Header("Content-Type", "application/json");

  private static final String CANNED_TITLE = "MY_TITLE";

  private static final String CANNED_DESCRIPTION = "MY_DESCRIPTION";

  private static final String CANNED_ID = "abcdefg";

  private static final String CANNED_ISO_DATE = "2018-12-10T13:09:40Z";

  private static final String CANNED_EPOCH_DATE = "1544447380000";

  private static final FilterBuilder FILTER_BUILDER = new GeotoolsFilterBuilder();

  private static final CatalogFramework MOCK_FRAMEWORK = mock(CatalogFramework.class);

  private static final Subject MOCK_SUBJECT = mock(Subject.class);

  private static final ConfigurationApplication MOCK_CONFIG = mock(ConfigurationApplication.class);

  private static final TemplateTransformer TRANSFORMER =
      new TemplateTransformer(getWriter(), getAttributeRegistry());

  private static final EndpointUtil UTIL =
      new EndpointUtil(
          null, // No interaction
          MOCK_FRAMEWORK,
          FILTER_BUILDER,
          null, // No interaction
          null, // No interaction
          MOCK_CONFIG);

  private static final SearchFormsApplication APPLICATION =
      new SearchFormsApplication(
          MOCK_FRAMEWORK, FILTER_BUILDER, TRANSFORMER, UTIL, () -> MOCK_SUBJECT);

  // Will be initialized by setUpClass() when the port is known
  private static String localhostFormsUrl = null;

  /*
   * ---------------------------------------------------------------------------------------------
   * Test Exec
   * ---------------------------------------------------------------------------------------------
   */

  @BeforeClass
  public static void setUpClass() {
    Spark.port(getAvailablePort());
    APPLICATION.init();
    Spark.awaitInitialization();
    localhostFormsUrl = format("http://localhost:%d/forms/query", Spark.port());
  }

  @AfterClass
  public static void tearDownClass() {
    stop();
  }

  @Before
  public void setUp() {
    when(MOCK_SUBJECT.isGuest()).thenReturn(false);
    when(MOCK_CONFIG.getMaximumUploadSize()).thenReturn(1024);
  }

  @After
  public void tearDown() {
    reset(MOCK_FRAMEWORK, MOCK_SUBJECT, MOCK_CONFIG);
  }

  @Test
  public void testTextEqualJsonToXml() throws IngestException, SourceUnavailableException {
    testJsonToXml(
        getJsonFilter("=", "language", "english"),
        getXmlFilter("PropertyIsEqualTo", "language", "english"));
  }

  @Test
  public void testTextEqualXmlToJson()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    testXmlToJson(
        getXmlFilter("PropertyIsEqualTo", "language", "english"),
        getJsonResponse("=", "language", "english"));
  }

  @Test
  public void testDateBetweenJsonToXml() throws IngestException, SourceUnavailableException {
    testJsonToXml(
        getJsonFilter("DURING", "created", CANNED_ISO_DATE + "/" + CANNED_ISO_DATE),
        getXmlFilter("During", "created", CANNED_EPOCH_DATE + "/" + CANNED_EPOCH_DATE));
  }

  @Test
  public void testDateBetweenXmlToJson()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    testXmlToJson(
        getXmlFilter("During", "created", CANNED_EPOCH_DATE + "/" + CANNED_EPOCH_DATE),
        getJsonResponse(
            "DURING",
            "created",
            CANNED_ISO_DATE + "/" + CANNED_ISO_DATE,
            CANNED_ISO_DATE,
            CANNED_ISO_DATE));
  }

  private static void testJsonToXml(String requestJson, String expectedXml)
      throws IngestException, SourceUnavailableException {
    ArgumentCaptor<CreateRequest> requestCaptor = ArgumentCaptor.forClass(CreateRequest.class);

    // Prepare
    MetacardImpl metacardWithIdAndCreatedDate = new MetacardImpl();
    metacardWithIdAndCreatedDate.setId(CANNED_ID);
    metacardWithIdAndCreatedDate.setCreatedDate(new Date());

    doReturn(
            new CreateResponseImpl(
                new CreateRequestImpl(Collections.emptyList()),
                Collections.emptyMap(),
                Collections.singletonList(metacardWithIdAndCreatedDate)))
        .when(MOCK_FRAMEWORK)
        .create(requestCaptor.capture());

    // Execute
    int statusCode =
        RestAssured.given()
            .header(CONTENT_IS_JSON)
            .content(requestJson)
            .post(localhostFormsUrl)
            .statusCode();

    Metacard searchForm = requestCaptor.getValue().getMetacards().get(0);
    String capturedXml = ((QueryTemplateMetacard) searchForm).getFormsFilter();

    assertThat(statusCode, is(200));
    assertThat(capturedXml, is(expectedXml));
  }

  private static void testXmlToJson(String formXml, String expectedResponseJson)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    // Prepare
    QueryTemplateMetacard queryTemplateMetacard =
        new QueryTemplateMetacard(CANNED_TITLE, CANNED_DESCRIPTION, CANNED_ID);
    queryTemplateMetacard.setFormsFilter(formXml);
    queryTemplateMetacard.setCreatedDate(Date.from(Instant.parse(CANNED_ISO_DATE)));
    queryTemplateMetacard.setModifiedDate(Date.from(Instant.parse(CANNED_ISO_DATE)));

    QueryResponseImpl response =
        new QueryResponseImpl(new QueryRequestImpl(new QueryImpl(Filter.INCLUDE)));
    response.addResult(new ResultImpl(queryTemplateMetacard), true);

    doReturn(response).when(MOCK_FRAMEWORK).query(any());

    // Execute
    String json =
        RestAssured.given().header(CONTENT_IS_JSON).get(localhostFormsUrl).body().asString();
    assertThat(json, is(expectedResponseJson));
  }
}
