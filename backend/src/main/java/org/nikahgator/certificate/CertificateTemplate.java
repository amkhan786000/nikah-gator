// STUB: replaced by feature/certificate-designer
package org.nikahgator.certificate;

import lombok.Getter;
import lombok.Setter;

/** Per-masjid certificate wording and look. This stub is a plain object holding the defaults. */
@Getter @Setter
public class CertificateTemplate {
    private String title = "Nikah Certificate";
    private boolean bismillahEnabled = true;
    private String masjidDisplayName = "{{masjidName}}";
    private String addressText = "{{masjidAddress}}";
    private String phoneText;
    private String introText = "This is to certify that the marriage (nikah) of";
    private String bodyText = "{{groomName}} son of {{groomFather}}\nwith\n{{brideName}} daughter of {{brideFather}}\n"
            + "was solemnized according to the Islamic rites on {{nikahDate}} at {{nikahTime}}.\n"
            + "Mahr: {{mahr}}. Wali: {{wali}} ({{waliRelation}}). Witnesses: {{witnesses}}.";
    private String footerText;
    private String leftSignatureLabel = "Imam / Officiant: {{imamName}}";
    private String rightSignatureLabel = "Date";
    private String accentColor = "#4f7a00";
    private String logoPath;
    private String paperSize = "LETTER";

    public boolean isBismillahEnabled() {
        return bismillahEnabled;
    }
}
