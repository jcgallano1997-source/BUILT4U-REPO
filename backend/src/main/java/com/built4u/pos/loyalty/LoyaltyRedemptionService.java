package com.built4u.pos.loyalty;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.customer.Customer;
import com.built4u.pos.customer.CustomerRepository;
import com.built4u.pos.item.Item;
import com.built4u.pos.item.ItemRepository;
import com.built4u.pos.loyalty.dto.RedeemRewardRequest;
import com.built4u.pos.loyalty.dto.RedeemRewardResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Redeems a customer's loyalty points for a catalog reward. Atomic: locks the
 * customer + reward rows, validates the balance, decrements the linked item's
 * stock by 1 for ITEM rewards, deducts the points, records the redemption, and
 * posts a REDEEM ledger entry — all in one transaction.
 */
@Service
@RequiredArgsConstructor
public class LoyaltyRedemptionService {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final CustomerRepository customerRepository;
    private final LoyaltyRewardRepository rewardRepository;
    private final LoyaltyRedemptionRepository redemptionRepository;
    private final ItemRepository itemRepository;
    private final LoyaltyLedgerService loyaltyLedgerService;

    @Transactional
    public RedeemRewardResult redeem(RedeemRewardRequest req) {
        Long siteId = TenantContext.requireSiteId();

        Customer customer = customerRepository
            .findBySiteIdAndCustomerIdForUpdate(siteId, req.customerId())
            .orElseThrow(() -> new NotFoundException("Customer " + req.customerId() + " not found"));

        LoyaltyReward reward = rewardRepository
            .findBySiteIdAndIdForUpdate(siteId, req.rewardId())
            .orElseThrow(() -> new NotFoundException("Reward " + req.rewardId() + " not found"));
        if (!reward.isActive()) {
            throw new BadRequestException("Reward '" + reward.getName() + "' is not active.");
        }

        BigDecimal cost = reward.getPointsCost();
        BigDecimal balance = customer.getPoints() == null ? BigDecimal.ZERO : customer.getPoints();
        if (cost.compareTo(balance) > 0) {
            throw new BadRequestException(
                "Not enough points — '" + reward.getName() + "' costs " + cost.toPlainString()
                + " but the customer has " + balance.toPlainString() + ".");
        }

        String itemName = null;
        if ("ITEM".equals(reward.getRewardType()) && reward.getItemId() != null) {
            Item item = itemRepository.findBySiteIdAndItemIdForUpdate(siteId, reward.getItemId())
                .orElseThrow(() -> new BadRequestException(
                    "The linked inventory item no longer exists — update the reward."));
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            if (qty.compareTo(ONE) < 0) {
                throw new BadRequestException(
                    "'" + item.getItemName() + "' is out of stock — cannot redeem this reward.");
            }
            item.setQuantity(qty.subtract(ONE));
            itemRepository.save(item);
            itemName = item.getItemName();
        }

        customer.setPoints(balance.subtract(cost));
        customerRepository.save(customer);

        redemptionRepository.save(LoyaltyRedemption.builder()
            .siteId(siteId).rewardId(reward.getId()).customerId(customer.getCustomerId())
            .rewardName(reward.getName()).rewardType(reward.getRewardType()).itemId(reward.getItemId())
            .pointsSpent(cost).note(blankToNull(req.note())).build());

        loyaltyLedgerService.post(siteId, customer.getCustomerId(), "REDEEM",
            cost.negate(), null, "Redeemed reward '" + reward.getName() + "'", null);

        return new RedeemRewardResult(reward.getName(), reward.getRewardType(), itemName, cost, customer.getPoints());
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
