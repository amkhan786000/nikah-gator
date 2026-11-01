package org.nikahgator.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nikahgator.dto.PaymentDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.entity.NikahFee;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.entity.Payment;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.repository.NikahFeeRepository;
import org.nikahgator.repository.NikahRequestRepository;
import org.nikahgator.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual recording of nikah fees and donations (no card processor yet). Every query is limited to the imam's own
 * masjid; payments are never deleted, only voided or refunded with a reason.
 */
@Service
public class PaymentService {
    private static final Set<String> KINDS = Set.of(Payment.FEE, Payment.DONATION);
    private static final Set<String> METHODS = Set.of("CASH", "CHECK", "ZELLE", "CARD", "OTHER");

    private final PaymentRepository payments;
    private final NikahFeeRepository fees;
    private final NikahRequestRepository nikahs;
    private final MasjidRepository masjids;
    private final AppUserRepository users;

    public PaymentService(PaymentRepository payments, NikahFeeRepository fees, NikahRequestRepository nikahs,
                          MasjidRepository masjids, AppUserRepository users) {
        this.payments = payments;
        this.fees = fees;
        this.nikahs = nikahs;
        this.masjids = masjids;
        this.users = users;
    }

    // ---------------------------------------------------------------- masjid settings

    public MasjidSettings masjid(AppUser imam) {
        return toSettings(masjidEntity(imam));
    }

    @Transactional
    public MasjidSettings updateMasjid(AppUser imam, MasjidSettings in) {
        Masjid m = masjidEntity(imam);
        m.setNikahFeeCents(in.nikahFeeCents());
        m.setSuggestedDonationCents(in.suggestedDonationCents());
        return toSettings(masjids.save(m));
    }

    // ---------------------------------------------------------------- per nikah

    @Transactional(readOnly = true)
    public Summary summary(AppUser imam, Long nikahId) {
        NikahRequest n = nikah(imam, nikahId);
        Masjid m = masjidEntity(imam);
        List<Payment> all = payments.findByRequestIdAndMasjidIdOrderByPaidAtDescIdDesc(n.getId(), n.getMasjidId());
        NikahFee fee = fees.findById(n.getId()).orElse(null);
        int feeCents = fee != null ? fee.getFeeCents() : m.getNikahFeeCents();
        boolean waived = fee != null && fee.isWaived();
        int paid = sum(all, Payment.FEE);
        int donations = sum(all, Payment.DONATION);
        Map<Long, String> names = new HashMap<>();
        List<PaymentView> views = all.stream().map(p -> toView(p, names)).toList();
        return new Summary(feeCents, waived, fee != null ? fee.getWaivedReason() : null, paid,
                PaymentCalc.balance(feeCents, waived, paid), donations, PaymentCalc.status(feeCents, waived, paid),
                m.getSuggestedDonationCents(), views);
    }

    /** Payment status of every nikah of the masjid, for the badge in the nikah list. */
    @Transactional(readOnly = true)
    public List<StatusRow> statuses(AppUser imam) {
        Long masjidId = masjidId(imam);
        int defaultFee = masjidEntity(imam).getNikahFeeCents();
        List<NikahRequest> all = nikahs.findByMasjidIdOrderByScheduledAtDescIdDesc(masjidId);
        Map<Long, NikahFee> feeById = new HashMap<>();
        fees.findAllById(all.stream().map(NikahRequest::getId).toList()).forEach(f -> feeById.put(f.getRequestId(), f));
        Map<Long, Integer> paidById = new HashMap<>();
        for (Payment p : payments.findByMasjidIdAndStatus(masjidId, Payment.PAID)) {
            if (Payment.FEE.equals(p.getKind())) paidById.merge(p.getRequestId(), p.getAmountCents(), Integer::sum);
        }
        return all.stream().map(n -> {
            NikahFee f = feeById.get(n.getId());
            int fee = f != null ? f.getFeeCents() : defaultFee;
            boolean waived = f != null && f.isWaived();
            int paid = paidById.getOrDefault(n.getId(), 0);
            return new StatusRow(n.getId(), PaymentCalc.status(fee, waived, paid), PaymentCalc.balance(fee, waived, paid));
        }).toList();
    }

    @Transactional
    public PaymentView record(AppUser imam, Long nikahId, RecordPayment in) {
        NikahRequest n = nikah(imam, nikahId);
        String kind = in.kind().trim().toUpperCase();
        String method = in.method().trim().toUpperCase();
        if (!KINDS.contains(kind)) throw new ApiException(HttpStatus.BAD_REQUEST, "Payment type must be FEE or DONATION.");
        if (!METHODS.contains(method)) throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown payment method.");
        if (in.paidAt().isAfter(LocalDate.now().plusDays(1))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The date received cannot be in the future.");
        }
        snapshotFee(imam, n);

        Payment p = new Payment();
        p.setRequestId(n.getId());
        p.setMasjidId(n.getMasjidId());
        p.setAmountCents(in.amountCents());
        p.setKind(kind);
        p.setMethod(method);
        p.setPaidAt(in.paidAt());
        p.setPayerName(blank(in.payerName()) != null ? blank(in.payerName()) : n.getGroomName());
        p.setNote(blank(in.note()));
        p.setReceivedBy(imam.getId());
        p.setReceiptNumber(nextReceiptNumber(n.getMasjidId(), in.paidAt().getYear()));
        return toView(payments.save(p), new HashMap<>());
    }

    @Transactional
    public PaymentView refund(AppUser imam, Long nikahId, Long paymentId, Reason in) {
        return changeStatus(imam, nikahId, paymentId, Payment.REFUNDED, in.reason());
    }

    @Transactional
    public PaymentView voidPayment(AppUser imam, Long nikahId, Long paymentId, Reason in) {
        return changeStatus(imam, nikahId, paymentId, Payment.VOID, in.reason());
    }

    @Transactional
    public Summary waive(AppUser imam, Long nikahId, Waiver in) {
        NikahRequest n = nikah(imam, nikahId);
        NikahFee f = snapshotFee(imam, n);
        if (in.waived() && (in.reason() == null || in.reason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please give a reason for waiving the fee.");
        }
        f.setWaived(in.waived());
        f.setWaivedReason(in.waived() ? in.reason().trim() : null);
        f.setUpdatedBy(imam.getId());
        fees.save(f);
        return summary(imam, nikahId);
    }

    @Transactional(readOnly = true)
    public Receipt receipt(AppUser imam, Long nikahId, Long paymentId) {
        NikahRequest n = nikah(imam, nikahId);
        Payment p = payments.findByIdAndRequestIdAndMasjidId(paymentId, n.getId(), n.getMasjidId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!Payment.PAID.equals(p.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No receipt for a " + p.getStatus().toLowerCase() + " payment.");
        }
        Masjid m = masjidEntity(imam);
        String address = String.join(", ", java.util.stream.Stream.of(m.getAddressLine(), m.getCity(),
                ((m.getState() == null ? "" : m.getState()) + " " + (m.getZip() == null ? "" : m.getZip())).trim())
                .filter(s -> s != null && !s.isBlank()).toList());
        return new Receipt(p.getReceiptNumber(), m.getName(), address, m.getPhone(), p.getPayerName(), n.getGroomName(),
                n.getBrideName(), p.getKind(), p.getAmountCents(), p.getCurrency(), p.getMethod(), p.getPaidAt(),
                p.getNote(), userName(p.getReceivedBy(), new HashMap<>()));
    }

    // ---------------------------------------------------------------- helpers

    private PaymentView changeStatus(AppUser imam, Long nikahId, Long paymentId, String status, String reason) {
        NikahRequest n = nikah(imam, nikahId);
        Payment p = payments.findByIdAndRequestIdAndMasjidId(paymentId, n.getId(), n.getMasjidId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!Payment.PAID.equals(p.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This payment is already " + p.getStatus().toLowerCase() + ".");
        }
        p.setStatus(status);
        p.setStatusReason(reason.trim());
        p.setStatusChangedBy(imam.getId());
        p.setStatusChangedAt(LocalDateTime.now());
        return toView(payments.save(p), new HashMap<>());
    }

    /** Fixes the fee of this nikah at the masjid's current default the first time money or a waiver is recorded. */
    private NikahFee snapshotFee(AppUser imam, NikahRequest n) {
        return fees.findById(n.getId()).orElseGet(() -> {
            NikahFee f = new NikahFee();
            f.setRequestId(n.getId());
            f.setFeeCents(masjidEntity(imam).getNikahFeeCents());
            f.setUpdatedBy(imam.getId());
            return fees.save(f);
        });
    }

    private String nextReceiptNumber(Long masjidId, int year) {
        payments.bumpReceiptCounter(masjidId, year);
        return "NGR-" + masjidId + "-" + year + "-" + String.format("%04d", payments.lastInsertId());
    }

    private static int sum(List<Payment> all, String kind) {
        return all.stream().filter(p -> Payment.PAID.equals(p.getStatus()) && kind.equals(p.getKind()))
                .mapToInt(Payment::getAmountCents).sum();
    }

    private PaymentView toView(Payment p, Map<Long, String> names) {
        return new PaymentView(p.getId(), p.getKind(), p.getAmountCents(), p.getStatus(), p.getMethod(), p.getPaidAt(),
                p.getPayerName(), p.getNote(), p.getReceiptNumber(), p.getStatusReason(), p.getStatusChangedAt(),
                userName(p.getReceivedBy(), names), p.getCreatedAt());
    }

    private String userName(Long id, Map<Long, String> cache) {
        if (id == null) return null;
        return cache.computeIfAbsent(id, i -> users.findById(i).map(AppUser::getFullName).orElse(null));
    }

    private NikahRequest nikah(AppUser imam, Long id) {
        return nikahs.findByIdAndMasjidId(id, masjidId(imam))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nikah not found"));
    }

    private Masjid masjidEntity(AppUser imam) {
        return masjids.findById(masjidId(imam)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
    }

    private static Long masjidId(AppUser imam) {
        if (imam.getMasjidId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        }
        return imam.getMasjidId();
    }

    private static MasjidSettings toSettings(Masjid m) {
        return new MasjidSettings(m.getId(), m.getName(), m.getAddressLine(), m.getCity(), m.getState(), m.getZip(),
                m.getPhone(), m.getEmail(), m.getNikahFeeCents(), m.getSuggestedDonationCents());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
