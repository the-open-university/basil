package uk.ac.open.kmi.basil.it;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CRUDTest extends AuthenticatedTestBase {
	private static final Logger log = LoggerFactory.getLogger(CollectionTest.class);
	private static final String TEST_DOCS_ENTITY = "Description of a test API";
	private static final String TEST_DOCS_NAME = "This is a test API";

	@Rule
	public TestName name = new TestName();

	private static String createdId = null;

	@Test
	public void CRUD1_CreateAPI() throws Exception {
		log.info("#{}", name.getMethodName());
		String body = loadQueryString("select_1");
		HttpPut put = new HttpPut(BasilTestServer.getServerBaseUrl() + "/basil");
		put.addHeader("X-Basil-Endpoint", extractEndpoint(body));
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContent(IOUtils.toInputStream(body));
		put.setEntity(entity);
		HttpResponse response = executor.execute(builder.buildOtherRequest(put)).assertStatus(201).getResponse();
		log.debug(" > Response headers:");
		for (Header h : response.getAllHeaders()) {
			log.debug(" >> {}: {}", h.getName(), h.getValue());
		}
		String l = response.getFirstHeader("Location").getValue();
		createdId = l.substring(l.lastIndexOf('/') + 1);
		log.info("Api {} created", createdId);
	}

	@Test
	public void CRUD2_AccessCreatedAPIRedirect() throws Exception {
		log.info("#{}", name.getMethodName());
		log.trace("Attempting to access {}", createdId);
		executor.execute(builder.buildGetRequest("/basil/" + createdId).withRedirects(true)).assertStatus(200)
				.assertContentType("text/plain").getEntity().getContent();

	}

	@Test
	public void CRUD3_AccessCreatedAPISpec() throws Exception {
		log.info("#{}", name.getMethodName());
		log.trace("Attempting to access {}", createdId);
		executor.execute(builder.buildGetRequest("/basil/" + createdId + "/spec").withRedirects(false))
				.assertStatus(200).assertContentType("text/plain").getEntity().getContent();

	}

	@Test
	public void CRUD4_AccessCreatedAPI303() throws Exception {
		log.info("#{}", name.getMethodName());
		log.trace("Attempting to access {}", createdId);
		executor.execute(builder.buildGetRequest("/basil/" + createdId).withRedirects(false)).assertStatus(303);
	}

	@Test
	public void CRUD5_PutDocs() throws Exception {
		log.info("#{}", name.getMethodName());
		HttpPut put = new HttpPut(BasilTestServer.getServerBaseUrl() + "/basil/" + createdId + "/docs");
		put.addHeader("X-Basil-Name", TEST_DOCS_NAME);
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContent(IOUtils.toInputStream(TEST_DOCS_ENTITY));
		put.setEntity(entity);
		executor.execute(builder.buildOtherRequest(put).withRedirects(false)).assertStatus(201);
	}

	@Test
	public void CRUD6_AccessDocs() throws Exception {
		log.info("#{}", name.getMethodName());
		executor.execute(builder.buildGetRequest("/basil/" + createdId + "/docs").withRedirects(false))
				.assertStatus(200).assertContentType("text/plain").assertContentContains(TEST_DOCS_ENTITY)
				.assertHeader("X-Basil-Name", TEST_DOCS_NAME);
	}
}