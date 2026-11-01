package org.nikahgator.certificate;

import org.nikahgator.audit.Auditable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** How a masjid's nikah certificate looks and reads. One row per masjid. */
@Entity
@Table(name = "certificate_templates")
@Getter @Setter
public class CertificateTemplate extends Auditable {
    public static final String DEFAULT_TITLE = "Nikah Certificate";
    public static final String DEFAULT_INTRO = "This is to certify that the marriage (nikah) of";
    public static final String DEFAULT_BODY = "{{groomName}} son of {{groomFather}} with {{brideName}} daughter of {{brideFather}} "
            + "was solemnized according to the Islamic rites on {{nikahDate}} at {{nikahTime}}, with mahr: {{mahr}}. "
            + "Wali: {{wali}} ({{waliRelation}}). Witnesses: {{witnesses}}.";
    public static final String DEFAULT_COLOR = "#4f7a00";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    private String title = DEFAULT_TITLE;

    @Column(name = "bismillah_enabled")
    private boolean bismillahEnabled = true;

    @Column(name = "masjid_display_name")
    private String masjidDisplayName;

    @Column(name = "address_text")
    private String addressText;

    @Column(name = "phone_text")
    private String phoneText;

    @Column(name = "intro_text")
    private String introText = DEFAULT_INTRO;

    @Column(name = "body_text")
    private String bodyText = DEFAULT_BODY;

    @Column(name = "footer_text")
    private String footerText = "";

    @Column(name = "left_signature_label")
    private String leftSignatureLabel = "Imam / Officiant";

    @Column(name = "right_signature_label")
    private String rightSignatureLabel = "Date";

    @Column(name = "accent_color")
    private String accentColor = DEFAULT_COLOR;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "paper_size")
    private String paperSize = "LETTER";

}
