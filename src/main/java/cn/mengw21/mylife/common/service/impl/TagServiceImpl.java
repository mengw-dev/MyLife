package cn.mengw21.mylife.common.service.impl;

import cn.mengw21.mylife.common.service.TagService;
import cn.mengw21.mylife.module.tag.entity.Tag;
import cn.mengw21.mylife.module.tag.mapper.TagMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 * <p>
 * 批量查询已有标签，只对缺失的名称插入新行，避免逐个 upsert 造成的重复查询。
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public Map<String, Tag> loadOrCreate(Long userId, List<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyMap();
        }

        // 归一化：trim、去空、去重（保持传入顺序）
        List<String> normalized = names.stream()
            .map(String::trim)
            .filter(name -> !name.isEmpty())
            .distinct()
            .toList();
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }

        // 已存在的按名称映射
        Map<String, Tag> existing = tagMapper.selectList(
                Wrappers.<Tag>lambdaQuery()
                    .eq(Tag::getUserId, userId)
                    .in(Tag::getName, normalized)
            ).stream()
            .collect(Collectors.toMap(Tag::getName, Function.identity()));

        // 缺失的名称批量创建
        Map<String, Tag> result = new LinkedHashMap<>();
        for (String name : normalized) {
            Tag tag = existing.get(name);
            if (tag == null) {
                tag = new Tag();
                tag.setUserId(userId);
                tag.setName(name);
                tagMapper.insert(tag);
            }
            result.put(name, tag);
        }
        return result;
    }
}