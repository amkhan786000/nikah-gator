package org.nikahgator.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nikahgator.dto.ChecklistDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.ChecklistItem;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.ChecklistItemRepository;
import org.nikahgator.repository.DocumentRepository;
import org.nikahgator.repository.NikahRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-nikah checklist. Defaults are created the first time it is opened. Everything is limited to the imam's masjid. */
@Service
public class ChecklistService {

    /** code, label */
    static final String[][] DEFAULTS = {
            {"MARRIAGE_LICENSE", "Marriage license"},
            {"GROOM_ID", "Groom ID"},
            {"BRIDE_ID", "Bride ID"},
            {"WALI_CONSENT", "Wali consent / presence"},
            {"WITNESSES", "Two witnesses"},
            {"PREMARITAL_COUNSELING", "Pre-marital counseling"},
            {"MAHR_AGREED", "Mahr agreed"},
    };

    private final ChecklistItemRepository items;
    private final DocumentRepository documents;
    private final NikahRequestRepository nikahs;

    public ChecklistService(ChecklistItemRepository items, DocumentRepository documents, NikahRequestRepository nikahs) {
        this.items = items;
        this.documents = documents;
        this.nikahs = nikahs;
    }

    /** The nikah, only if it belongs to the imam's masjid. */
    public NikahRequest nikahOf(AppUser imam, Long nikahId) {
        if (imam.getMasjidId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        }
        return nikahs.findByIdAndMasjidId(nikahId, imam.getMasjidId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nikah not found"));
    }

    @Transactional
    public List<ItemDto> list(AppUser imam, Long nikahId) {
        nikahOf(imam, nikahId);
        return ensure(nikahId).stream().map(ChecklistService::toDto).toList();
    }

    @Transactional
    public ItemDto add(AppUser imam, Long nikahId, NewItem in) {
        nikahOf(imam, nikahId);
        String label = in.label().trim();
        if (label.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a name for the item");
        int next = ensure(nikahId).stream().mapToInt(ChecklistItem::getSortOrder).max().orElse(-1) + 1;
        ChecklistItem it = new ChecklistItem();
        it.setRequestId(nikahId);
        it.setCode("CUSTOM");
        it.setLabel(label);
        it.setCustom(true);
        it.setSortOrder(next);
        return toDto(items.save(it));
    }

    @Transactional
    public ItemDto update(AppUser imam, Long nikahId, Long itemId, ItemUpdate in) {
        ChecklistItem it = find(imam, nikahId, itemId);
        if (in.required() != null) it.setRequired(in.required());
        if (in.done() != null && in.done() != it.isDone()) setDone(it, in.done());
        return toDto(items.save(it));
    }

    @Transactional
    public void delete(AppUser imam, Long nikahId, Long itemId) {
        ChecklistItem it = find(imam, nikahId, itemId);
        if (!it.isCustom()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Standard items can't be deleted; mark them not required instead");
        }
        documents.findByChecklistId(itemId).forEach(d -> {
            d.setChecklistId(null); // the documents stay, just no longer linked
            documents.save(d);
        });
        items.delete(it);
    }

    /** Marks the item done (used when a document is uploaded to it). */
    void markDone(ChecklistItem it) {
        if (!it.isDone()) {
            setDone(it, true);
            items.save(it);
        }
    }

    ChecklistItem find(AppUser imam, Long nikahId, Long itemId) {
        nikahOf(imam, nikahId);
        return items.findByIdAndRequestId(itemId, nikahId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Checklist item not found"));
    }

    /** done/total of required items for every nikah of the imam's masjid (a nikah with no list yet counts the defaults). */
    @Transactional(readOnly = true)
    public List<Progress> progress(AppUser imam) {
        if (imam.getMasjidId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        }
        List<NikahRequest> mine = nikahs.findByMasjidIdOrderByScheduledAtDescIdDesc(imam.getMasjidId());
        Map<Long, int[]> byNikah = new HashMap<>();
        if (!mine.isEmpty()) {
            for (ChecklistItem it : items.findByRequestIdIn(mine.stream().map(NikahRequest::getId).toList())) {
                int[] c = byNikah.computeIfAbsent(it.getRequestId(), k -> new int[2]);
                if (!it.isRequired()) continue;
                c[1]++;
                if (it.isDone()) c[0]++;
            }
        }
        return mine.stream().map(n -> {
            int[] c = byNikah.get(n.getId());
            return c == null ? new Progress(n.getId(), 0, DEFAULTS.length) : new Progress(n.getId(), c[0], c[1]);
        }).toList();
    }

    private List<ChecklistItem> ensure(Long nikahId) {
        List<ChecklistItem> all = items.findByRequestIdOrderBySortOrderAscIdAsc(nikahId);
        if (!all.isEmpty()) return all;
        all = new ArrayList<>();
        for (int i = 0; i < DEFAULTS.length; i++) {
            ChecklistItem it = new ChecklistItem();
            it.setRequestId(nikahId);
            it.setCode(DEFAULTS[i][0]);
            it.setLabel(DEFAULTS[i][1]);
            it.setSortOrder(i);
            all.add(items.save(it));
        }
        return all;
    }

    private static void setDone(ChecklistItem it, boolean done) {
        it.setDone(done);
        it.setDoneAt(done ? LocalDateTime.now() : null);
    }

    private static ItemDto toDto(ChecklistItem it) {
        return new ItemDto(it.getId(), it.getCode(), it.getLabel(), it.isRequired(), it.isDone(), it.getDoneAt(), it.isCustom());
    }
}
