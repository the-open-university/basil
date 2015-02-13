package uk.ac.open.kmi.stoner;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.out.JsonLDWriter;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapNull;
import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.riot.writer.NQuadsWriter;
import org.apache.jena.riot.writer.NTriplesWriter;
import org.apache.jena.riot.writer.RDFJSONWriter;
import org.apache.jena.riot.writer.RDFXMLPlainWriter;
import org.apache.jena.riot.writer.TurtleWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import uk.ac.open.kmi.stoner.sparql.QueryParameter;
import uk.ac.open.kmi.stoner.sparql.Specification;
import uk.ac.open.kmi.stoner.sparql.VariablesBinder;
import uk.ac.open.kmi.stoner.store.Store;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraphOne;
import com.hp.hpl.jena.sparql.resultset.JSONOutput;
import com.hp.hpl.jena.sparql.resultset.XMLOutput;
import com.hp.hpl.jena.sparql.util.Context;

@Path("{id:([^/]+)}/api{ext:(\\.[a-zA-Z0-9]+)?}")
public class ApiResource extends AbstractResource {

	private Response performQuery(String id,
			MultivaluedMap<String, String> parameters, String extension) {
		try {
			Store store = getDataStore();
			if (!store.existsSpec(id)) {
				return Response.status(404).build();
			}

			Specification specification = store.loadSpec(id);
			VariablesBinder binder = new VariablesBinder(specification);

			List<String> missing = new ArrayList<String>();
			for (QueryParameter qp : specification.getParameters()) {
				if (parameters.containsKey(qp.getName())) {
					List<String> values = parameters.get(qp.getName());
					binder.bind(qp.getName(), values.get(0));
				} else if (!qp.isOptional()) {
					missing.add(qp.getName());
				}
			}

			if (!missing.isEmpty()) {
				StringBuilder ms = new StringBuilder();
				ms.append("Missing mandatory query parameters: ");
				for (String p : missing) {
					ms.append(p);
					ms.append("\t");
				}
				ms.append("\n");
				return Response.status(400).entity(ms.toString()).build();
			}

			Query q = binder.toQuery();
			QueryExecution qe = QueryExecutionFactory.sparqlService(
					specification.getEndpoint(), q);
			Object entity = null;

			MediaType type = null;
			if (!extension.equals("")) {
				type = getFromExtension(extension);
				if (type == null) {
					// NOT FOUND HERE
					return Response.status(404).entity("Not found").build();
				}
			}
			// No extension
			if (type == null) {
				type = getBestAcceptable();
			}
			// No best acceptable
			if (type == null) {
				return buildNotAcceptable();
			}

			if (q.isSelectType()) {
				entity = prepareEntity(type, qe.execSelect());
			} else if (q.isConstructType()) {
				entity = prepareEntity(type, qe.execConstruct(), q
						.getPrefixMapping().getNsPrefixMap());
			} else if (q.isAskType()) {
				entity = prepareEntity(type, qe.execAsk());
			} else if (q.isDescribeType()) {
				entity = prepareEntity(type, qe.execDescribe(), q
						.getPrefixMapping().getNsPrefixMap());
			} else {
				return Response.serverError()
						.entity("Unsupported query type: " + q.getQueryType())
						.build();
			}

			// If entity is null then format is not acceptable
			// ie we don't have an implementation of that object/type map
			if (entity == null) {
				return buildNotAcceptable();
			}
			ResponseBuilder rb;
			rb = Response.ok().entity(entity);
			addHeaders(rb, id);

			return rb.build();
		} catch (IOException e) {
			throw new WebApplicationException(e);
		}
	}

	private Response buildNotAcceptable() {
		return Response.notAcceptable(
				Variant.mediaTypes(MediaType.TEXT_PLAIN_TYPE).add().build())
				.build();
	}

	@SuppressWarnings("unchecked")
	private Object prepareEntity(MediaType type, boolean b) {

		// text/plain
		if (MediaType.TEXT_PLAIN_TYPE.equals(type)) {
			if (b) {
				return "True\n";
			} else {
				return "False\n";
			}
		}

		// xml
		if (MediaType.TEXT_XML_TYPE.equals(type)
				|| MediaType.APPLICATION_XML_TYPE.equals(type)) {
			StringBuilder p = new StringBuilder();
			p.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
			p.append("\n");
			p.append("<data>");
			p.append("\n\t");
			p.append("<vars>");
			// boolean
			p.append("\n\t\t");
			p.append("<var>");
			p.append("boolean");
			p.append("</var>");

			p.append("\n\t");
			p.append("</vars>");

			p.append("\n\t");
			p.append("<item>");

			p.append("\n\t\t");
			String v = "boolean";

			p.append("<");
			p.append(v);
			p.append(" ");
			p.append("type=\"");
			p.append("boolean");
			p.append("\"");
			p.append(">");
			if (b)
				p.append("true");
			else
				p.append("false");
			p.append("</");
			p.append(v);
			p.append(">");

			p.append("\n\t");
			p.append("</item>");

			p.append("\n");
			p.append("</data>");
			p.append("\n");
			return p.toString();
		}

		if (MediaType.APPLICATION_JSON_TYPE.equals(type)) {
			JSONObject o = new JSONObject();
			o.put("bool", b);
			return o.toJSONString() + "\n";
		}

		// sparql-results+xml
		if (MoreMediaType.SPARQL_RESULTS_XML_TYPE.equals(type)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLOutput xOut = new XMLOutput(null);
			xOut.format(baos, b);
			return new String(baos.toByteArray());
		}

		// sparql-results+json
		if (MoreMediaType.SPARQL_RESULTS_JSON_TYPE.equals(type)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JSONOutput xOut = new JSONOutput();
			xOut.format(baos, b);
			return new String(baos.toByteArray());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Object prepareEntity(MediaType type, Model model,
			Map<String, String> prefixes) {

		// text/plain
		if (MediaType.TEXT_PLAIN_TYPE.equals(type)
				|| MoreMediaType.NTRIPLES_TYPE.equals(type)) {
			StringWriter sw = new StringWriter();
			NTriplesWriter.write(sw, model.getGraph().find(null, null, null));
			return sw.toString();
		}

		// xml
		if (MediaType.TEXT_XML_TYPE.equals(type)
				|| MediaType.APPLICATION_XML_TYPE.equals(type)) {
			Iterator<Triple> tr = model.getGraph().find(null, null, null);
			StringBuilder p = new StringBuilder();
			p.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
			p.append("\n");
			p.append("<data>");
			p.append("\n\t");
			p.append("<vars>");
			// subject
			p.append("\n\t\t");
			p.append("<var>");
			p.append("subject");
			p.append("</var>");
			// predicate
			p.append("\n\t\t");
			p.append("<var>");
			p.append("predicate");
			p.append("</var>");
			// object
			p.append("\n\t\t");
			p.append("<var>");
			p.append("object");
			p.append("</var>");
			p.append("\n\t");
			p.append("</vars>");
			p.append("\n\t");
			p.append("<items>");
			while (tr.hasNext()) {
				Triple t = tr.next();
				p.append("\n\t\t");
				p.append("<item>");
				// subject
				p.append("\n\t\t\t");
				String v = "subject";
				Node n = t.getSubject();
				p.append("<");
				p.append(v);
				p.append(" ");
				p.append("type=\"");
				if (n.isBlank()) {
					p.append("anon");
				} else if (n.isLiteral()) {
					p.append("literal");
				} else if (n.isURI()) {
					p.append("uri");
				}
				p.append("\"");
				p.append(">");
				p.append(n.toString());
				p.append("</");
				p.append(v);
				p.append(">");

				// predicate
				p.append("\n\t\t\t");
				v = "predicate";
				n = t.getSubject();
				p.append("<");
				p.append(v);
				// p.append(" ");
				// p.append("type=\"");
				// if (n.isBlank()) {
				// p.append("anon");
				// } else if (n.isLiteral()) {
				// p.append("literal");
				// } else if (n.isURI()) {
				// p.append("uri");
				// }
				// p.append("\"");
				p.append(">");
				p.append(n.toString());
				p.append("</");
				p.append(v);
				p.append(">");

				// object
				p.append("\n\t\t\t");
				v = "object";
				n = t.getSubject();
				p.append("<");
				p.append(v);
				p.append(" ");
				p.append("type=\"");
				if (n.isBlank()) {
					p.append("anon");
				} else if (n.isLiteral()) {
					p.append("literal");
				} else if (n.isURI()) {
					p.append("uri");
				}
				p.append("\"");
				p.append(">");
				p.append(n.toString());
				p.append("</");
				p.append(v);
				p.append(">");

				p.append("\n\t\t");
				p.append("</item>");
			}

			p.append("\n\t");
			p.append("</items>");
			p.append("\n");
			p.append("</data>");
			p.append("\n");
			return p.toString();
		}

		// application/json
		if (MediaType.APPLICATION_JSON_TYPE.equals(type)) {
			Iterator<Triple> tr = model.getGraph().find(null, null, null);
			JSONObject o = new JSONObject();
			JSONArray vars = new JSONArray();
			vars.add("subject");
			vars.add("predicate");
			vars.add("object");
			o.put("vars", vars);
			JSONArray items = new JSONArray();
			while (tr.hasNext()) {
				Triple t = tr.next();
				JSONObject item = new JSONObject();
				item.put("subject", t.getSubject().toString());
				item.put("predicate", t.getPredicate().toString());
				item.put("object", t.getObject().toString());
				items.add(item);
			}
			o.put("items", items);
			return o.toJSONString() + "\n";
		}

		// application/rdf+json
		if (MoreMediaType.RDFJSON_TYPE.equals(type)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			RDFJSONWriter.output(baos, model.getGraph());
			try {
				return new String(baos.toByteArray(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// This will never happen
				new WebApplicationException(500);
			}
		}

		// application/ld+json
		if (MoreMediaType.JSONLD_TYPE.equals(type)) {
			StringWriter w = new StringWriter();
			JsonLDWriter jw = new JsonLDWriter(RDFFormat.JSONLD_PRETTY);
			PrefixMap p = new PrefixMapStd();
			p.putAll(prefixes);
			jw.write(w, new DatasetGraphOne(model.getGraph()), p, null,
					Context.emptyContext);
			return w.toString();
		}

		// application/rdf+xml
		if (MoreMediaType.RDFXML_TYPE.equals(type)) {
			StringWriter w = new StringWriter();
			RDFXMLPlainWriter writer = new RDFXMLPlainWriter();
			PrefixMap p = new PrefixMapStd();
			p.putAll(prefixes);
			writer.write(w, model.getGraph(), p, "", Context.emptyContext);
			return w.toString();
		}

		// turtle
		if (MoreMediaType.TEXT_TURTLE_TYPE.equals(type)
				|| MoreMediaType.APPLICATION_TURTLE_TYPE.equals(type)) {
			StringWriter w = new StringWriter();
			TurtleWriter writer = new TurtleWriter();
			PrefixMap p = new PrefixMapStd();
			p.putAll(prefixes);
			writer.write(w, model.getGraph(), p, "", Context.emptyContext);
			return w.toString();
		}

		// text/x-nquads
		if (MoreMediaType.TEXT_X_NQUADS_TYPE.equals(type)) {
			StringWriter w = new StringWriter();
			NQuadsWriter writer = new NQuadsWriter();
			DatasetGraph dg = DatasetGraphFactory.createMem();
			dg.addGraph(NodeFactory.createURI(requestUri.getRequestUri()
					.toString()), model.getGraph());
			writer.write(w, dg, PrefixMapNull.empty, null, Context.emptyContext);
			return w.toString();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private Object prepareEntity(MediaType type, ResultSet rs) {
		// text/plain
		if (MediaType.TEXT_PLAIN_TYPE.equals(type)) {
			return ResultSetFormatter.asText(rs);
		}

		// xml
		if (MediaType.TEXT_XML_TYPE.equals(type)
				|| MediaType.APPLICATION_XML_TYPE.equals(type)) {
			StringBuilder p = new StringBuilder();
			p.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
			p.append("\n");
			p.append("<data>");
			p.append("\n\t");
			p.append("<vars>");
			for (String v : rs.getResultVars()) {
				p.append("\n\t\t");
				p.append("<var>");
				p.append(v);
				p.append("</var>");
			}
			p.append("\n\t");
			p.append("</vars>");
			while (rs.hasNext()) {
				QuerySolution r = rs.next();
				p.append("\n\t");
				p.append("<item>");
				Iterator<String> vn = r.varNames();
				while (vn.hasNext()) {
					p.append("\n\t\t");
					String v = vn.next();
					RDFNode n = r.get(v);
					p.append("<");
					p.append(v);
					p.append(" ");
					p.append("type=\"");
					if (n.isAnon()) {
						p.append("anon");
					} else if (n.isLiteral()) {
						p.append("literal");
					} else if (n.isURIResource()) {
						p.append("uri");
					} else if (n.isResource()) {
						p.append("resource");
					}
					p.append("\"");
					p.append(">");
					p.append(n.toString());
					p.append("</");
					p.append(v);
					p.append(">");
				}
				p.append("\n\t");
				p.append("</item>");
			}
			p.append("\n");
			p.append("</data>");
			p.append("\n");
			return p.toString();
		}

		// application/json
		if (MediaType.APPLICATION_JSON_TYPE.equals(type)) {
			JSONObject o = new JSONObject();
			JSONArray vars = new JSONArray();
			for (String v : rs.getResultVars()) {
				vars.add(v);
			}
			o.put("vars", vars);
			JSONArray items = new JSONArray();
			while (rs.hasNext()) {
				QuerySolution t = rs.next();
				JSONObject item = new JSONObject();
				Iterator<String> vi = t.varNames();
				while (vi.hasNext()) {
					JSONObject cell = new JSONObject();
					String vn = vi.next();
					RDFNode n = t.get(vn);
					if (n.isAnon()) {
						cell.put("type", "anon");
					} else if (n.isLiteral()) {
						cell.put("type", "literal");
					} else if (n.isURIResource()) {
						cell.put("type", "uri");
					}
					cell.put("value", n.toString());
					item.put(vn, cell);
				}
				items.add(item);
			}
			o.put("items", items);
			return o.toJSONString() + "\n";
		}

		// sparql-results+xml
		if (MoreMediaType.SPARQL_RESULTS_XML_TYPE.equals(type)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLOutput xOut = new XMLOutput(null);
			xOut.format(baos, rs);
			return new String(baos.toByteArray());
		}

		// sparql-results+json
		if (MoreMediaType.SPARQL_RESULTS_JSON_TYPE.equals(type)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JSONOutput xOut = new JSONOutput();
			xOut.format(baos, rs);
			return new String(baos.toByteArray());
		}

		return null;
	}

	private List<MediaType> getAvailableVariants() {
		return Arrays.asList(new MediaType[] { MediaType.TEXT_PLAIN_TYPE,
				MoreMediaType.NTRIPLES_TYPE, MediaType.TEXT_XML_TYPE,
				MediaType.APPLICATION_XML_TYPE,
				MediaType.APPLICATION_JSON_TYPE, MoreMediaType.RDFJSON_TYPE,
				MoreMediaType.RDFXML_TYPE,
				MoreMediaType.APPLICATION_TURTLE_TYPE,
				MoreMediaType.TEXT_TURTLE_TYPE,
				MoreMediaType.TEXT_X_NQUADS_TYPE,
				MoreMediaType.SPARQL_RESULTS_JSON_TYPE,
				MoreMediaType.SPARQL_RESULTS_XML_TYPE });
	}

	public MediaType getFromExtension(String ext) {
		if (MoreMediaType.extensions.containsKey(ext)) {
			return MoreMediaType.extensions.get(ext);
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @return null if none of the exisitng variants are acceptable
	 */
	private MediaType getBestAcceptable() {
		// This list is sorted by the client preference
		List<MediaType> acceptHeaders = requestHeaders
				.getAcceptableMediaTypes();
		if (acceptHeaders == null || acceptHeaders.size() == 0) {
			// Default type is text/plain
			return MediaType.TEXT_PLAIN_TYPE;
		}

		for (MediaType mt : acceptHeaders) {
			String qValue = mt.getParameters().get("q");
			if (qValue != null && Double.valueOf(qValue).doubleValue() == 0.0) {
				break;
			}

			for (MediaType variant : getAvailableVariants()) {
				if (variant.isCompatible(mt)) {
					return variant;
				}
			}

		}
		return null;
	}

	@POST
	@Consumes("application/x-www-form-urlencoded")
	public Response post(@PathParam("id") String id,
			@PathParam("ext") String extension,
			MultivaluedMap<String, String> form) {
		return performQuery(id, form, extension.substring(1));
	}

	@GET
	public Response get(@PathParam("id") String id,
			@PathParam("ext") String extension) {
		return performQuery(id, requestUri.getQueryParameters(), extension.substring(1));
	}
}