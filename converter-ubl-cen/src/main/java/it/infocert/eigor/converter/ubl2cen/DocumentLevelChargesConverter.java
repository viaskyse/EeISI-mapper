package it.infocert.eigor.converter.ubl2cen;

import it.infocert.eigor.api.ConversionIssue;
import it.infocert.eigor.api.ConversionResult;
import it.infocert.eigor.api.CustomMapping;
import it.infocert.eigor.api.IConversionIssue;
import it.infocert.eigor.api.conversion.StringToDoubleConverter;
import it.infocert.eigor.model.core.enums.Untdid5305DutyTaxFeeCategories;
import it.infocert.eigor.model.core.enums.Untdid7161SpecialServicesCodes;
import it.infocert.eigor.model.core.model.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.List;

/**
 * The Document Level Charges Custom Converter
 */
public class DocumentLevelChargesConverter extends CustomConverterUtils implements CustomMapping<Document> {

    public ConversionResult<BG0000Invoice> toBG0021(Document document, BG0000Invoice invoice, List<IConversionIssue> errors) {

        StringToDoubleConverter strDblConverter = new StringToDoubleConverter();

        BG0021DocumentLevelCharges bg0021 = null;

        Element rootElement = document.getRootElement();
        List<Namespace> namespacesInScope = rootElement.getNamespacesIntroduced();

        List<Element> allowanceCharge = findNamespaceChildren(rootElement, namespacesInScope, "AllowanceCharge");
        
        for(Element elemAllowance : allowanceCharge) {

        	Element chargeIndicator = findNamespaceChild(elemAllowance, namespacesInScope, "ChargeIndicator");
        	if (chargeIndicator != null && chargeIndicator.getText().equals("true")) {
        		
        		bg0021 = new BG0021DocumentLevelCharges();

        		Element amount = findNamespaceChild(elemAllowance, namespacesInScope, "Amount");
        		if (amount != null) {
        			try {
        				BT0099DocumentLevelChargeAmount bt0099 = new BT0099DocumentLevelChargeAmount(strDblConverter.convert(amount.getText()));
        				bg0021.getBT0099DocumentLevelChargeAmount().add(bt0099);
        			}catch (NumberFormatException e) {
        				errors.add(ConversionIssue.newError(e, e.getMessage()));
        			}
        		}
                	
        		Element baseAmount = findNamespaceChild(elemAllowance, namespacesInScope, "BaseAmount");
        		if (baseAmount != null) {
        			try {
        				BT0100DocumentLevelChargeBaseAmount bt0100 = new BT0100DocumentLevelChargeBaseAmount(strDblConverter.convert(baseAmount.getText()));
        				bg0021.getBT0100DocumentLevelChargeBaseAmount().add(bt0100);
        			}catch (NumberFormatException e) {
        				errors.add(ConversionIssue.newError(e, e.getMessage()));
        			}
        		}
        		
        		Element multiplierFactorNumeric = findNamespaceChild(elemAllowance, namespacesInScope, "MultiplierFactorNumeric");
        		if (multiplierFactorNumeric != null) {
        			try {
        				BT0101DocumentLevelChargePercentage bt0101 = new BT0101DocumentLevelChargePercentage(strDblConverter.convert(multiplierFactorNumeric.getText()));
        				bg0021.getBT0101DocumentLevelChargePercentage().add(bt0101);
        			}catch (NumberFormatException e) {
        				errors.add(ConversionIssue.newError(e, e.getMessage()));
        			}
        		}
                    
        		Element taxCategory = findNamespaceChild(elemAllowance, namespacesInScope, "TaxCategory");
        		if (taxCategory != null) {
                    	
        			Element id = findNamespaceChild(taxCategory, namespacesInScope, "ID");
        			if (id != null) {
        				try{
        					BT0102DocumentLevelChargeVatCategoryCode bt0102 = new BT0102DocumentLevelChargeVatCategoryCode(Untdid5305DutyTaxFeeCategories.valueOf(id.getText()));
        					bg0021.getBT0102DocumentLevelChargeVatCategoryCode().add(bt0102);
						}catch (IllegalArgumentException e) {
							errors.add(ConversionIssue.newError(e, "Untdid5305DutyTaxFeeCategories non trovato"));
						}
        			}
        			Element percent = findNamespaceChild(taxCategory, namespacesInScope, "Percent");
        			if (percent != null) {
        				try {
        					BT0103DocumentLevelChargeVatRate bt0103 = new BT0103DocumentLevelChargeVatRate(strDblConverter.convert(percent.getText()));
        					bg0021.getBT0103DocumentLevelChargeVatRate().add(bt0103);
        				}catch (NumberFormatException e) {
        					errors.add(ConversionIssue.newError(e, e.getMessage()));
        				}
        			}
        		}
                    
        		Element allowanceChargeReason = findNamespaceChild(elemAllowance, namespacesInScope, "AllowanceChargeReason");
        		if (allowanceChargeReason != null) {
        			BT0104DocumentLevelChargeReason bt0104 = new BT0104DocumentLevelChargeReason(allowanceChargeReason.getText());
        			bg0021.getBT0104DocumentLevelChargeReason().add(bt0104);
        		}
                    
        		Element allowanceChargeReasonCode = findNamespaceChild(elemAllowance, namespacesInScope, "AllowanceChargeReasonCode");
        		if (allowanceChargeReasonCode != null) {
        			try{
        				BT0105DocumentLevelChargeReasonCode bt0105 = new BT0105DocumentLevelChargeReasonCode(Untdid7161SpecialServicesCodes.valueOf(allowanceChargeReasonCode.getText()));
        				bg0021.getBT0105DocumentLevelChargeReasonCode().add(bt0105);
					}catch (IllegalArgumentException e) {
						errors.add(ConversionIssue.newError(e, "Untdid7161SpecialServicesCodes non trovato"));
					}
        		}

        		invoice.getBG0021DocumentLevelCharges().add(bg0021);
        	}
        }
        return new ConversionResult<>(errors, invoice);
    }

    @Override
    public void map(BG0000Invoice cenInvoice, Document document, List<IConversionIssue> errors) {
        toBG0021(document, cenInvoice, errors);
    }
}