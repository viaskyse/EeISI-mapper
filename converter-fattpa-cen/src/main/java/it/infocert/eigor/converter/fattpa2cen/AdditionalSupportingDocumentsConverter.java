package it.infocert.eigor.converter.fattpa2cen;

import it.infocert.eigor.api.*;
import it.infocert.eigor.api.configuration.DefaultEigorConfigurationLoader;
import it.infocert.eigor.api.configuration.EigorConfiguration;
import it.infocert.eigor.api.conversion.ConversionFailedException;
import it.infocert.eigor.api.conversion.converter.AttachmentToFileReferenceConverter;
import it.infocert.eigor.api.conversion.converter.TypeConverter;
import it.infocert.eigor.api.errors.ErrorCode;
import it.infocert.eigor.api.errors.ErrorMessage;
import it.infocert.eigor.api.xml.LoggingResourceResolver;
import it.infocert.eigor.model.core.datatypes.FileReference;
import it.infocert.eigor.model.core.model.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The Additional Supporting Documents Custom Converter
 */
public class AdditionalSupportingDocumentsConverter implements CustomMapping<Document> {

    private final static Logger log = LoggerFactory.getLogger(AdditionalSupportingDocumentsConverter.class);

    public ConversionResult<BG0000Invoice> toBG0024(Document document, BG0000Invoice invoice, List<IConversionIssue> errors) {

        BG0024AdditionalSupportingDocuments bg0024;


        TypeConverter<Element, FileReference> strToBinConverter = AttachmentToFileReferenceConverter.newConverter(DefaultEigorConfigurationLoader.configuration(), ErrorCode.Location.FATTPA_IN);

        Element rootElement = document.getRootElement();
        Element fatturaElettronicaBody = rootElement.getChild("FatturaElettronicaBody");
        Element fatturaElettronicaHeader = rootElement.getChild("FatturaElettronicaHeader");

        if (fatturaElettronicaBody != null) {
            List<Element> allegati = fatturaElettronicaBody.getChildren();
            for (Element allegato : allegati) {
                if (allegato.getName().equals("Allegati")) {
                    bg0024 = new BG0024AdditionalSupportingDocuments();
                    Element nomeAttachment = allegato.getChild("NomeAttachment");
                    if (nomeAttachment != null) {
                        BT0122SupportingDocumentReference supportingDocumentReference = new BT0122SupportingDocumentReference(nomeAttachment.getText());
                        bg0024.getBT0122SupportingDocumentReference().add(supportingDocumentReference);
                    }
                    Element descrizioneAttachment = allegato.getChild("DescrizioneAttachment");
                    if (descrizioneAttachment != null) {
                        BT0123SupportingDocumentDescription supportingDocumentDescription = new BT0123SupportingDocumentDescription(descrizioneAttachment.getText());
                        bg0024.getBT0123SupportingDocumentDescription().add(supportingDocumentDescription);
                    }
                    Element attachment = allegato.getChild("Attachment");
                    if (attachment != null) {
                        try {
                            Element formatoAttachment = allegato.getChild("FormatoAttachment");
                            attachment.setAttribute("mimeCode", (formatoAttachment == null) ? "text/csv" : getFullMimeNameFromShortFormat(formatoAttachment.getText()));
                            attachment.setAttribute("filename", nomeAttachment.getText());
                            BT0125AttachedDocumentAndAttachedDocumentMimeCodeAndAttachedDocumentFilename bt0125 =
                                    new BT0125AttachedDocumentAndAttachedDocumentMimeCodeAndAttachedDocumentFilename(strToBinConverter.convert(attachment));
                            bg0024.getBT0125AttachedDocumentAndAttachedDocumentMimeCodeAndAttachedDocumentFilename().add(bt0125);
                        } catch (IllegalArgumentException | ConversionFailedException e) {
                            EigorRuntimeException ere = new EigorRuntimeException(
                                    e,
                                    ErrorMessage.builder()
                                            .message(e.getMessage())
                                            .location(ErrorCode.Location.FATTPA_IN)
                                            .action(ErrorCode.Action.HARDCODED_MAP)
                                            .error(ErrorCode.Error.ILLEGAL_VALUE)
                                            .addParam(ErrorMessage.SOURCEMSG_PARAM, e.getMessage())
                                            .addParam(ErrorMessage.OFFENDINGITEM_PARAM, attachment.toString())
                                            .build());
                            errors.add(ConversionIssue.newError(ere));
                        }
                    }
                    BT0125AttachedDocumentAndAttachedDocumentMimeCodeAndAttachedDocumentFilename bt0125 = bg0024.getBT0125AttachedDocumentAndAttachedDocumentMimeCodeAndAttachedDocumentFilename(0);
                    FileReference fileReference = bt0125.getValue();
                    String filePath = fileReference.getFilePath().replace("\\null", "");

                    EigorConfiguration eigorConfiguration = new DefaultEigorConfigurationLoader().loadConfiguration();
                    AttachmentUtil attachmentUtil = new AttachmentUtil(new File(eigorConfiguration.getMandatoryString("eigor.workdir")));
                    Element terzoIntermediarioOSoggettoEmittente = fatturaElettronicaHeader.getChild("TerzoIntermediarioOSoggettoEmittente");
                    if (terzoIntermediarioOSoggettoEmittente != null) {
                        try {
                            Element datiAnagrafici = terzoIntermediarioOSoggettoEmittente.getChild("DatiAnagrafici");
                            if (datiAnagrafici != null) {
                                Element idFiscaleIVA = datiAnagrafici.getChild("IdFiscaleIVA");
                                if (idFiscaleIVA != null) {
                                    Element idPaese = idFiscaleIVA.getChild("IdPaese");
                                    if (idPaese != null) {
                                        attachmentUtil.appendToFileInBase64(new File(filePath), idPaese.getText());
                                    }
                                    Element idCodice = idFiscaleIVA.getChild("IdCodice");
                                    if (idCodice != null) {
                                        attachmentUtil.appendToFileInBase64(new File(filePath), idCodice.getText());
                                    }
                                }
                                Element codiceFiscale = datiAnagrafici.getChild("CodiceFiscale");
                                if (codiceFiscale != null) {
                                    attachmentUtil.appendToFileInBase64(new File(filePath), codiceFiscale.getText());
                                }
                                Element anagrafica = datiAnagrafici.getChild("Anagrafica");
                                if (anagrafica != null) {
                                    Element denominazione = anagrafica.getChild("Denominazione");
                                    if (denominazione != null) {
                                        attachmentUtil.appendToFileInBase64(new File(filePath), denominazione.getText());
                                    }
                                    Element titolo = anagrafica.getChild("Titolo");
                                    if (titolo != null) {
                                        attachmentUtil.appendToFileInBase64(new File(filePath), titolo.getText());
                                    }
                                    Element codEORI = anagrafica.getChild("CodEORI");
                                    if (codEORI != null) {
                                        attachmentUtil.appendToFileInBase64(new File(filePath), codEORI.getText());
                                    }
                                }
                            }
                        } catch (IllegalArgumentException | IOException e) {
                            EigorRuntimeException ere = new EigorRuntimeException(
                                    e,
                                    ErrorMessage.builder()
                                            .message(e.getMessage())
                                            .location(ErrorCode.Location.FATTPA_IN)
                                            .action(ErrorCode.Action.HARDCODED_MAP)
                                            .error(ErrorCode.Error.ILLEGAL_VALUE)
                                            .addParam(ErrorMessage.SOURCEMSG_PARAM, e.getMessage())
                                            .addParam(ErrorMessage.OFFENDINGITEM_PARAM, terzoIntermediarioOSoggettoEmittente.toString())
                                            .build());
                            errors.add(ConversionIssue.newError(ere));
                        }
                    }
                    Element soggettoEmittente = fatturaElettronicaHeader.getChild("SoggettoEmittente");
                    if (soggettoEmittente != null) {
                        try {
                            attachmentUtil.appendToFileInBase64(new File(filePath), soggettoEmittente.getText());
                        } catch (IllegalArgumentException | IOException e) {
                            EigorRuntimeException ere = new EigorRuntimeException(
                                    e,
                                    ErrorMessage.builder()
                                            .message(e.getMessage())
                                            .location(ErrorCode.Location.FATTPA_IN)
                                            .action(ErrorCode.Action.HARDCODED_MAP)
                                            .error(ErrorCode.Error.ILLEGAL_VALUE)
                                            .addParam(ErrorMessage.SOURCEMSG_PARAM, e.getMessage())
                                            .addParam(ErrorMessage.OFFENDINGITEM_PARAM, soggettoEmittente.toString())
                                            .build());
                            errors.add(ConversionIssue.newError(ere));
                        }
                    }
                    Element datiGenerali = fatturaElettronicaBody.getChild("DatiGenerali");
                    if (datiGenerali != null) {
                        Element datiContratto = datiGenerali.getChild("DatiContratto");
                        if (datiContratto != null) {
                            try {
                                Element codiceCUP = datiContratto.getChild("CodiceCUP");
                                if (codiceCUP != null) {
                                    attachmentUtil.appendToFileInBase64(new File(filePath), codiceCUP.getText());
                                }
                                Element codiceCIG = datiContratto.getChild("CodiceCIG");
                                if (codiceCIG != null) {
                                    attachmentUtil.appendToFileInBase64(new File(filePath), codiceCIG.getText());
                                }
                            } catch (IllegalArgumentException | IOException e) {
                                EigorRuntimeException ere = new EigorRuntimeException(
                                        e,
                                        ErrorMessage.builder()
                                                .message(e.getMessage())
                                                .location(ErrorCode.Location.FATTPA_IN)
                                                .action(ErrorCode.Action.HARDCODED_MAP)
                                                .error(ErrorCode.Error.ILLEGAL_VALUE)
                                                .addParam(ErrorMessage.SOURCEMSG_PARAM, e.getMessage())
                                                .addParam(ErrorMessage.OFFENDINGITEM_PARAM, datiContratto.toString())
                                                .build());
                                errors.add(ConversionIssue.newError(ere));
                            }
                        }
                    }
                    Element datiVeicoli = fatturaElettronicaBody.getChild("DatiVeicoli");
                    if (datiVeicoli != null) {
                        try {
                            Element data = datiVeicoli.getChild("Data");
                            if (data != null) {
                                attachmentUtil.appendToFileInBase64(new File(filePath), data.getText());
                            }
                            Element totalePercorso = datiVeicoli.getChild("TotalePercorso");
                            if (totalePercorso != null) {
                                attachmentUtil.appendToFileInBase64(new File(filePath), totalePercorso.getText());
                            }
                        } catch (IllegalArgumentException | IOException e) {
                            EigorRuntimeException ere = new EigorRuntimeException(
                                    e,
                                    ErrorMessage.builder()
                                            .message(e.getMessage())
                                            .location(ErrorCode.Location.FATTPA_IN)
                                            .action(ErrorCode.Action.HARDCODED_MAP)
                                            .error(ErrorCode.Error.ILLEGAL_VALUE)
                                            .addParam(ErrorMessage.SOURCEMSG_PARAM, e.getMessage())
                                            .addParam(ErrorMessage.OFFENDINGITEM_PARAM, datiVeicoli.toString())
                                            .build());
                            errors.add(ConversionIssue.newError(ere));
                        }
                    }
                    Element datiOrdineAcquisto = datiGenerali.getChild("DatiOrdineAcquisto");
                    if (datiOrdineAcquisto != null) {
                        try {
                            Element codiceCommessaConvenzione = datiOrdineAcquisto.getChild("CodiceCommessaConvenzione");
                            if (codiceCommessaConvenzione != null) {
                                attachmentUtil.appendToFileInBase64(new File(filePath), codiceCommessaConvenzione.getText());
                            }
                            Element codiceCUP = datiOrdineAcquisto.getChild("CodiceCUP");
                            if (codiceCUP != null) {
                                attachmentUtil.appendToFileInBase64(new File(filePath), codiceCUP.getText());
                            }
                            Element codiceCIG = datiOrdineAcquisto.getChild("CodiceCIG");
                            if (codiceCIG != null) {
                                attachmentUtil.appendToFileInBase64(new File(filePath), codiceCIG.getText());
                            }
                        } catch (IllegalArgumentException | IOException e) {
                            EigorRuntimeException ere = new EigorRuntimeException(
                                    e,
                                    ErrorMessage.builder()
                                            .message(e.getMessage())
                                            .location(ErrorCode.Location.FATTPA_IN)
                                            .action(ErrorCode.Action.HARDCODED_MAP)
                                            .error(ErrorCode.Error.ILLEGAL_VALUE)
                                            .addParam(ErrorMessage.SOURCEMSG_PARAM, e.getMessage())
                                            .addParam(ErrorMessage.OFFENDINGITEM_PARAM, datiOrdineAcquisto.toString())
                                            .build());
                            errors.add(ConversionIssue.newError(ere));
                        }
                    }
                    invoice.getBG0024AdditionalSupportingDocuments().add(bg0024);
                }
            }
        }

        return new ConversionResult<>(errors, invoice);
    }

    @Override
    public void map(BG0000Invoice cenInvoice, Document document, List<IConversionIssue> errors, ErrorCode.Location callingLocation, EigorConfiguration eigorConfiguration) {
        toBG0024(document, cenInvoice, errors);
    }

    private String getFullMimeNameFromShortFormat(String shortFormat) {
        if (shortFormat == null) return "text/csv";
        switch (shortFormat.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "ods":
                return "application/vnd.oasis.opendocument.spreadsheet";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "png":
                return "image/png";
            case "jpeg":
                return "image/jpeg";
            case "csv":
                return "text/csv";
            default:
                log.warn("MimeType " + shortFormat + " not supported [pdf, ods, xlsx, png, jpeg, csv], replaced by default value: \"text/csv\"");
                return "text/csv";
        }
    }
}
