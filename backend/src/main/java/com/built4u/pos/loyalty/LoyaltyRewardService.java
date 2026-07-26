package com.built4u.pos.loyalty;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.item.Item;
import com.built4u.pos.item.ItemRepository;
import com.built4u.pos.loyalty.dto.RewardDto;
import com.built4u.pos.loyalty.dto.SaveRewardRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Loyalty reward catalog ({@code pos_loyalty_reward}), a per-site catalog. */
@Service
@RequiredArgsConstructor
public class LoyaltyRewardService {

    private final LoyaltyRewardRepository repo;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<RewardDto> list() {
        long siteId = TenantContext.requireSiteId();
        return repo.findBySiteIdOrderBySortOrderAscNameAsc(siteId).stream()
            .map(r -> RewardDto.from(r, itemName(siteId, r.getItemId())))
            .toList();
    }

    /** Active rewards for the current site (Customers-screen redeem panel). */
    @Transactional(readOnly = true)
    public List<RewardDto> listActive() {
        long siteId = TenantContext.requireSiteId();
        return repo.findBySiteIdAndActiveTrueOrderBySortOrderAscNameAsc(siteId).stream()
            .map(r -> RewardDto.from(r, itemName(siteId, r.getItemId())))
            .toList();
    }

    @Transactional
    public RewardDto create(SaveRewardRequest r) {
        long siteId = TenantContext.requireSiteId();
        Long itemId = validate(siteId, r);
        String name = r.name().trim();
        repo.findBySiteIdAndNameIgnoreCase(siteId, name).ifPresent(x -> {
            throw new ConflictException("A reward named '" + name + "' already exists at this site");
        });
        LoyaltyReward saved = repo.save(LoyaltyReward.builder()
            .siteId(siteId).name(name).description(blankToNull(r.description()))
            .pointsCost(r.pointsCost()).rewardType(r.rewardType()).itemId(itemId)
            .sortOrder(r.sortOrder()).active(r.active()).build());
        return RewardDto.from(saved, itemName(siteId, itemId));
    }

    @Transactional
    public RewardDto update(Long id, SaveRewardRequest r) {
        long siteId = TenantContext.requireSiteId();
        Long itemId = validate(siteId, r);
        LoyaltyReward reward = repo.findBySiteIdAndId(siteId, id)
            .orElseThrow(() -> new NotFoundException("Reward " + id + " not found"));
        String name = r.name().trim();
        repo.findBySiteIdAndNameIgnoreCase(siteId, name)
            .filter(other -> !other.getId().equals(id))
            .ifPresent(o -> {
                throw new ConflictException("A reward named '" + name + "' already exists at this site");
            });
        reward.setName(name);
        reward.setDescription(blankToNull(r.description()));
        reward.setPointsCost(r.pointsCost());
        reward.setRewardType(r.rewardType());
        reward.setItemId(itemId);
        reward.setSortOrder(r.sortOrder());
        reward.setActive(r.active());
        return RewardDto.from(repo.save(reward), itemName(siteId, itemId));
    }

    @Transactional
    public void delete(Long id) {
        long siteId = TenantContext.requireSiteId();
        LoyaltyReward reward = repo.findBySiteIdAndId(siteId, id)
            .orElseThrow(() -> new NotFoundException("Reward " + id + " not found"));
        repo.delete(reward);
    }

    /** Returns the resolved itemId (null for FREETEXT) after validation. */
    private Long validate(long siteId, SaveRewardRequest r) {
        if ("ITEM".equals(r.rewardType())) {
            if (r.itemId() == null) {
                throw new BadRequestException("An item-linked reward must reference an inventory item.");
            }
            itemRepository.findBySiteIdAndItemId(siteId, r.itemId())
                .orElseThrow(() -> new BadRequestException(
                    "Inventory item " + r.itemId() + " not found at this site."));
            return r.itemId();
        }
        return null; // FREETEXT — never carries an item link
    }

    private String itemName(long siteId, Long itemId) {
        if (itemId == null) return null;
        return itemRepository.findBySiteIdAndItemId(siteId, itemId).map(Item::getItemName).orElse(null);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
