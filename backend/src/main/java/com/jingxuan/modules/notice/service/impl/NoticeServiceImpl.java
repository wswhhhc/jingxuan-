package com.jingxuan.modules.notice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysNotice;
import com.jingxuan.entity.SysUser;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.SysNoticeMapper;
import com.jingxuan.modules.notice.dto.NoticeRequest;
import com.jingxuan.modules.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 公告 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements NoticeService {

    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNotice(NoticeRequest request, Long publisherId) {
        SysNotice notice = new SysNotice();
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setTopFlag(request.getTopFlag());
        notice.setStatus(request.getStatus());
        notice.setPublisherId(publisherId);
        // 如果状态为已发布，同时设置发布时间
        if (request.getStatus() != null && request.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        baseMapper.insert(notice);
        return notice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(Long id, NoticeRequest request) {
        SysNotice notice = new SysNotice();
        notice.setId(id);
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setTopFlag(request.getTopFlag());
        notice.setStatus(request.getStatus());
        // 如果置为已发布，设置发布时间
        if (request.getStatus() != null && request.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        baseMapper.updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(Long id) {
        SysNotice notice = new SysNotice();
        notice.setId(id);
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        baseMapper.updateById(notice);
    }

    @Override
    public PageResult<SysNotice> queryNoticeList(int pageNum, int pageSize, Integer status) {
        Page<SysNotice> page = new Page<>(pageNum, pageSize);
        Page<SysNotice> result = baseMapper.selectPage(page,
                Wrappers.<SysNotice>lambdaQuery()
                        .eq(status != null, SysNotice::getStatus, status)
                        .orderByDesc(SysNotice::getTopFlag)
                        .orderByDesc(SysNotice::getCreateTime));
        enrichPublisherNames(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @Override
    public PageResult<SysNotice> getPublishedNotices(int pageNum, int pageSize) {
        Page<SysNotice> page = new Page<>(pageNum, pageSize);
        Page<SysNotice> result = baseMapper.selectPage(page,
                Wrappers.<SysNotice>lambdaQuery()
                        .eq(SysNotice::getStatus, 1)
                        .orderByDesc(SysNotice::getTopFlag)
                        .orderByDesc(SysNotice::getPublishTime));
        enrichPublisherNames(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    private void enrichPublisherNames(List<SysNotice> notices) {
        List<Long> publisherIds = notices.stream()
                .map(SysNotice::getPublisherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (publisherIds.isEmpty()) {
            return;
        }

        Map<Long, String> userNameMap = sysUserMapper.selectBatchIds(publisherIds).stream()
                .collect(Collectors.toMap(
                        SysUser::getId,
                        user -> {
                            if (user.getRealName() != null && !user.getRealName().isBlank()) {
                                return user.getRealName();
                            }
                            return user.getUsername();
                        },
                        (left, right) -> left
                ));

        notices.forEach(notice -> notice.setPublisherName(userNameMap.get(notice.getPublisherId())));
    }
}
