package com.drama.service;

import com.drama.entity.Drama;
import com.drama.entity.DramaEpisode;
import com.drama.entity.PromotionLink;
import com.drama.entity.RechargePlan;
import com.drama.entity.User;
import com.drama.entity.UserWatchHistory;
import com.drama.exception.BusinessException;
import com.drama.mapper.DramaEpisodeMapper;
import com.drama.mapper.DramaMapper;
import com.drama.mapper.PromotionLinkMapper;
import com.drama.mapper.UserMapper;
import com.drama.mapper.UserWatchHistoryMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class H5Service {

    private final PromotionLinkMapper promotionLinkMapper;
    private final DramaMapper dramaMapper;
    private final DramaEpisodeMapper dramaEpisodeMapper;
    private final UserMapper userMapper;
    private final UserWatchHistoryMapper userWatchHistoryMapper;
    private final RechargePlanGroupService rechargePlanGroupService;
    private final RechargePlanService rechargePlanService;
    private final RechargeService rechargeService;
    private final VodService vodService;

    public Map<String, Object> getDramaByPromo(String promoId, String deviceId) {
        PromotionLink link = requirePromotion(promoId);
        User user = ensureAnonymousUser(deviceId, link);
        Drama drama = requireDrama(link.getDramaId());
        List<DramaEpisode> episodes = loadEpisodes(drama.getId());

        Map<String, Object> dramaMap = new LinkedHashMap<>();
        dramaMap.put("id", drama.getId());
        dramaMap.put("public_id", drama.getPublicId() != null ? drama.getPublicId() : "");
        dramaMap.put("title", drama.getTitle() != null ? drama.getTitle() : "");
        dramaMap.put("free_episodes", resolveFreeEpisodes(link, drama));
        dramaMap.put("beans_per_episode", resolveBeansPerEpisode(link, drama));
        dramaMap.put("total_episodes", episodes.size());

        List<Map<String, Object>> epList = new ArrayList<>();
        for (DramaEpisode ep : episodes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ep.getId());
            item.put("episode", ep.getEpisodeNum());
            item.put("episode_num", ep.getEpisodeNum());
            item.put("title", ep.getTitle() != null ? ep.getTitle() : "");
            item.put("duration", ep.getDuration() != null ? ep.getDuration() : 0);
            epList.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("drama", dramaMap);
        data.put("episodes", epList);
        data.put("free_episodes", resolveFreeEpisodes(link, drama));
        data.put("plans", resolvePlans(link.getPlanGroupId()));
        data.put("user", basicUserMap(user));
        return data;
    }

    @Transactional
    public Map<String, Object> getPlayInfo(String promoId, int episodeNum, String deviceId) {
        if (episodeNum <= 0) {
            throw new BusinessException(400, "episode 必须大于 0");
        }
        PromotionLink link = requirePromotion(promoId);
        User user = ensureAnonymousUser(deviceId, link);
        Drama drama = requireDrama(link.getDramaId());
        List<DramaEpisode> episodes = loadEpisodes(drama.getId());
        DramaEpisode target = episodes.stream()
                .filter(x -> x.getEpisodeNum() != null && x.getEpisodeNum() == episodeNum)
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "剧集不存在"));

        int freeEpisodes = resolveFreeEpisodes(link, drama);
        int beansPerEpisode = Math.max(resolveBeansPerEpisode(link, drama), 0);
        UserWatchHistory history = userWatchHistoryMapper.selectLatestByUserDrama(user.getId(), drama.getId());
        int highestUnlocked = Math.max(freeEpisodes, history != null && history.getEpisodeNum() != null ? history.getEpisodeNum() : 0);

        if (episodeNum > highestUnlocked && beansPerEpisode > 0) {
            int needUnlockCount = episodeNum - highestUnlocked;
            int requiredBeans = needUnlockCount * beansPerEpisode;
            int balance = nz(user.getCoinBalance());
            if (balance < requiredBeans) {
                throw new BusinessException(402, "金豆不足，无法解锁该集");
            }
            user.setCoinBalance(balance - requiredBeans);
            userMapper.updateById(user);
        }

        upsertWatchHistory(user.getId(), drama.getId(), Math.max(episodeNum, highestUnlocked));

        String resolvedUrl = resolveEpisodePlayUrl(target);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("episode", episodeNum);
        data.put("url", resolvedUrl);
        data.put("video_id", firstNonBlank(target.getVodVideoId(), target.getVideoId()));
        data.put("vod_video_id", target.getVodVideoId() != null ? target.getVodVideoId() : "");
        data.put("vod_status", target.getVodStatus() != null ? target.getVodStatus() : "");
        data.put("remaining_beans", userMapper.selectById(user.getId()).getCoinBalance());
        return data;
    }

    private String resolveEpisodePlayUrl(DramaEpisode episode) {
        String vodVideoId = firstNonBlank(episode.getVodVideoId(), episode.getVideoId());
        if (!vodVideoId.isBlank() && vodService.isConfigured()) {
            try {
                String url = vodService.getPreferredPlayUrl(vodVideoId);
                if (!url.isBlank()) {
                    return url;
                }
            } catch (Exception ignored) {
                // 如果 VOD 临时取地址失败，继续回退到手工配置的 video_url。
            }
        }
        String fallback = episode.getVideoUrl() != null ? episode.getVideoUrl() : "";
        if (!fallback.isBlank()) {
            return fallback;
        }
        throw new BusinessException(404, "当前剧集还没有可播放的视频地址");
    }

    public Map<String, Object> getUserInfo(String deviceId) {
        User user = requireUserByDeviceId(deviceId);
        return basicUserMap(user);
    }

    @Transactional
    public Map<String, Object> pay(String deviceId, Integer planId, String promoId) {
        if (planId == null) {
            throw new BusinessException(400, "plan_id 不能为空");
        }
        PromotionLink link = requirePromotion(promoId);
        User user = ensureAnonymousUser(deviceId, link);
        RechargePlan plan = rechargePlanService.require(planId);

        int addedCoins = nz(plan.getBeanCount()) + nz(plan.getExtraBean());
        user.setCoinBalance(nz(user.getCoinBalance()) + addedCoins);
        userMapper.updateById(user);

        Map<String, Object> rechargeBody = new LinkedHashMap<>();
        rechargeBody.put("user_id", displayUserId(user));
        rechargeBody.put("drama_id", link.getDramaId());
        rechargeBody.put("drama_name", nullToEmpty(link.getDramaName()));
        rechargeBody.put("amount", plan.getAmount() != null ? plan.getAmount() : BigDecimal.ZERO);
        rechargeBody.put("coins", addedCoins);
        rechargeBody.put("payment_status", "paid");
        rechargeBody.put("pay_status", "paid");
        rechargeBody.put("promotion_id", link.getPromoteId());
        rechargeBody.put("promote_id", link.getPromoteId());
        rechargeBody.put("new_user_id", deviceId);
        rechargeBody.put("country", nullToEmpty(user.getCountry()));
        rechargeBody.put("payment_method", "stripe");
        rechargeBody.put("platform", nullToEmpty(link.getPlatform()));
        Map<String, Object> order = rechargeService.create(rechargeBody);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order", order);
        data.put("added_coins", addedCoins);
        data.put("beans", user.getCoinBalance());
        data.put("client_secret", "mock_client_secret");
        return data;
    }

    private PromotionLink requirePromotion(String promoId) {
        if (promoId == null || promoId.isBlank()) {
            throw new BusinessException(400, "promo_id 不能为空");
        }
        PromotionLink link = promotionLinkMapper.selectByPromoteId(promoId.trim());
        if (link == null) {
            throw new BusinessException(404, "推广链接不存在");
        }
        return link;
    }

    private Drama requireDrama(Integer dramaId) {
        if (dramaId == null) {
            throw new BusinessException(400, "推广链接未绑定短剧");
        }
        Drama drama = dramaMapper.selectById(dramaId);
        if (drama == null) {
            throw new BusinessException(404, "短剧不存在");
        }
        return drama;
    }

    private User requireUserByDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException(400, "缺少设备标识");
        }
        User user = userMapper.selectByDeviceId(deviceId.trim());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private User ensureAnonymousUser(String deviceId, PromotionLink link) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException(400, "缺少设备标识");
        }
        User existing = userMapper.selectByDeviceId(deviceId.trim());
        if (existing != null) {
            boolean changed = false;
            if ((existing.getPromoteId() == null || existing.getPromoteId().isBlank())
                    && link.getPromoteId() != null && !link.getPromoteId().isBlank()) {
                existing.setPromoteId(link.getPromoteId());
                changed = true;
            }
            if ((existing.getPromoteName() == null || existing.getPromoteName().isBlank())
                    && link.getPromoteName() != null && !link.getPromoteName().isBlank()) {
                existing.setPromoteName(link.getPromoteName());
                changed = true;
            }
            if (changed) {
                userMapper.updateById(existing);
            }
            return existing;
        }

        User user = new User();
        user.setUserCode(userMapper.selectNextUserCode());
        user.setUsername("guest_" + shortId(deviceId));
        user.setStatus(1);
        user.setPromoteId(link.getPromoteId());
        user.setPromoteName(link.getPromoteName());
        user.setCoinBalance(0);
        user.setDeviceId(deviceId.trim());
        user.setNewUserId(deviceId.trim());
        userMapper.insert(user);
        return Objects.requireNonNullElseGet(userMapper.selectById(user.getId()), () -> user);
    }

    private void upsertWatchHistory(Integer userPk, Integer dramaId, int episodeNum) {
        UserWatchHistory row = userWatchHistoryMapper.selectLatestByUserDrama(userPk, dramaId);
        if (row == null) {
            row = new UserWatchHistory();
            row.setUserPk(userPk);
            row.setDramaId(dramaId);
            row.setEpisodeNum(episodeNum);
            row.setProgressSec(0);
            userWatchHistoryMapper.insert(row);
            return;
        }
        row.setEpisodeNum(Math.max(nz(row.getEpisodeNum()), episodeNum));
        row.setProgressSec(0);
        userWatchHistoryMapper.update(row);
    }

    private List<DramaEpisode> loadEpisodes(Integer dramaId) {
        List<DramaEpisode> episodes = new ArrayList<>(dramaEpisodeMapper.selectByDramaId(dramaId));
        episodes.sort(Comparator.comparing(x -> x.getEpisodeNum() != null ? x.getEpisodeNum() : 0));
        return episodes;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolvePlans(Integer planGroupId) {
        List<Map<String, Object>> groups = rechargePlanGroupService.frontendGroups();
        Map<String, Object> targetGroup = null;
        if (planGroupId != null) {
            for (Map<String, Object> group : groups) {
                if (toInt(group.get("id")) == planGroupId) {
                    targetGroup = group;
                    break;
                }
            }
        }
        if (targetGroup == null && !groups.isEmpty()) {
            targetGroup = groups.get(0);
        }
        List<Map<String, Object>> rawPlans =
                targetGroup != null && targetGroup.get("plans") instanceof List
                        ? (List<Map<String, Object>>) targetGroup.get("plans")
                        : List.of();

        List<Map<String, Object>> plans = new ArrayList<>();
        for (Map<String, Object> raw : rawPlans) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", raw.get("id"));
            item.put("name", raw.get("name"));
            item.put("price", raw.get("amount"));
            item.put("amount", raw.get("amount"));
            item.put("currency", raw.get("currency"));
            item.put("beans", toInt(raw.get("bean_count")) + toInt(raw.get("extra_bean")));
            item.put("bean_count", raw.get("bean_count"));
            item.put("extra_bean", raw.get("extra_bean"));
            item.put("description", raw.get("description"));
            item.put("unlock_full_series", raw.get("unlock_full_series"));
            plans.add(item);
        }
        return plans;
    }

    private Map<String, Object> basicUserMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("user_id", displayUserId(user));
        m.put("beans", nz(user.getCoinBalance()));
        m.put("device_id", nullToEmpty(user.getDeviceId()));
        m.put("promotion_id", nullToEmpty(user.getPromoteId()));
        return m;
    }

    private String displayUserId(User user) {
        if (user.getUserCode() != null && !user.getUserCode().isBlank()) {
            return user.getUserCode();
        }
        return user.getId() != null ? String.format("%08d", user.getId()) : "";
    }

    private int resolveFreeEpisodes(PromotionLink link, Drama drama) {
        return link.getFreeEpisodes() != null ? link.getFreeEpisodes() : nz(drama.getFreeEpisodes());
    }

    private int resolveBeansPerEpisode(PromotionLink link, Drama drama) {
        return link.getBeanCount() != null ? link.getBeanCount() : nz(drama.getBeansPerEpisode());
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }

    private static int toInt(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String shortId(String s) {
        String t = s != null ? s.trim() : "";
        if (t.length() <= 6) {
            return t;
        }
        return t.substring(t.length() - 6);
    }
}
