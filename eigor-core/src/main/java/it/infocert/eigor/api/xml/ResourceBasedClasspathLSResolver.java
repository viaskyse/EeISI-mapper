package it.infocert.eigor.api.xml;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.valueOf;


class ResourceBasedClasspathLSResolver implements LSResourceResolver {

    private final String basePath;
    private static Logger log = LoggerFactory.getLogger(ResourceBasedClasspathLSResolver.class);

    ResourceBasedClasspathLSResolver(String xsdClasspathPath) {
        URL resource = getClass().getResource(xsdClasspathPath);
        checkNotNull( resource, "xsd at '%s' not found.", xsdClasspathPath );
        this.basePath = xsdClasspathPath.subSequence( 0, xsdClasspathPath.lastIndexOf("/") ).toString();
    }

    @Override
    public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {

        if(log.isTraceEnabled()) {
            log.trace("Resolving LSInput for type='{}', namespaceURI='{}', publicId='{}', systemId='{}', baseURI='{}'.",
                    type, namespaceURI, publicId, systemId, baseURI);
        }

        ClasspathLSInput lsInput = null;
        if(baseURI==null) {

            lsInput = new ClasspathLSInput(
                    mergeClasspaths(systemId),
                    "UTF-16",
                    publicId,
                    systemId,
                    baseURI
            );

        }else{

            LinkedList<String> baseUriTokensToTry = new LinkedList<String>( Arrays.asList( baseURI.split("/") ) );
            baseUriTokensToTry.removeLast();
            baseUriTokensToTry.add(systemId);

            LinkedList<String> baseUriToken = new LinkedList<String>();

            String res = null;
            while(!baseUriTokensToTry.isEmpty()) {
                baseUriToken.add(0, baseUriTokensToTry.removeLast());

                String resToTry = basePath + "/" + baseUriToken.stream().collect(Collectors.joining("/"));
                if(getClass().getResourceAsStream(resToTry) != null) {
                    res = resToTry;
                    break;
                }
            }

            if(res!=null) {
                lsInput = new ClasspathLSInput(
                        res,
                        "UTF-16",
                        publicId,
                        systemId,
                        baseURI
                );
            }

        }

        if(log.isTraceEnabled()) {
            log.trace("Resolved LSInput for type='{}', namespaceURI='{}', publicId='{}', systemId='{}', baseURI='{}', result is '{}'.",
                    type, namespaceURI, publicId, systemId, baseURI, valueOf(lsInput));
        }


        return lsInput;

    }

    private String mergeClasspaths(String systemId) {
        LinkedList<String> resourceArr = new LinkedList<>( Arrays.asList( basePath.split("/") ) );
        LinkedList<String> systemIdArr = new LinkedList<>( Arrays.asList( systemId.split("/") ) );

        while(systemIdArr.get(0).equals("..")) {
            systemIdArr.remove(0);
            resourceArr.removeLast();
        }

        String resultingResourcePath = resourceArr.stream().collect(Collectors.joining("/")) + "/" +
                systemIdArr.stream().collect(Collectors.joining("/"));

        if(log.isTraceEnabled()) {
            log.trace("Base XSD at resource path '{}', imported XSD with system ID '{}', resulting resource path is '{}'.", basePath, systemId, resultingResourcePath);
        }


        return resultingResourcePath;
    }

    private static class ClasspathLSInput implements LSInput {

        private final String resource;
        private final String systemId;
        private final String encoding;
        private final String baseURI;
        private final String publicId;

        public ClasspathLSInput(String resource, String encoding, String publicId, String systemId, String baseURI) {
            this.resource = resource;
            this.systemId = systemId;
            this.encoding = encoding;
            this.baseURI = baseURI;
            this.publicId = publicId;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ClasspathLSInput.class.getSimpleName() + "[", "]")
                    .add("resource='" + resource + "'")
                    .add("systemId='" + systemId + "'")
                    .add("encoding='" + encoding + "'")
                    .add("baseURI='" + baseURI + "'")
                    .add("publicId='" + publicId + "'")
                    .toString();
        }

        /**
         * An attribute of a language and binding dependent type that represents
         * a stream of 16-bit units. The application must encode the stream
         * using UTF-16 (defined in [Unicode] and in [ISO/IEC 10646]). It is not a requirement to have an XML declaration when
         * using character streams. If an XML declaration is present, the value
         * of the encoding attribute will be ignored.
         */
        @Override
        public Reader getCharacterStream() {

            logResource();

            InputStream resourceAsStream = checkNotNull( getClass().getResourceAsStream(resource), "unable to find %s", resource );
            return new InputStreamReader(resourceAsStream);
        }



        /**
         * An attribute of a language and binding dependent type that represents
         * a stream of 16-bit units. The application must encode the stream
         * using UTF-16 (defined in [Unicode] and in [ISO/IEC 10646]). It is not a requirement to have an XML declaration when
         * using character streams. If an XML declaration is present, the value
         * of the encoding attribute will be ignored.
         *
         * @param characterStream
         */
        @Override
        public void setCharacterStream(Reader characterStream) {
            throw new UnsupportedOperationException();
        }

        /**
         * An attribute of a language and binding dependent type that represents
         * a stream of bytes.
         * <br> If the application knows the character encoding of the byte
         * stream, it should set the encoding attribute. Setting the encoding in
         * this way will override any encoding specified in an XML declaration
         * in the data.
         */
        @Override
        public InputStream getByteStream() {
            logResource();
            return getClass().getResourceAsStream( resource );
        }

        /**
         * An attribute of a language and binding dependent type that represents
         * a stream of bytes.
         * <br> If the application knows the character encoding of the byte
         * stream, it should set the encoding attribute. Setting the encoding in
         * this way will override any encoding specified in an XML declaration
         * in the data.
         *
         * @param byteStream
         */
        @Override
        public void setByteStream(InputStream byteStream) {
            throw new UnsupportedOperationException();
        }

        /**
         * String data to parse. If provided, this will always be treated as a
         * sequence of 16-bit units (UTF-16 encoded characters). It is not a
         * requirement to have an XML declaration when using
         * <code>stringData</code>. If an XML declaration is present, the value
         * of the encoding attribute will be ignored.
         */
        @Override
        public String getStringData() {
            try {
                return IOUtils.toString( getByteStream(), "UTF-16" );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * String data to parse. If provided, this will always be treated as a
         * sequence of 16-bit units (UTF-16 encoded characters). It is not a
         * requirement to have an XML declaration when using
         * <code>stringData</code>. If an XML declaration is present, the value
         * of the encoding attribute will be ignored.
         *
         * @param stringData
         */
        @Override
        public void setStringData(String stringData) {
            throw new UnsupportedOperationException();
        }

        /**
         * The system identifier, a URI reference [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>], for this
         * input source. The system identifier is optional if there is a byte
         * stream, a character stream, or string data. It is still useful to
         * provide one, since the application will use it to resolve any
         * relative URIs and can include it in error messages and warnings. (The
         * LSParser will only attempt to fetch the resource identified by the
         * URI reference if there is no other input available in the input
         * source.)
         * <br> If the application knows the character encoding of the object
         * pointed to by the system identifier, it can set the encoding using
         * the <code>encoding</code> attribute.
         * <br> If the specified system ID is a relative URI reference (see
         * section 5 in [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>]), the DOM
         * implementation will attempt to resolve the relative URI with the
         * <code>baseURI</code> as the base, if that fails, the behavior is
         * implementation dependent.
         */
        @Override
        public String getSystemId() {
            return systemId;
        }

        /**
         * The system identifier, a URI reference [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>], for this
         * input source. The system identifier is optional if there is a byte
         * stream, a character stream, or string data. It is still useful to
         * provide one, since the application will use it to resolve any
         * relative URIs and can include it in error messages and warnings. (The
         * LSParser will only attempt to fetch the resource identified by the
         * URI reference if there is no other input available in the input
         * source.)
         * <br> If the application knows the character encoding of the object
         * pointed to by the system identifier, it can set the encoding using
         * the <code>encoding</code> attribute.
         * <br> If the specified system ID is a relative URI reference (see
         * section 5 in [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>]), the DOM
         * implementation will attempt to resolve the relative URI with the
         * <code>baseURI</code> as the base, if that fails, the behavior is
         * implementation dependent.
         *
         * @param systemId
         */
        @Override
        public void setSystemId(String systemId) {
            throw new UnsupportedOperationException();
        }

        /**
         * The public identifier for this input source. This may be mapped to an
         * input source using an implementation dependent mechanism (such as
         * catalogues or other mappings). The public identifier, if specified,
         * may also be reported as part of the location information when errors
         * are reported.
         */
        @Override
        public String getPublicId() {
            return publicId;
        }

        /**
         * The public identifier for this input source. This may be mapped to an
         * input source using an implementation dependent mechanism (such as
         * catalogues or other mappings). The public identifier, if specified,
         * may also be reported as part of the location information when errors
         * are reported.
         *
         * @param publicId
         */
        @Override
        public void setPublicId(String publicId) {
            throw new UnsupportedOperationException();
        }

        /**
         * The base URI to be used (see section 5.1.4 in [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>]) for
         * resolving a relative <code>systemId</code> to an absolute URI.
         * <br> If, when used, the base URI is itself a relative URI, an empty
         * string, or null, the behavior is implementation dependent.
         */
        @Override
        public String getBaseURI() {
            return baseURI;
        }

        /**
         * The base URI to be used (see section 5.1.4 in [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>]) for
         * resolving a relative <code>systemId</code> to an absolute URI.
         * <br> If, when used, the base URI is itself a relative URI, an empty
         * string, or null, the behavior is implementation dependent.
         *
         * @param baseURI
         */
        @Override
        public void setBaseURI(String baseURI) {
            throw new UnsupportedOperationException();
        }

        /**
         * The character encoding, if known. The encoding must be a string
         * acceptable for an XML encoding declaration ([<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>] section
         * 4.3.3 "Character Encoding in Entities").
         * <br> This attribute has no effect when the application provides a
         * character stream or string data. For other sources of input, an
         * encoding specified by means of this attribute will override any
         * encoding specified in the XML declaration or the Text declaration, or
         * an encoding obtained from a higher level protocol, such as HTTP [<a href='http://www.ietf.org/rfc/rfc2616.txt'>IETF RFC 2616</a>].
         */
        @Override
        public String getEncoding() {
            return encoding;
        }

        /**
         * The character encoding, if known. The encoding must be a string
         * acceptable for an XML encoding declaration ([<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>] section
         * 4.3.3 "Character Encoding in Entities").
         * <br> This attribute has no effect when the application provides a
         * character stream or string data. For other sources of input, an
         * encoding specified by means of this attribute will override any
         * encoding specified in the XML declaration or the Text declaration, or
         * an encoding obtained from a higher level protocol, such as HTTP [<a href='http://www.ietf.org/rfc/rfc2616.txt'>IETF RFC 2616</a>].
         *
         * @param encoding
         */
        @Override
        public void setEncoding(String encoding) {
            throw new UnsupportedOperationException();
        }

        /**
         * If set to true, assume that the input is certified (see section 2.13
         * in [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>]) when
         * parsing [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>].
         */
        @Override
        public boolean getCertifiedText() {
            return false;
        }

        /**
         * If set to true, assume that the input is certified (see section 2.13
         * in [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>]) when
         * parsing [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>].
         *
         * @param certifiedText
         */
        @Override
        public void setCertifiedText(boolean certifiedText) {
            throw new UnsupportedOperationException();
        }

        private void logResource() {
            if(log.isTraceEnabled()){
                log.trace("Loading resource: '{}'.", resource);
            }
        }

    }
}


