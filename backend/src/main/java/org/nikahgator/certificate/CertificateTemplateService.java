// STUB: replaced by feature/certificate-designer
package org.nikahgator.certificate;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.entity.NikahRequest;
import org.springframework.stereotype.Service;

@Service
public class CertificateTemplateService {

    /** Never null: an unsaved default template until the imam designs one. */
    public CertificateTemplate forMasjid(Long masjidId) {
        return new CertificateTemplate();
    }

    /** Replaces {{placeholders}}; unknown or empty values become a dash. */
    public String render(String text, Map<String, String> values) {
        if (text == null) return "";
        String out = text;
        for (Map.Entry<String, String> e : values.entrySet()) {
            String v = e.getValue() == null || e.getValue().isBlank() ? "-" : e.getValue();
            out = out.replace("{{" + e.getKey() + "}}", v);
        }
        return out.replaceAll("\\{\\{[a-zA-Z]+}}", "-");
    }

    public Map<String, String> valuesFor(Masjid m, NikahRequest n, AppUser imam, String certNumberOrNull) {
        Map<String, String> v = new HashMap<>();
        v.put("groomName", n.getGroomName());
        v.put("brideName", n.getBrideName());
        v.put("groomFather", n.getGroomFatherName());
        v.put("brideFather", n.getBrideFatherName());
        if (n.getScheduledAt() != null) {
            v.put("nikahDate", n.getScheduledAt().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)));
            v.put("nikahTime", n.getScheduledAt().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)));
        }
        v.put("mahr", n.getMahrDescription());
        v.put("wali", n.getWaliName());
        v.put("waliRelation", n.getWaliRelation());
        List<String> w = Stream.of(n.getWitness1Name(), n.getWitness2Name()).filter(s -> s != null && !s.isBlank()).toList();
        v.put("witnesses", String.join(", ", w));
        v.put("masjidName", m.getName());
        StringBuilder a = new StringBuilder();
        for (String s : new String[] { m.getAddressLine(), m.getCity(), m.getState(), m.getZip() }) {
            if (s != null && !s.isBlank()) {
                if (a.length() > 0) a.append(", ");
                a.append(s.trim());
            }
        }
        v.put("masjidAddress", a.toString());
        v.put("imamName", imam == null ? null : imam.getFullName());
        v.put("certNumber", certNumberOrNull);
        return v;
    }

    /** The masjid's logo file, or null when it has none. */
    public Path logoFile(Long masjidId) {
        return null;
    }
}
