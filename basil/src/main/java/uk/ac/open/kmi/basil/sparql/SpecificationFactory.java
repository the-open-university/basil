package uk.ac.open.kmi.basil.sparql;

import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementWalker;

public class SpecificationFactory {

	public static Specification create(String endpoint, String sparql) {
		VariablesCollector collector = new VariablesCollector();
		Query q = QueryFactory.create(sparql);
		Element element = q.getQueryPattern();
		ElementWalker.walk(element, collector);
		Set<String> vars = collector.getVariables();
		VariableParser parser;
		Specification spec = new Specification();
		spec.setEndpoint(endpoint);
		spec.setQuery(sparql);
		
		for (String var : vars) {
			parser = new VariableParser(var);
			if (parser.isParameter()) {
				spec.map(var, parser.getParameter());
			}
		}
		return spec;
	}
}