package it.infocert.eigor.converter.fattpa2cen.converters;

import it.infocert.eigor.api.conversion.converter.TypeConverter;
import it.infocert.eigor.model.core.enums.Untdid2005DateTimePeriodQualifiers;

public class ItalianCodeStringToUntdid2005DateTimePeriodQualifiersConverter implements TypeConverter<String, Untdid2005DateTimePeriodQualifiers> {

    @Override
    public Untdid2005DateTimePeriodQualifiers convert(String s) {
        switch (s) {
            case "I":
                return Untdid2005DateTimePeriodQualifiers.Code3;
            case "D":
            case "S":
                return Untdid2005DateTimePeriodQualifiers.Code432;
            default:
                return null;
        }
    }

    @Override
    public Class<String> getSourceClass() {
        return String.class;
    }

    @Override
    public Class<Untdid2005DateTimePeriodQualifiers> getTargetClass() {
        return Untdid2005DateTimePeriodQualifiers.class;
    }

    public static TypeConverter<String, Untdid2005DateTimePeriodQualifiers> newConverter() {
        return new ItalianCodeStringToUntdid2005DateTimePeriodQualifiersConverter();
    }
}
