package com.jpm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Edits pom.xml safely using the XML DOM API.
 */
public final class PomEditor {

    public enum UpdateStatus {
        ADDED,
        ALREADY_EXISTS
    }

    public UpdateStatus addDependency(Path pomPath, Dependency dependency)
            throws PomEditException, IOException {
        if (pomPath == null) {
            throw new PomEditException("pom.xml path must not be null");
        }
        if (dependency == null) {
            throw new PomEditException("Dependency must not be null");
        }
        if (!Files.exists(pomPath)) {
            throw new PomEditException("No pom.xml found");
        }

        Document document = loadDocument(pomPath);
        Element projectElement = getProjectElement(document);
        Element dependenciesElement = ensureDependenciesElement(document, projectElement);

        if (containsDependency(dependenciesElement, dependency)) {
            return UpdateStatus.ALREADY_EXISTS;
        }

        appendDependency(document, dependenciesElement, dependency);
        saveDocument(document, pomPath);
        return UpdateStatus.ADDED;
    }

    private Document loadDocument(Path pomPath) throws PomEditException, IOException {
        DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();

        try (InputStream inputStream = Files.newInputStream(pomPath)) {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();
            return document;
        } catch (ParserConfigurationException | SAXException ex) {
            throw new PomEditException("Invalid pom.xml", ex);
        }
    }

    private DocumentBuilderFactory createSecureDocumentBuilderFactory() throws PomEditException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            return factory;
        } catch (ParserConfigurationException ex) {
            throw new PomEditException("Unable to configure secure XML parser", ex);
        }
    }

    private Element getProjectElement(Document document) throws PomEditException {
        Element root = document.getDocumentElement();
        if (root == null || !isNamedElement(root, "project")) {
            throw new PomEditException("Invalid pom.xml");
        }
        return root;
    }

    private Element ensureDependenciesElement(Document document, Element projectElement) {
        Element dependenciesElement = findDirectChild(projectElement, "dependencies");
        if (dependenciesElement != null) {
            return dependenciesElement;
        }

        String namespaceUri = projectElement.getNamespaceURI();
        Element createdDependencies = namespaceUri == null
                ? document.createElement("dependencies")
                : document.createElementNS(namespaceUri, "dependencies");

        Element buildElement = findDirectChild(projectElement, "build");
        if (buildElement != null) {
            projectElement.insertBefore(createdDependencies, buildElement);
        } else {
            projectElement.appendChild(createdDependencies);
        }
        return createdDependencies;
    }

    private boolean containsDependency(Element dependenciesElement, Dependency dependency) {
        NodeList children = dependenciesElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!isNamedElement(child, "dependency")) {
                continue;
            }

            Element existingDependency = (Element) child;
            String existingGroupId = getChildText(existingDependency, "groupId");
            String existingArtifactId = getChildText(existingDependency, "artifactId");

            if (dependency.sameCoordinates(existingGroupId, existingArtifactId)) {
                return true;
            }
        }
        return false;
    }

    private void appendDependency(Document document, Element dependenciesElement, Dependency dependency) {
        String namespaceUri = dependenciesElement.getNamespaceURI();
        Element dependencyElement = namespaceUri == null
                ? document.createElement("dependency")
                : document.createElementNS(namespaceUri, "dependency");

        dependencyElement.appendChild(createTextElement(document, namespaceUri, "groupId", dependency.getGroupId()));
        dependencyElement.appendChild(createTextElement(document, namespaceUri, "artifactId", dependency.getArtifactId()));
        dependencyElement.appendChild(createTextElement(document, namespaceUri, "version", dependency.getVersion()));

        dependenciesElement.appendChild(document.createTextNode(System.lineSeparator()));
        dependenciesElement.appendChild(dependencyElement);
        dependenciesElement.appendChild(document.createTextNode(System.lineSeparator()));
    }

    private Element createTextElement(Document document, String namespaceUri, String name, String value) {
        Element element = namespaceUri == null
                ? document.createElement(name)
                : document.createElementNS(namespaceUri, name);
        element.setTextContent(value);
        return element;
    }

    private void saveDocument(Document document, Path pomPath) throws PomEditException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));

            Files.writeString(
                    pomPath,
                    writer.toString(),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
        } catch (TransformerException ex) {
            throw new PomEditException("Failed to save pom.xml", ex);
        }
    }

    private Element findDirectChild(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (isNamedElement(child, localName)) {
                return (Element) child;
            }
        }
        return null;
    }

    private boolean isNamedElement(Node node, String expectedName) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }

        String nodeLocalName = node.getLocalName();
        if (expectedName.equals(nodeLocalName)) {
            return true;
        }
        return expectedName.equals(node.getNodeName());
    }

    private String getChildText(Element parent, String childName) {
        Element child = findDirectChild(parent, childName);
        return child == null ? "" : child.getTextContent().trim();
    }

    public static final class PomEditException extends Exception {
        public PomEditException(String message) {
            super(message);
        }

        public PomEditException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

