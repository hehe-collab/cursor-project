package com.drama.service;

import com.drama.config.CacheConfig;
import com.drama.dto.UserQueryParam;
import com.drama.dto.UserStatsRow;
import com.drama.entity.User;
import com.drama.exception.BusinessException;
import com.drama.mapper.UserMapper;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String USERNAME_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;

    /**
     * 用户统计（带缓存，1分钟过期）
     * 避免频繁查询大表
     */
    @Cacheable(value = CacheConfig.CACHE_USER_STATS, key = "#q.hashCode()", unless = "#result == null")
    public Map<String, Object> stats(UserQueryParam q) {
        UserStatsRow row =
                userMapper.selectStatsAggregate(
                        q.getUserId(),
                        q.getUsername(),
                        q.getToken(),
                        q.getPromotionId(),
                        q.getCountry(),
                        q.getStartDate(),
                        q.getEndDate());
        Map<String, Object> m = new LinkedHashMap<>();
        long total = nzLong(row != null ? row.getTotalCount() : null);
        m.put("total_count", total);
        m.put("active_count", nzLong(row != null ? row.getActiveCount() : null));
        m.put("inactive_count", nzLong(row != null ? row.getInactiveCount() : null));
        m.put("total_coins", row != null && row.getTotalCoins() != null ? row.getTotalCoins() : 0L);
        m.put("total", total);
        m.put("vip_count", 0L);
        return m;
    }

    public Map<String, Object> list(UserQueryParam q) {
        int page = Math.max(1, q.getPage());
        int pageSize = Math.min(100, Math.max(1, q.getPageSize()));
        int offset = (page - 1) * pageSize;
        long total =
                userMapper.countByParam(
                        q.getUserId(),
                        q.getUsername(),
                        q.getToken(),
                        q.getPromotionId(),
                        q.getCountry(),
                        q.getStartDate(),
                        q.getEndDate());
        List<User> rows =
                userMapper.selectByParam(
                        q.getUserId(),
                        q.getUsername(),
                        q.getToken(),
                        q.getPromotionId(),
                        q.getCountry(),
                        q.getStartDate(),
                        q.getEndDate(),
                        offset,
                        pageSize);
        List<Map<String, Object>> list = rows.stream().map(this::toApiUser).collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", list);
        return data;
    }

    public Map<String, Object> getById(int id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException(404, "用户不存在");
        }
        Map<String, Object> data = toApiUser(u);
        data.put("watch_history", List.of());
        data.put("favorites", List.of());
        return data;
    }

    @CacheEvict(value = CacheConfig.CACHE_USER_STATS, allEntries = true)
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        User u = new User();
        u.setUserCode(userMapper.selectNextUserCode());
        String un = str(body.get("username")).trim();
        if (un.isBlank()) {
            u.setUsername(randomUsername(6));
        } else {
            u.setUsername(un);
        }
        u.setPhone(nullIfBlank(str(body.get("phone"))));
        u.setAvatar(nullIfBlank(str(body.get("avatar"))));
        u.setStatus(userStatusOrDefault(body.get("status")));
        u.setPromoteId(nullIfBlank(str(body.get("promotion_id"))));
        u.setPromoteName(nullIfBlank(str(body.get("promote_name"))));
        u.setCoinBalance(coinsFromBody(body));
        u.setToken(nullIfBlank(str(body.get("token"))));
        u.setCountry(nullIfBlank(str(body.get("country"))));
        u.setNewUserId(nullIfBlank(str(body.get("new_user_id"))));
        userMapper.insert(u);
        User saved = userMapper.selectById(u.getId());
        if (saved == null) {
            throw new BusinessException(500, "创建失败");
        }
        return toApiUser(saved);
    }

    @CacheEvict(value = CacheConfig.CACHE_USER_STATS, allEntries = true)
    @Transactional
    public void update(int id, Map<String, Object> body) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (body.containsKey("username")) {
            existing.setUsername(str(body.get("username")));
        }
        if (body.containsKey("phone")) {
            existing.setPhone(nullIfBlank(str(body.get("phone"))));
        }
        if (body.containsKey("avatar")) {
            existing.setAvatar(nullIfBlank(str(body.get("avatar"))));
        }
        if (body.containsKey("status")) {
            existing.setStatus(userStatusOrDefault(body.get("status")));
        }
        if (body.containsKey("promotion_id")) {
            existing.setPromoteId(nullIfBlank(str(body.get("promotion_id"))));
        }
        if (body.containsKey("promote_name")) {
            existing.setPromoteName(nullIfBlank(str(body.get("promote_name"))));
        }
        if (body.containsKey("coins") || body.containsKey("coin_balance")) {
            existing.setCoinBalance(coinsFromBody(body));
        }
        if (body.containsKey("token")) {
            existing.setToken(nullIfBlank(str(body.get("token"))));
        }
        if (body.containsKey("country")) {
            existing.setCountry(nullIfBlank(str(body.get("country"))));
        }
        if (body.containsKey("new_user_id")) {
            existing.setNewUserId(nullIfBlank(str(body.get("new_user_id"))));
        }
        userMapper.updateById(existing);
    }

    @CacheEvict(value = CacheConfig.CACHE_USER_STATS, allEntries = true)
    @Transactional
    public void delete(int id) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "用户不存在");
        }
        userMapper.deleteById(id);
    }

    private Map<String, Object> toApiUser(User row) {
        Map<String, Object> m = new LinkedHashMap<>();
        int coins = row.getCoinBalance() != null ? row.getCoinBalance() : 0;
        m.put("id", row.getId());
        String displayUserId =
                row.getUserCode() != null && !row.getUserCode().isBlank()
                        ? row.getUserCode()
                        : (row.getId() != null ? String.format("%08d", row.getId()) : "");
        m.put("user_id", displayUserId);
        m.put("username", row.getUsername() != null ? row.getUsername() : "");
        m.put("token", row.getToken() != null ? row.getToken() : "");
        m.put("promotion_id", row.getPromoteId() != null ? row.getPromoteId() : "");
        m.put("country", row.getCountry() != null ? row.getCountry() : "");
        m.put("register_time", row.getCreatedAt() != null ? DT.format(row.getCreatedAt()) : "");
        m.put("created_at", row.getCreatedAt() != null ? DT.format(row.getCreatedAt()) : "");
        m.put("phone", row.getPhone() != null ? row.getPhone() : "");
        m.put("coins", coins);
        m.put("beans", coins);
        m.put("new_user_id", row.getNewUserId() != null ? row.getNewUserId() : "");
        m.put("avatar", row.getAvatar() != null ? row.getAvatar() : "");
        m.put("status", row.getStatus() != null ? row.getStatus() : 1);
        m.put("promote_name", row.getPromoteName() != null ? row.getPromoteName() : "");
        return m;
    }

    private static int coinsFromBody(Map<String, Object> body) {
        if (body.containsKey("coins")) {
            return intOrZero(body.get("coins"));
        }
        if (body.containsKey("coin_balance")) {
            return intOrZero(body.get("coin_balance"));
        }
        return 0;
    }

    private static int userStatusOrDefault(Object o) {
        if (o == null) {
            return 1;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        String s = str(o).trim().toLowerCase();
        if ("active".equals(s) || "1".equals(s) || "正常".equals(s)) {
            return 1;
        }
        if ("banned".equals(s) || "inactive".equals(s) || "0".equals(s) || "禁用".equals(s)) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static long nzLong(Long v) {
        return v != null ? v : 0L;
    }

    private static String str(Object o) {
        return o == null ? "" : Objects.toString(o, "");
    }

    private static String nullIfBlank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static int intOrZero(Object o) {
        if (o == null || str(o).isBlank()) {
            return 0;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt(o.toString().trim());
    }

    private static String randomUsername(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(USERNAME_CHARS.charAt(RANDOM.nextInt(USERNAME_CHARS.length())));
        }
        return sb.toString();
    }
}
