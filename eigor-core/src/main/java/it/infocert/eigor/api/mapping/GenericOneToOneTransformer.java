package it.infocert.eigor.api.mapping;



import it.infocert.eigor.api.ConversionIssue;
import it.infocert.eigor.api.IConversionIssue;
import it.infocert.eigor.api.SyntaxErrorInInvoiceFormatException;
import it.infocert.eigor.api.conversion.ConversionRegistry;
import it.infocert.eigor.api.errors.ErrorCode;
import it.infocert.eigor.api.utils.IReflections;
import it.infocert.eigor.model.core.datatypes.Identifier;
import it.infocert.eigor.model.core.model.AbstractBT;
import it.infocert.eigor.model.core.model.BG0000Invoice;
import it.infocert.eigor.model.core.model.BTBG;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Generic class to transform both cen objects in XML elements and viceversa,
 * based on a 1-1 configurable mapping
 */
@SuppressWarnings("unchecked")
public class GenericOneToOneTransformer extends GenericTransformer {
    private final String xPath;
    private final String cenPath;
    private final ErrorCode.Location callingLocation;

    public GenericOneToOneTransformer(String xPath, String cenPath, IReflections reflections, ConversionRegistry conversionRegistry, ErrorCode.Location callingLocation) {
        super(reflections, conversionRegistry, callingLocation);
        this.xPath = xPath;
        this.cenPath = cenPath;
        this.callingLocation = callingLocation;
        log = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Transform a {@link BG0000Invoice} into an XML DOM that can be later serialized
     *
     * @param invoice  the {@link BG0000Invoice} from which to take the values
     * @param document the {@link Document} to populate with BT values
     * @param errors   a list of {@link ConversionIssue}, to be filled if an error occurs during the conversion
     * @throws SyntaxErrorInInvoiceFormatException
     */

    public void transformCenToXml(BG0000Invoice invoice, Document document, final List<IConversionIssue> errors) throws SyntaxErrorInInvoiceFormatException {

        final String logPrefix = "(" + cenPath + " => " + xPath + ") ";

        log.trace("{} resolving", logPrefix);
        List<BTBG> bts = getAllBTs(cenPath, invoice, errors);
        if (bts == null) return;

        List<Element> elements = getAllXmlElements(xPath, document, bts.size(), cenPath, errors);
        if (elements == null) return;

        for (int i = 0; i < bts.size(); i++) {
            AbstractBT btbg = (AbstractBT) bts.get(i);
            Object value = btbg.getValue();
            Element element = elements.get(i);
            if (value != null) {
                Class<?> aClass = value.getClass();
                String converted = conversionRegistry.convert(aClass, String.class, value);
                log.info("CEN '{}' with value '{}' mapped to XML element '{}' with value '{}'.",
                        btbg.denomination(), String.valueOf(value), element.getName(), converted);
                element.setText(converted);
            }
        }
    }

    /**
     * Transform a {@link Document} into an {@link BG0000Invoice}
     *
     * @param document the {@link Document} to read values from
     * @param invoice  the {@link BG0000Invoice} to populate with values from the XML
     * @param errors   a list of {@link ConversionIssue}, to be filled if an error occurs during the conversion
     * @throws SyntaxErrorInInvoiceFormatException
     */
    public void transformXmlToCen(Document document, BG0000Invoice invoice, final List<IConversionIssue> errors) throws SyntaxErrorInInvoiceFormatException {
        final String logPrefix = "(" + xPath + " => " + cenPath + ") :";

        log.trace("{} resolving BT class constructor.", logPrefix);
        Class<? extends BTBG> btBgByName = invoiceUtils.getBtBgByName(cenPath.substring(cenPath.lastIndexOf('/')+1));
        Constructor<?> cons = null;
        for (Constructor<?> constructor : btBgByName.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes!=null && parameterTypes.length>=1 && Identifier.class.equals(parameterTypes[0])) {
                cons = constructor;
                break;
            }
        }

        if (cons != null) {
            final Element node = getSingleNodeFromXpath(document, xPath);

            if (node != null) {
                Object assignedValue = addNewCenObjectWithIdentifierToInvoice(cenPath, invoice, node, errors);
                log.info("XML element '{}' mapped to CEN '{}' with value '{}'.",
                        xPath, cenPath, assignedValue);
            }
        } else {
            final String xPathText = getNodeTextFromXPath(document, xPath);
            if (xPathText != null) {
                Object assignedValue = addNewCenObjectFromStringValueToInvoice(cenPath, invoice, xPathText, errors);
                log.info("XML element '{}' with value '{}' mapped to CEN '{}' with value '{}'.",
                        xPath, xPathText, cenPath, assignedValue);
            }
        }
    }


}
