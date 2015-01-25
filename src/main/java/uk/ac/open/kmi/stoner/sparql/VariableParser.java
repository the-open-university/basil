package uk.ac.open.kmi.stoner.sparql;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableParser implements QueryParameter {

	private String variable;
	private String name;
	private boolean isParameter = false;
	private boolean isForcedIri = false;
	private boolean isForcedPlainLiteral = false;
	private boolean isForcedLangedLiteral = false;
	private boolean isForcedDatatype = false;
	private boolean isPlain = false;
	private String datatypePrefix = null;
	private String lang = null;
	private String datatypeLocalName = null;
	private boolean isNeat = true;
	private boolean isOptional;

	public VariableParser(String variable) {
		this.variable = variable;
		Pattern p = Pattern
				.compile("[\\$\\?]([_]{1,2})([^_]+)_?([a-zA-Z0-9]+)?_?([a-zA-Z0-9]+)?.*$");
		Matcher m = p.matcher(this.variable);
		if (m.matches()) {
			this.isParameter = true;
			this.isOptional = m.group(1).length() == 2;
			this.name = m.group(2);
			if (m.group(3) != null) {
				if (m.group(3).toLowerCase().equals("iri")) {
					this.isForcedIri = true;
				} else if (m.group(3).toLowerCase().equals("literal")) {
					this.isForcedPlainLiteral = true;
				} else if (m.group(3).length() == 2 && m.group(4) == null) {
					// specifies lang
					this.isForcedLangedLiteral = true;
					this.lang = m.group(3).toLowerCase();
				} else if (m.group(4) != null) {
					this.datatypePrefix = m.group(3);
					this.datatypeLocalName = m.group(4);
					this.isForcedDatatype = true;
				} else {
					// Let's check if group(3) is a well known XSD Datatype
					Set<String> xsdDatatypes = new HashSet<String>();
					xsdDatatypes.add("string");
					xsdDatatypes.add("int");
					xsdDatatypes.add("integer");
					xsdDatatypes.add("boolean");
					xsdDatatypes.add("double");
					xsdDatatypes.add("long");
					xsdDatatypes.add("anyURI");
					xsdDatatypes.add("date");
					xsdDatatypes.add("dateTime");
					xsdDatatypes.add("gYear");
					xsdDatatypes.add("gMonth");
					// TODO add others
					// ...
					//
					if (xsdDatatypes.contains(m.group(3))) {
						this.datatypeLocalName = m.group(3);
						this.datatypePrefix = "xsd";
						this.isForcedDatatype = true;
					} else {
						// Let's guess what we can...
						// XXX Maybe we should report why?
						this.isNeat = false;
						this.isPlain = true;
					}
				}
			} else {
				this.isPlain = true;
			}

		} else {
			// It's not a parameter
		}
	}

	public boolean isParameter() {
		return isParameter;
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#isNeat()
	 */
	public boolean isNeat() {
		return isNeat;
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#getParameterName()
	 */
	public String getParameterName() throws ParameterException {
		if (isParameter()) {
			return this.name;
		} else {
			throw new ParameterException();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#isForcedIri()
	 */
	public boolean isForcedIri() throws ParameterException {
		if (isParameter()) {
			return this.isForcedIri;
		} else {
			throw new ParameterException();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#isForcedTypedLiteral()
	 */
	public boolean isForcedTypedLiteral() throws ParameterException {
		if (isParameter()) {
			return this.isForcedDatatype;
		} else {
			throw new ParameterException();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#isForcedPlainLiteral()
	 */
	public boolean isForcedPlainLiteral() throws ParameterException {
		if (isParameter()) {
			return this.isForcedPlainLiteral;
		} else {
			throw new ParameterException();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#isForcedLangedLiteral()
	 */
	public boolean isForcedLangedLiteral() throws ParameterException {
		if (isParameter()) {
			return this.isForcedLangedLiteral;
		} else {
			throw new ParameterException();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#isPlain()
	 */
	public boolean isPlain() throws ParameterException {
		if (isParameter()) {
			return this.isPlain;
		} else {
			throw new ParameterException();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#isOptional()
	 */
	public boolean isOptional() throws ParameterException {
		if (isParameter()) {
			return this.isOptional;
		} else {
			throw new ParameterException();
		}
	}

	public String getDatatypePrefix() throws ParameterException {
		if (isParameter() && this.datatypePrefix != null) {
			return this.datatypePrefix;
		} else {
			throw new ParameterException(
					"Not that kind of parameter: datatype prefix is missing.");
		}
	}

	public String getDatatypeLocalName() throws ParameterException {
		if (isParameter() && this.datatypeLocalName != null) {
			return this.datatypeLocalName;
		} else {
			throw new ParameterException(
					"Not that kind of parameter: datatype local name is missing.");
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.stoner.sparql.QueryParameter#getLang()
	 */
	public String getLang() throws ParameterException {
		if (isParameter() && this.lang != null) {
			return this.lang;
		} else {
			throw new ParameterException(
					"Not that kind of parameter: lang is missing.");
		}
	}

}